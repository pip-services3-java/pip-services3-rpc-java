package org.pipservices3.rpc.services;

/**
 * Interface to perform Swagger registrations.
 */
public interface ISwaggerService {
    /**
     * Perform required Swagger registration steps.
     */
    void registerOpenApiSpec(String baseRoute, String swaggerRoute);
}
