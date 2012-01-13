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
package com.sn.solr.utils.rank;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.MapFieldSelector;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.search.DocIterator;
import org.apache.solr.search.DocList;
import org.apache.solr.search.SolrIndexSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sn.solr.utils.common.AppHelper;
import com.sn.solr.utils.common.Pair;

/**
 * <code>RankEngine</code> provides implementation for various ranking strategies
 * defined in {@link com.sn.solr.utils.rank.RankStrategy}. This class is not 
 * compute intensive, the operations performed by this class is very trivial.
 * 
 * <p>
 * Does not have adefault strategy of its own. Calling class needs to handle a 
 * default strategy. <code>RankEngine</code> is used by
 * {@link com.sn.solr.utils.rank.RankComponent} to handle ranking.
 * 
 * <p>
 * <code>RankEngine</code> provides static implementation for all ranking 
 * strategies. For facet based ranking logic, the wrapper method 
 * {@link #computeFacetBasedRank(List, RankStrategy)}  can be used.
 * 
 * @author Sathiya N Sundararjan
 * @since 0.1.0
 * @see #computeFacetBasedRank(List, RankStrategy)
 * @see #computeOrdinalBasedRank(List, long)
 */
public class RankEngine {
	
	private static final Logger LOG = LoggerFactory.getLogger(RankEngine.class);
	
	/**
	 * Wrapper class that computes rank for the ranking strategies that leverages
	 * facet results to compute the ranks.
	 * 
	 * {@link RankStrategy#ORDINAL}, {@link RankStrategy#LEGACY_DENSE} are not 
	 * computed based on facet results, they work differently.
	 * 
	 * @see #computeOrdinalBasedRank(List, long)
	 * @see #computeLegacyDenseRank(ResponseBuilder, String, String)
	 * 
	 * @param pairList List of {@link Pair} objects that holds the value of rank 
	 * field & respective count.
	 * @param rankStrategy Strategy identified as defined in {@link RankStrategy}
	 * @return
	 */
	public static Map<String, Number> computeFacetBasedRank(List<Pair<String, Number>> pairList, RankStrategy rankStrategy){
		Map<String, Number> rankMap = null;
		LOG.info("Computing rank using strategy: {}", rankStrategy.getDescription());
		switch(rankStrategy){
			case DENSE:
				rankMap = computeDenseRank(pairList);
				break;
			case STANDARD:
				rankMap = computeStandardRank(pairList);
				break;
			case MODIFIED:
				rankMap = computeModifiedRank(pairList);
				break;
			case FRACTIONAL:
				rankMap = computeFractionalRank(pairList);
				break;
			case ORDINAL:
				rankMap = computeDenseRank(pairList);
				break;
		}
		return rankMap;
	}

	/**
	 * Provides implementation for Dense ranking ["1223"] as identified by the
	 * {@link RankStrategy#DENSE}
	 * 
	 * @param pairList List of {@link Pair} objects that holds the value of rank 
	 * field & respective count.
	 */
	public static Map<String, Number> computeDenseRank(List<Pair<String, Number>> pairList) {
		Map<String, Number> rankMap = new HashMap<String, Number>();
		int rank = 1;
		for (Pair<String, Number> p : pairList) {
			rankMap.put(p.getKey(), rank);
			rank++;
		}
		return rankMap;
	}
	
	/**
	 * Provides implementation for Standard competition ranking ["1224"] as 
	 * identified by the {@link RankStrategy#STANDARD}
	 * 
	 * @param pairList List of {@link Pair} objects that holds the value of rank 
	 * field & respective count.
	 */
	public static Map<String, Number> computeStandardRank(List<Pair<String, Number>> pairList) {
		Map<String, Number> rankMap = new HashMap<String, Number>();
		int rank = 1;
		for (Pair<String, Number> p : pairList) {
			rankMap.put(p.getKey(), rank);
			rank = rank + p.getValue().intValue();
		}
		return rankMap;
	}
	
	/**
	 * Provides implementation for Modified competition ranking ["1334"] as 
	 * identified by the {@link RankStrategy#MODIFIED}
	 * 
	 * @param pairList List of {@link Pair} objects that holds the value of rank 
	 * field & respective count.
	 */
	public static Map<String, Number> computeModifiedRank(List<Pair<String, Number>> pairList) {
		Map<String, Number> rankMap = new HashMap<String, Number>();
		int rank = 0;
		for (Pair<String, Number> p : pairList) {
			rank = rank + p.getValue().intValue();
			rankMap.put(p.getKey(), rank);
		}
		return rankMap;
	}
	
	/**
	 * Provides implementation for Fractional ranking ["1 2.5 2.5 4"] as 
	 * identified by the {@link RankStrategy#FRACTIONAL}
	 * 
	 * @param pairList List of {@link Pair} objects that holds the value of rank 
	 * field & respective count.
	 */
	public static Map<String, Number> computeFractionalRank(List<Pair<String, Number>> pairList) {
		Map<String, Number> rankMap = new HashMap<String, Number>();
		Map<String, Number> stdCompRankMap = computeStandardRank(pairList);
		Map<String, Number> modCompRankMap = computeModifiedRank(pairList);
		float rank = 1;
		int stdCompRank = 0, modCompRank = 0;
		for (Pair<String, Number> p : pairList) {
			stdCompRank = stdCompRankMap.get(p.getKey()).intValue();
			modCompRank = modCompRankMap.get(p.getKey()).intValue();
			rank = (stdCompRank + modCompRank) / 2f;
			rankMap.put(p.getKey(), rank);
		}
		return rankMap;
	}
	
	/**
	 * Provides implementation for Ordinal ranking ["1234"] as identified by the
	 * {@link RankStrategy#ORDINAL}. This is the most simplest form of ranking
	 * strategy and it does not use facet results. Clients could simply skip this
	 * & generate it baed on row numbers.
	 * 
	 * @param pairList List of {@link Pair} objects that holds the value of rank 
	 * field & respective count.
	 */
	public static Map<String, Number> computeOrdinalBasedRank(List<Pair<String, Number>> pairList, long start) {
		LOG.info("Computing rank using strategy: {}", RankStrategy.ORDINAL.getDescription());
		Map<String, Number> rankMap = new HashMap<String, Number>();
		long rank = start;
		for (Pair<String, Number> p : pairList) {
			rank++;
			rankMap.put(p.getKey(), rank);
		}
		return rankMap;
	}
	
	/**
	 * Provides implementation for Dense ranking ["1223"] as identified by the
	 * {@link RankStrategy#LEGACY_DENSE} the difference is that this
	 * implementation is computed without using facet results so this will 
	 * noticeably slower than computing rank based on facets
	 * use {@link RankStrategy#DENSE}. Besides this implementation might cause 
	 * lot of cache evictions putting stress on memory. 
	 *
	 * @see #computeDenseRank(List)
	 * 
	 * @param pairList List of {@link Pair} objects that holds the value of rank 
	 * field & respective count.
	 */
	@Deprecated
	public static Map<String, Number> computeLegacyDenseRank(ResponseBuilder rb, String idField, String rankField)
			throws IOException {
		SolrIndexSearcher searcher = rb.req.getSearcher();
		SolrParams params = rb.req.getParams();// .getParams(FacetParams.FACET_FIELD);

		String _start = params.get(CommonParams.START);
		String _rows = params.get(CommonParams.ROWS);
		int start = 0;
		int rows = 10;

		if (_start != null & AppHelper.isInteger(_start))
			start = new Integer(_start);
		if (_rows != null & AppHelper.isInteger(_rows))
			rows = new Integer(_rows);

		LOG.info("Computing rank using strategy: {}", RankStrategy.ORDINAL.getDescription());
		FieldSelector fs = new MapFieldSelector(new String[] { idField, rankField });
		Map<String, Number> rankMap = new HashMap<String, Number>();
		DocList docs = searcher.getDocList(rb.getQuery(), rb.getFilters(), rb.getSortSpec().getSort(), 0, start + rows, 0);
		int denseRank = 1;
		int _CurrScore = 0;
		int _PrevScore = 0;
		int i = 0;
		for (DocIterator it = docs.iterator(); it.hasNext();) {
			Document doc = searcher.doc(it.nextDoc(), fs);
			_CurrScore = new Integer(doc.get(rankField));
			if (i == 0) {
				_PrevScore = _CurrScore;
			}
			if (_PrevScore != _CurrScore) {
				_PrevScore = _CurrScore;
				denseRank++;
			}
			if(i >= start){
				rankMap.put(doc.get(idField), denseRank);
			}
			i++;
		}

		return rankMap;
	}
	
}
