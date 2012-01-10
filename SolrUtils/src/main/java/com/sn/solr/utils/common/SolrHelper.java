package com.sn.solr.utils.common;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.DocIterator;
import org.apache.solr.search.DocSlice;
import org.apache.solr.search.SolrIndexReader;

public class SolrHelper {
	
	public static final String RESP_EL_TAG = "response";
	
	public static final String FACET_CNT_TAG = "facet_counts";

	public static final String FACET_FIELD_TAG = "facet_fields";

	public static Set<String> getReturnFields(SolrQueryRequest req) {
		Set<String> fields = new HashSet<String>();
		String fl = req.getParams().get(CommonParams.FL);
		if (StringUtils.isEmpty(fl)) {
			return fields;
		}
		String[] fls = StringUtils.split(fl, ",");
		IndexSchema schema = req.getSchema();
		for (String f : fls) {
			if ("*".equals(f)) {
				Map<String, SchemaField> fm = schema.getFields();
				for (String fieldname : fm.keySet()) {
					SchemaField sf = fm.get(fieldname);
					if (sf.stored() && (!"content".equals(fieldname))) {
						fields.add(fieldname);
					}
				}
			} else {
				fields.add(f);
			}
		}
		return fields;
	}
	
	@SuppressWarnings("unchecked")
	public static NamedList<Number> getRankFieldFacets(ResponseBuilder b, String fieldName) {
		SolrQueryResponse res = b.rsp;
		NamedList<Number> list = new NamedList<Number>();
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
	
	public static SolrDocumentList getSolrDocList(SolrQueryRequest req, SolrQueryResponse res) throws CorruptIndexException, IOException{
		DocSlice slice = (DocSlice) res.getValues().get(RESP_EL_TAG);
		Set<String> returnFields = SolrHelper.getReturnFields(req);
		SolrIndexReader reader = req.getSearcher().getReader();
		SolrDocumentList docList = new SolrDocumentList();
		for (DocIterator it = slice.iterator(); it.hasNext();) {
			int docId = it.nextDoc();
			Document doc = reader.document(docId);
			SolrDocument sdoc = new SolrDocument();
			for (Fieldable f : doc.getFields()) {
				String fn = f.name();
				if (returnFields.contains(fn)) {
					sdoc.addField(fn, doc.get(fn));
				}
			}
			docList.add(sdoc);
		}
		docList.setMaxScore(slice.maxScore());
		docList.setNumFound(slice.matches());
		return docList;
	}
	
	public static List<Pair<String, Number>> createPairList(NamedList<Number> list) {
		List<Pair<String, Number>> pairList = new ArrayList<Pair<String, Number>>();
		if (list != null) {
			for (Map.Entry<String, Number> e : list) {
				pairList.add(new Pair<String, Number>(e.getKey(), e.getValue()));
			}
		}
		return pairList;
	}

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
