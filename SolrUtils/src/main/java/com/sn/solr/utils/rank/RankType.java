package com.sn.solr.utils.rank;

public enum RankType {
	
	STD_COMP ( new StandardCompRank() ),
	MOD_COMP ( new ModifiedCompRank() ),
	DENSE ( new DenseRank() ),
	ORDINAL ( new DenseRank() ),
	FRACTIONAL ( new FractionalRank() ),
	DEFAULT ( new DenseRank() );
	
	private Rank rankImpl;
	
	private RankType(Rank rankImpl) {
		this.rankImpl = rankImpl;
	}
	
	public Rank getRankImpl(){
		return rankImpl;
	}
	
	public static RankType getByKey(String key){
		for(RankType r : values()){
			if(r.getRankImpl().getKey().equals(key)){
				return r;
			}
		}
		return DEFAULT;
	}

}
