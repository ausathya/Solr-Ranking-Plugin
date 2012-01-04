package com.sn.solr.utils.comp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.document.MapFieldSelector;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.AppendedSolrParams;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.component.FacetComponent;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.DocIterator;
import org.apache.solr.search.DocList;
import org.apache.solr.search.DocSlice;
import org.apache.solr.search.SolrIndexReader;
import org.apache.solr.search.SolrIndexSearcher;

import com.sn.solr.utils.common.Pair;
import com.sn.solr.utils.rank.RankType;

public class RankComponent extends FacetComponent {
	
	private static final Logger LOG = Logger.getLogger(RankComponent.class);
	
	private static final String RESP_EL_TAG = "response";
	
	private static final String FACET_CNT_TAG = "facet_counts";
	
	private static final String FACET_FIELD_TAG = "facet_fields";
	
	private static final String RANK_FIELD_TAG = "rank";
	
	private static final String RANK_SOURCE_FIELD = "SCORE";
	
	private static final String ID_FIELD = "ID";
	
	private RankType rankType = RankType.DEFAULT;

	@Override
	public void prepare(ResponseBuilder builder) throws IOException {
		SolrParams params = builder.req.getParams();
		SolrQuery rankInvariants = new SolrQuery();
		rankInvariants.setFacet(true);
		rankInvariants.addFacetField(RANK_SOURCE_FIELD);
		rankInvariants.setFacetLimit(-1);
		AppendedSolrParams finalParams = new AppendedSolrParams(params, rankInvariants);
		builder.req.setParams(finalParams);
		super.prepare(builder);
		String _rankType = builder.req.getParams().get("sn.rank.type", ""); 
		if(_rankType != null){
			rankType = RankType.getByKey(_rankType);
		}
	}

	@Override
	public void process(ResponseBuilder rb) throws IOException {
		super.process(rb);
		Set<String> returnFields = getReturnFields(rb);

		DocSlice slice = (DocSlice) rb.rsp.getValues().get(RESP_EL_TAG);
		SolrIndexReader reader = rb.req.getSearcher().getReader();
		SolrDocumentList docList = new SolrDocumentList();
		Map<String, Number> rankMap = new HashMap<String, Number>(); 
		
		List<Pair<String, Number>> pairList = convertPairList(getRankFieldFacets(rb));
		
		if(rankType != RankType.DEFAULT){
			rankMap = rankType.getRankImpl().computeRank(pairList);
		} else {
			rankMap = computeDenseRank(rb);
		}
		
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
			if(rankType != null){
				sdoc.addField(RANK_FIELD_TAG, rankMap.get(doc.get(RANK_SOURCE_FIELD)));
			} else {
				sdoc.addField(RANK_FIELD_TAG, rankMap.get(doc.get(ID_FIELD)));
			}
			docList.add(sdoc);
		}
		docList.setMaxScore(slice.maxScore());
		docList.setNumFound(slice.matches());
		if (rb.rsp.getValues() != null) {
			rb.rsp.getValues().remove(FACET_CNT_TAG);
		}
		rb.rsp.getValues().remove(RESP_EL_TAG);
		rb.rsp.add(RESP_EL_TAG, docList);
	}
	
	@SuppressWarnings("unchecked")
	public NamedList<Number> getRankFieldFacets(ResponseBuilder b) {
		SolrQueryResponse res = b.rsp;
		NamedList<Number> list = new NamedList<Number>();
		try {
			NamedList<Object> respList = res.getValues();
			if (respList != null) {
				NamedList<Object> fc = (NamedList<Object>) respList.get(FACET_CNT_TAG);
				if (fc != null) {
					NamedList<NamedList<Number>> ff = (NamedList<NamedList<Number>>) fc.get(FACET_FIELD_TAG);
					if (ff != null) {
						list = ff.get(RANK_SOURCE_FIELD);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}
	
	@SuppressWarnings("unchecked")
	public List<Pair<String, Number>> convertPairList(NamedList<Number> list) {
		List<Pair<String, Number>> pairList = new ArrayList<Pair<String, Number>>();
		if (list != null) {
			for (Map.Entry<String, Number> e : list) {
				pairList.add(new Pair<String, Number>(e.getKey(), e.getValue()));
			}
		}
		return pairList;
	}
	

	@SuppressWarnings("unchecked")
	public Map<String, Number> computeDenseRank(ResponseBuilder rb) throws IOException {
		SolrIndexSearcher searcher = rb.req.getSearcher();
		SolrParams params = rb.req.getParams();// .getParams(FacetParams.FACET_FIELD);

		String _start = params.get(CommonParams.START);
		String _rows = params.get(CommonParams.ROWS);
		int start = 0;
		int rows = 10;

		if (_start != null & isInteger(_start))
			start = new Integer(_start);
		if (_rows != null & isInteger(_rows))
			rows = new Integer(_rows);

		FieldSelector fs = new MapFieldSelector(new String[] { ID_FIELD, RANK_SOURCE_FIELD });
		CircularFifoBuffer buffer = new CircularFifoBuffer(rows);

		DocList docs = searcher.getDocList(rb.getQuery(), rb.getFilters(), rb
				.getSortSpec().getSort(), 0, start + rows, 0);
		int denseRank = 1;
		int _CurrScore = 0;
		int _PrevScore = 0;
		int i = 0;
		for (DocIterator it = docs.iterator(); it.hasNext();) {
			Document doc = searcher.doc(it.nextDoc(), fs);
			_CurrScore = new Integer(doc.get(RANK_SOURCE_FIELD));
			if (i == 0) {
				_PrevScore = _CurrScore;
			}
			if (_PrevScore != _CurrScore) {
				_PrevScore = _CurrScore;
				denseRank++;
			}
			buffer.add(new Pair<String, Integer>(doc.get(ID_FIELD), denseRank));
			i++;
		}

		Map<String, Number> rankMap = new HashMap<String, Number>();
		for (Iterator it = buffer.iterator(); it.hasNext();) {
			Pair<String, Number> pair = (Pair<String, Number>) it.next();
			rankMap.put(pair.getKey(), pair.getValue());
		}
		return rankMap;
	}

	private Set<String> getReturnFields(ResponseBuilder rb) {
		Set<String> fields = new HashSet<String>();
		String flp = rb.req.getParams().get(CommonParams.FL);
		if (StringUtils.isEmpty(flp)) {
			return fields;
		}
		String[] fls = StringUtils.split(flp, ",");
		IndexSchema schema = rb.req.getSchema();
		for (String fl : fls) {
			if ("*".equals(fl)) {
				Map<String, SchemaField> fm = schema.getFields();
				for (String fieldname : fm.keySet()) {
					SchemaField sf = fm.get(fieldname);
					if (sf.stored() && (!"content".equals(fieldname))) {
						fields.add(fieldname);
					}
				}
			} else if (ID_FIELD.equals(fl)) {
				SchemaField usf = schema.getUniqueKeyField();
				fields.add(usf.getName());
			} else {
				fields.add(fl);
			}
		}
		return fields;
	}

	@Override
	public String getDescription() {
		return "Custom Rank Component to generate dense ranking based on score";
	}

	@Override
	public String getSource() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSourceId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getVersion() {
		// TODO Auto-generated method stub
		return null;
	}

	public static boolean isInteger(String str) {
		if (str == null) {
			return false;
		}
		int length = str.length();
		if (length == 0) {
			return false;
		}
		int i = 0;
		if (str.charAt(0) == '-') {
			if (length == 1) {
				return false;
			}
			i = 1;
		}
		for (; i < length; i++) {
			char c = str.charAt(i);
			if (c <= '/' || c >= ':') {
				return false;
			}
		}
		return true;
	}

}
