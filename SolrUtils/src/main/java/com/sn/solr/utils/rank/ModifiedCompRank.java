package com.sn.solr.utils.rank;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sn.solr.utils.common.Pair;

public class ModifiedCompRank extends AbstractRank {

	
	@Override
	public Map<String, Number> computeRank(List<Pair<String, Number>> pairList) {
		System.out.println(getDescription());
		Map<String, Number> rankMap = new HashMap<String, Number>();
		int denseRank = 0;
		for (Pair<String, Number> pair : pairList) {
			denseRank = denseRank + pair.getValue().intValue();
			rankMap.put(pair.getKey(), denseRank);
		}
		return rankMap;
	}

	@Override
	public String getDescription() {
		return "Modified Competition Ranking [1334] ";
	}

	@Override
	public String getKey() {
		return "modComp";
	}

}
