package org.pipservices.services;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.model.Resource;
import org.pipservices.commons.config.*;
import org.pipservices.components.count.*;
import org.pipservices.commons.errors.*;
import org.pipservices.components.log.*;
import org.pipservices.commons.refer.*;
import org.pipservices.commons.run.*;
import com.sun.net.httpserver.*;

public abstract class RestService
		implements IOpenable, IConfigurable, IReferenceable, IUnreferenceable, IRegisterable {

//	private final static ConfigParams _defaultConfig = ConfigParams.fromTuples(
//		"connection.protocol", "http",
//		"connection.host", "0.0.0.0",
//		//"connection.port", 3000,
//		"connection.request_max_size", 1024 * 1024,
//		"connection.connect_timeout", 60000,
//		"connection.debug", true
//	);

	private static final ConfigParams _defaultConfig = ConfigParams.fromTuples("base_route", "",
			"dependencies.endpoint", "pip-services:endpoint:http:*:1.0");

	@SuppressWarnings("restriction")
	protected HttpServer _server;
	protected DependencyResolver _dependencyResolver = new DependencyResolver(_defaultConfig);
	protected CompositeLogger _logger = new CompositeLogger();
	protected CompositeCounters _counters = new CompositeCounters();
	protected String _url;
	protected String _route;
	protected HttpEndpoint _endpoint;

	private ConfigParams _config;
	private IReferences _references;
	private boolean _localEndpoint;
	private boolean _opened;

	protected RestService() {
	}

	public void setReferences(IReferences references) throws ReferenceException, ConfigException {
		_logger.setReferences(references);
		_counters.setReferences(references);
		_dependencyResolver.setReferences(references);

		_references = references;

		// Get endpoint
		_endpoint = (HttpEndpoint) _dependencyResolver.getOneOptional("endpoint");
		_localEndpoint = _endpoint == null;

		// Or create a local one
		if (_endpoint == null)
			_endpoint = createLocalEndpoint();

		// Add registration callback to the endpoint

		_endpoint.register(this);
	}

	public void configure(ConfigParams config) throws ConfigException {

		config = config.setDefaults(_defaultConfig);
		_dependencyResolver.configure(config);

		_route = config.getAsStringWithDefault("base_route", _route);
	}

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

	public boolean isOpen() {
		return _opened;
	}

	/**
	 * Does instrumentation of performed business method by counting elapsed time.
	 * 
	 * @param correlationId the unique id to identify distributed transaction
	 * @param name          the name of called business method
	 * @return ITiming instance to be called at the end of execution of the method.
	 */
	protected Timing instrument(String correlationId, String name) {
		_logger.trace(correlationId, "Executing %s method", name);
		return _counters.beginTiming(name + ".exec_time");
	}

	public void open(String correlationId) throws ApplicationException {
		if (isOpen()) return;
		
		if (_endpoint == null) {
			_endpoint = createLocalEndpoint();
			_endpoint.register(this);
			_localEndpoint = true;
		}
		
		if (_localEndpoint) {
			_endpoint.open(correlationId);
		} else {
			_opened = true;
		}
	}

	public void close(String correlationId) throws ApplicationException {
		if (!isOpen()) {
			if (_endpoint == null) {
				throw new InvalidStateException(correlationId, "NO_ENDPOINT", "HTTP endpoint is missing");
			}

			if (_localEndpoint) {
				_endpoint.close(correlationId);
			}

			_opened = false;
		}
	}

	protected Response sendError(Exception ex) {
		return HttpResponseSender.sendError(ex);
	}

	protected Response sendResult(Object result) {
		return HttpResponseSender.sendResult(result);
	}

	protected Response sendEmptyResult() {
		return HttpResponseSender.sendEmptyResult();
	}

	protected Response sendCreatedResult(Object result) {
		return HttpResponseSender.sendCreatedResult(result);
	}

	protected Response sendDeleted(Object result) {
		return HttpResponseSender.sendDeletedResult(result);
	}

	protected void registerRoute(String method, String route, Inflector<ContainerRequestContext, Response> action) {
		if (_endpoint == null)
			return;

		if (route.charAt(0) != '/')
			route = "/" + route;

		if (_route != null && _route.length() > 0) {
			String baseRoute = _route;
			if (baseRoute.charAt(0) != '/')
				baseRoute = "/" + baseRoute;
			route = baseRoute + route;
		}

		_endpoint.registerRoute(method, route, action);
	}

	protected void registerResource(Resource resource) {
		if (_endpoint == null)
			return;

		_endpoint.registerResource(resource);
	}

}
