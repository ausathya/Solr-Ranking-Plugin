package com.sn.solr.utils.common;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;

public class SolrHelper {
	
	public static final String RESP_EL_TAG = "response";
	
	public static final String FACET_CNT_TAG = "facet_counts";

	public static final String FACET_FIELD_TAG = "facet_fields";

	public static Set<String> getReturnFields(ResponseBuilder rb) {
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
			} else {
				fields.add(fl);
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
	
}
