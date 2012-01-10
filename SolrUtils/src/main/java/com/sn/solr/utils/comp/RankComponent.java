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
 * Requires paramter @see {@link #RANK_REQ_PARAM} to be set as part of the request.
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

	private static final String ID_FIELD = "ID";

	private static final String RANK_FIELD_TAG = "rank";

	private static final String RANK_SRC_FIELD = "SCORE";
	
	/*
	 * RANK_REQ_PARAM = "sn.rank.type". Ex: sn.rank.type=dense
	 */
	public static final String RANK_REQ_PARAM = "sn.rank.type";

	//Default Rank Strategy if no rank strategy is present in the request
	private RankStrategy rankStrategy = RankStrategy.ORDINAL;

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
		super.prepare(rb);
		String reqRankStrategy = rb.req.getParams().get(RANK_REQ_PARAM, "");
		if (reqRankStrategy != null) {
			rankStrategy = RankStrategy.getByKey(reqRankStrategy);
		}
		if (!(rankStrategy.equals(RankStrategy.ORDINAL) || rankStrategy.equals(RankStrategy.LEGACY_DENSE))) {
			SolrQuery rankInvariants = new SolrQuery().setFacet(true).addFacetField(RANK_SRC_FIELD).setFacetLimit(-1);
			rb.req.setParams(new AppendedSolrParams(rb.req.getParams(), rankInvariants));
		}
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
		//Construct New Response derived from response from previous chain
		SolrDocumentList docList = SolrHelper.getSolrDocList(rb.req, rb.rsp);
		rb.rsp.add(RESP_EL_TAG, docList);

		//Process ranking
		Map<String, Number> rankMap = null;
		List<Pair<String, Number>> pairList = null;
		if (rankStrategy.equals(RankStrategy.LEGACY_DENSE)) {
			rankMap = RankEngine.computeLegacyDenseRank(rb, ID_FIELD, RANK_SRC_FIELD);
		} else if (rankStrategy.equals(RankStrategy.ORDINAL)) {
			pairList = SolrHelper.createPairList(docList, ID_FIELD);
			String _start = rb.req.getParams().get(CommonParams.START);
			int start = 0;
			if (_start != null && AppHelper.isInteger(_start))
				start = new Integer(_start);
			rankMap = RankEngine.computeOrdinalBasedRank(pairList, start);
		} else if (!rankStrategy.equals(RankStrategy.ORDINAL)) {
			pairList = SolrHelper.createPairList(SolrHelper.getFacetsByField(rb.rsp, RANK_SRC_FIELD));
			rankMap = RankEngine.computeFacetBasedRank(pairList, rankStrategy);
		}
		
		//Add computed ranks to response
		for (SolrDocument sdoc : docList) {
			if (rankStrategy.equals(RankStrategy.ORDINAL) || rankStrategy.equals(RankStrategy.LEGACY_DENSE)) {
				sdoc.addField(RANK_FIELD_TAG, rankMap.get(sdoc.get(ID_FIELD)));
			} else {
				sdoc.addField(RANK_FIELD_TAG, rankMap.get(sdoc.get(RANK_SRC_FIELD)));
			}
		}

		//Finally remove any facet results from response
		if (rb.rsp.getValues() != null) {
			rb.rsp.getValues().remove(SolrHelper.FACET_CNT_TAG);
		}
		rb.rsp.getValues().remove(RESP_EL_TAG);
		LOG.info("SolrUtils - Rank Component Time: ", AppHelper.getDiffTime(startTime));
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
}