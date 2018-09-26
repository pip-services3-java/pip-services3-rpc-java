package org.pipservices.rpc.services;

import java.time.ZonedDateTime;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.process.Inflector;
import org.pipservices.commons.config.ConfigParams;
import org.pipservices.commons.convert.StringConverter;
import org.pipservices.commons.errors.ConfigException;

/**
 * Service returns heartbeat via HTTP/REST protocol.
 * 
 * The service responds on /heartbeat route (can be changed)
 * with a string with the current time in UTC.
 * 
 * This service route can be used to health checks by loadbalancers and 
 * container orchestrators.
 * 
 * ### Configuration parameters ###
 * 
 * base_route:              base route for remote URI (default: "")
 * route:                   route to heartbeat operation (default: "heartbeat")
 * dependencies:
 *   endpoint:              override for HTTP Endpoint dependency
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
 * - *:endpoint:http:*:1.0          (optional) [[HttpEndpoint]] reference
 * <p>
 * ### Example ###
 * <pre>
 * {@code
 * HeartbeatService service = new HeartbeatService();
 * service.configure(ConfigParams.fromTuples(
 *     "route", "ping",
 *     "connection.protocol", "http",
 *     "connection.host", "localhost",
 *     "connection.port", 8080
 * ));
 * 
 * service.open("123");
 * System.out.println("The Heartbeat service is accessible at http://+:8080/ping");
 * }
 * </pre>
 * @see RestService
 */
public class HeartbeatRestService extends RestService {
	private String _route = "heartbeat";

	/**
	 * Creates a new instance of this service.
	 */
	public HeartbeatRestService() {
		super();
	}

	/**
	 * Configures component by passing configuration parameters.
	 * 
	 * @param config configuration parameters to be set.
	 * @throws ConfigException when configuration is wrong.
	 */
	public void configure(ConfigParams config) throws ConfigException {
		super.configure(config);

		_route = config.getAsStringWithDefault("route", _route);
	}

	/**
	 * Registers all service routes in HTTP endpoint.
	 */
	public void register() {
		registerRoute("get", _route, new Inflector<ContainerRequestContext, Response>() {
			@Override
			public Response apply(ContainerRequestContext request) {
				return heartbeat(request);
			}
		});
	}

	/**
	 * Handles heartbeat requests
	 * 
	 * @param request an HTTP request
	 * @return http response to the request.
	 */
	private Response heartbeat(ContainerRequestContext request) {
		String result = StringConverter.toString(ZonedDateTime.now());
		return sendResult(result);
	}
}
