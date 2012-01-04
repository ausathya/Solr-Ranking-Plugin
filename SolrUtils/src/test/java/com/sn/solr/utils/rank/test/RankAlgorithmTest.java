package com.sn.solr.utils.rank.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sn.solr.utils.common.Pair;
import com.sn.solr.utils.rank.RankType;
import com.sn.solr.utils.rank.data.RankDataProvider;
import com.sn.solr.utils.rank.data.RankDataSet;

public class RankAlgorithmTest {
	
	List<Pair<String, Number>> pairList = new ArrayList<Pair<String, Number>>();
	
	@Test(testName="Method to test multiple Rank Implementations",
			dataProvider="rankTypes", dataProviderClass = RankDataProvider.class)
	@Parameters({"rankTypeKey"})
	public void testRank(String[] rankTypeKey) {
		for(String key : rankTypeKey) {
			RankType rankType = RankType.getByKey(key);
			validateResult(rankType.getRankImpl().computeRank(RankDataSet.getDataSet()), rankType);
		}
	}
	
	public static void validateResult(Map<String, Number> result, RankType rankType){
		for(RankDataSet testData : RankDataSet.values()){
			String key = testData.getRankKey();
			Assert.assertNotNull(key, "No valid rank type found.");
			switch(rankType){
				case DENSE:
					assertResult(result.get(key).intValue(), testData.getDenseResult());
					break;
				case STD_COMP:
					assertResult(result.get(key).intValue(), testData.getStdCompResult());
					break;
				case MOD_COMP:
					assertResult(result.get(key).intValue(), testData.getModCompResult());
					break;
				case FRACTIONAL:
					assertResult(result.get(key).floatValue(), testData.getFractionalResult());
					break;
			}
		}
	}
	
	public static void assertResult(Number key, Number result){
		Assert.assertEquals(key, result, "Failed for Data Set: "+ key);
	}

}
