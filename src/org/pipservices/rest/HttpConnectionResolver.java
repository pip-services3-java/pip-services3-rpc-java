package org.pipservices.rest;

import java.net.URI;
import java.util.List;

import javax.ws.rs.core.UriBuilder;
import org.pipservices.commons.config.ConfigParams;
import org.pipservices.commons.config.IConfigurable;
import org.pipservices.components.connect.ConnectionParams;
import org.pipservices.components.connect.ConnectionResolver;
import org.pipservices.commons.errors.ApplicationException;
import org.pipservices.commons.errors.ConfigException;
import org.pipservices.commons.refer.IReferenceable;
import org.pipservices.commons.refer.IReferences;

public class HttpConnectionResolver implements IReferenceable, IConfigurable  {
	
	protected ConnectionResolver _connectionResolver = new ConnectionResolver();

    public void setReferences(IReferences references) {
        _connectionResolver.setReferences(references);
    }

    public void configure(ConfigParams config){
        _connectionResolver.configure(config);
    }
    
    private void validateConnection(String correlationId, ConnectionParams connection) throws ApplicationException {
        if (connection == null)
            throw new ConfigException(correlationId, "NO_CONNECTION", "HTTP connection is not set");

        String uri = connection.getUri();
        if (!isNullOrEmpty(uri))
            return;

        String protocol = connection.getProtocol("http");
        if ("http" != protocol)
        {
            throw new ConfigException(
                correlationId, "WRONG_PROTOCOL", "Protocol is not supported by REST connection")
                .withDetails("protocol", protocol);
        }

        String host = connection.getHost();
        if (host == null)
            throw new ConfigException(correlationId, "NO_HOST", "Connection host is not set");

        int port = connection.getPort();
        if (port == 0)
            throw new ConfigException(correlationId, "NO_PORT", "Connection port is not set");
    }
    
    public static boolean isNull(String str) {
        return str == null ? true : false;
    }

    public static boolean isNullOrEmpty(String param) {
        if (isNull(param) || param.trim().length() == 0) {
            return true;
        }
        return false;
    }
    
    
    private void updateConnection(ConnectionParams connection)
    {
        if (isNullOrEmpty(connection.getUri()))
        {
            String uri = connection.getProtocol() + "://" + connection.getHost();
            if (connection.getPort() != 0)
                uri += ":" + connection.getPort();
            connection.setUri(uri);
        }
        else
        {
        	URI uri = UriBuilder.fromUri(connection.getUri()).build();
            connection.setProtocol(uri.getScheme());
            connection.setHost(uri.getHost());
            connection.setPort(uri.getPort());
        }
    }
    
    
    public ConnectionParams resolve(String correlationId) throws ApplicationException
    {
    	ConnectionParams connection = _connectionResolver.resolve(correlationId);
        validateConnection(correlationId, connection);
        updateConnection(connection);
        return connection;
    }
    
    public List<ConnectionParams> resolveAll(String correlationId) throws ApplicationException
    {
    	List<ConnectionParams> connections = _connectionResolver.resolveAll(correlationId);
        for (ConnectionParams connection : connections)
        {
            validateConnection(correlationId, connection);
            updateConnection(connection);
        }
        return connections;
    }
    
    public void register(String correlationId) throws ApplicationException
    {
    	ConnectionParams connection = _connectionResolver.resolve(correlationId);
        validateConnection(correlationId, connection);

        _connectionResolver.register(correlationId, connection);
    }
    
}
