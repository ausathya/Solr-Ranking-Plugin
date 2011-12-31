package com.ch.search.custom.solr.comp;

import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Version;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.search.DocIterator;
import org.apache.solr.search.DocList;
import org.apache.solr.search.SolrIndexSearcher;

import com.ch.search.custom.solr.common.FieldDetailsEnum;

public class FacetIdNameComponent extends SearchComponent {
	
	private static final Logger LOG = Logger.getLogger(FacetIdNameComponent.class);
	
	private static final String FACET_CNT_TAG = "facet_counts";
	
	private static final String FACET_FIELD_TAG = "facet_fields";
	
	private static final String FACET_IDNAMES_TAG = "facet_idNames";
	
	private static final QueryParser queryParser;
	
	static {
		queryParser = new QueryParser(Version.LUCENE_35, null, new StandardAnalyzer(Version.LUCENE_35));
	}
	
	String[] idNameFields = null;
	
	@Override
	public void prepare(ResponseBuilder builder) throws IOException {
		idNameFields = builder.req.getParams().get("idNameFields", "").split(",");
	}

	@SuppressWarnings("unchecked")
	@Override
	public void process(ResponseBuilder b)  {
		if(idNameFields.length > 0){
			NamedList<Object> fIdNames = new NamedList<Object>();
			SolrQueryResponse res = b.rsp;
			SolrIndexSearcher searcher = b.req.getSearcher();
			try{
				NamedList<Object> respList = res.getValues();
				if (respList != null) {
					NamedList<Object> fc = (NamedList<Object>) respList.get(FACET_CNT_TAG);
					if (fc != null) {
						NamedList<NamedList<Number>> ff = (NamedList<NamedList<Number>>) fc.get(FACET_FIELD_TAG);
						if (ff != null) {
							//	ff.getName(0)	-->	DEV_ID //	ff.get(idNameFields[0])	-->	{423=858,607=138,3580=4} 
							for(String field : idNameFields){
								NamedList<Number> list =  ff.get(field);
								if(list != null && list.size() > 0){
									FieldDetailsEnum e = FieldDetailsEnum.getEnumByKey(field);
									fIdNames.add(e.getIdField(), constructIdNameList(e, searcher, list));
								}
							}
						}
					}
					fc.add(FACET_IDNAMES_TAG, fIdNames);
				}
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
		LOG.debug("[FacetIdNameComponent] Component Processing completed");
	}
	
	private static NamedList<String> constructIdNameList(FieldDetailsEnum field, SolrIndexSearcher searcher, NamedList<Number> list) throws Exception{
		NamedList<String> idNameList = new NamedList<String>();
		Query q = null;
		String qStr = field.getIdField() + ":";
		FieldSelector fs = field.getFieldSelector();
		String nameField = field.getNameField();
		for (Map.Entry<String, Number> entry : list) {
			q = queryParser.parse(qStr + entry.getKey());
			DocList docs = searcher.getDocList(q, (Query) null, null, 0, 1);
			for (DocIterator it = docs.iterator(); it.hasNext();) {
				Document doc = searcher.doc(it.nextDoc(), fs);
				idNameList.add(entry.getKey(), doc.get(nameField));
			}
		}
		return idNameList;
	}

	@Override
	public String getDescription() {
		return "CustomFacetComponent";
	}

	@Override
	public String getSource() {
		return "";
	}

	@Override
	public String getSourceId() {
		return "";
	}

	@Override
	public String getVersion() {
		return "1.0";
	}
}