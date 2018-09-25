package org.pipservices.rpc.services;

import java.util.*;

import javax.ws.rs.container.*;
import javax.ws.rs.core.*;

import org.glassfish.jersey.process.*;
import org.pipservices.commons.commands.*;
import org.pipservices.commons.errors.*;
import org.pipservices.commons.refer.*;
import org.pipservices.components.count.*;
import org.pipservices.commons.run.*;

public class CommandableHttpService extends RestService {
	private ICommandable _controller;	
	
	public CommandableHttpService(String baseRoute) {
		this._baseRoute = baseRoute;
		_dependencyResolver.put("controller", "none");
	}

	@Override
	public void setReferences(IReferences references) throws ReferenceException, ConfigException {
		super.setReferences(references);

		_controller = (ICommandable) _dependencyResolver.getOneRequired("controller");
	}

	@Override
	public void register() {
		if (_controller == null) return;
		
		List<ICommand> commands = _controller.getCommandSet().getCommands();

		for (ICommand command : commands) {
			registerRoute("post", command.getName(), new Inflector<ContainerRequestContext, Response>() {
				@Override
	            public Response apply(ContainerRequestContext request) {
	            	return executeCommand(command, request);
	            }
			});
		}
	}

	private Response executeCommand(ICommand command, ContainerRequestContext request) {
		try {
			String json = getBodyAsString(request);

			Parameters parameters = json == null
					? new Parameters() : Parameters.fromJson(json);

			String correlationId = getQueryParameter(request, "correlation_id");

			Timing timing = instrument(correlationId, _baseRoute + '.' + command.getName());

			try {
				Object result = command.execute(correlationId, parameters);
				return sendResult(result);
			} finally {
				timing.endTiming();				
			}
		} catch (Exception ex) {
			return sendError(ex);
		}
	}
	
}
