package org.pipservices3.rpc.services;

import java.time.ZonedDateTime;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.process.Inflector;
import org.pipservices3.commons.config.ConfigParams;
import org.pipservices3.commons.convert.StringConverter;
import org.pipservices3.commons.errors.ConfigException;

/**
 * Service returns heartbeat via HTTP/REST protocol.
 * <p>
 * The service responds on /heartbeat route (can be changed)
 * with a string with the current time in UTC.
 * <p>
 * This service route can be used to health checks by loadbalancers and 
 * container orchestrators.
 * <p>
 * ### Configuration parameters ###
 * <ul>
 * <li>base_route:              base route for remote URI (default: "")
 * <li>route:                   route to heartbeat operation (default: "heartbeat")
 * <li>dependencies:
 *   <ul>
 *   <li>endpoint:              override for HTTP Endpoint dependency
 *   </ul>
 * <li>connection(s):           
 *   <ul>
 *   <li>discovery_key:         (optional) a key to retrieve the connection from <a href="https://pip-services3-java.github.io/pip-services3-components-java/org/pipservices3/components/connect/IDiscovery.html">IDiscovery</a>
 *   <li>protocol:              connection protocol: http or https
 *   <li>host:                  host name or IP address
 *   <li>port:                  port number
 *   <li>uri:                   resource URI or connection string with all parameters in it
 *   </ul>
 * </ul>
 * <p>
 * ### References ###
 * <ul>
 * <li>*:logger:*:*:1.0               (optional) <a href="https://pip-services3-java.github.io/pip-services3-components-java/org/pipservices3/components/log/ILogger.html">ILogger</a> components to pass log messages
 * <li>*:counters:*:*:1.0             (optional) <a href="https://pip-services3-java.github.io/pip-services3-components-java/org/pipservices3/components/count/ICounters.html">ICounters</a> components to pass collected measurements
 * <li>*:discovery:*:*:1.0            (optional) <a href="https://pip-services3-java.github.io/pip-services3-components-java/org/pipservices3/components/connect/IDiscovery.html">IDiscovery</a> services to resolve connection
 * <li>*:endpoint:http:*:1.0          (optional) {@link HttpEndpoint} reference
 * </ul>
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
	 * @param request a HTTP request
	 * @return http response to the request.
	 */
	private Response heartbeat(ContainerRequestContext request) {
		String result = StringConverter.toString(ZonedDateTime.now());
		return sendResult(result);
	}
}
