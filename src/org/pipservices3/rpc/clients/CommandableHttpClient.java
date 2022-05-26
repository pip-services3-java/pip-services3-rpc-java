package org.pipservices3.rpc.clients;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import org.pipservices3.commons.errors.*;

/**
 * Abstract client that calls commandable HTTP service.
 * <p>
 * Commandable services are generated automatically for <a href="https://pip-services3-java.github.io/pip-services3-commons-java/org/pipservices3/commons/commands/ICommandable.html">ICommandable</a> objects.
 * Each command is exposed as POST operation that receives all parameters
 * in body object.
 * <p>
 * ### Configuration parameters ###
 * <ul>
 * <li>base_route:              base route for remote URI
 * <li>connection(s):           
 *   <ul>
 *   <li>discovery_key:         (optional) a key to retrieve the connection from <a href="https://pip-services3-java.github.io/pip-services3-components-java/org/pipservices3/components/connect/IDiscovery.html">IDiscovery</a>
 *   <li>protocol:              connection protocol: http or https
 *   <li>host:                  host name or IP address
 *   <li>port:                  port number
 *   <li>uri:                   resource URI or connection string with all parameters in it
 *   </ul>
 * <li>options:
 *   <ul>
 *   <li>retries:               number of retries (default: 3)
 *   <li>connect_timeout:       connection timeout in milliseconds (default: 10 sec)
 *   <li>timeout:               invocation timeout in milliseconds (default: 10 sec)
 *   </ul>
 * </ul>  
 * <p>
 * ### References ###
 * <ul>
 * <li>*:logger:*:*:1.0         (optional) <a href="https://pip-services3-java.github.io/pip-services3-components-java/org/pipservices3/components/log/ILogger.html">ILogger</a> components to pass log messages
 * <li>*:counters:*:*:1.0         (optional) <a href="https://pip-services3-java.github.io/pip-services3-components-java/org/pipservices3/components/count/ICounters.html">ICounters</a> components to pass collected measurements
 * <li>*:tracer:*:*:1.0        (optional) <a href="https://pip-services3-java.github.io/pip-services3-components-java/org/pipservices3/components/trace/ITracer.html">ITracer</a> components to record traces
 * <li>*:discovery:*:*:1.0        (optional) <a href="https://pip-services3-java.github.io/pip-services3-components-java/org/pipservices3/components/connect/IDiscovery.html">IDiscovery</a> services to resolve connection
 * </ul>
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
		return call(type, correlationId, HttpMethod.POST, route, entity);
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
		return call(type, correlationId, HttpMethod.POST, route, entity);
	}

}
