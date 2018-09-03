package org.pipservices.services;

import java.io.InputStream;
import java.util.List;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.process.Inflector;
import org.pipservices.commons.commands.*;
import org.pipservices.commons.errors.ConfigException;
import org.pipservices.commons.refer.IReferences;
import org.pipservices.commons.refer.ReferenceException;
import org.pipservices.components.count.*;
import org.pipservices.commons.run.Parameters;

public class CommandableHttpService extends RestService {
	private ICommandable _controller;	
	
	public CommandableHttpService(String baseRoute) {
		this._route = baseRoute;
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
			String body = "";

			try (InputStream streamReader = request.getEntityStream()) {
				byte[] data = new byte[streamReader.available()];
				streamReader.read(data, 0, data.length);
				body = new String(data, "UTF-8");
			} catch (Exception ex) {
				return sendError(ex);
			}

			Parameters parameters = body == null || body.trim().length() == 0
					? new Parameters() : Parameters.fromJson(body);

			String correlationId = request.getUriInfo().getPathParameters().containsKey("correlation_id")
					? request.getUriInfo().getPathParameters().getFirst("correlation_id")
					: parameters.getAsStringWithDefault("correlation_id", "");

			Timing timing = instrument(correlationId, _route + '.' + command.getName());

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
