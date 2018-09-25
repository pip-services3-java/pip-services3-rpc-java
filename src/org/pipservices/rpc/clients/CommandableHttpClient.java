package org.pipservices.rpc.clients;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

import org.pipservices.commons.errors.*;

public class CommandableHttpClient extends RestClient {

	public CommandableHttpClient(String baseRoute) {
		this._baseRoute = baseRoute;
	}

	public <T> T callCommand(Class<T> type, String route, String correlationId, Object entity)
		throws ApplicationException {
		return execute(type, correlationId, HttpMethod.POST, route, entity);
	}

	public <T> T callCommand(GenericType<T> type, String route, String correlationId, Object entity)
		throws ApplicationException {
		return execute(type, correlationId, HttpMethod.POST, route, entity);
	}
	
}
