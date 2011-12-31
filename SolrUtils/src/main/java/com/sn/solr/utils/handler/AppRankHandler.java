package com.sn.solr.utils.handler;

import org.apache.log4j.Logger;
import org.apache.solr.handler.StandardRequestHandler;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;

public class AppRankHandler extends StandardRequestHandler {

	private static final Logger LOG = Logger.getLogger(AppRankHandler.class);
	
	

	public void handleRequestBody(SolrQueryRequest request, SolrQueryResponse response) throws Exception {
		super.handleRequestBody(request, response);
		LOG.info("[AppRankHandler] - Completed Processing Request.");
	}
}
