package com.sn.solr.utils.rank;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sn.solr.utils.common.Pair;

public class FractionalRank extends AbstractRank {

	
	@Override
	public Map<String, Number> computeRank(List<Pair<String, Number>> pairList) {
		System.out.println(getDescription());
		Map<String, Number> rankMap = new HashMap<String, Number>();
		Map<String, Number> stdCompRankMap = RankType.STD_COMP.getRankImpl().computeRank(pairList);
		Map<String, Number> modCompRankMap = RankType.MOD_COMP.getRankImpl().computeRank(pairList);
		float denseRank = 1;
		int stdCompRank = 0, modCompRank = 0;
		for (Pair<String, Number> pair : pairList) {
			stdCompRank = stdCompRankMap.get(pair.getKey()).intValue();
			modCompRank = modCompRankMap.get(pair.getKey()).intValue();
			denseRank = (stdCompRank + modCompRank) / 2f;
			rankMap.put(pair.getKey(), denseRank);
		}
		return rankMap;
	}

	@Override
	public String getDescription() {
		return "Fractional Ranking [1,2.5,2.5,4] ";
	}

	@Override
	public String getKey() {
		return "fractional";
	}

}
