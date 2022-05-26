package org.pipservices3.rpc.test;

import jakarta.ws.rs.core.GenericType;
import org.pipservices3.commons.errors.ApplicationException;
import org.pipservices3.rpc.clients.RestClient;

/**
 * REST client used for automated testing.
 */
public class TestRestClient extends RestClient {
    public TestRestClient(String baseRoute) {
        this._baseRoute = baseRoute;
    }

    /**
     * Executes a remote method via HTTP/REST protocol.
     *
     * @param type          the class type of data.
     * @param correlationId (optional) transaction id to trace execution through
     *                      call chain.
     * @param method        HTTP method: "get", "head", "post", "put", "delete"
     * @param route         a command route. Base route will be added to this route
     * @param requestEntity request body object.
     * @return result object.
     * @throws ApplicationException when error occured.
     */
    protected <T> T call(Class<T> type, String correlationId, String method, String route, Object requestEntity)
            throws ApplicationException {

        return super.call(type, correlationId, method, route, requestEntity);
    }

    /**
     * Executes a remote method via HTTP/REST protocol.
     *
     * @param type          the generic class type of data.
     * @param correlationId (optional) transaction id to trace execution through
     *                      call chain.
     * @param method        HTTP method: "get", "head", "post", "put", "delete"
     * @param route         a command route. Base route will be added to this route
     * @param requestEntity request body object.
     * @return result object.
     * @throws ApplicationException when error occured.
     */
    protected <T> T call(GenericType<T> type, String correlationId, String method, String route,
                         Object requestEntity) throws ApplicationException {

        return super.call(type, correlationId, method, route, requestEntity);
    }
}
