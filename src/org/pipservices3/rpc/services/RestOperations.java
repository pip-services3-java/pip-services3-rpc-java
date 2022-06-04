package org.pipservices3.rpc.services;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import org.pipservices3.commons.config.ConfigParams;
import org.pipservices3.commons.config.IConfigurable;
import org.pipservices3.commons.data.FilterParams;
import org.pipservices3.commons.data.PagingParams;
import org.pipservices3.commons.errors.*;
import org.pipservices3.commons.refer.DependencyResolver;
import org.pipservices3.commons.refer.IReferenceable;
import org.pipservices3.commons.refer.IReferences;
import org.pipservices3.commons.refer.ReferenceException;
import org.pipservices3.components.count.CompositeCounters;
import org.pipservices3.components.log.CompositeLogger;

import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public abstract class RestOperations implements IConfigurable, IReferenceable {
    protected CompositeLogger _logger = new CompositeLogger();
    protected CompositeCounters _counters = new CompositeCounters();
    protected DependencyResolver _dependencyResolver = new DependencyResolver();

    @Override
    public void configure(ConfigParams config) throws ConfigException {
        this._dependencyResolver.configure(config);
    }

    @Override
    public void setReferences(IReferences references) throws ReferenceException, ConfigException {
        this._logger.setReferences(references);
        this._counters.setReferences(references);
        this._dependencyResolver.setReferences(references);
    }

    /**
     * Returns correlationId from request
     *
     * @param req -  http request
     * @return Returns correlationId from request
     */
    protected String getCorrelationId(ContainerRequestContext req) {
        var correlationId = getQueryParameter(req, "correlation_id");
        if (correlationId == null || correlationId.equals("")) {
            correlationId = req.getHeaderString("correlation_id");
        }
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

    protected FilterParams getFilterParams(ContainerRequestContext req) {
        var value = new HashMap<>(req.getUriInfo().getQueryParameters());
        value.remove("skip");
        value.remove("take");
        value.remove("total");
        value.remove("correlation_id");

        return FilterParams.fromValue(value);
    }

    protected PagingParams getPagingParams(ContainerRequestContext req) {
        var params = req.getUriInfo().getQueryParameters();
        var value = Map.of(
                "skip", params.getFirst("skip"),
                "take", params.getFirst("take"),
                "total", params.getFirst("total")
        );

        return PagingParams.fromValue(value);
    }

    protected Response sendResult(Object result) {
        return HttpResponseSender.sendResult(result);
    }

    protected Response sendEmptyResult() {
        return HttpResponseSender.sendEmptyResult();
    }

    protected Response sendCreatedResult(Object result) {
        return HttpResponseSender.sendCreatedResult(result);
    }

    protected Response sendDeletedResult(Object result) {
        return HttpResponseSender.sendDeletedResult(result);
    }

    protected Response sendError(Exception error) {
        return HttpResponseSender.sendError(error);
    }

    protected Response sendBadRequest(ContainerRequestContext req, String message) {
        var correlationId = this.getCorrelationId(req);
        var error = new BadRequestException(correlationId, "BAD_REQUEST", message);
        return this.sendError(error);
    }

    protected Response sendUnauthorized(ContainerRequestContext req, String message) {
        var correlationId = this.getCorrelationId(req);
        var error = new UnauthorizedException(correlationId, "UNAUTHORIZED", message);
        return this.sendError(error);
    }

    protected Response sendNotFound(ContainerRequestContext req, String message) {
        var correlationId = this.getCorrelationId(req);
        var error = new NotFoundException(correlationId, "NOT_FOUND", message);
        return this.sendError(error);
    }

    protected Response sendConflict(ContainerRequestContext req, String message) {
        var correlationId = this.getCorrelationId(req);
        var error = new ConflictException(correlationId, "CONFLICT", message);
        return this.sendError(error);
    }

    protected Response sendSessionExpired(ContainerRequestContext req, String message) {
        var correlationId = this.getCorrelationId(req);
        var error = new UnknownException(correlationId, "SESSION_EXPIRED", message);
        error.setStatus(440);
        return this.sendError(error);
    }

    protected Response sendInternalError(ContainerRequestContext req, String message) {
        var correlationId = this.getCorrelationId(req);
        var error = new UnknownException(correlationId, "INTERNAL", message);
        return this.sendError(error);
    }

    protected Response sendServerUnavailable(ContainerRequestContext req, String message) {
        var correlationId = this.getCorrelationId(req);
        var error = new ConflictException(correlationId, "SERVER_UNAVAILABLE", message);
        error.setStatus(503);
        return this.sendError(error);
    }

    public Method invoke(String operation) {
        Method func = null;
        Method[] methods = this.getClass().getMethods();
        for (var method : methods) {
            if (method.getName().equals(operation)) {
                func = method;
                break;
            }
        }

        return func;
    }
}
