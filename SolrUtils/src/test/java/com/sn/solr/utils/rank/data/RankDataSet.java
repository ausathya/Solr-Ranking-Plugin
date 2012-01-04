package com.sn.solr.utils.rank.data;

import java.util.ArrayList;
import java.util.List;

import com.sn.solr.utils.common.Pair;

public enum RankDataSet {
	
	SET_1("1", 10, 1, 1, 10, (1+10)/2f ),
	SET_2("2", 20, 2, 11, 30, (11+30)/2f ),
	SET_3("7", 10, 3, 31, 40, (31+40)/2f ),
	SET_4("14", 1, 4, 41, 41, (41+41)/2f ),
	SET_5("15", 40, 5, 42, 81, (42+81)/2f ),
	SET_6("16", 5, 6, 82, 86, (82+86)/2f ),
	SET_7("25", 5, 7, 87, 91, (87+91)/2f ),
	SET_8("29", 8, 8, 92, 99, (92+99)/2f ),
	SET_9("31", 10, 9, 100, 109, (100+109)/2f ),
	SET_10("32", 60, 10, 110, 169, (110+169)/2f );
	
	private String rankKey;
	
	private int rankValue;
	
	private int denseResult;
	
	private int stdCompResult;
	
	private int modCompResult;
	
	private float fractionalResult;
	
	private Pair<String, Number> pair;
	
	RankDataSet(String rankKey, int rankValue, int denseResult, int stdCompResult, int modCompResult, float fractionalResult){
		this.rankKey = rankKey;
		this.rankValue = rankValue;
		this.denseResult = denseResult;
		this.stdCompResult = stdCompResult;
		this.modCompResult = modCompResult;
		this.fractionalResult = fractionalResult;
		this.pair = new Pair<String, Number>(rankKey, rankValue);
	}
	
	public String getRankKey(){
		return rankKey;
	}
	
	public int getDenseResult(){
		return denseResult;
	}
	
	public int getRankValue(){
		return rankValue;
	}
	
	public int getStdCompResult(){
		return stdCompResult;
	}
	
	public int getModCompResult(){
		return modCompResult;
	}
	
	public float getFractionalResult(){
		return fractionalResult;
	}
	
	public Pair<String, Number> getPair(){
		return pair;
	}
	
	private static List<Pair<String, Number>> pairList = null;
	
	public static List<Pair<String, Number>> getDataSet(){
		if(pairList == null){
			pairList = new  ArrayList<Pair<String, Number>>();
			for(RankDataSet data : RankDataSet.values()){
				pairList.add(data.getPair());
			}
		}
		return pairList;
	}
	
}
