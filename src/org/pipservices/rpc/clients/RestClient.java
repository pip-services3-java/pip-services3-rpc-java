package org.pipservices.rpc.clients;

import java.io.*;
import java.net.*;

import javax.ws.rs.*;
import javax.ws.rs.client.*;
import javax.ws.rs.core.*;

import org.glassfish.jersey.client.*;
import org.glassfish.jersey.jackson.*;

import org.pipservices.commons.config.*;
import org.pipservices.components.count.*;
import org.pipservices.commons.data.*;
import org.pipservices.commons.errors.*;
import org.pipservices.components.log.*;
import org.pipservices.rpc.connect.*;
import org.pipservices.commons.refer.*;
import org.pipservices.commons.run.*;
import org.pipservices.components.connect.*;

/**
 * Abstract client that calls remove endpoints using HTTP/REST protocol.
 * <p>
 * ### Configuration parameters ###
 * <ul>
 * <li>base_route:              base route for remote URI
 * <li>connection(s):           
 *   <ul>
 *   <li>discovery_key:         (optional) a key to retrieve the connection from <a href="https://raw.githubusercontent.com/pip-services-java/pip-services-components-java/master/doc/api/org/pipservices/components/connect/IDiscovery.html">IDiscovery</a>
 *   <li>protocol:              connection protocol: http or https
 *   <li>host:                  host name or IP address
 *   <li>port:                  port number
 *   <li>uri:                   resource URI or connection string with all parameters in it
 *   </ul>
 * <li>options:
 *   <ul>
 *   <li>retries:               number of retries (default: 3)
 *   <li>connect_timeout:       connection timeout in milliseconds (default: 10 sec)
 *   <li>timeout:               invocation timeout in milliseconds (default: 10 sec)
 *   </ul>
 * </ul>
 * <p>
 * ### References ###
 * <ul>
 * <li>*:logger:*:*:1.0         (optional) <a href="https://raw.githubusercontent.com/pip-services-java/pip-services-components-java/master/doc/api/org/pipservices/components/log/ILogger.html">ILogger</a> components to pass log messages
 * <li>*:counters:*:*:1.0         (optional) <a href="https://raw.githubusercontent.com/pip-services-java/pip-services-components-java/master/doc/api/org/pipservices/components/count/ICounters.html">ICounters</a> components to pass collected measurements
 * <li>*:discovery:*:*:1.0        (optional) <a href="https://raw.githubusercontent.com/pip-services-java/pip-services-components-java/master/doc/api/org/pipservices/components/connect/IDiscovery.html">IDiscovery</a> services to resolve connection
 * </ul>
 * <p>
 * ### Example ###
 * <pre>
 * {@code
 * class MyRestClient extends RestClient implements IMyClient {
 *    ...
 * 
 *    public MyData getData(String correlationId, String id) {
 *        Timing timing = this.instrument(correlationId, 'myclient.get_data');
 *        MyData result = this.execute(MyData.class, correlationId, HttpMethod.POST, "/get_data", new MyData(id));
 *        timing.endTiming();
 *        return result;        
 *    }
 *    ...
 * }
 * 
 * MyRestClient client = new MyRestClient();
 * client.configure(ConfigParams.fromTuples(
 *     "connection.protocol", "http",
 *     "connection.host", "localhost",
 *     "connection.port", 8080
 * ));
 * 
 * MyData data = client.getData("123", "1");
 * ...
 * }
 * </pre>
 */
public class RestClient implements IOpenable, IConfigurable, IReferenceable {

	private final static ConfigParams _defaultConfig = ConfigParams.fromTuples("connection.protocol", "http",
			// "connection.host", "localhost",
			// "connection.port", 3000,
			"connection.request_max_size", 1024 * 1024, "connection.connect_timeout", 60000, "options.retries", 1,
			"connection.debug", true);

	/**
	 * The connection resolver.
	 */
	protected HttpConnectionResolver _connectionResolver = new HttpConnectionResolver();
	/**
	 * The logger.
	 */
	protected CompositeLogger _logger = new CompositeLogger();
	/**
	 * The performance counters.
	 */
	protected CompositeCounters _counters = new CompositeCounters();
	/**
	 * The configuration options.
	 */
	protected ConfigParams _options = new ConfigParams();
	/**
	 * The base route.
	 */
	protected String _baseRoute;
	/**
	 * The number of retries.
	 */
	protected int _retries = 1;
	/**
	 * The remote service uri which is calculated on open.
	 */
	protected String _url;
	/**
	 * The HTTP client.
	 */
	protected Client _client;

	/**
	 * Creates a new instance of the client.
	 */
	protected RestClient() {
		this(null);
	}

	/**
	 * Creates a new instance of the client.
	 * 
	 * @param baseRoute a base route for remote service.
	 */
	protected RestClient(String baseRoute) {
		_baseRoute = baseRoute;
	}

	/**
	 * Configures component by passing configuration parameters.
	 * 
	 * @param config configuration parameters to be set.
	 * @throws ConfigException when configuration is wrong.
	 */
	public void configure(ConfigParams config) throws ConfigException {
		config = config.setDefaults(_defaultConfig);
		_connectionResolver.configure(config);

		_options = _options.override(config.getSection("options"));

		_retries = config.getAsIntegerWithDefault("options.retries", _retries);
		_baseRoute = config.getAsStringWithDefault("base_route", _baseRoute);
	}

	/**
	 * Sets references to dependent components.
	 * 
	 * @param references references to locate the component dependencies.
	 * @throws ReferenceException when no found references.
	 */
	public void setReferences(IReferences references) throws ReferenceException {
		_logger.setReferences(references);
		_counters.setReferences(references);
		_connectionResolver.setReferences(references);
	}

	/**
	 * Adds instrumentation to log calls and measure call time. It returns a Timing
	 * object that is used to end the time measurement.
	 * 
	 * @param correlationId (optional) transaction id to trace execution through
	 *                      call chain.
	 * @param name          a method name.
	 * @return Timing object to end the time measurement.
	 */
	protected Timing instrument(String correlationId, String name) {
		_logger.trace(correlationId, "Calling %s method", name);
		return _counters.beginTiming(name + ".call_time");
	}

	/**
	 * Checks if the component is opened.
	 * 
	 * @return true if the component has been opened and false otherwise.
	 */
	public boolean isOpen() {
		return _client != null;
	}

	/**
	 * Opens the component.
	 * 
	 * @param correlationId (optional) transaction id to trace execution through
	 *                      call chain.
	 * @throws ApplicationException when error occured.
	 */
	public void open(String correlationId) throws ApplicationException {
		// Skip if already opened
		if (_client != null)
			return;

		ConnectionParams connection = _connectionResolver.resolve(correlationId);

		String protocol = connection.getProtocol("http");
		String host = connection.getHost();
		int port = connection.getPort();
		_url = protocol + "://" + host + ":" + port;

		ClientConfig clientConfig = new ClientConfig();
		// clientConfig.getProperties().put(JSONConfiguration.FEATURE_POJO_MAPPING,
		// Boolean.TRUE);
		clientConfig.register(new JacksonFeature());
		_client = ClientBuilder.newClient(clientConfig);

		_logger.debug(correlationId, "Connected via REST to %s", _url);
	}

	/**
	 * Closes component and frees used resources.
	 * 
	 * @param correlationId (optional) transaction id to trace execution through
	 *                      call chain.
	 * @throws ApplicationException when error occured.
	 */
	public void close(String correlationId) throws ApplicationException {
		if (_client == null)
			return;

		_client.close();
		_client = null;
		_url = null;

		_logger.debug(correlationId, "Disconnected from %s", _url);
	}

//	private static String createEntityContent(String correlationId, Object value)
//		throws ApplicationException {
//		
//		try {
//			if (value == null)
//				return null;
//			String result = JsonConverter.toJson(value);
//			return result;
//		} catch (Exception ex) {
//			throw new InvocationException(
//				correlationId,
//				"SERIALIZATION_FAILED",
//				"Failed to serialize HTTP request object"
//			).withCause(ex);		
//		}
//	}

	private URI createRequestUri(String route) {
		StringBuilder builder = new StringBuilder(_url);

		if (_baseRoute != null && _baseRoute.trim().length() > 0) {
			if (_baseRoute.charAt(0) != '/')
				builder.append('/');
			builder.append(_baseRoute);
		}

		if (route.charAt(0) != '/')
			builder.append('/');
		builder.append(route);

		String uri = builder.toString();

		URI result = UriBuilder.fromUri(uri).build();

		return result;
	}

//	private static String constructQueryString(String correlationId, Map<String, String> parameters)
//		throws ApplicationException {
//		
//		StringBuilder builder = new StringBuilder();
//
//		try {
//			for (String name : parameters.keySet()) {
//				if (builder.length() > 0)
//					builder.append('&');
//				builder.append(name);
//				builder.append('=');
//				builder.append(URLEncoder.encode(parameters.get(name), "UTF-8"));
//			}
//	
//			return builder.toString();
//		} catch (Exception ex) {
//			throw new InvocationException(
//				correlationId,
//				"QUERY_BUILD_FAILED",
//				"Failed to build HTTP query"
//			).withCause(ex);
//		}
//	}

	private String addQueryParameter(String query, String name, String value) {
		try {
			name = URLEncoder.encode(name, "UTF-8");
			value = value != null ? URLEncoder.encode(value, "UTF-8") : "";
		} catch (UnsupportedEncodingException ex) {
			// Do nothihng...
		}

		int pos = query.indexOf('?');
		String path = pos >= 0 ? query.substring(0, pos) : query;
		String parameters = pos >= 0 ? query.substring(pos) : "";
		return path + "?" + (parameters.equals("") ? "" : "&") + name + "=" + value;
	}

	/**
	 * Adds a correlation id (correlation_id) to invocation parameter map.
	 * 
	 * @param route         invocation parameters.
	 * @param correlationId (optional) a correlation id to be added.
	 * @return invocation parameters with added correlation id.
	 */
	protected String addCorrelationId(String route, String correlationId) {
		return addQueryParameter(route, "correlation_id", correlationId);
	}

	/**
	 * Adds filter parameters (with the same name as they defined) to invocation
	 * parameter map.
	 * 
	 * @param route  invocation parameters.
	 * @param filter (optional) filter parameters
	 * @return invocation parameters with added filter parameters.
	 */
	protected String addFilterParams(String route, FilterParams filter) {
		for (String key : filter.keySet()) {
			route = addQueryParameter(route, key, filter.get(key));
		}
		return route;
	}

	/**
	 * Adds paging parameters (skip, take, total) to invocation parameter map.
	 * 
	 * @param route  invocation parameters.
	 * @param paging (optional) paging parameters
	 * @return invocation parameters with added paging parameters.
	 */
	protected String addPagingParams(String route, PagingParams paging) {
		if (paging.getSkip() != null)
			route = addQueryParameter(route, "skip", paging.getSkip().toString());
		if (paging.getTake() != null)
			route = addQueryParameter(route, "take", paging.getTake().toString());
		if (paging.hasTotal())
			route = addQueryParameter(route, "total", paging.getTake().toString());
		return route;
	}

	protected Response executeRequest(String correlationId, String method, URI uri, String mediaType, Entity<?> body)
			throws ApplicationException {

		if (_client == null) {
			throw new InvalidStateException(correlationId, "NOT_OPENED", "Client is not opened");
		}

		Response response = null;
		int retries = Math.min(1, Math.max(5, _retries));
		while (retries > 0) {
			try {
				if (method.equals(HttpMethod.GET))
					response = _client.target(uri).request(mediaType).get();
				else if (method.equals(HttpMethod.POST))
					response = _client.target(uri).request(mediaType).post(body);
				else if (method.equals(HttpMethod.PUT))
					response = _client.target(uri).request(mediaType).put(body);
				else if (method.equals(HttpMethod.DELETE))
					response = _client.target(uri).request(mediaType).delete();
				else
					throw new UnsupportedOperationException("Invalid request type");

				retries = 0;
			} catch (Exception ex) {
				retries--;
				if (retries < 0) {
					throw ex;
				} else {
					_logger.trace(correlationId, "Connection failed to uri " + uri + ". Retrying...");
				}
			}
		}

		if (response == null) {
			throw new UnknownException(correlationId, "NO_RESPONSE",
					"Unable to get a result from " + method + " " + uri);
		}

		if (response.getStatus() >= 400) {
			ErrorDescription errorObject = null;
			try {
				errorObject = response.readEntity(ErrorDescription.class);
			} catch (Exception ex) {
				// Todo: This may not work as expected. Find another way to get content string
				String responseContent = response.readEntity(String.class);
				throw new UnknownException(correlationId, "UNKNOWN_ERROR", responseContent);
			}

			if (errorObject != null)
				throw new ApplicationExceptionFactory().create(errorObject);
		}

		return response;
	}

	private Response executeJsonRequest(String correlationId, String method, String route, Object requestEntity)
			throws ApplicationException {

		route = addCorrelationId(route, correlationId);
		URI uri = createRequestUri(route);

		Entity<?> body = Entity.entity(requestEntity, MediaType.APPLICATION_JSON);
		return executeRequest(correlationId, method, uri, MediaType.APPLICATION_JSON, body);
	}

	/**
	 * Executes a remote method via HTTP/REST protocol.
	 * 
	 * @param type          the class type of data.
	 * @param correlationId (optional) transaction id to trace execution through
	 *                      call chain.
	 * @param method        HTTP method: "get", "head", "post", "put", "delete"
	 * @param route         a command route. Base route will be added to this route
	 * @param requestEntity request body object.
	 * @return result object.
	 * @throws ApplicationException when error occured.
	 */
	protected <T> T execute(Class<T> type, String correlationId, String method, String route, Object requestEntity)
			throws ApplicationException {

		Response response = executeJsonRequest(correlationId, method, route, requestEntity);

		try {
			T result = response.readEntity(type);
			return result;
		} catch (Throwable ex) {
			throw new InvocationException(correlationId, "SERIALIZATION_FAILED", "Failed to deserialize HTTP response")
					.withCause(ex);
		}
	}

	/**
	 * Executes a remote method via HTTP/REST protocol.
	 * 
	 * @param type          the generic class type of data.
	 * @param correlationId (optional) transaction id to trace execution through
	 *                      call chain.
	 * @param method        HTTP method: "get", "head", "post", "put", "delete"
	 * @param route         a command route. Base route will be added to this route
	 * @param requestEntity request body object.
	 * @return result object.
	 * @throws ApplicationException when error occured.
	 */
	protected <T> T execute(GenericType<T> type, String correlationId, String method, String route,
			Object requestEntity) throws ApplicationException {

		Response response = executeJsonRequest(correlationId, method, route, requestEntity);

		try {
			T result = response.readEntity(type);
			return result;
		} catch (Throwable ex) {
			throw new InvocationException(correlationId, "SERIALIZATION_FAILED", "Failed to deserialize HTTP response")
					.withCause(ex);
		}
	}

}
