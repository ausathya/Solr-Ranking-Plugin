package com.sn.solr.utils.rank;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.MapFieldSelector;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.search.DocIterator;
import org.apache.solr.search.DocList;
import org.apache.solr.search.SolrIndexSearcher;

import com.sn.solr.utils.common.Pair;
import com.sn.solr.utils.common.Utils;

public class RankEngine {
	
	public static Map<String, Number> computeFacetBasedRank(List<Pair<String, Number>> pairList, String rankTypeKey){
		Map<String, Number> rankMap = new HashMap<String, Number>();
		RankType rankType = RankType.getByKey(rankTypeKey);
		switch(rankType){
			case DENSE:
				rankMap = computeDenseRank(pairList);
				break;
			case STD_COMP:
				rankMap = computeStdCompRank(pairList);
				break;
			case MOD_COMP:
				rankMap = computeModCompRank(pairList);
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

	public static Map<String, Number> computeDenseRank(List<Pair<String, Number>> pairList) {
		Map<String, Number> rankMap = new HashMap<String, Number>();
		int rank = 1;
		for (Pair<String, Number> pair : pairList) {
			rankMap.put(pair.getKey(), rank);
			rank++;
		}
		return rankMap;
	}
	
	public static Map<String, Number> computeStdCompRank(List<Pair<String, Number>> pairList) {
		Map<String, Number> rankMap = new HashMap<String, Number>();
		int rank = 1;
		for (Pair<String, Number> pair : pairList) {
			rankMap.put(pair.getKey(), rank);
			rank = rank + pair.getValue().intValue();
		}
		return rankMap;
	}
	
	public static Map<String, Number> computeModCompRank(List<Pair<String, Number>> pairList) {
		Map<String, Number> rankMap = new HashMap<String, Number>();
		int rank = 0;
		for (Pair<String, Number> pair : pairList) {
			rank = rank + pair.getValue().intValue();
			rankMap.put(pair.getKey(), rank);
		}
		return rankMap;
	}
	
	public static Map<String, Number> computeFractionalRank(List<Pair<String, Number>> pairList) {
		Map<String, Number> rankMap = new HashMap<String, Number>();
		Map<String, Number> stdCompRankMap = computeStdCompRank(pairList);
		Map<String, Number> modCompRankMap = computeModCompRank(pairList);
		float rank = 1;
		int stdCompRank = 0, modCompRank = 0;
		for (Pair<String, Number> pair : pairList) {
			stdCompRank = stdCompRankMap.get(pair.getKey()).intValue();
			modCompRank = modCompRankMap.get(pair.getKey()).intValue();
			rank = (stdCompRank + modCompRank) / 2f;
			rankMap.put(pair.getKey(), rank);
		}
		return rankMap;
	}
	
	public static Map<String, Number> computeOrdinalBasedRank(List<Pair<String, Number>> pairList, long start) {
		Map<String, Number> rankMap = new HashMap<String, Number>();
		long rank = start;
		for (Pair<String, Number> pair : pairList) {
			rank++;
			rankMap.put(pair.getKey(), rank);
		}
		return rankMap;
	}
	
	@SuppressWarnings("unchecked")
	public static Map<String, Number> computeLegacyDenseRank(ResponseBuilder rb, String idField, String rankField)
			throws IOException {
		SolrIndexSearcher searcher = rb.req.getSearcher();
		SolrParams params = rb.req.getParams();// .getParams(FacetParams.FACET_FIELD);

		String _start = params.get(CommonParams.START);
		String _rows = params.get(CommonParams.ROWS);
		int start = 0;
		int rows = 10;

		if (_start != null & Utils.isInteger(_start))
			start = new Integer(_start);
		if (_rows != null & Utils.isInteger(_rows))
			rows = new Integer(_rows);

		FieldSelector fs = new MapFieldSelector(new String[] { idField, rankField });
		CircularFifoBuffer buffer = new CircularFifoBuffer(rows);

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
			buffer.add(new Pair<String, Integer>(doc.get(idField), denseRank));
			i++;
		}

		Map<String, Number> rankMap = new HashMap<String, Number>();
		for (Iterator it = buffer.iterator(); it.hasNext();) {
			Pair<String, Number> pair = (Pair<String, Number>) it.next();
			rankMap.put(pair.getKey(), pair.getValue());
		}
		return rankMap;
	}
	
}
