package org.pipservices.status;

import java.io.IOException;
import java.time.ZonedDateTime;

import org.pipservices.commons.config.ConfigParams;
import org.pipservices.commons.errors.ConfigException;
import org.pipservices.rest.RestService;

public class HeartbeatRestService extends RestService{
	
	 private String _route = "heartbeat";

     public HeartbeatRestService()
     {
     }

     public void configure(ConfigParams config) throws ConfigException
     {
         super.configure(config);

         _route = config.getAsStringWithDefault("route", _route);
     }


     public void register()
     {
         registerRoute( "get", _route, (request, response) -> {
			try {
				sendResult(response, ZonedDateTime.now());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} ); 
     }
}
