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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.AppendedSolrParams;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.component.FacetComponent;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.search.DocIterator;
import org.apache.solr.search.DocSlice;
import org.apache.solr.search.SolrIndexReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sn.solr.utils.common.Pair;
import com.sn.solr.utils.common.SolrHelper;
import com.sn.solr.utils.common.Utils;
import com.sn.solr.utils.rank.RankEngine;
import com.sn.solr.utils.rank.RankType;

/**
 * Rank Component that extends Solr Component to provide ranking implementation.
 * 
 * <p>
 * Requires paramter @see #HTTP_RANK_PARAM to be set as pasrt of the request.
 * This parameter determines the type of ranking. If not present uses default
 * ranking.
 * 
 * <p>
 * Supports number of ranking strategies supported by the
 * <code>com.sn.solr.utils.rank.RankEngine</code>. Refer to
 * <code>com.sn.solr.utils.rank.RankEngine</code> for details of different
 * ranking strategy implementations.
 * 
 * <p>
 * It is highly recommended that this component be used by configuring a
 * seperate handler. The component itself doesn't consume lot of hardware
 * resources rather it depends on the Solr's native component to do the heavy
 * lifting.
 * 
 * @author Sathiya N Sundararjan
 * @see #setDataSource
 * @see #getJdbcTemplate
 * @see org.springframework.jdbc.core.JdbcTemplate
 */
public class RankComponent extends FacetComponent {

	private static final Logger LOG = LoggerFactory.getLogger(RankComponent.class);

	private static final String ID_FIELD = "ID";

	private static final String RANK_FIELD_TAG = "rank";

	private static final String RANK_SOURCE_FIELD = "SCORE";

	private static final String HTTP_RANK_PARAM = "sn.rank.type";

	private String rankTypeKey = RankType.ORDINAL.getKey();

	@Override
	public void prepare(ResponseBuilder builder) throws IOException {
		SolrQuery rankInvariants = new SolrQuery().setFacet(true).addFacetField(RANK_SOURCE_FIELD).setFacetLimit(-1);
		builder.req.setParams(new AppendedSolrParams(builder.req.getParams(), rankInvariants));
		super.prepare(builder);
		String reqRankTypeKey = builder.req.getParams().get(HTTP_RANK_PARAM, "");
		if (reqRankTypeKey != null) {
			rankTypeKey = reqRankTypeKey;
		}
	}

	@Override
	public void process(ResponseBuilder rb) throws IOException {
		super.process(rb);
		long startTime = System.nanoTime();
		Set<String> returnFields = SolrHelper.getReturnFields(rb);
		DocSlice slice = (DocSlice) rb.rsp.getValues().get(RESP_EL_TAG);
		SolrIndexReader reader = rb.req.getSearcher().getReader();
		SolrDocumentList docList = new SolrDocumentList();
		Map<String, Number> rankMap = new HashMap<String, Number>();

		for (DocIterator it = slice.iterator(); it.hasNext();) {
			int docId = it.nextDoc();
			Document doc = reader.document(docId);
			SolrDocument sdoc = new SolrDocument();
			for (Fieldable f : doc.getFields()) {
				String fn = f.name();
				if (returnFields.contains(fn)) {
					sdoc.addField(fn, doc.get(fn));
				}
			}
			docList.add(sdoc);
		}
		docList.setMaxScore(slice.maxScore());
		docList.setNumFound(slice.matches());
		rb.rsp.add(RESP_EL_TAG, docList);

		List<Pair<String, Number>> pairList = null;

		if (rankTypeKey.equals(RankType.LEGACY_DENSE.getKey())) {
			rankMap = RankEngine.computeLegacyDenseRank(rb, ID_FIELD, RANK_SOURCE_FIELD);
		} else if (rankTypeKey.equals(RankType.ORDINAL.getKey())) {
			pairList = createPairList(docList);
			String _start = rb.req.getParams().get(CommonParams.START);
			int start = 0;
			if (_start != null & Utils.isInteger(_start))
				start = new Integer(_start);
			rankMap = RankEngine.computeOrdinalBasedRank(pairList, start);
		} else if (!rankTypeKey.equals(RankType.ORDINAL.getKey())) {
			pairList = createPairList(SolrHelper.getRankFieldFacets(rb, RANK_SOURCE_FIELD));
			rankMap = RankEngine.computeFacetBasedRank(pairList, rankTypeKey);
		}

		for (SolrDocument sdoc : docList) {
			if (rankTypeKey.equals(RankType.ORDINAL.getKey()) || rankTypeKey.equals(RankType.LEGACY_DENSE.getKey())) {
				sdoc.addField(RANK_FIELD_TAG, rankMap.get(sdoc.get(ID_FIELD)));
			} else {
				sdoc.addField(RANK_FIELD_TAG, rankMap.get(sdoc.get(RANK_SOURCE_FIELD)));
			}
		}

		if (rb.rsp.getValues() != null) {
			rb.rsp.getValues().remove(SolrHelper.FACET_CNT_TAG);
		}
		rb.rsp.getValues().remove(RESP_EL_TAG);
		LOG.info("SolrUtils - Rank Component Time: " + Utils.getDiffTime(startTime));
	}

	public List<Pair<String, Number>> createPairList(NamedList<Number> list) {
		List<Pair<String, Number>> pairList = new ArrayList<Pair<String, Number>>();
		if (list != null) {
			for (Map.Entry<String, Number> e : list) {
				pairList.add(new Pair<String, Number>(e.getKey(), e.getValue()));
			}
		}
		return pairList;
	}

	public List<Pair<String, Number>> createPairList(SolrDocumentList list) {
		List<Pair<String, Number>> pairList = new ArrayList<Pair<String, Number>>();
		if (list != null) {
			for (SolrDocument doc : list) {
				pairList.add(new Pair<String, Number>((String) doc.get(ID_FIELD), 1));
			}
		}
		return pairList;
	}


	@Override
	public String getDescription() {
		return "Custom Rank Component to generate dense ranking based on score";
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
