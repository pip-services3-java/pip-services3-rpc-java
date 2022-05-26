package org.pipservices3.rpc.connect;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.ws.rs.core.UriBuilder;
import org.pipservices3.commons.config.ConfigParams;
import org.pipservices3.commons.config.IConfigurable;
import org.pipservices3.components.auth.CredentialParams;
import org.pipservices3.components.auth.CredentialResolver;
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
	 * The base credential resolver.
	 */
	protected CredentialResolver _credentialResolver = new CredentialResolver();

	/**
	 * Configures component by passing configuration parameters.
	 * 
	 * @param config configuration parameters to be set.
	 */
	public void configure(ConfigParams config) {
		_connectionResolver.configure(config);
		_credentialResolver.configure(config);
	}

	/**
	 * Sets references to dependent components.
	 * 
	 * @param references references to locate the component dependencies.
	 */
	public void setReferences(IReferences references) {
		_connectionResolver.setReferences(references);
		_credentialResolver.setReferences(references);
	}

	private void validateConnection(String correlationId, ConnectionParams connection, CredentialParams credential) throws ApplicationException {
		if (connection == null)
			throw new ConfigException(correlationId, "NO_CONNECTION", "HTTP connection is not set");

		String uri = connection.getUri();
		if (uri != null && uri.length() > 0)
			return;

		String protocol = connection.getProtocolWithDefault("http");
		if (!"http".equals(protocol) && !"https".equals(protocol)) {
			throw new ConfigException(correlationId, "WRONG_PROTOCOL", "Protocol is not supported by REST connection")
					.withDetails("protocol", protocol);
		}

		String host = connection.getHost();
		if (host == null)
			throw new ConfigException(correlationId, "NO_HOST", "Connection host is not set");

		int port = connection.getPort();
		if (port == 0)
			throw new ConfigException(correlationId, "NO_PORT", "Connection port is not set");

		// Check HTTPS credentials
		if (protocol.equals("https")) {
			// Check for credential
			if (credential == null) {
				throw new ConfigException(
						correlationId, "NO_CREDENTIAL", "SSL certificates are not configured for HTTPS protocol");
			} else {
				// Sometimes when we use https we are on an internal network and do not want to have to deal with security.
				// When we need a https connection and we don't want to pass credentials, flag is 'credential.internal_network',
				// this flag just has to be present and non null for this functionality to work.
				if (credential.getAsNullableString("internal_network") == null) {
					if (credential.getAsNullableString("ssl_key_file") == null) {
						throw new ConfigException(
								correlationId, "NO_SSL_KEY_FILE", "SSL key file is not configured in credentials");
					} else if (credential.getAsNullableString("ssl_crt_file") == null) {
						throw new ConfigException(
								correlationId, "NO_SSL_CRT_FILE", "SSL crt file is not configured in credentials");
					}
				}
			}
		}
	}

	private ConnectionParams composeConnection(List<ConnectionParams> connections, CredentialParams credential) {
		var unpackConn = new ConfigParams[connections.size()];

		AtomicInteger i = new AtomicInteger();
		connections.forEach(
				(c) -> {
					unpackConn[i.get()] = ConfigParams.fromValue(c);
					i.getAndIncrement();
				}
		);

		var connection = ConnectionParams.mergeConfigs(unpackConn);

		var uri = connection.getAsString("uri");

		if (uri == null || uri.equals("")) {
			var protocol = connection.getAsStringWithDefault("protocol", "http");
			var host = connection.getAsString("host");
			var port = connection.getAsInteger("port");

			uri = protocol + "://" + host;
			if (port > 0) {
				uri += ":" + port;
			}
			connection.setAsObject("uri", uri);
		} else {
			var address = URI.create(uri);
			var protocol = address.getScheme();

			connection.setAsObject("protocol", protocol);
			connection.setAsObject("host", address.getHost());
			connection.setAsObject("port", address.getPort());
		}

		if (Objects.equals(connection.getAsString("protocol"), "https") && credential != null) {
			if (credential.getAsNullableString("internal_network") == null) {
				connection =  connection.override(credential);
			}
		}

		return ConnectionParams.fromConfig(connection);
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
		CredentialParams credential = _credentialResolver.lookup(correlationId);
		this.validateConnection(correlationId, connection, credential);
		return this.composeConnection(List.of(connection), credential);
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
	public ConnectionParams resolveAll(String correlationId) throws ApplicationException {
		var connections = this._connectionResolver.resolveAll(correlationId);
		var credential = this._credentialResolver.lookup(correlationId);

		connections = connections != null ? connections : new ArrayList<>();
		for (var connection : connections)
			this.validateConnection(correlationId, connection, credential);

		return this.composeConnection(connections, credential);
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
		var connection = this._connectionResolver.resolve(correlationId);
		var credential = this._credentialResolver.lookup(correlationId);

		// Validate connection
		this.validateConnection(correlationId, connection, credential);

		this._connectionResolver.register(correlationId, connection);
	}

}
