/*
 * Copyright 20011-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sn.solr.utils.comp;

import static com.sn.solr.utils.common.SolrHelper.RESP_EL_TAG;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.AppendedSolrParams;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.handler.component.FacetComponent;
import org.apache.solr.handler.component.ResponseBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sn.solr.utils.common.AppHelper;
import com.sn.solr.utils.common.Pair;
import com.sn.solr.utils.common.SolrHelper;
import com.sn.solr.utils.rank.RankEngine;
import com.sn.solr.utils.rank.RankStrategy;

/**
 * <code>RankComponent</code> that extends Solr Component to provide 
 * a ranking component that handles ranking.
 * 
 * <p>
 * Requires paramter @see {@link #PARAM_RANK_STRATEGY} to be set as part of the request.
 * This parameter determines the ranking strategy. If not present uses default
 * ranking strategy of ORDINAL ranking.
 * 
 * <p>
 * {@link com.sn.solr.utils.rank.RankEngine} provides implementation for
 * number of ranking strategies defined in {@link com.sn.solr.utils.rank.RankStrategy}.
 * Refer to {@link com.sn.solr.utils.rank.RankEngine} for details of different
 * ranking strategy implementations.
 * 
 * <p>
 * It is <b>highly recommended</b> that this component be used by configuring a
 * seperate handler. <code>RankComponent</code> itself doesn't consume lot of hardware
 * resources rather it relies on the Solr's native components to do the heavy
 * lifting.
 * 
 * @author Sathiya N Sundararjan
 * @since 0.1.0
 * @see #prepare(ResponseBuilder)
 * @see #process(ResponseBuilder)
 */
public class RankComponent extends FacetComponent {

	private static final Logger LOG = LoggerFactory.getLogger(RankComponent.class);
	
	// Request Param Identifiers
	public static final String PARAM_ID_FIELD = "sn.id.field";
	
	public static final String PARAM_RANK_STRATEGY = "sn.rank.strategy";
	
	public static final String PARAM_RANK_FIELD = "sn.rank.field";
	
	public static final String PARAM_RANK_FIELD_SORT = "sn.rank.field.sort";
	
	private static final String RANK_TAG = "rank";

	// Request Defaults
	private static final RankStrategy DEFAULT_RANK_STRATEGY = RankStrategy.ORDINAL;
	
	private static final String FIELD_ID = "ID";

	private static final String FIELD_RANK = "SCORE";
	
	private static final SolrQuery.ORDER FIELD_RANK_SORT = SolrQuery.ORDER.asc;

	/**
	 * <p>
	 * Process request parameters & determines the ranking strategy based on
	 * request. Creates invariants that are needed for request processing, this is 
	 * added on top of user request parameters. Appends invariants to existing
	 * request params & call super.prepare(). 
	 * 
	 * @param rb
	 */
	@SuppressWarnings("deprecation")
	@Override
	public void prepare(ResponseBuilder rb) throws IOException {
		SolrParams params = rb.req.getParams();
		RankStrategy rankStrategy = getRankStrategy(params);
		String rankField = getRankField(params);
		
		if (!(rankStrategy.equals(RankStrategy.ORDINAL) || rankStrategy.equals(RankStrategy.LEGACY_DENSE))) {
			SolrQuery invariants = new SolrQuery().setFacet(true).addFacetField(rankField).setFacetLimit(-1);
			AppendedSolrParams appendedParams = new AppendedSolrParams(params, invariants);
			LOG.info("Setting Invariants: {} Appended Params{}", new Object[]{ invariants, appendedParams});
			rb.req.setParams(appendedParams);
		}
		super.prepare(rb);
	}
	
	/**
	 * <p>
	 * Calls super.process() to complete the processing required by parent. 
	 * Constructs new response with results from previous processing that needs
	 * to be returned with additional rank response.
	 * 
	 * <p>
	 * Executes call to compute appropriate ranking based on the strategy 
	 * requested & adds the computed rank values to each document. Finally 
	 * removes the additional response data forced by this component that was 
	 * not intended by user request.
	 * 
	 * @param rb ResponseBuilder
	 */
	@SuppressWarnings("deprecation")
	@Override
	public void process(ResponseBuilder rb) throws IOException {
		//Complete parent component process
		super.process(rb);
		long startTime = System.nanoTime();
		//Prepare Params
		SolrParams params = rb.req.getParams();
		LOG.debug("REQUEST: " + rb.req);
		RankStrategy rankStrategy = getRankStrategy(params);
		String idField = getIdField(params);
		String rankField = getRankField(params);
		SolrQuery.ORDER rankFieldSort = getRankFieldSortOrder(params, rankField); 
		LOG.info("Params Passed - RankStrategy: {} IdField: {} RankField: {} RankSort: {}", new Object[]{ rankStrategy, idField, rankField, rankFieldSort });
		//Construct New Response derived from response from previous chain
		SolrDocumentList docList = SolrHelper.getSolrDocList(rb.req, rb.rsp);
		rb.rsp.add(RESP_EL_TAG, docList);

		//Process ranking
		Map<String, Number> rankMap = null;
		List<Pair<String, Number>> pairList = null;
		if (rankStrategy.equals(RankStrategy.LEGACY_DENSE)) {
			rankMap = RankEngine.computeLegacyDenseRank(rb, idField, rankField);
		} else if (rankStrategy.equals(RankStrategy.ORDINAL)) {
			pairList = SolrHelper.createPairList(docList, idField);
			String _start = rb.req.getParams().get(CommonParams.START);
			int start = 0;
			if (_start != null && AppHelper.isInteger(_start))
				start = new Integer(_start);
			rankMap = RankEngine.computeOrdinalBasedRank(pairList, start);
		} else {
			pairList = SolrHelper.createPairList(SolrHelper.getFacetsByField(rb.rsp, rankField), rankFieldSort);
			rankMap = RankEngine.computeFacetBasedRank(pairList, rankStrategy);
		}
		LOG.debug("RANK MAP: " + rankMap);
		
		//Add computed ranks to response
		for (SolrDocument d : docList) {
			if (rankStrategy.equals(RankStrategy.ORDINAL) || rankStrategy.equals(RankStrategy.LEGACY_DENSE)) {
				d.addField(RANK_TAG, rankMap.get(d.get(idField)).intValue());
			} else {
				d.setField(RANK_TAG, rankMap.get(String.valueOf(d.get(rankField))).intValue());
			}
		}
		LOG.debug("SOLR DOC LIST: " + docList);

		//Finally remove any facet results from response
		if (rb.rsp.getValues() != null) {
			rb.rsp.getValues().remove(SolrHelper.FACET_CNT_TAG);
		}
		rb.rsp.getValues().remove(RESP_EL_TAG);
		LOG.info("SolrUtils - Rank Component Time: {}", AppHelper.getDiffTime(startTime));
	}

	@Override
	public String getDescription() {
		return "Custom Rank Component to generate ranking of results for different ranking strategy based on a score field";
	}

	@Override
	public String getSource() {
		return "$URL:$";
	}

	@Override
	public String getSourceId() {
		return "$Id:$";
	}

	@Override
	public String getVersion() {
		return "$Revision:$";
	}
	
	private static RankStrategy getRankStrategy(SolrParams params){
		RankStrategy rankStrategy = DEFAULT_RANK_STRATEGY;
		String _rankStrategy = params.get(PARAM_RANK_STRATEGY, null);
		if (_rankStrategy != null) {
			rankStrategy = RankStrategy.getByKey(_rankStrategy);
		}
		return rankStrategy;
	}

	private static String getIdField(SolrParams params){
		return params.get(PARAM_ID_FIELD, FIELD_ID);
	}
	
	private static String getRankField(SolrParams params){
		return params.get(PARAM_RANK_FIELD, FIELD_RANK);
	}
	
	private static SolrQuery.ORDER getRankFieldSortOrder(SolrParams params, String rankField){
		SolrQuery.ORDER rankFieldSort = FIELD_RANK_SORT; 
		if(params.get(CommonParams.SORT, rankField+" asc").contains(rankField+" desc")){
			rankFieldSort = SolrQuery.ORDER.desc;
		}
		return rankFieldSort;
	}
}