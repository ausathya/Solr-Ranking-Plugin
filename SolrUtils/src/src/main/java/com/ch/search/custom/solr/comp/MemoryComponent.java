package com.ch.search.custom.solr.comp;

import java.io.IOException;

import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.request.UnInvertedField;
import org.apache.solr.search.SolrIndexSearcher;

public class MemoryComponent extends SearchComponent {
	String[] fieldNames = null;

	@Override
	public void prepare(ResponseBuilder builder) throws IOException {
		System.out.println("Inside Prepare");
		fieldNames = builder.req.getParams().get("_facetFields", "").split(",");
	}

	@Override
	public void process(ResponseBuilder builder) throws IOException {
		System.out.println("START Process");
		long totalMemorySize = 0;
		if (fieldNames != null) {
			SolrIndexSearcher searcher = builder.req.getSearcher();
			for (String fieldName : fieldNames) {
				UnInvertedField field = UnInvertedField.getUnInvertedField(fieldName, searcher);
				totalMemorySize += field.memSize();
				builder.rsp.add(fieldName, field.memSize());
			}
			builder.rsp.add("total", totalMemorySize);
		}
		System.out.println("END Process");
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