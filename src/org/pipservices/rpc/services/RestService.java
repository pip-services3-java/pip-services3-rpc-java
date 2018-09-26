package org.pipservices.rpc.services;

import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;

import javax.ws.rs.container.*;
import javax.ws.rs.core.*;

import org.glassfish.jersey.process.*;
import org.glassfish.jersey.server.model.*;
import org.pipservices.commons.config.*;
import org.pipservices.commons.convert.JsonConverter;
import org.pipservices.components.count.*;
import org.pipservices.commons.errors.*;
import org.pipservices.components.log.*;
import org.pipservices.commons.refer.*;
import org.pipservices.commons.run.*;
import com.sun.net.httpserver.*;

/**
 * Abstract service that receives remove calls via HTTP/REST protocol.
 * 
 * ### Configuration parameters ###
 * 
 * base_route:              base route for remote URI
 * dependencies:
 *   endpoint:              override for HTTP Endpoint dependency
 *   controller:            override for Controller dependency
 * connection(s):           
 *   discovery_key:         (optional) a key to retrieve the connection from IDiscovery
 *   protocol:              connection protocol: http or https
 *   host:                  host name or IP address
 *   port:                  port number
 *   uri:                   resource URI or connection string with all parameters in it
 * 
 * ### References ###
 * 
 * - *:logger:*:*:1.0               (optional) ILogger components to pass log messages
 * - *:counters:*:*:1.0             (optional) ICounters components to pass collected measurements
 * - *:discovery:*:*:1.0            (optional) IDiscovery services to resolve connection
 * - *:endpoint:http:*:1.0          (optional) HttpEndpoint reference
 * <p>
 * ### Example ###
 * <pre>
 * {@code
 * class MyRestService extends RestService {
 *    private IMyController _controller;
 *    ...
 *    public MyRestService() {
 *       super();
 *       this._dependencyResolver.put(
 *           "controller",
 *           new Descriptor("mygroup","controller","*","*","1.0")
 *       );
 *    }
 * 
 *    public void setReferences(IReferences references) {
 *       base.setReferences(references);
 *       this._controller = (IMyController)this._dependencyResolver.getRequired("controller");
 *    }
 * 
 *    public void register() {
 *        ...
 *    }
 * }
 * 
 * MyRestService service = new MyRestService();
 * service.configure(ConfigParams.fromTuples(
 *     "connection.protocol", "http",
 *     "connection.host", "localhost",
 *     "connection.port", 8080
 * ));
 * service.setReferences(References.fromTuples(
 *    new Descriptor("mygroup","controller","default","default","1.0"), controller
 * ));
 * 
 * service.open("123");
 * System.out.println("The REST service is running on port 8080");
 * });
 * }
 * </pre>
 */
public abstract class RestService implements IOpenable, IConfigurable, IReferenceable, IUnreferenceable, IRegisterable {

	private static final ConfigParams _defaultConfig = ConfigParams.fromTuples(
			// "base_route", "",
			"dependencies.endpoint", "pip-services:endpoint:http:*:1.0");

	private ConfigParams _config;
	private IReferences _references;
	private boolean _localEndpoint;
	private boolean _opened;

	/**
	 * The base route.
	 */
	protected String _baseRoute;
	/**
	 * The HTTP endpoint that exposes this service.
	 */
	protected HttpEndpoint _endpoint;
	/**
	 * The dependency resolver.
	 */
	protected DependencyResolver _dependencyResolver = new DependencyResolver(_defaultConfig);
	/**
	 * The logger.
	 */
	protected CompositeLogger _logger = new CompositeLogger();
	/**
	 * The performance counters.
	 */
	protected CompositeCounters _counters = new CompositeCounters();

	@SuppressWarnings("restriction")
	protected HttpServer _server;
	protected String _url;

	protected RestService() {
	}

	/**
	 * Configures component by passing configuration parameters.
	 * 
	 * @param config configuration parameters to be set.
	 * @throws ConfigException when configuration is wrong.
	 */
	public void configure(ConfigParams config) throws ConfigException {
		_config = config.setDefaults(_defaultConfig);

		_dependencyResolver.configure(config);

		_baseRoute = config.getAsStringWithDefault("base_route", _baseRoute);
	}

	/**
	 * Sets references to dependent components.
	 * 
	 * @param references references to locate the component dependencies.
	 * @throws ReferenceException when no found references.
	 * @throws ConfigException    when configuration is wrong.
	 */
	public void setReferences(IReferences references) throws ReferenceException, ConfigException {
		_logger.setReferences(references);
		_counters.setReferences(references);
		_dependencyResolver.setReferences(references);

		_references = references;

		// Get endpoint
		_endpoint = (HttpEndpoint) _dependencyResolver.getOneOptional("endpoint");

		// Or create a local one
		if (_endpoint == null) {
			_endpoint = createLocalEndpoint();
			_localEndpoint = true;
		} else {
			_localEndpoint = false;
		}

		// Add registration callback to the endpoint
		_endpoint.register(this);
	}

	/**
	 * Unsets (clears) previously set references to dependent components.
	 */
	public void unsetReferences() {
		// Remove registration callback from endpoint
		if (_endpoint != null) {
			_endpoint.unregister(this);
			_endpoint = null;
		}
	}

	private HttpEndpoint createLocalEndpoint() throws ConfigException, ReferenceException {
		HttpEndpoint endpoint = new HttpEndpoint();

		if (_config != null)
			endpoint.configure(_config);

		if (_references != null)
			endpoint.setReferences(_references);

		return endpoint;
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
		_logger.trace(correlationId, "Executing %s method", name);
		return _counters.beginTiming(name + ".exec_time");
	}

	/**
	 * Checks if the component is opened.
	 * 
	 * @return true if the component has been opened and false otherwise.
	 */
	public boolean isOpen() {
		return _opened;
	}

	/**
	 * Opens the component.
	 * 
	 * @param correlationId (optional) transaction id to trace execution through
	 *                      call chain.
	 * @throws ApplicationException when error occured.
	 */
	public void open(String correlationId) throws ApplicationException {
		if (isOpen())
			return;

		if (_endpoint == null) {
			_endpoint = createLocalEndpoint();
			_endpoint.register(this);
			_localEndpoint = true;
		}

		if (_localEndpoint)
			_endpoint.open(correlationId);

		_opened = true;
	}

	/**
	 * Closes component and frees used resources.
	 * 
	 * @param correlationId (optional) transaction id to trace execution through
	 *                      call chain.
	 * @throws ApplicationException when error occured.
	 */
	public void close(String correlationId) throws ApplicationException {
		if (!_opened)
			return;

		if (_endpoint == null) {
			throw new InvalidStateException(correlationId, "NO_ENDPOINT", "HTTP endpoint is missing");
		}

		if (_localEndpoint) {
			_endpoint.close(correlationId);
		}

		_opened = false;
	}

	/**
	 * Sends error serialized as ErrorDescription object and appropriate HTTP status
	 * code. If status code is not defined, it uses 500 status code.
	 * 
	 * @param ex an error object to be sent.
	 * @return HTTP response status
	 */
	protected Response sendError(Exception ex) {
		return HttpResponseSender.sendError(ex);
	}

	/**
	 * Creates a callback function that sends result as JSON object. That callack
	 * function call be called directly or passed as a parameter to business logic
	 * components.
	 * 
	 * If object is not null it returns 200 status code. For null results it returns
	 * 204 status code. If error occur it sends ErrorDescription with approproate
	 * status code.
	 * 
	 * @param result a body object to result.
	 * @return execution result.
	 */
	protected Response sendResult(Object result) {
		return HttpResponseSender.sendResult(result);
	}

	/**
	 * Creates a callback function that sends an empty result with 204 status code.
	 * If error occur it sends ErrorDescription with approproate status code.
	 * 
	 * @return HTTP response status with no content.
	 */
	protected Response sendEmptyResult() {
		return HttpResponseSender.sendEmptyResult();
	}

	/**
	 * Creates a callback function that sends newly created object as JSON. That
	 * callack function call be called directly or passed as a parameter to business
	 * logic components.
	 * 
	 * If object is not null it returns 201 status code. For null results it returns
	 * 204 status code. If error occur it sends ErrorDescription with approproate
	 * status code.
	 * 
	 * @param result a body object to created result
	 * @return execution result.
	 */
	protected Response sendCreatedResult(Object result) {
		return HttpResponseSender.sendCreatedResult(result);
	}

	/**
	 * Creates a callback function that sends deleted object as JSON. That callack
	 * function call be called directly or passed as a parameter to business logic
	 * components.
	 * 
	 * If object is not null it returns 200 status code. For null results it returns
	 * 204 status code. If error occur it sends ErrorDescription with approproate
	 * status code.
	 * 
	 * @param result a body object to deleted result
	 * @return execution result.
	 */
	protected Response sendDeleted(Object result) {
		return HttpResponseSender.sendDeletedResult(result);
	}

	protected String getQueryParameter(ContainerRequestContext request, String name) {
		try {
			name = URLEncoder.encode(name, "UTF-8");
			if (request.getUriInfo().getQueryParameters().containsKey(name)) {
				String value = request.getUriInfo().getQueryParameters().getFirst(name);
				value = value != null ? URLDecoder.decode(value, "UTF-8") : null;
			}
		} catch (UnsupportedEncodingException ex) {
			// Do nothing...
		}

		return null;
	}

	/**
	 * Gets string value of request body.
	 * 
	 * @param request HTTP request
	 * @return string value of data.
	 * @throws ApplicationException when error occured.
	 */
	protected String getBodyAsString(ContainerRequestContext request) throws ApplicationException {
		try {
			InputStream streamReader = request.getEntityStream();
			byte[] data = new byte[streamReader.available()];
			streamReader.read(data, 0, data.length);
			String value = new String(data, "UTF-8");
			return value;
		} catch (IOException ex) {
			throw new InvocationException(null, "READ_ERROR", "Cannot read input stream").wrap(ex);
		}
	}

	/**
	 * Gets request body from json string.
	 * 
	 * @param type    the class type of result object.
	 * @param request HTTP request
	 * @return converted object value
	 * @throws ApplicationException when error occured.
	 */
	protected <T> T getBodyAsJson(Class<T> type, ContainerRequestContext request) throws ApplicationException {
		if (!request.getMediaType().toString().contains(MediaType.APPLICATION_JSON)) {
			throw new InvocationException(null, "EXPECTED_JSON", "Expected application/json media type");
		}

		String json = getBodyAsString(request);

		try {
			return JsonConverter.fromJson(type, json);
		} catch (IOException ex) {
			throw new InvocationException(null, "READ_ERROR", "Failed to deserialize request from JSON").wrap(ex);
		}
	}

	/**
	 * Registers a route in HTTP endpoint.
	 * 
	 * @param method HTTP method: "get", "head", "post", "put", "delete"
	 * @param route  a command route. Base route will be added to this route
	 * @param action an action function that is called when operation is invoked.
	 */
	protected void registerRoute(String method, String route, Inflector<ContainerRequestContext, Response> action) {
		if (_endpoint == null)
			return;

		if (route.charAt(0) != '/')
			route = "/" + route;

		if (_baseRoute != null && _baseRoute.length() > 0) {
			String baseRoute = _baseRoute;
			if (baseRoute.charAt(0) != '/')
				baseRoute = "/" + baseRoute;
			route = baseRoute + route;
		}

		_endpoint.registerRoute(method, route, action);
	}

	/**
	 * Registers resource in this objects Rest service.
	 * 
	 * @param resource resource for registration
	 */
	protected void registerResource(Resource resource) {
		if (_endpoint == null)
			return;

		_endpoint.registerResource(resource);
	}

}
