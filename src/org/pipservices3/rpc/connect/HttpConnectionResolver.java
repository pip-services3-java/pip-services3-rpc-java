package org.pipservices3.rpc.connect;

import java.net.URI;
import java.util.List;

import javax.ws.rs.core.UriBuilder;
import org.pipservices3.commons.config.ConfigParams;
import org.pipservices3.commons.config.IConfigurable;
import org.pipservices3.components.connect.ConnectionParams;
import org.pipservices3.components.connect.ConnectionResolver;
import org.pipservices3.commons.errors.ApplicationException;
import org.pipservices3.commons.errors.ConfigException;
import org.pipservices3.commons.refer.IReferenceable;
import org.pipservices3.commons.refer.IReferences;

/**
 * Helper class to retrieve connections for HTTP-based services abd clients.
 * <p>
 * In addition to regular functions of ConnectionResolver is able to parse http:// URIs
 * and validate connection parameters before returning them.
 * <p>
 * ### Configuration parameters ###
 * <ul>
 * <li>connection:    
 *   <ul>
 *   <li>discovery_key:               (optional) a key to retrieve the connection from <a href="https://pip-services3-java.github.io/pip-services3-components-java/org/pipservices3/components/connect/IDiscovery.html">IDiscovery</a>
 *   <li>...                          other connection parameters
 *   </ul>
 * <li>connections:                   alternative to connection
 *   <ul>
 *   <li>[connection params 1]:       first connection parameters
 *   <li>...
 *   <li>[connection params N]:       Nth connection parameters
 *   <li>...
 *   </ul>
 * </ul>  
 * <p>
 * ### References ###
 * <ul>
 * <li>*:discovery:*:*:1.0            (optional) <a href="https://pip-services3-java.github.io/pip-services3-components-java/org/pipservices3/components/connect/IDiscovery.html">IDiscovery</a> services
 * </ul> 
 * <p>
 * ### Example ###
 * <pre>
 * {@code
 * ConfigParams config = ConfigParams.fromTuples(
 *      "connection.host", "10.1.1.100",
 *      "connection.port", 8080
 * );
 * 
 * HttpConnectionResolver connectionResolver = new HttpConnectionResolver();
 * connectionResolver.configure(config);
 * connectionResolver.setReferences(references);
 * 
 * ConnectionParams params = connectionResolver.resolve("123");
 * }
 * </pre>
 * @see ConnectionParams 
 * @see ConnectionResolver
 */
public class HttpConnectionResolver implements IReferenceable, IConfigurable {
	/**
	 * The base connection resolver.
	 */
	protected ConnectionResolver _connectionResolver = new ConnectionResolver();

	/**
	 * Configures component by passing configuration parameters.
	 * 
	 * @param config configuration parameters to be set.
	 */
	public void configure(ConfigParams config) {
		_connectionResolver.configure(config);
	}

	/**
	 * Sets references to dependent components.
	 * 
	 * @param references references to locate the component dependencies.
	 */
	public void setReferences(IReferences references) {
		_connectionResolver.setReferences(references);
	}

	private void validateConnection(String correlationId, ConnectionParams connection) throws ApplicationException {
		if (connection == null)
			throw new ConfigException(correlationId, "NO_CONNECTION", "HTTP connection is not set");

		String uri = connection.getUri();
		if (uri != null && uri.length() > 0)
			return;

		String protocol = connection.getProtocol("http");
		if (!"http".equals(protocol)) {
			throw new ConfigException(correlationId, "WRONG_PROTOCOL", "Protocol is not supported by REST connection")
					.withDetails("protocol", protocol);
		}

		String host = connection.getHost();
		if (host == null)
			throw new ConfigException(correlationId, "NO_HOST", "Connection host is not set");

		int port = connection.getPort();
		if (port == 0)
			throw new ConfigException(correlationId, "NO_PORT", "Connection port is not set");
	}

	private void updateConnection(ConnectionParams connection) {
		if (connection.getUri() == null || connection.getUri().length() == 0) {
			String uri = connection.getProtocol() + "://" + connection.getHost();
			if (connection.getPort() != 0)
				uri += ":" + connection.getPort();
			connection.setUri(uri);
		} else {
			URI uri = UriBuilder.fromUri(connection.getUri()).build();
			connection.setProtocol(uri.getScheme());
			connection.setHost(uri.getHost());
			connection.setPort(uri.getPort());
		}
	}

	/**
	 * Resolves a single component connection. If connections are configured to be
	 * retrieved from Discovery service it finds a IDiscovery and resolves the
	 * connection there.
	 * 
	 * @param correlationId (optional) transaction id to trace execution through
	 *                      call chain.
	 * @return resolved connection.
	 * @throws ApplicationException when error occured.
	 */
	public ConnectionParams resolve(String correlationId) throws ApplicationException {
		ConnectionParams connection = _connectionResolver.resolve(correlationId);
		validateConnection(correlationId, connection);
		updateConnection(connection);
		return connection;
	}

	/**
	 * Resolves all component connection. If connections are configured to be
	 * retrieved from Discovery service it finds a IDiscovery and resolves the
	 * connection there.
	 * 
	 * @param correlationId (optional) transaction id to trace execution through
	 *                      call chain.
	 * @return resolved connections.
	 * @throws ApplicationException when error occured.
	 */
	public List<ConnectionParams> resolveAll(String correlationId) throws ApplicationException {
		List<ConnectionParams> connections = _connectionResolver.resolveAll(correlationId);
		for (ConnectionParams connection : connections) {
			validateConnection(correlationId, connection);
			updateConnection(connection);
		}
		return connections;
	}

	/**
	 * Registers the given connection in all referenced discovery services. This
	 * method can be used for dynamic service discovery.
	 * 
	 * @param correlationId (optional) transaction id to trace execution through
	 *                      call chain.
	 * @throws ApplicationException when error occured.
	 */
	public void register(String correlationId) throws ApplicationException {
		ConnectionParams connection = _connectionResolver.resolve(correlationId);
		validateConnection(correlationId, connection);
		_connectionResolver.register(correlationId, connection);
	}

}
