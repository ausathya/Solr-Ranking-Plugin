package com.sn.solr.utils.rank.data;

import org.testng.annotations.DataProvider;

public class RankDataProvider {

	public static final Object[][] RANK_TYPES = new Object[][] { { new String[]{"dense", "modComp", "stdComp", "fractional" } } };
	
	@DataProvider(name = "rankTypes")
	public static Object[][] rankTypes() {
		return RANK_TYPES;

	}
}
