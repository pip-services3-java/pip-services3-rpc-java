package org.pipservices3.rpc.services;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.process.Inflector;
import org.pipservices3.commons.commands.CommandSet;
import org.pipservices3.commons.commands.ICommand;
import org.pipservices3.commons.commands.ICommandable;
import org.pipservices3.commons.config.ConfigParams;
import org.pipservices3.commons.errors.ConfigException;
import org.pipservices3.commons.refer.IReferences;
import org.pipservices3.commons.refer.ReferenceException;
import org.pipservices3.commons.run.Parameters;

/**
 * Abstract service that receives remove calls via HTTP/REST protocol
 * to operations automatically generated for commands defined in <a href="https://pip-services3-java.github.io/pip-services3-components-java/org/pipservices3/commons/commands/ICommandable.html">ICommandable</a> components.
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
 * <li>*:logger:*:*:1.0           (optional) <a href="https://pip-services3-java.github.io/pip-services3-components-java/org/pipservices3/components/log/ILogger.html">ILogger</a> components to pass log messages
 * <li>*:counters:*:*:1.0         (optional) <a href="https://pip-services3-java.github.io/pip-services3-components-java/org/pipservices3/components/count/ICounters.html">ICounters</a> components to pass collected measurements
 * <li>*:discovery:*:*:1.0        (optional) <a href="https://pip-services3-java.github.io/pip-services3-components-java/org/pipservices3/components/connect/IDiscovery.html">IDiscovery</a> services to resolve connection
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
 *
 * @see RestService
 */
public class CommandableHttpService extends RestService {
    private ICommandable _controller;
    protected CommandSet _commandSet;
    protected boolean _swaggerAuto = true;

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
     * Configures component by passing configuration parameters.
     *
     * @param config configuration parameters to be set.
     */
    public void configure(ConfigParams config) throws ConfigException {
        super.configure(config);

        this._swaggerAuto = config.getAsBooleanWithDefault("swagger.auto", this._swaggerAuto);
    }

    /**
     * Registers all service routes in HTTP endpoint.
     */
    @Override
    public void register() {
        if (_controller == null)
            return;

        _commandSet = _controller.getCommandSet();
        var commands = _commandSet.getCommands();

        for (ICommand command : commands) {
            registerRoute(HttpMethod.POST, command.getName(), new Inflector<ContainerRequestContext, Response>() {
                @Override
                public Response apply(ContainerRequestContext request) {
                    return executeCommand(command, request);
                }
            });
        }

        if (this._swaggerAuto) {
            var swaggerConfig = this._config.getSection("swagger");
            var doc = new CommandableSwaggerDocument(this._baseRoute, swaggerConfig, commands);
            this.registerOpenApiSpec(doc.toString());
        }
    }

    private Response executeCommand(ICommand command, ContainerRequestContext request) {
        var correlationId = this.getCorrelationId(request);
        InstrumentTiming timing = instrument(correlationId, _baseRoute + '.' + command.getName());

        try {
            String json = getBodyAsString(request);

            Parameters parameters = json == null ? new Parameters() : Parameters.fromJson(json);

            Object result = command.execute(correlationId, parameters);
            return sendResult(result);
        } catch (Exception ex) {
            timing.endFailure(ex);
            return sendError(ex);
        } finally {
            timing.endTiming();
        }
    }

}
