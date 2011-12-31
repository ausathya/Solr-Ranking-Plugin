package com.ch.search.custom.solr.comp;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.document.MapFieldSelector;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.DocIterator;
import org.apache.solr.search.DocList;
import org.apache.solr.search.DocSlice;
import org.apache.solr.search.SolrIndexReader;
import org.apache.solr.search.SolrIndexSearcher;

import com.ch.search.custom.solr.common.Pair;

public class RankComponent extends SearchComponent {

	@Override
	public void prepare(ResponseBuilder builder) throws IOException {
		// NOOP
	}

	@Override
	public void process(ResponseBuilder rb) throws IOException {
		Set<String> returnFields = getReturnFields(rb);

		DocSlice slice = (DocSlice) rb.rsp.getValues().get("response");
		SolrIndexReader reader = rb.req.getSearcher().getReader();
		SolrDocumentList rl = new SolrDocumentList();
		Map<String, Integer> rankMap = computeDenseRank(rb);
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
			sdoc.addField("rank", rankMap.get(doc.get("ID")));
			rl.add(sdoc);
		}
		rl.setMaxScore(slice.maxScore());
		rl.setNumFound(slice.matches());
		rb.rsp.getValues().remove("response");
		rb.rsp.add("response", rl);
	}

	public Map<String, Integer> computeDenseRank(ResponseBuilder rb) throws IOException {
		SolrIndexSearcher searcher = rb.req.getSearcher();
		SolrParams params = rb.req.getParams();

		String _start = params.get(CommonParams.START);
		String _rows = params.get(CommonParams.ROWS);
		int start = 0;
		int rows = 10;

		if (_start != null & isInteger(_start))
			start = new Integer(_start);
		if (_rows != null & isInteger(_rows))
			rows = new Integer(_rows);

		FieldSelector fs = new MapFieldSelector(new String[] { "ID", "SCORE" });
		CircularFifoBuffer buffer = new CircularFifoBuffer(rows);

		DocList docs = searcher.getDocList(rb.getQuery(), rb.getFilters(), rb.getSortSpec().getSort(), 0, start + rows, 0);
		int denseRank = 1;
		int _CurrScore = 0;
		int _PrevScore = 0;
		int i = 0;
		for (DocIterator it = docs.iterator(); it.hasNext();) {
			Document doc = searcher.doc(it.nextDoc(), fs);
			_CurrScore = new Integer(doc.get("SCORE"));
			if (i == 0) {
				_PrevScore = _CurrScore;
			}
			if (_PrevScore != _CurrScore) {
				_PrevScore = _CurrScore;
				denseRank++;
			}
			buffer.add(new Pair<String, Integer>(doc.get("ID"), denseRank));
			i++;
		}

		Map<String, Integer> rankMap = new HashMap<String, Integer>();
		for (Iterator it = buffer.iterator(); it.hasNext();) {
			Pair<String, Integer> pair = (Pair<String, Integer>) it.next();
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
			} else if ("id".equals(fl)) {
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
