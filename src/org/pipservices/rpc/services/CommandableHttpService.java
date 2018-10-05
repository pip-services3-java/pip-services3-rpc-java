package org.pipservices.rpc.services;

import java.util.*;

import javax.ws.rs.container.*;
import javax.ws.rs.core.*;

import org.glassfish.jersey.process.*;
import org.pipservices.commons.commands.*;
import org.pipservices.commons.errors.*;
import org.pipservices.commons.refer.*;
import org.pipservices.components.count.*;
import org.pipservices.commons.run.*;

/**
 * Abstract service that receives remove calls via HTTP/REST protocol
 * to operations automatically generated for commands defined in <a href="https://raw.githubusercontent.com/pip-services-java/pip-services-commons-java/master/doc/api/org/pipservices/commons/commands/ICommandable.html">ICommandable</a> components.
 * Each command is exposed as POST operation that receives all parameters in body object.
 * <p>
 * Commandable services require only 3 lines of code to implement a robust external
 * HTTP-based remote interface.
 * <p>
 * ### Configuration parameters ###
 * <ul>
 * <li>base_route:              base route for remote URI
 * <li>dependencies:
 *   <ul>
 *   <li>endpoint:              override for HTTP Endpoint dependency
 *   <li>controller:            override for Controller dependency
 *   </ul>
 * <li>connection(s):           
 *   <ul>
 *   <li>discovery_key:         (optional) a key to retrieve the connection from <a href="https://raw.githubusercontent.com/pip-services-java/pip-services-components-java/master/doc/api/org/pipservices/components/connect/IDiscovery.html">IDiscovery</a>
 *   <li>protocol:              connection protocol: http or https
 *   <li>host:                  host name or IP address
 *   <li>port:                  port number
 *   <li>uri:                   resource URI or connection string with all parameters in it
 *   </ul>
 * </ul>
 * <p>
 * ### References ###
 * <ul>
 * <li>*:logger:*:*:1.0         (optional) <a href="https://raw.githubusercontent.com/pip-services-java/pip-services-components-java/master/doc/api/org/pipservices/components/log/ILogger.html">ILogger</a> components to pass log messages
 * <li>*:counters:*:*:1.0         (optional) <a href="https://raw.githubusercontent.com/pip-services-java/pip-services-components-java/master/doc/api/org/pipservices/components/count/ICounters.html">ICounters</a> components to pass collected measurements
 * <li>*:discovery:*:*:1.0        (optional) <a href="https://raw.githubusercontent.com/pip-services-java/pip-services-components-java/master/doc/api/org/pipservices/components/connect/IDiscovery.html">IDiscovery</a> services to resolve connection
 * <li>*:endpoint:http:*:1.0          (optional) {@link HttpEndpoint} reference
 * </ul>
 * <p>
 * ### Example ###
 * <pre>
 * {@code
 * class MyCommandableHttpService extends CommandableHttpService {
 *    public MyCommandableHttpService() {
 *       super();
 *       this._dependencyResolver.put(
 *           "controller",
 *           new Descriptor("mygroup","controller","*","*","1.0")
 *       );
 *    }
 * }
 * 
 * MyCommandableHttpService service = new MyCommandableHttpService();
 * service.configure(ConfigParams.fromTuples(
 *     "connection.protocol", "http",
 *     "connection.host", "localhost",
 *     "connection.port", 8080
 * ));
 * service.setReferences(References.fromTuples(
 *    new Descriptor("mygroup","controller","default","default","1.0"), controller
 * ));
 * 
 * service.open("123");
 * System.out.println("The REST service is running on port 8080");
 * }
 * </pre>
 * @see RestService
 */
public class CommandableHttpService extends RestService {
	private ICommandable _controller;

	/**
	 * Creates a new instance of the service.
	 * 
	 * @param baseRoute a service base route.
	 */
	public CommandableHttpService(String baseRoute) {
		this._baseRoute = baseRoute;
		_dependencyResolver.put("controller", "none");
	}

	/**
	 * Sets references to dependent components.
	 * 
	 * @param references references to locate the component dependencies.
	 * @throws ReferenceException when no found references.
	 * @throws ConfigException    when configuration is wrong.
	 */
	@Override
	public void setReferences(IReferences references) throws ReferenceException, ConfigException {
		super.setReferences(references);

		_controller = (ICommandable) _dependencyResolver.getOneRequired("controller");
	}

	/**
	 * Registers all service routes in HTTP endpoint.
	 */
	@Override
	public void register() {
		if (_controller == null)
			return;

		List<ICommand> commands = _controller.getCommandSet().getCommands();

		for (ICommand command : commands) {
			registerRoute("post", command.getName(), new Inflector<ContainerRequestContext, Response>() {
				@Override
				public Response apply(ContainerRequestContext request) {
					return executeCommand(command, request);
				}
			});
		}
	}

	private Response executeCommand(ICommand command, ContainerRequestContext request) {
		try {
			String json = getBodyAsString(request);

			Parameters parameters = json == null ? new Parameters() : Parameters.fromJson(json);

			String correlationId = getQueryParameter(request, "correlation_id");

			Timing timing = instrument(correlationId, _baseRoute + '.' + command.getName());

			try {
				Object result = command.execute(correlationId, parameters);
				return sendResult(result);
			} finally {
				timing.endTiming();
			}
		} catch (Exception ex) {
			return sendError(ex);
		}
	}

}
