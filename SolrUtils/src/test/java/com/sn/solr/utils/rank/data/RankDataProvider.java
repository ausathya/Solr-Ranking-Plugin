package com.sn.solr.utils.rank.data;

import org.testng.annotations.DataProvider;

public class RankDataProvider {

	public static final Object[][] RANK_STRATEGIES = new Object[][] { { new String[]{"dense", "modified", "standard", "fractional" } } };
	
	@DataProvider(name = "rankStrategies")
	public static Object[][] rankStrategies() {
		return RANK_STRATEGIES;

	}
}
