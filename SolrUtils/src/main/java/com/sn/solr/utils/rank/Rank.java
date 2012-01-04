package com.sn.solr.utils.rank;

import java.util.List;
import java.util.Map;

import com.sn.solr.utils.common.Pair;

public interface Rank {
	
	public String getKey();

	public String getDescription();
	
	public Map<String, Number> computeRank(List<Pair<String, Number>> pairList);

}
