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

@SuppressWarnings("restriction")
public class HttpEndpoint implements IOpenable, IConfigurable, IReferenceable {

	private static final ConfigParams _defaultConfig = ConfigParams.fromTuples(
		"connection.protocol", "http",
		"connection.host", "0.0.0.0",
		"connection.port", 3000,
		
		"options.request_max_size", 1024 * 1024,
		"options.connect_timeout", 60000,
		"options.debug", true
	);

	protected HttpConnectionResolver _connectionResolver = new HttpConnectionResolver();
	protected CompositeLogger _logger = new CompositeLogger();
	protected CompositeCounters _counters = new CompositeCounters();
	protected DependencyResolver _dependencyResolver = new DependencyResolver(_defaultConfig);

	private String _url;
	private HttpServer _server;
	private ResourceConfig _resources;
	private List<IRegisterable> _registrations = new ArrayList<IRegisterable>();

	public void setReferences(IReferences references) throws ReferenceException {
		_logger.setReferences(references);
		_counters.setReferences(references);
		_dependencyResolver.setReferences(references);
		_connectionResolver.setReferences(references);
	}

	public void configure(ConfigParams config) throws ConfigException {
		config = config.setDefaults(_defaultConfig);
		_dependencyResolver.configure(config);
		_connectionResolver.configure(config);
	}

	protected Timing Instrument(String correlationId, String name) {
		_logger.trace(correlationId, "Executing {0} method", name);
		return _counters.beginTiming(name + ".exec_time");
	}

	@Override
	public boolean isOpen() {
		return _server != null;
	}

	@Override
	public void open(String correlationId) throws ApplicationException {
		if (isOpen()) return;

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
			throw new ConnectionException(correlationId, "CANNOT_CONNECT", "Opening HTTP endpoint failed")
				.wrap(ex).withDetails("url", _url);
		}
	}

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
	
	public void register(IRegisterable registration) {
		_registrations.add(registration);
	}

	public void unregister(IRegisterable registration) {
		_registrations.remove(registration);
	}

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

	
	public void registerResource(Resource resource) {
		if (_resources != null)
			_resources.registerResources(resource);
	}
}
