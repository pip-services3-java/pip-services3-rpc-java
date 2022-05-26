package org.pipservices3.rpc.test;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.GenericType;
import org.pipservices3.commons.errors.ApplicationException;
import org.pipservices3.rpc.clients.CommandableHttpClient;

public class TestCommandableHttpClient extends CommandableHttpClient {
    /**
     * Creates a new instance of the client.
     *
     * @param baseRoute a base route for remote service.
     */
    public TestCommandableHttpClient(String baseRoute) {
        super(baseRoute);
    }

    /**
     * Calls a remote method via HTTP commadable protocol. The call is made via POST
     * operation and all parameters are sent in body object. The complete route to
     * remote method is defined as baseRoute + "/" + name.
     *
     * @param type          the class type.
     * @param route         a name of the command to call.
     * @param correlationId (optional) transaction id to trace execution through
     *                      call chain.
     * @param entity        body object.
     * @return result of the command.
     * @throws ApplicationException when error occured.
     */
    @Override
    public <T> T callCommand(Class<T> type, String route, String correlationId, Object entity)
            throws ApplicationException {
        return super.callCommand(type, route, correlationId, entity);
    }

    /**
     * Calls a remote method via HTTP commadable protocol. The call is made via POST
     * operation and all parameters are sent in body object. The complete route to
     * remote method is defined as baseRoute + "/" + name.
     *
     * @param type          the generic class type.
     * @param route         a name of the command to call.
     * @param correlationId (optional) transaction id to trace execution through
     *                      call chain.
     * @param entity        body object.
     * @return result of the command.
     * @throws ApplicationException when error occured.
     */
    @Override
    public <T> T callCommand(GenericType<T> type, String route, String correlationId, Object entity)
            throws ApplicationException {
        return super.callCommand(type, route, correlationId, entity);
    }
}
