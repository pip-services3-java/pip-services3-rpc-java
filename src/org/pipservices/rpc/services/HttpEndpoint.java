package org.pipservices.rpc.services;

import java.net.URI;
import java.util.*;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.pipservices.commons.config.*;
import org.pipservices.components.connect.ConnectionParams;
import org.pipservices.components.count.*;
import org.pipservices.commons.errors.*;
import org.pipservices.components.log.CompositeLogger;
import org.pipservices.rpc.connect.HttpConnectionResolver;
import org.pipservices.commons.refer.*;
import org.pipservices.commons.run.IOpenable;

import com.sun.net.httpserver.HttpServer;

/**
 * Used for creating HTTP endpoints. An endpoint is a URL, at which a given service can be accessed by a client. 
 * 
 * ### Configuration parameters ###
 * 
 * Parameters to pass to the configure() method for component configuration:
 * 
 * - __connection(s)__ - the connection resolver's connections;
 *     - "connection.discovery_key" - the key to use for connection resolving in a discovery service;
 *     - "connection.protocol" - the connection's protocol;
 *     - "connection.host" - the target host;
 *     - "connection.port" - the target port;
 *     - "connection.uri" - the target URI.
 * 
 * ### References ###
 * 
 * A logger, counters, and a connection resolver can be referenced by passing the 
 * following references to the object's setReferences() method:
 * 
 * - logger: <code>"\*:logger:\*:\*:1.0"</code>;
 * - counters: <code>"\*:counters:\*:\*:1.0"</code>;
 * - discovery: <code>"\*:discovery:\*:\*:1.0"</code> (for the connection resolver).
 * <p>
 * ### Examples ###
 * <pre>
 * {@code
 *     public MyMethod(String correlationId, ConfigParams _config, IReferences _references) {
 *         HttpEndpoint endpoint = new HttpEndpoint();
 *         if (this._config)
 *             endpoint.configure(this._config);
 *         if (this._references)
 *             endpoint.setReferences(this._references);
 *         ...
 * 
 *         this._endpoint.open(correlationId);
 *         ...
 *     }
 * }
 * </pre>
 */
@SuppressWarnings("restriction")
public class HttpEndpoint implements IOpenable, IConfigurable, IReferenceable {

	private static final ConfigParams _defaultConfig = ConfigParams.fromTuples("connection.protocol", "http",
			"connection.host", "0.0.0.0", "connection.port", 3000,

			"options.request_max_size", 1024 * 1024, "options.connect_timeout", 60000, "options.debug", true);

	protected HttpConnectionResolver _connectionResolver = new HttpConnectionResolver();
	protected CompositeLogger _logger = new CompositeLogger();
	protected CompositeCounters _counters = new CompositeCounters();
	protected DependencyResolver _dependencyResolver = new DependencyResolver(_defaultConfig);

	private String _url;
	private HttpServer _server;
	private ResourceConfig _resources;
	private List<IRegisterable> _registrations = new ArrayList<IRegisterable>();

	/**
	 * Configures this HttpEndpoint using the given configuration parameters.
	 * 
	 * __Configuration parameters:__ - __connection(s)__ - the connection resolver's
	 * connections; - "connection.discovery_key" - the key to use for connection
	 * resolving in a discovery service; - "connection.protocol" - the connection's
	 * protocol; - "connection.host" - the target host; - "connection.port" - the
	 * target port; - "connection.uri" - the target URI.
	 * 
	 * @param config configuration parameters, containing a "connection(s)" section.
	 * @throws ConfigException when configuration is wrong.
	 * @see ConfigParams
	 */
	public void configure(ConfigParams config) throws ConfigException {
		config = config.setDefaults(_defaultConfig);
		_dependencyResolver.configure(config);
		_connectionResolver.configure(config);
	}

	/**
	 * Sets references to this endpoint's logger, counters, and connection resolver.
	 * 
	 * __References:__ - logger: <code>"\*:logger:\*:\*:1.0"</code> - counters:
	 * <code>"\*:counters:\*:\*:1.0"</code> - discovery:
	 * <code>"\*:discovery:\*:\*:1.0"</code> (for the connection resolver)
	 * 
	 * @param references an IReferences object, containing references to a logger,
	 *                   counters, and a connection resolver.
	 * @throws ReferenceException when no found references.
	 * @see IReferences
	 */
	public void setReferences(IReferences references) throws ReferenceException {
		_logger.setReferences(references);
		_counters.setReferences(references);
		_dependencyResolver.setReferences(references);
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
	protected Timing Instrument(String correlationId, String name) {
		_logger.trace(correlationId, "Executing {0} method", name);
		return _counters.beginTiming(name + ".exec_time");
	}

	/**
	 * Checks if the component is opened.
	 * 
	 * @return whether or not this endpoint is open with an actively listening REST
	 *         server.
	 */
	@Override
	public boolean isOpen() {
		return _server != null;
	}

	/**
	 * Opens a connection using the parameters resolved by the referenced connection
	 * resolver and creates a REST server (service) using the set options and
	 * parameters.
	 * 
	 * @param correlationId (optional) transaction id to trace execution through
	 *                      call chain.
	 * @throws ApplicationException when error occured.
	 */
	@Override
	public void open(String correlationId) throws ApplicationException {
		if (isOpen())
			return;

		ConnectionParams connection = _connectionResolver.resolve(correlationId);
		String protocol = connection.getProtocol("http");
		String host = connection.getHost();
		int port = connection.getPort();
		URI uri = UriBuilder.fromUri(protocol + "://" + host).port(port).path("/").build();
		_url = uri.toString();

		try {
			_resources = new ResourceConfig();

			performRegistrations();

			_server = JdkHttpServerFactory.createHttpServer(uri, _resources);
//			_server.start();

			_logger.info(correlationId, "Opened REST service at %s", _url);
		} catch (Exception ex) {
			_server = null;
			throw new ConnectionException(correlationId, "CANNOT_CONNECT", "Opening HTTP endpoint failed").wrap(ex)
					.withDetails("url", _url);
		}
	}

	/**
	 * Closes this endpoint and the REST server (service) that was opened earlier.
	 * 
	 * @param correlationId (optional) transaction id to trace execution through
	 *                      call chain.
	 * @throws ApplicationException when error occured.
	 */
	@Override
	public void close(String correlationId) throws ApplicationException {
		if (_server != null) {
			// Eat exceptions
			try {
				_server.stop(0);
				_logger.info(correlationId, "Closed HTTP endpoint at %s", _url);
			} catch (Exception ex) {
				_logger.warn(correlationId, "Failed while closing HTTP endpoint: %s", ex);
			}
			_server = null;
			_resources = null;
			_url = null;
		}
	}

	private void performRegistrations() {
		for (IRegisterable registration : _registrations)
			registration.register();
	}

	/**
	 * Registers a registerable object for dynamic endpoint discovery.
	 * 
	 * @param registration the registration to add.
	 * 
	 * @see IRegisterable
	 */
	public void register(IRegisterable registration) {
		_registrations.add(registration);
	}

	/**
	 * Unregisters a registerable object, so that it is no longer used in dynamic
	 * endpoint discovery.
	 * 
	 * @param registration the registration to remove.
	 * 
	 * @see IRegisterable
	 */
	public void unregister(IRegisterable registration) {
		_registrations.remove(registration);
	}

	/**
	 * Registers an action in this objects REST server (service) by the given method
	 * and route.
	 * 
	 * @param method the HTTP method of the route.
	 * @param route  the route to register in this object's REST server (service).
	 * @param action the action to perform at the given route.
	 */
	public void registerRoute(String method, String route, Inflector<ContainerRequestContext, Response> action) {
		// Routes cannot start with '/'
		if (route.charAt(0) == '/')
			route = route.substring(1);

		Resource.Builder builder = Resource.builder().addChildResource(route);

		method = method.toUpperCase();
		builder.addMethod(method).handledBy(action);

		Resource resource = builder.build();

		registerResource(resource);
	}

	/**
	 * Registers resource in this objects Rest service.
	 * 
	 * @param resource resource for registration
	 */
	public void registerResource(Resource resource) {
		if (_resources != null)
			_resources.registerResources(resource);
	}
}
