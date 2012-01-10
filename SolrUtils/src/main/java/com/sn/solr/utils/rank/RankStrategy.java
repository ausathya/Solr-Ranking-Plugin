/*
 * Copyright 20011-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sn.solr.utils.rank;

/**
 * <code>RankStrategy</code> defines set of ranking strategies. Strategy defined
 * here is used by various classes to refer to different ranking strategies.
 * 
 * <p>
 * String paramter retruned by {@link #getKey()} hold the key by which a ranking
 * strategy can be picked.
 * 
 * 
 * @author Sathiya N Sundararjan
 * @since 0.1.0
 * @see #getKey()
 * @see #getByKey(String)
 */
public enum RankStrategy {
	
	/**
	 * Standard competition ranking ["1224"]. 
	 */
	STANDARD ( "standard", "Standard Competition Ranking." ),
	/**
	 * Modified competition ranking ["1334"]. 
	 */
	MODIFIED ( "modified", "Modified Competition Ranking." ),
	/**
	 * Dense ranking ["1223"]. 
	 */
	DENSE ( "dense", "Dense Ranking." ),
	/**
	 * Ordinal ranking ["1234"]
	 */
	ORDINAL ( "ordinal", "Ordinal Ranking." ),
	/**
	 * Fractional ranking ["1 2.5 2.5 4"].
	 */
	FRACTIONAL ( "fractional", "Fractional Ranking." ),
	/** 
	 * Match settings and bugs in Lucene's 2.0 release. 
	 * @deprecated Use {@link #DENSE} instead.
	 */
	LEGACY_DENSE ( "legacyDense", "Legacy Dense Ranking, only used for comparison purpose." );
	
	private String key;
	
	private String description;
	
	private RankStrategy(String key, String description) {
		this.key = key;
		this.description = description;
	}
	
	/**
	 * Holds #{@link String} key of current ranking strategy.  
	 * @see #getByKey(String) to get Strategy by passing key. 
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Provides a description of the current ranking strategys. 
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * <p>
	 * String paramter retruned by {@link #getKey()} hold the key by which a ranking
	 * strategy can be picked with a String.
	 * 
	 * @param String key to get the ranking strategy
	 * @see #getKey()
	 */
	public static RankStrategy getByKey(String key){
		for(RankStrategy r : values()){
			if(r.getKey().equals(key)){
				return r;
			}
		}
		return null;
	}

}
