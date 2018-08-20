package org.pipservices.rest;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.pipservices.commons.commands.*;
import org.pipservices.components.count.*;
import org.pipservices.connect.HttpConnectionResolver;
import org.pipservices.commons.refer.ReferenceException;
import org.pipservices.commons.run.Parameters;

public class CommandableHttpService extends RestService {
	
	public CommandableHttpService(String baseRoute)
    {
        this._route = baseRoute;
        _dependencyResolver.put("controller", "none");
    }

	@Override
	public void register() {
		try {
		ICommandable controller = (ICommandable)_dependencyResolver.getOneRequired("controller");
        List<ICommand> commands = controller.getCommandSet().getCommands();

        for (ICommand command : commands)
        {
            registerRoute( "post", command.getName(), (request, response) -> 
            {
                try
                {
                    String body = "";

                    try( InputStream streamReader = request.getEntityStream() ) 
                    {
                    	byte[] data = new byte[streamReader.available()];
                    	streamReader.read(data, 0, data.length);
                        body = new String(data,"UTF-8");
                    }
                    catch(Exception e){}

                    Parameters parameters = HttpConnectionResolver.isNullOrEmpty(body) ? new Parameters() : Parameters.fromJson(body);
                    String correlationId = request.getUriInfo().getPathParameters().containsKey("correlation_id")
                       ? request.getUriInfo().getPathParameters().getFirst("correlation_id")
                       : parameters.getAsStringWithDefault("correlation_id", "");

                    Timing timing = instrument(correlationId, _route + '.' + command.getName());
                    Object result = command.execute(correlationId, parameters);
                    sendResult(response, result);
                }
                catch (Exception ex)
                {
                    try {
						sendError(response, ex);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                }
            });
        }
		} catch (ReferenceException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
}
