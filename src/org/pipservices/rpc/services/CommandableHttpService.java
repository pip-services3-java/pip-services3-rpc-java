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
 * to operations automatically generated for commands defined in ICommandable components.
 * Each command is exposed as POST operation that receives all parameters in body object.
 * 
 * Commandable services require only 3 lines of code to implement a robust external
 * HTTP-based remote interface.
 * 
 * ### Configuration parameters ###
 * 
 * base_route:              base route for remote URI
 * dependencies:
 *   endpoint:              override for HTTP Endpoint dependency
 *   controller:            override for Controller dependency
 * connection(s):           
 *   discovery_key:         (optional) a key to retrieve the connection from IDiscovery
 *   protocol:              connection protocol: http or https
 *   host:                  host name or IP address
 *   port:                  port number
 *   uri:                   resource URI or connection string with all parameters in it
 * 
 * ### References ###
 * 
 * - *:logger:*:*:1.0               (optional) ILogger components to pass log messages
 * - *:counters:*:*:1.0             (optional) ICounters components to pass collected measurements
 * - *:discovery:*:*:1.0            (optional) IDiscovery services to resolve connection
 * - *:endpoint:http:*:1.0          (optional) HttpEndpoint reference
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
