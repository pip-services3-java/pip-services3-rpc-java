package org.pipservices.rest;

import java.io.IOException;

import javax.ws.rs.HttpMethod;

import org.pipservices.commons.data.FilterParams;
import org.pipservices.commons.data.PagingParams;
import org.pipservices.commons.errors.ApplicationException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;


public class CommandableHttpClient extends RestClient {
	
	public CommandableHttpClient(String baseRoute)
    {
        this._route = baseRoute;
    }
	
	public <T> T callCommand(String route, String correlationId, FilterParams filter, PagingParams paging) 
			throws JsonMappingException, JsonParseException, ApplicationException, IOException {
        return execute(correlationId, HttpMethod.POST, route, filter, paging);	
    }
	
	public <T> T callCommand(String route, String correlationId, Object entity) 
			throws JsonMappingException, JsonParseException, ApplicationException, IOException {
        return execute(correlationId, HttpMethod.POST, route, entity);	
    }
	
}
