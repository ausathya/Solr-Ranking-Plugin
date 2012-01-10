package com.sn.solr.utils.handler;

import org.apache.solr.handler.StandardRequestHandler;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppRankHandler extends StandardRequestHandler {

	private static final Logger LOG = LoggerFactory.getLogger(AppRankHandler.class);
	
	public void handleRequestBody(SolrQueryRequest request, SolrQueryResponse response) throws Exception {
		super.handleRequestBody(request, response);
		LOG.info("[AppRankHandler] - Completed Processing Request.");
	}
}
