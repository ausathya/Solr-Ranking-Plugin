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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrQueryResponse;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.DocIterator;
import org.apache.solr.search.DocSlice;

/**
 * <code>SolrHelper</code> class provides bunch of methods that help with 
 * parsing & transforming Solr Query Response. Besides this it has other methods 
 * that helps with object transformation between Solr domain objects & this 
 * project domain objects.
 * 
 * <p>
 * Users will not be interacting with this class directly. This class will be 
 * primarily used by Custom Solr Components & Handlers in this project.
 * 
 * @author Sathiya N Sundararjan
 * @since 0.1.0
 */
public class SolrHelper {
	
	public static final String RESP_EL_TAG = "response";
	
	public static final String FACET_CNT_TAG = "facet_counts";

	public static final String FACET_FIELD_TAG = "facet_fields";

	/**
	 * Util method to extract the Facet response from Solr Query Response for a 
	 * given facet field. If not found returns null.
	 * 
	 * @param res {@link SolrQueryResponse}
	 * @param fieldName {@link String} field identifier string
	 */
	@SuppressWarnings("unchecked")
	public static NamedList<Number> getFacetsByField(SolrQueryResponse res, String fieldName) {
		NamedList<Number> list = null;
		try {
			NamedList<Object> respList = res.getValues();
			if (respList != null) {
				NamedList<Object> fc = (NamedList<Object>) respList.get(FACET_CNT_TAG);
				if (fc != null) {
					NamedList<NamedList<Number>> ff = (NamedList<NamedList<Number>>) fc.get(FACET_FIELD_TAG);
					if (ff != null) {
						list = ff.get(fieldName);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}
	
	/**
	 * Constructs {@link SolrDocumentList} from the current {@link SolrQueryRequest}
	 * and {@link SolrQueryResponse}.
	 * 
	 * @param req {@link SolrQueryRequest}
	 * @param res {@link SolrQueryResponse}
	 * @return
	 * @throws CorruptIndexException
	 * @throws IOException
	 */
	public static SolrDocumentList getSolrDocList(SolrQueryRequest req, SolrQueryResponse res) throws CorruptIndexException, IOException{
		DocSlice slice = (DocSlice) res.getValues().get(RESP_EL_TAG);
		Set<String> returnFields = SolrHelper.getReturnFields(req);
		SolrDocumentList docList = new SolrDocumentList();
		for (DocIterator it = slice.iterator(); it.hasNext();) {
			int docId = it.nextDoc();
			Document doc = req.getSearcher().getReader().document(docId);
			SolrDocument sdoc = new SolrDocument();
			for (Object f : doc.getFields()) {
				Fieldable fld = (Fieldable)f;
				String fn = fld.name();
				if (returnFields.contains(fn)) {
					if(!fld.isBinary()){
						sdoc.addField(fn, doc.get(fn));
					} else{
						long value = 0;
						byte[] by = doc.getBinaryValue(fn);
						for (int i = 0; i < by.length; i++) {
						   value = (value << 8) + (by[i] & 0xff);
						}
						sdoc.addField(fn, value);
					}
				}
			}
			docList.add(sdoc);
		}
		docList.setMaxScore(slice.maxScore());
		docList.setNumFound(slice.matches());
		return docList;
	}
	
	/**
	 * Util method to return a list of fields. 
	 * 
	 * @param req {@link SolrQueryRequest}
	 * @return {@link Set} Returns set of {@link String} field names.
	 */
	public static Set<String> getReturnFields(SolrQueryRequest req) {
		Set<String> fields = new HashSet<String>();
		String fl = req.getParams().get(CommonParams.FL);
		System.out.println("FL: " + fl);
		if (fl == null || fl.equals("")) {
			return fields;
		}
		String[] fls = fl.split(",");
		IndexSchema schema = req.getSchema();
		for (String f : fls) {
			if ("*".equals(f)) {
				Map<String, SchemaField> fm = schema.getFields();
				for (String fieldname : fm.keySet()) {
					SchemaField sf = fm.get(fieldname);
					if (sf.stored()) {
						fields.add(fieldname);
					}
				}
			} else {
				fields.add(f);
			}
		}
		return fields;
	}
	
	/**
	 * Provides a utility method to convert Solr {@link NamedList} to 
	 * {@link Pair} list. Each entry K, V from the {@link NamedList} results 
	 * in a new {@link Pair} and added to the {@link List}.
	 * 
	 * @param list {@link NamedList} that needs to be transformed.
	 * @return {@link List} of {@link Pair}
	 */
	public static List<Pair<String, Number>> createPairList(NamedList<Number> list, SolrQuery.ORDER order) {
		List<Pair<String, Number>> pairList = new ArrayList<Pair<String, Number>>();
		if (list != null) {
			for (Map.Entry<String, Number> e : list) {
				pairList.add(new Pair<String, Number>(e.getKey(), e.getValue()));
			}
		}
		if(order.equals(SolrQuery.ORDER.desc)) {
			Collections.reverse(pairList);
		}
		return pairList;
	}

	/**
	 * Method that transforms Solr {@link SolrDocumentList} to 
	 * {@link Pair} list. Each entry K from the {@link SolrDocumentList} results 
	 * in a new {@link Pair} with K as K and 1 as V, subsequently added to the
	 * {@link List}.
	 * 
	 * @param list {@link SolrDocumentList} that needs to be transformed.
	 * @return {@link List} of {@link Pair}
	 */
	public static List<Pair<String, Number>> createPairList(SolrDocumentList list, String fieldName) {
		List<Pair<String, Number>> pairList = new ArrayList<Pair<String, Number>>();
		if (list != null) {
			for (SolrDocument doc : list) {
				pairList.add(new Pair<String, Number>((String) doc.get(fieldName), 1));
			}
		}
		return pairList;
	}
}
