package org.pipservices3.rpc.services;

import com.sun.net.httpserver.HttpServer;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.ext.Provider;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.pipservices3.commons.config.ConfigParams;
import org.pipservices3.commons.config.IConfigurable;
import org.pipservices3.commons.convert.JsonConverter;
import org.pipservices3.commons.errors.ApplicationException;
import org.pipservices3.commons.errors.ConfigException;
import org.pipservices3.commons.errors.ConnectionException;
import org.pipservices3.commons.refer.DependencyResolver;
import org.pipservices3.commons.refer.IReferenceable;
import org.pipservices3.commons.refer.IReferences;
import org.pipservices3.commons.refer.ReferenceException;
import org.pipservices3.commons.run.IOpenable;
import org.pipservices3.commons.validate.Schema;
import org.pipservices3.commons.validate.ValidationException;
import org.pipservices3.components.connect.ConnectionParams;
import org.pipservices3.components.count.CompositeCounters;
import org.pipservices3.components.count.CounterTiming;
import org.pipservices3.components.log.CompositeLogger;
import org.pipservices3.rpc.connect.HttpConnectionResolver;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Used for creating HTTP endpoints. An endpoint is a URL, at which a given service can be accessed by a client.
 * <p>
 * ### Configuration parameters ###
 * <p>
 * Parameters to pass to the {@link HttpEndpoint#configure} method for component configuration:
 * <ul>
 * <li>cors_headers - a comma-separated list of allowed CORS headers
 * <li>cors_origins - a comma-separated list of allowed CORS origins
 * <li>connection(s): the connection resolver's connections;
 *     <ul>
 *     <li>"connection.discovery_key" - the key to use for connection resolving in a discovery service;
 *     <li>"connection.protocol" - the connection's protocol;
 *     <li>"connection.host" - the target host;
 *     <li>"connection.port" - the target port;
 *     <li>"connection.uri" - the target URI.
 *     </ul>
 * <li>credential - the HTTPS credentials:
 *     <ul>
 *     <li>"credential.ssl_key_file" - the SSL private key in PEM
 *     <li>"credential.ssl_crt_file" - the SSL certificate in PEM
 *     <li>"credential.ssl_ca_file" - the certificate authorities (root cerfiticates) in PEM
 *     </ul>
 * </ul>
 * <p>
 * ### References ###
 * <p>
 * A logger, counters, and a connection resolver can be referenced by passing the
 * following references to the object's <code>setReferences()</code> method:
 * <ul>
 * <li>logger: <code>"*:logger:*:*:1.0"</code>;
 * <li>counters: <code>"*:counters:*:*:1.0"</code>;
 * <li>discovery: <code>"*:discovery:*:*:1.0"</code> (for the connection resolver).
 * </ul>
 * <p>
 * ### Examples ###
 * <pre>
 * {@code
 *     public MyMethod(String correlationId, ConfigParams _config, IReferences _references) {
 *         HttpEndpoint endpoint = new HttpEndpoint();
 *         if (this._config)
 *             endpoint.configure(this._config);
 *         if (this._references)
 *             endpoint.setReferences(this._references);
 *         ...
 *
 *         this._endpoint.open(correlationId);
 *         ...
 *     }
 * }
 * </pre>
 */
public class HttpEndpoint implements IOpenable, IConfigurable, IReferenceable {

    private static final ConfigParams _defaultConfig = ConfigParams.fromTuples(
            "connection.protocol", "http",
            "connection.host", "0.0.0.0",
            "connection.port", 3000,

            "credential.ssl_key_file", null,
            "credential.ssl_crt_file", null,
            "credential.ssl_ca_file", null,

            "options.maintenance_enabled", false,
            "options.request_max_size", 1024 * 1024,
            "options.connect_timeout", 60000,
            "options.connect_timeout", 60000,
            "options.debug", true);

    protected HttpConnectionResolver _connectionResolver = new HttpConnectionResolver();
    protected CompositeLogger _logger = new CompositeLogger();
    protected CompositeCounters _counters = new CompositeCounters();
    protected DependencyResolver _dependencyResolver = new DependencyResolver(_defaultConfig);

    private String _url;
    private HttpServer _server;
    private ResourceConfig _resources;
    private final List<IRegisterable> _registrations = new ArrayList<>();

    private boolean _protocolUpgradeEnabled = false;
    private boolean _maintenanceEnabled = false;
    private long _fileMaxSize = 200 * 1024 * 1024;

    private List<String> _allowedHeaders = List.of("correlation_id");
    private List<String> _allowedOrigins = new ArrayList<>();

    /**
     * Configures this HttpEndpoint using the given configuration parameters.
     * <p>
     * __Configuration parameters:__
     * - __connection(s)__ - the connection resolver's connections;
     * <p>
     * - "connection.discovery_key" - the key to use for connection resolving in a discovery service;
     * <p>
     * - "connection.protocol" - the connection's protocol;
     * <p>
     * - "connection.host" - the target host;
     * <p>
     * - "connection.port" - the target port;
     * <p>
     * - "connection.uri" - the target URI.
     * <p>
     * - "credential.ssl_key_file" - SSL private key in PEM
     * <p>
     * - "credential.ssl_crt_file" - SSL certificate in PEM
     * <p>
     * - "credential.ssl_ca_file" - Certificate authority (root certificate) in PEM
     *
     * @param config configuration parameters, containing a "connection(s)" section.
     * @see ConfigParams (in the PipServices "Commons" package)
     */
    @Override
    public void configure(ConfigParams config) throws ConfigException {
        config = config.setDefaults(_defaultConfig);
        _connectionResolver.configure(config);

        this._maintenanceEnabled = config.getAsBooleanWithDefault("options.maintenance_enabled", this._maintenanceEnabled);
        this._fileMaxSize = config.getAsLongWithDefault("options.file_max_size", this._fileMaxSize);
        this._protocolUpgradeEnabled = config.getAsBooleanWithDefault("options.protocol_upgrade_enabled", this._protocolUpgradeEnabled);

        var headers = config.getAsStringWithDefault("cors_headers", "").split(",");

        for (var header : headers) {
            header = header.trim();
            if (!header.equals("")) {
                final String finalHeader = header;
                this._allowedHeaders = this._allowedHeaders.stream().filter(h -> !h.equals(finalHeader)).collect(Collectors.toList());
                this._allowedHeaders.add(finalHeader);
            }
        }

        var origins = config.getAsStringWithDefault("cors_origins", "").split(",");
        for (var origin : origins) {
            origin = origin.trim();
            if (!origin.equals("")) {
                final String finalOrigin = origin;
                this._allowedOrigins = this._allowedOrigins.stream().filter(h -> !h.equals(finalOrigin)).collect(Collectors.toList());
                this._allowedOrigins.add(finalOrigin);
            }
        }

        _dependencyResolver.configure(config);

    }

    /**
     * Sets references to this endpoint's logger, counters, and connection resolver.
     * <p>
     * __References:__ - logger: <code>"\*:logger:\*:\*:1.0"</code> - counters:
     * <code>"\*:counters:\*:\*:1.0"</code> - discovery:
     * <code>"\*:discovery:\*:\*:1.0"</code> (for the connection resolver)
     *
     * @param references an IReferences object, containing references to a logger,
     *                   counters, and a connection resolver.
     * @throws ReferenceException when no found references.
     * @see IReferences
     */
    public void setReferences(IReferences references) throws ReferenceException {
        _logger.setReferences(references);
        _counters.setReferences(references);
        _dependencyResolver.setReferences(references);
        _connectionResolver.setReferences(references);
    }

    /**
     * Adds instrumentation to log calls and measure call time. It returns a Timing
     * object that is used to end the time measurement.
     *
     * @param correlationId (optional) transaction id to trace execution through
     *                      call chain.
     * @param name          a method name.
     * @return Timing object to end the time measurement.
     */
    protected CounterTiming Instrument(String correlationId, String name) {
        _logger.trace(correlationId, "Executing {0} method", name);
        return _counters.beginTiming(name + ".exec_time");
    }

    /**
     * Gets an HTTP server instance.
     *
     * @return an HTTP server instance of <code>null</code> if endpoint is closed.
     */
    public HttpServer getServer() {
        return this._server;
    }

    /**
     * Checks if the component is opened.
     *
     * @return whether or not this endpoint is open with an actively listening REST
     * server.
     */
    @Override
    public boolean isOpen() {
        return _server != null;
    }

    /**
     * Opens a connection using the parameters resolved by the referenced connection
     * resolver and creates a REST server (service) using the set options and
     * parameters.
     *
     * @param correlationId (optional) transaction id to trace execution through
     *                      call chain.
     * @throws ApplicationException when error occured.
     */
    @Override
    public void open(String correlationId) throws ApplicationException {
        if (isOpen())
            return;

        SSLContext sslContext = null;
        ConnectionParams connection = _connectionResolver.resolve(correlationId);
        String protocol = connection.getProtocolWithDefault("http");
        String host = connection.getHost();
        int port = connection.getPort();
        URI uri = UriBuilder.fromUri(protocol + "://" + host).port(port).path("/").build();
        _url = uri.toString();

        try {
            _resources = new ResourceConfig();

            if (Objects.equals(connection.getAsStringWithDefault("protocol", "http"), "https")) {
                // TODO check using of certificates
                var sslKeyFile = connection.getAsNullableString("ssl_key_file");
                var privateKey = readCertFile(sslKeyFile);

                var sslCrtFile = connection.getAsNullableString("ssl_crt_file");

                var ca = new ArrayList<>();
                var sslCaFile = connection.getAsNullableString("ssl_ca_file");
                if (sslCaFile != null) {
                    var caText = readCertFile(sslCaFile);
                    while (caText != null && caText.trim().length() > 0) {
                        var crtIndex = caText.lastIndexOf("-----BEGIN CERTIFICATE-----");
                        if (crtIndex > -1) {
                            ca.add(caText.substring(crtIndex));
                            caText = caText.substring(0, crtIndex);
                        }
                    }
                }

                sslContext = SSLContext.getInstance("SSL");

                KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                keyStore.load(null);
                FileInputStream fis = new FileInputStream(sslCrtFile);
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                Certificate cert = cf.generateCertificate(fis);
                keyStore.setCertificateEntry("ssl", cert);

                KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                keyManagerFactory.init(keyStore, privateKey.toCharArray());

                try {
                    sslContext.init(keyManagerFactory.getKeyManagers(), null, null);
                } catch (KeyManagementException e) {
                    throw new IllegalStateException(e);
                }
            }

            performRegistrations();

            if (sslContext != null)
                _server = JdkHttpServerFactory.createHttpServer(uri, _resources, sslContext);
            else
                _server = JdkHttpServerFactory.createHttpServer(uri, _resources);
//			_server.start();

            _logger.info(correlationId, "Opened REST service at %s", _url);
        } catch (Exception ex) {
            _server = null;
            throw new ConnectionException(correlationId, "CANNOT_CONNECT", "Opening HTTP endpoint failed").wrap(ex)
                    .withDetails("url", _url);
        }
    }

    private String readCertFile(String path) throws IOException {
        if (path == null) return null;

        try (FileInputStream fis = new FileInputStream(path)) {
            return new String(fis.readAllBytes(), Charset.defaultCharset());
        }
    }

    /**
     * Closes this endpoint and the REST server (service) that was opened earlier.
     *
     * @param correlationId (optional) transaction id to trace execution through
     *                      call chain.
     */
    @Override
    public void close(String correlationId) {
        if (_server != null) {
            // Eat exceptions
            try {
                _server.stop(0);
                _logger.info(correlationId, "Closed HTTP endpoint at %s", _url);
            } catch (Exception ex) {
                _logger.warn(correlationId, "Failed while closing HTTP endpoint: %s", ex);
            }
            _server = null;
            _resources = null;
            _url = null;
        }
    }

    private void performRegistrations() {
        for (IRegisterable registration : _registrations)
            registration.register();
    }

    /**
     * Registers a registerable object for dynamic endpoint discovery.
     *
     * @param registration the registration to add.
     * @see IRegisterable
     */
    public void register(IRegisterable registration) {
        _registrations.add(registration);
    }

    /**
     * Unregisters a registerable object, so that it is no longer used in dynamic
     * endpoint discovery.
     *
     * @param registration the registration to remove.
     * @see IRegisterable
     */
    public void unregister(IRegisterable registration) {
        _registrations.remove(registration);
    }

    /**
     * Returns correlationId from request
     *
     * @param req -  http request
     * @return Returns correlationId from request
     */
    public String getCorrelationId(ContainerRequestContext req) {
        var correlationId = getQueryParameter(req, "correlation_id");
        if (correlationId == null)
            correlationId = req.getHeaderString("correlation_id");

        return correlationId;
    }

    protected String getQueryParameter(ContainerRequestContext request, String name) {
        String value = null;
        name = URLEncoder.encode(name, StandardCharsets.UTF_8);
        if (request.getUriInfo().getQueryParameters().containsKey(name)) {
            value = request.getUriInfo().getQueryParameters().getFirst(name);
            value = value != null ? URLDecoder.decode(value, StandardCharsets.UTF_8) : null;
        }

        return value;
    }

    /**
     * Registers an action in this objects REST server (service) by the given method
     * and route.
     *
     * @param method the HTTP method of the route.
     * @param route  the route to register in this object's REST server (service).
     * @param action the action to perform at the given route.
     */
    public void registerRoute(String method, String route, Inflector<ContainerRequestContext, Response> action) {
        // Routes cannot start with '/'
        if (route.charAt(0) == '/')
            route = route.substring(1);

        Resource.Builder builder = Resource.builder().addChildResource(route);

        method = method.toUpperCase();

        builder.addMethod(method).handledBy(action);

        Resource resource = builder.build();

        if (_resources != null)
            _resources.registerResources(resource);
    }

    /**
     * Registers an action in this objects REST server (service) by the given method
     * and route.
     *
     * @param method the HTTP method of the route.
     * @param route  the route to register in this object's REST server (service).
     * @param schema the schema to use for parameter validation.
     * @param action the action to perform at the given route.
     */
    public void registerRoute(String method, String route, Schema schema, Inflector<ContainerRequestContext, Response> action) {
        // Hack!!! Wrapping action to preserve prototyping context
        Inflector<ContainerRequestContext, Response> actionCurl = new Inflector<ContainerRequestContext, Response>() {
            @Override
            public Response apply(ContainerRequestContext req) {
                // TODO: maybe replace this on Jersey filters
                if (schema != null) {
                    var params = getAllParams(req);
                    var correlationId = getCorrelationId(req);

                    try {
                        schema.validateAndThrowException(correlationId, params, false);
                    } catch (ValidationException err) {
                        return HttpResponseSender.sendError(err);
                    }
                }

                // Todo: perform verification?
                return action.apply(req);
            }
        };

        this.registerRoute(method, route, actionCurl);
    }

    public void registerRouteWithAuth(String method, String route, Schema schema,
                                      AuthorizeFunction<ContainerRequestContext, Inflector<ContainerRequestContext, Response>, Response> authorize,
                                      Inflector<ContainerRequestContext, Response> action) {

        this.registerRoute(method, route, (req) -> authorize.apply(req, action));
    }

    @Provider
    private static class InterceptorRegister implements ContainerRequestFilter {
        private final Function<ContainerRequestContext, ?> _interceptor;
        private final String _route;

        public InterceptorRegister(Function<ContainerRequestContext, ?> interceptor, String route) {
            _interceptor = interceptor;
            _route = route;
        }

        @Override
        public void filter(ContainerRequestContext ctx) throws IOException {
            Pattern stringPattern = Pattern.compile(_route);
            var matcher = stringPattern.matcher(fixRoute(ctx.getUriInfo().getPath(true)));
            var resMatch = new ArrayList<String>();

            while (matcher.find())
                resMatch.add(matcher.group());

            if (!resMatch.isEmpty() && _route != null && !_route.equals(""))
                _interceptor.apply(ctx);

        }
    }

    private static String fixRoute(String route) {
        if (route != null && route.length() > 0 && !route.startsWith("/")) {
            route = "/" + route;
        }
        return route;
    }


    public void registerInterceptor(String route, Function<ContainerRequestContext, ?> action) {
        route = fixRoute(route);

        _resources.register(new InterceptorRegister(action, route));
    }

    private Map<?, ?> getAllParams(ContainerRequestContext req) {

        var pathParams = req.getUriInfo().getPathParameters();
        var queryParams = req.getUriInfo().getQueryParameters();

        byte[] bodyBytes;
        Map<String, Object> body;

        try {
            bodyBytes = req.getEntityStream().readAllBytes();
            var json = new String(bodyBytes, StandardCharsets.UTF_8);

            body = JsonConverter.toMap(json);

            // return req to the initial state
            req.setEntityStream(new ByteArrayInputStream(bodyBytes));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        var params = new HashMap<>();
        params.put("body", body);


        pathParams.forEach((k, v) -> {
            if (k != null && !k.isEmpty() && v != null && !v.stream().findFirst().orElse("").isEmpty())
                params.put(k, v.stream().findFirst().get());
        });

        queryParams.forEach((k, v) -> {
            if (k != null && !k.isEmpty() && v != null && !v.stream().findFirst().orElse("").isEmpty())
                params.put(k, v.stream().findFirst().get());
        });

        return params;
    }
}
