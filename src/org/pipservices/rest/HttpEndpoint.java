package org.pipservices.rest;

import java.net.URI;
import java.util.*;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.pipservices.commons.config.*;
import org.pipservices.components.connect.ConnectionParams;
import org.pipservices.components.count.*;
import org.pipservices.commons.errors.*;
import org.pipservices.components.log.CompositeLogger;
import org.pipservices.commons.refer.*;
import org.pipservices.commons.run.IOpenable;

import com.sun.net.httpserver.HttpServer;

public class HttpEndpoint implements IOpenable, IConfigurable, IReferenceable {

	private static final ConfigParams _defaultConfig = ConfigParams.fromTuples(
            "connection.protocol", "http",
            "connection.host", "0.0.0.0",
            "connection.port", 3000,

            "options.request_max_size", 1024*1024,
            "options.connect_timeout", 60000,
            "options.debug", true
        );
	
	
	protected HttpConnectionResolver _connectionResolver = new HttpConnectionResolver();
    protected CompositeLogger _logger = new CompositeLogger();
    protected CompositeCounters _counters = new CompositeCounters();
    protected DependencyResolver _dependencyResolver = new DependencyResolver(_defaultConfig);
    
    protected String _url;
    
//    protected RouteBuilder _routeBuilder;
//    
    protected HttpServer _server;
    
    private List<IRegisterable> _registrations = new ArrayList<IRegisterable>();
    
    
    public void setReferences(IReferences references) throws ReferenceException
    {
        _logger.setReferences(references);
        _counters.setReferences(references);
        _dependencyResolver.setReferences(references);
        _connectionResolver.setReferences(references);
    }

    public void configure(ConfigParams config) throws ConfigException
    {
        config = config.setDefaults(_defaultConfig);
        _dependencyResolver.configure(config);
        _connectionResolver.configure(config);
    }
    
    protected Timing Instrument(String correlationId, String name)
    {
        _logger.trace(correlationId, "Executing {0} method", name);
        return _counters.beginTiming(name + ".exec_time");
    }

	@Override
	public boolean isOpened() {
		return _server != null;
	}

	@SuppressWarnings("restriction")
	@Override
	public void open(String correlationId) throws ApplicationException {
		
		if (isOpened()) return;
		
		ConnectionParams connection = _connectionResolver.resolve(correlationId);
        String protocol = connection.getProtocol("http");
        String host = connection.getHost();
        int port = connection.getPort();
        URI uri = UriBuilder.fromUri(protocol + "://" + host).port(port).path("/").build();
        _url = uri.toString();
        
        try {
        	ResourceConfig resourceConfig = new ResourceConfig();
        	resourceConfig.getSingletons().add(this);
        	for( IRegisterable registration : _registrations )
        	{
        		resourceConfig.register(registration);
        	}
        	
        	
        	_server = JdkHttpServerFactory.createHttpServer(uri, resourceConfig);
            _server.start();

            // Register the service URI
            _connectionResolver.register(correlationId);
            
            _logger.info(correlationId, "Opened REST service at %s", _url);
        } catch (Exception ex) {       	
            _server = null;            
            throw new ConnectionException(correlationId, "CANNOT_CONNECT", "Opening REST service failed")
            	.wrap(ex).withDetails("url", _url);
        }
	}
	
	
	@SuppressWarnings("restriction")
	@Override
	public void close(String correlationId) throws ApplicationException {
		 if (_server != null) {
	            // Eat exceptions
	            try {
	                _server.stop(0);
	                _logger.info(correlationId, "Closed REST service at %s", _url);
	            } catch (Exception ex) {
	                _logger.warn(correlationId, "Failed while closing REST service: %s", ex);
	            }
	            _server = null;
	            _url = null;
	        }	
	}
	
	public void register(IRegisterable registration)
    {
        _registrations.add(registration);
    }
	
	public void unregister(IRegisterable registration)
    {
        _registrations.remove(registration);
    }

    public void registerRoute(String method, String route, IActionable action)
    {
    	
        // Routes cannot start with '/'
        if (route.charAt(0) == '/')
            route = route.substring(1);
    }
	
}
