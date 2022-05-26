package org.pipservices3.rpc.services;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import org.pipservices3.commons.convert.StringConverter;
import org.pipservices3.commons.errors.ConfigException;
import org.pipservices3.commons.refer.Descriptor;
import org.pipservices3.commons.refer.IReferences;
import org.pipservices3.commons.refer.ReferenceException;
import org.pipservices3.components.info.ContextInfo;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Function;

public class StatusOperations extends RestOperations {
    private final ZonedDateTime _startTime = ZonedDateTime.now();
    private IReferences _references2;
    private ContextInfo _contextInfo;

    public StatusOperations() {
        this._dependencyResolver.put("context-info", new Descriptor("pip-services", "context-info", "default", "*", "1.0"));
    }

    /**
     * Sets references to dependent components.
     *
     * @param references references to locate the component dependencies.
     */
    public void setReferences(IReferences references) throws ReferenceException, ConfigException {
        this._references2 = references;
        super.setReferences(references);

        this._contextInfo = this._dependencyResolver.getOneOptional(ContextInfo.class, "context-info");
    }

    public Function<ContainerRequestContext, Response> getStatusOperation() {
        return this::status;
    }

    /**
     * Handles status requests
     *
     * @param req an HTTP request
     * @return an HTTP response
     */
    public Response status(ContainerRequestContext req) {
        var id = this._contextInfo != null ? this._contextInfo.getContextId() : "";
        var name = this._contextInfo != null ? this._contextInfo.getName() : "Unknown";
        var description = this._contextInfo != null ? this._contextInfo.getDescription() : "";
        var uptime = System.currentTimeMillis() - this._startTime.toInstant().toEpochMilli();
        var properties = this._contextInfo != null ? this._contextInfo.getProperties() : "";

        var components = new ArrayList<>();
        if (this._references2 != null) {
            for (var locator : this._references2.getAllLocators())
                components.add(locator.toString());
        }

        var status = Map.of(
                "id", id,
                "name", name,
                "description", description,
                "start_time", StringConverter.toString(this._startTime),
                "current_time", StringConverter.toString(ZonedDateTime.now()),
                "uptime", uptime,
                "properties", properties,
                "components", components
        );

        return this.sendResult(status);
    }
}
