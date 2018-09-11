package org.pipservices.clients;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Map;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientRequest;
import org.glassfish.jersey.client.ClientResponse;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.pipservices.commons.config.*;
import org.pipservices.components.count.*;
import org.pipservices.commons.data.FilterParams;
import org.pipservices.commons.data.PagingParams;
import org.pipservices.commons.errors.*;
import org.pipservices.components.log.*;
import org.pipservices.connect.HttpConnectionResolver;
import org.pipservices.commons.refer.*;
import org.pipservices.commons.run.*;
import org.pipservices.components.connect.*;
import org.pipservices.commons.convert.JsonConverter;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
//import com.sun.jersey.api.client.*;
//import com.sun.jersey.api.client.config.*;
//import com.sun.jersey.api.json.*;

public class RestClient implements IOpenable, IConfigurable, IReferenceable {

	private final static ConfigParams _defaultConfig = ConfigParams.fromTuples(
			"connection.protocol", "http",
			// "connection.host", "localhost",
			// "connection.port", 3000,
			"connection.request_max_size", 1024 * 1024,
			"connection.connect_timeout", 60000,
			"options.retries", 1,
			"connection.debug", true
	);

	protected HttpConnectionResolver _connectionResolver = new HttpConnectionResolver();
	protected CompositeLogger _logger = new CompositeLogger();
	protected CompositeCounters _counters = new CompositeCounters();
	protected ConfigParams _options = new ConfigParams();
	protected String _route;
	protected int _retries = 1;
	protected String _url;
	protected Client _client;
	protected WebTarget _resource;

	protected RestClient() {
		this(null);
	}

	protected RestClient(String route) {
		_route = route;
	}

	public void setReferences(IReferences references) throws ReferenceException {
		_logger.setReferences(references);
		_counters.setReferences(references);
		_connectionResolver.setReferences(references);
	}

	public void configure(ConfigParams config) throws ConfigException {
		config = config.setDefaults(_defaultConfig);
		_connectionResolver.configure(config);

		_options = _options.override(config.getSection("options"));

		_retries = config.getAsIntegerWithDefault("options.retries", _retries);

		_route = config.getAsStringWithDefault("base_route", _route);
	}

	/**
	 * Does instrumentation of performed business method by counting elapsed time.
	 * 
	 * @param correlationId the unique id to identify distributed transaction
	 * @param name          the name of called business method
	 * @return ITiming instance to be called at the end of execution of the method.
	 */
	protected Timing instrument(String correlationId, String name) {
		_logger.trace(correlationId, "Calling %s method", name);
		return _counters.beginTiming(name + ".call_time");
	}

	public boolean isOpen() {
		return _client != null && _resource != null;
	}

	public void open(String correlationId) throws ApplicationException {
		// Skip if already opened
		if (_resource != null)
			return;

		ConnectionParams connection = _connectionResolver.resolve(correlationId);

		String protocol = connection.getProtocol("http");
		String host = connection.getHost();
		int port = connection.getPort();
		_url = protocol + "://" + host + ":" + port;

		if (_route != null && _route.length() > 0)
			_url += "/" + _route;

		ClientConfig clientConfig = new ClientConfig();
		//clientConfig.getProperties().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
		clientConfig.register(new JacksonFeature());
		_client = ClientBuilder.newClient(clientConfig);

		_resource = _client.target(_url);
		// _resource.property("accept", "application/json");
		// _resource.property("type", MediaType.APPLICATION_JSON);

		_logger.debug(correlationId, "Connected via REST to %s", _url);
	}

	public void close(String correlationId) throws ApplicationException {
		_client.close();
		_client = null;
		_url = null;
		_logger.debug(correlationId, "Disconnected from %s", _url);
	}

	private static String createEntityContent(Object value) throws JsonProcessingException {
		if (value == null)
			return null;
		String result = JsonConverter.toJson(value);
		return result;
	}

	private URI createRequestUri(String route) {
		StringBuilder builder = new StringBuilder(_url);

		if (_route != null && _route.trim().length() > 0) {
			if (_route.charAt(0) != '/')
				builder.append('/');
			builder.append(_route);
		}

		if (route.charAt(0) != '/')
			builder.append('/');
		builder.append(route);

		String uri = builder.toString();

		URI result = UriBuilder.fromUri(uri).build();

		return result;
	}

	private static String constructQueryString(Map<String, String> parameters) throws UnsupportedEncodingException {
		StringBuilder builder = new StringBuilder();

		for (String name : parameters.keySet()) {
			if (builder.length() > 0)
				builder.append('&');
			builder.append(name);
			builder.append('=');
			builder.append(URLEncoder.encode(parameters.get(name), "UTF-8"));
		}

		return builder.toString();
	}

	private String addQueryParameter(String query, String name, String value) {
		int pos = query.indexOf('?');
		String path = pos >= 0 ? query.substring(0, pos) : query;
		String parametars = pos >= 0 ? query.substring(pos) : "";
		return path + "?" + (parametars.equals("") ? "" : "&") + name + "=" + value;
	}

	protected String addCorrelationId(String route, String correlationId) {
		return addQueryParameter(route, "correlation_id", correlationId);
	}

	protected String addFilterParams(String route, FilterParams filter) {
		for (String key : filter.keySet()) {
			route = addQueryParameter(route, key, filter.get(key));
		}
		return route;
	}

	protected String addPagingParams(String route, PagingParams paging) {
		if (paging.getSkip() != null)
			route = addQueryParameter(route, "skip", paging.getSkip().toString());
		if (paging.getTake() != null)
			route = addQueryParameter(route, "take", paging.getTake().toString());
		if (paging.hasTotal())
			route = addQueryParameter(route, "total", paging.getTake().toString());
		return route;
	}

	private ClientResponse executeRequest(String correlationId, ClientRequest request)
			throws ApplicationException, JsonMappingException, JsonParseException, IOException {
		if (_client == null)
			throw new UnsupportedOperationException("REST client is not configured");

		Response response = null;
		int retries = Math.min(1, Math.max(5, _retries));
		while (retries > 0) {
			try {
				if (request.getMethod().equals(HttpMethod.GET))
					response = _client.target(request.getUri()).request(request.getMediaType()).get();
				else if (request.getMethod().equals(HttpMethod.POST))
					response = _client.target(request.getUri()).request(request.getMediaType())
							.post((Entity<?>) request.getEntity());
				else if (request.getMethod().equals(HttpMethod.PUT))
					response = _client.target(request.getUri()).request(request.getMediaType())
							.put((Entity<?>) request.getEntity());
				else if (request.getMethod().equals(HttpMethod.DELETE))
					response = _client.target(request.getUri()).request(request.getMediaType()).delete();
				else
					throw new UnsupportedOperationException("Invalid request type");

				retries = 0;
			} catch (Exception ex) {
				retries--;
				if (retries < 0) {
					throw ex;
				} else {
					_logger.trace(correlationId, "Connection failed to uri '{uri}'. Retrying...");
				}
			}
		}

		if (response == null) {
			throw new ApplicationExceptionFactory().create(ErrorDescriptionFactory.create(
					new UnknownException("Unable to get a result from uri '{uri}' with method '{method}'", "", ""),
					correlationId));
		}

		ClientResponse result = new ClientResponse(request, response);

		if (result.getStatus() >= 400) {
			String responseContent = result.readEntity(String.class);

			ErrorDescription errorObject = null;
			try {
				errorObject = JsonConverter.fromJson(ErrorDescription.class, responseContent);
			} finally {
				if (errorObject == null) {
					errorObject = ErrorDescriptionFactory.create(new UnknownException(correlationId,
							"UNKNOWN_ERROR with result status: '{result.StatusCode}'", responseContent));
				}
			}

			throw new ApplicationExceptionFactory().create(errorObject);
		}

		return result;
	}

	@SuppressWarnings("unchecked")
	protected <T> T execute(String correlationId, String method, String route)
			throws ApplicationException, JsonMappingException, JsonParseException, IOException {
		route = addCorrelationId(route, correlationId);
		URI uri = createRequestUri(route);

		ClientRequest request = new ClientRequest(null);
		request.setMethod(method);
		request.setUri(uri);

		return (T) executeRequest(correlationId, request);
	}

	@SuppressWarnings("unchecked")
	protected <T> T execute(String correlationId, String method, String route, FilterParams filter, PagingParams paging)
			throws ApplicationException, JsonMappingException, JsonParseException, IOException {
		route = addCorrelationId(route, correlationId);
		route = addFilterParams(route, filter);
		route = addPagingParams(route, paging);
		URI uri = createRequestUri(route);

		ClientRequest request = new ClientRequest(null);
		request.setMethod(method);
		request.setUri(uri);

		return (T) executeRequest(correlationId, request);
	}

	@SuppressWarnings("unchecked")
	protected <T> T execute(String correlationId, String method, String route, Object entity)
			throws ApplicationException, JsonMappingException, JsonParseException, IOException {
		route = addCorrelationId(route, correlationId);
		URI uri = createRequestUri(route);

		ClientRequest request = new ClientRequest(null);
		request.setMethod(method);
		request.setUri(uri);
		request.setEntity(entity);

		return (T) executeRequest(correlationId, request);
	}
}
