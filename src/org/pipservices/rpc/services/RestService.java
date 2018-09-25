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

public abstract class RestService
	implements IOpenable, IConfigurable, IReferenceable, IUnreferenceable, IRegisterable {

	private static final ConfigParams _defaultConfig = ConfigParams.fromTuples(
		//"base_route", "",
		"dependencies.endpoint", "pip-services:endpoint:http:*:1.0"
	);

	private ConfigParams _config;
	private IReferences _references;
	private boolean _localEndpoint;
	private boolean _opened;

	protected String _baseRoute;
	protected HttpEndpoint _endpoint;
	protected DependencyResolver _dependencyResolver = new DependencyResolver(_defaultConfig);
	protected CompositeLogger _logger = new CompositeLogger();
	protected CompositeCounters _counters = new CompositeCounters();

	@SuppressWarnings("restriction")
	protected HttpServer _server;
	protected String _url;

	protected RestService() {}

	public void configure(ConfigParams config) throws ConfigException {
		_config = config.setDefaults(_defaultConfig);

		_dependencyResolver.configure(config);

		_baseRoute = config.getAsStringWithDefault("base_route", _baseRoute);
	}

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

	protected Timing instrument(String correlationId, String name) {
		_logger.trace(correlationId, "Executing %s method", name);
		return _counters.beginTiming(name + ".exec_time");
	}

	public boolean isOpen() {
		return _opened;
	}


	public void open(String correlationId) throws ApplicationException {
		if (isOpen()) return;
		
		if (_endpoint == null) {
			_endpoint = createLocalEndpoint();
			_endpoint.register(this);
			_localEndpoint = true;
		}
		
		if (_localEndpoint)
			_endpoint.open(correlationId);

		_opened = true;
	}

	public void close(String correlationId) throws ApplicationException {
		if (!_opened) return;
		
		if (_endpoint == null) {
			throw new InvalidStateException(correlationId, "NO_ENDPOINT", "HTTP endpoint is missing");
		}

		if (_localEndpoint) {
			_endpoint.close(correlationId);
		}

		_opened = false;
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
	
	protected String getBodyAsString(ContainerRequestContext request) throws ApplicationException {
		try {
			InputStream streamReader = request.getEntityStream();
			byte[] data = new byte[streamReader.available()];
			streamReader.read(data, 0, data.length);
			String value = new String(data, "UTF-8");
			return value;
		} catch (IOException ex) {
			throw new InvocationException(
				null, "READ_ERROR", "Cannot read input stream"
			).wrap(ex);
		}
	}

	protected <T> T getBodyAsJson(Class<T> type, ContainerRequestContext request) throws ApplicationException {
		if (!request.getMediaType().toString().contains(MediaType.APPLICATION_JSON)) {
			throw new InvocationException(
				null, "EXPECTED_JSON", "Expected application/json media type"
			);
		}
		
		String json = getBodyAsString(request);
		
		try {
			return JsonConverter.fromJson(type, json);
		} catch (IOException ex) {
			throw new InvocationException(
				null, "READ_ERROR", "Failed to deserialize request from JSON"
			).wrap(ex);
		}
	}
	
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

	protected void registerResource(Resource resource) {
		if (_endpoint == null)
			return;

		_endpoint.registerResource(resource);
	}

}
