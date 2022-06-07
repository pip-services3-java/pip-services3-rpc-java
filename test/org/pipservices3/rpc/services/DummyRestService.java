package org.pipservices3.rpc.services;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import org.pipservices3.commons.config.ConfigParams;
import org.pipservices3.commons.convert.JsonConverter;
import org.pipservices3.commons.convert.TypeCode;
import org.pipservices3.commons.data.FilterParams;
import org.pipservices3.commons.data.PagingParams;
import org.pipservices3.commons.errors.ApplicationException;
import org.pipservices3.commons.errors.ConfigException;
import org.pipservices3.commons.refer.Descriptor;
import org.pipservices3.commons.refer.IReferences;
import org.pipservices3.commons.refer.ReferenceException;
import org.pipservices3.commons.validate.FilterParamsSchema;
import org.pipservices3.commons.validate.ObjectSchema;
import org.pipservices3.rpc.Dummy;
import org.pipservices3.rpc.DummySchema;
import org.pipservices3.rpc.IDummyController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class DummyRestService extends RestService {
    private IDummyController _controller;
    private int _numberOfCalls = 0;
    private String _swaggerContent;
    private String _swaggerPath;

    public DummyRestService() {
        this._dependencyResolver.put("controller", new Descriptor("pip-services-dummies", "controller", "default", "*", "*"));
    }

    @Override
    public void configure(ConfigParams config) throws ConfigException {
        super.configure(config);

        this._swaggerContent = config.getAsNullableString("swagger.content");
        this._swaggerPath = config.getAsNullableString("swagger.path");
    }

    public void setReferences(IReferences references) throws ReferenceException, ConfigException {
        super.setReferences(references);
        this._controller = this._dependencyResolver.getOneRequired(IDummyController.class, "controller");
    }

    public int getNumberOfCalls() {
        return this._numberOfCalls;
    }

    public Object incrementNumberOfCalls(ContainerRequestContext req) {
        this._numberOfCalls++;
        return null;
    }

    private Response getPageByFilter(ContainerRequestContext req) {
        try {
            var res = this._controller.getPageByFilter(
                    this.getCorrelationId(req),
                    new FilterParams(req.getUriInfo().getPathParameters()),
                    PagingParams.fromValue(req.getUriInfo().getPathParameters())
            );
            return this.sendResult(res);
        } catch (ApplicationException err) {
            return sendError(err);
        }

    }

    private Response getOneById(ContainerRequestContext req) {
        var res = this._controller.getOneById(
                this.getCorrelationId(req),
                req.getUriInfo().getPathParameters().get("dummy_id").get(0)
        );
        return this.sendResult(res);
    }

    private Response create(ContainerRequestContext req) {
        try {
            var res = this._controller.create(
                    this.getCorrelationId(req),
                    JsonConverter.fromJson(
                            Dummy.class,
                            new String(req.getEntityStream().readAllBytes(), StandardCharsets.UTF_8)
                    )
            );
            return this.sendCreatedResult(res);
        } catch (IOException err) {
            return sendError(err);
        }
    }

    private Response update(ContainerRequestContext req) {
        try {
            var res = this._controller.update(
                    this.getCorrelationId(req),
                    JsonConverter.fromJson(
                            Dummy.class,
                            new String(req.getEntityStream().readAllBytes(), StandardCharsets.UTF_8)
                    )
            );
            return this.sendResult(res);
        } catch (IOException err) {
            return sendError(err);
        }
    }

    private Response deleteById(ContainerRequestContext req) {
        var res = this._controller.deleteById(
                this.getCorrelationId(req),
                req.getUriInfo().getPathParameters().get("dummy_id").stream().findFirst().get()
        );
        return this.sendDeletedResult(res);
    }

    private Response checkCorrelationId(ContainerRequestContext req) {
        var result = this._controller.checkCorrelationId(this.getCorrelationId(req));
        return this.sendResult(Map.of("correlation_id", result));
    }

    private Response raiseException(ContainerRequestContext req) {
        try {
            this._controller.raiseException(this.getCorrelationId(req));
            return sendEmptyResult();
        } catch (Exception ex) {
            return sendError(ex);
        }
    }

    @Override
    public void register() {
        this.registerInterceptor("/dummies$", this::incrementNumberOfCalls);

        this.registerRoute(
                HttpMethod.GET, "/dummies",
                new ObjectSchema()
                        .withOptionalProperty("skip", TypeCode.String)
                        .withOptionalProperty("take", TypeCode.String)
                        .withOptionalProperty("total", TypeCode.String)
                        .withOptionalProperty("body", new FilterParamsSchema()),
                this::getPageByFilter
        );

        this.registerRoute(
                HttpMethod.GET, "/dummies/check/correlation_id",
                new ObjectSchema(),
                this::checkCorrelationId
        );

        this.registerRoute(
                HttpMethod.GET, "/dummies/{dummy_id}",
                new ObjectSchema()
                        .withRequiredProperty("dummy_id", TypeCode.String),
                this::getOneById
        );

        this.registerRoute(
                HttpMethod.POST, "/dummies",
                new ObjectSchema()
                        .withRequiredProperty("body", new DummySchema()),
                this::create
        );

        this.registerRoute(
                HttpMethod.PUT, "/dummies",
                new ObjectSchema()
                        .withRequiredProperty("body", new DummySchema()),
                this::update
        );

        this.registerRoute(
                HttpMethod.DELETE, "/dummies/{dummy_id}",
                new ObjectSchema()
                        .withRequiredProperty("dummy_id", TypeCode.String),
                this::deleteById
        );

        this.registerRoute(
                HttpMethod.POST, "/dummies/raise_exception",
                new ObjectSchema(),
                this::raiseException
        );

        if (this._swaggerContent != null)
            this.registerOpenApiSpec(this._swaggerContent);

        if (this._swaggerPath != null)
            this.registerOpenApiSpecFromFile(this._swaggerPath);
    }


}
