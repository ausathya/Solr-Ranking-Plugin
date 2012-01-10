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
package com.sn.solr.utils.common;


/**
 * <code>Pair</code> class to hold immutable key, value pair once it is created.
 *  
 * @author Sathiya N Sundararjan
 * @since 0.1.0
 */
public class Pair<K, V> {
	
	private final K k;
	
	private final V v;
	
	public Pair(K k, V v){
		this.k = k;
		this.v = v;
	}
	
	public K getKey(){
		return k;
	}
	
	public V getValue(){
		return v;
	}
	
	public String toString(){
		return "K: " + k + "	V: " + v;
	}
	
	@SuppressWarnings("unchecked")
	public boolean equals(Object o) {
        if (o instanceof Pair) {
                Pair p = (Pair) o;
                return  (( this.k == p.k || ( this.k != null && p.k != null && this.k.equals(p.k))) &&
                 ( this.v == p.v || ( this.v != null && p.v != null && this.v.equals(p.v))) );
        }

        return false;
    }

}