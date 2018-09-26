package org.pipservices.rpc.clients;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

import org.pipservices.commons.errors.*;

/**
 * Abstract client that calls commandable HTTP service.
 * 
 * Commandable services are generated automatically for ICommandable objects.
 * Each command is exposed as POST operation that receives all parameters
 * in body object.
 * 
 * ### Configuration parameters ###
 * 
 * base_route:              base route for remote URI
 * connection(s):           
 *   discovery_key:         (optional) a key to retrieve the connection from IDiscovery
 *   protocol:              connection protocol: http or https
 *   host:                  host name or IP address
 *   port:                  port number
 *   uri:                   resource URI or connection string with all parameters in it
 * options:
 *   retries:               number of retries (default: 3)
 *   connect_timeout:       connection timeout in milliseconds (default: 10 sec)
 *   timeout:               invocation timeout in milliseconds (default: 10 sec)
 * 
 * ### References ###
 * 
 * - *:logger:*:*:1.0         (optional) ILogger components to pass log messages
 * - *:counters:*:*:1.0         (optional) ICounters components to pass collected measurements
 * - *:discovery:*:*:1.0        (optional) IDiscovery services to resolve connection
 * <p>
 * ### Example ###
 * <pre>
 * {@code
 * class MyCommandableHttpClient extends CommandableHttpClient implements IMyClient {
 *    ...
 * 
 *    public MyData getData(String correlationId, String id) {
 *        return this.callCommand(
 *        	  MyData.class,
 *            "get_data",
 *            correlationId,
 *            new MyData(id)
 *        );        
 *    }
 *    ...
 * }
 * 
 * MyCommandableHttpClient client = new MyCommandableHttpClient();
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
public class CommandableHttpClient extends RestClient {

	/**
	 * Creates a new instance of the client.
	 * 
	 * @param baseRoute a base route for remote service.
	 */
	public CommandableHttpClient(String baseRoute) {
		this._baseRoute = baseRoute;
	}

	/**
	 * Calls a remote method via HTTP commadable protocol. The call is made via POST
	 * operation and all parameters are sent in body object. The complete route to
	 * remote method is defined as baseRoute + "/" + name.
	 * 
	 * @param type          the class type.
	 * @param route         a name of the command to call.
	 * @param correlationId (optional) transaction id to trace execution through
	 *                      call chain.
	 * @param entity        body object.
	 * @return result of the command.
	 * @throws ApplicationException when error occured.
	 */
	public <T> T callCommand(Class<T> type, String route, String correlationId, Object entity)
			throws ApplicationException {
		return execute(type, correlationId, HttpMethod.POST, route, entity);
	}

	/**
	 * Calls a remote method via HTTP commadable protocol. The call is made via POST
	 * operation and all parameters are sent in body object. The complete route to
	 * remote method is defined as baseRoute + "/" + name.
	 * 
	 * @param type          the generic class type.
	 * @param route         a name of the command to call.
	 * @param correlationId (optional) transaction id to trace execution through
	 *                      call chain.
	 * @param entity        body object.
	 * @return result of the command.
	 * @throws ApplicationException when error occured.
	 */
	public <T> T callCommand(GenericType<T> type, String route, String correlationId, Object entity)
			throws ApplicationException {
		return execute(type, correlationId, HttpMethod.POST, route, entity);
	}

}
