package com.sn.solr.utils.rank;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sn.solr.utils.common.Pair;

public class DenseRank extends AbstractRank {

	
	@Override
	public Map<String, Number> computeRank(List<Pair<String, Number>> pairList) {
		System.out.println(getDescription());
		Map<String, Number> rankMap = new HashMap<String, Number>();
		int denseRank = 1;
		for (Pair<String, Number> pair : pairList) {
			rankMap.put(pair.getKey(), denseRank);
			denseRank++;
		}
		return rankMap;
	}

	@Override
	public String getDescription() {
		return "Dense ranking [1223] ";
	}

	@Override
	public String getKey() {
		return "dense";
	}

}
