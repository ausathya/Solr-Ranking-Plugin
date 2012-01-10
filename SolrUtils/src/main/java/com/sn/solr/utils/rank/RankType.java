package com.sn.solr.utils.rank;

public enum RankType {
	
	STD_COMP ( "stdComp", "Standard Competition Ranking." ),
	MOD_COMP ( "modComp", "Modified Competition Ranking." ),
	DENSE ( "dense", "Dense Ranking." ),
	ORDINAL ( "ordinal", "Ordinal Ranking." ),
	FRACTIONAL ( "fractional", "Fractional Ranking." ),
	LEGACY_DENSE ( "legacyDense", "Legacy Dense Ranking, only used for comparison purpose." );
	
	private String key;
	
	private String description;
	
	private RankType(String key, String description) {
		this.key = key;
		this.description = description;
	}
	
	public String getKey() {
		return key;
	}

	public String getDescription() {
		return description;
	}


	public static RankType getByKey(String key){
		for(RankType r : values()){
			if(r.getKey().equals(key)){
				return r;
			}
		}
		return null;
	}

}
