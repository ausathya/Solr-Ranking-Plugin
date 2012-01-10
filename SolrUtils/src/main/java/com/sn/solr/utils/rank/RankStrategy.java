package com.sn.solr.utils.rank;

public enum RankStrategy {
	
	STD_COMP ( "standard", "Standard Competition Ranking." ),
	MOD_COMP ( "modified", "Modified Competition Ranking." ),
	DENSE ( "dense", "Dense Ranking." ),
	ORDINAL ( "ordinal", "Ordinal Ranking." ),
	FRACTIONAL ( "fractional", "Fractional Ranking." ),
	LEGACY_DENSE ( "legacyDense", "Legacy Dense Ranking, only used for comparison purpose." );
	
	private String key;
	
	private String description;
	
	private RankStrategy(String key, String description) {
		this.key = key;
		this.description = description;
	}
	
	public String getKey() {
		return key;
	}

	public String getDescription() {
		return description;
	}


	public static RankStrategy getByKey(String key){
		for(RankStrategy r : values()){
			if(r.getKey().equals(key)){
				return r;
			}
		}
		return null;
	}

}
