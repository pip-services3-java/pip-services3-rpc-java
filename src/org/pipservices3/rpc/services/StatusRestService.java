package org.pipservices3.rpc.services;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.process.Inflector;
import org.pipservices3.commons.config.ConfigParams;
import org.pipservices3.commons.convert.StringConverter;
import org.pipservices3.commons.data.StringValueMap;
import org.pipservices3.commons.errors.ConfigException;
import org.pipservices3.commons.refer.Descriptor;
import org.pipservices3.commons.refer.IReferences;
import org.pipservices3.commons.refer.ReferenceException;
import org.pipservices3.commons.run.Parameters;
import org.pipservices3.components.info.ContextInfo;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service that returns microservice status information via HTTP/REST protocol.
 * <p>
 * The service responds on /status route (can be changed) with a JSON object:
 * <p>
 * {
 * <ul>
 *     <li>"id":            unique container id (usually hostname)
 *     <li>"name":          container name (from ContextInfo)
 *     <li>"description":   container description (from ContextInfo)
 *     <li>"start_time":    time when container was started
 *     <li>"current_time":  current time in UTC
 *     <li>"uptime":        duration since container start time in milliseconds
 *     <li>"properties":    additional container properties (from ContextInfo)
 *     <li>"components":    descriptors of components registered in the container
 * </ul>
 * <p>
 * }
 * <p>
 * ### Configuration parameters ###
 * <ul>
 * <li>base_route:              base route for remote URI
 * <li>route:                   status route (default: "status")
 * <li>dependencies:
 *   <ul>
 *   <li>endpoint:              override for HTTP Endpoint dependency
 *   <li>controller:            override for Controller dependency
 *   </ul>
 * <li>connection(s):
 *   <ul>
 *   <li>discovery_key:         (optional) a key to retrieve the connection from IDiscovery
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
 * StatusService service = new StatusService();
 * service.configure(ConfigParams.fromTuples(
 *     "connection.protocol", "http",
 *     "connection.host", "localhost",
 *     "connection.port", 8080
 * ));
 *
 * service.open("123");
 * System.out.println("The Status service is accessible at http://+:8080/status");
 * }
 * </pre>
 *
 * @see RestService
 */
public class StatusRestService extends RestService {
    private final ZonedDateTime _startTime = ZonedDateTime.now();
    private IReferences _references;
    private ContextInfo _contextInfo;
    private String _route = "status";

    /**
     * Creates a new instance of this service.
     */
    public StatusRestService() {
        _dependencyResolver.put("context-info", new Descriptor("pip-services", "context-info", "default", "*", "1.0"));
    }

    /**
     * Configures component by passing configuration parameters.
     *
     * @param config configuration parameters to be set.
     * @throws ConfigException when configuration is wrong.
     */
    @Override
    public void configure(ConfigParams config) throws ConfigException {
        super.configure(config);

        _route = config.getAsStringWithDefault("route", _route);
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
        _references = references;
        super.setReferences(references);

        _contextInfo = (ContextInfo) _dependencyResolver.getOneOptional("context-info");
    }

    /**
     * Registers all service routes in HTTP endpoint.
     */
    @Override
    public void register() {
        registerRoute(HttpMethod.GET, _route, new Inflector<ContainerRequestContext, Response>() {
            @Override
            public Response apply(ContainerRequestContext request) {
                return status(request);
            }
        });
    }

    /**
     * Handles status requests
     *
     * @param request an HTTP request
     * @return res   an HTTP response
     */
    private Response status(ContainerRequestContext request) {
        String id = _contextInfo != null ? _contextInfo.getContextId() : "";
        String name = _contextInfo != null ? _contextInfo.getName() : "Unknown";
        String description = _contextInfo != null ? _contextInfo.getDescription() : "";
        long uptime = Duration.between(_startTime, ZonedDateTime.now()).toMillis();
        StringValueMap properties = _contextInfo != null ? _contextInfo.getProperties() : null;

        List<String> components = new ArrayList<>();
        if (_references != null) {
            for (Object locator : _references.getAllLocators())
                components.add(locator.toString());
        }

        Parameters status = Parameters.fromTuples("id", id, "name", name, "description", description, "start_time",
                StringConverter.toString(_startTime), "current_time", StringConverter.toString(ZonedDateTime.now()),
                "uptime", uptime, "properties", properties, "components", components);

        return sendResult(status);
    }

}
