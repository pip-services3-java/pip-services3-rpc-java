package org.pipservices.rest;

import java.io.IOException;

import org.glassfish.jersey.server.ContainerResponse;
import org.pipservices.commons.convert.JsonConverter;
import org.pipservices.commons.errors.ApplicationException;
import org.pipservices.commons.errors.ErrorDescriptionFactory;

public class HttpResponseSender {
	
	private final static int INTERNAL_SERVER_ERROR = 500;
	private final static int NO_CONTENT = 204;
	private final static int CREATED = 201;
	private final static int OK = 200;
	
	public static void sendError(ContainerResponse response, Exception ex) throws IOException
    {
        // Unwrap exception
        if (ex instanceof RuntimeException)
        {
        	if( ex.getStackTrace().length > 0 )
        	{
                ex.setStackTrace( new StackTraceElement[]{ ex.getStackTrace()[0]} ); 
        	}
        }

        if (ex instanceof ApplicationException )
        {
            response.getHeaders().putSingle("Content-Type", "application/json" );
        	ApplicationException ex3 = (ApplicationException)ex;
            response.setStatus( ex3.getStatus() );
            String contentResult = JsonConverter.toJson( ErrorDescriptionFactory.create(ex3));
            response.getEntityStream().write( contentResult.getBytes() );
        }
        else
        {
            response.getHeaders().putSingle("Content-Type", "application/json" );
            response.setStatus( INTERNAL_SERVER_ERROR );
            String contentResult = JsonConverter.toJson( ErrorDescriptionFactory.create(ex,""));
            response.getEntityStream().write( contentResult.getBytes() );
        }
    }

    public static void sendResult(ContainerResponse response, Object result) throws IOException
    {
        if (result == null)
        {
        	response.setStatus( NO_CONTENT );
        }
        else
        {
        	response.getHeaders().putSingle("Content-Type", "application/json" );
            response.setStatus( OK );
            String contentResult = JsonConverter.toJson( result);
            response.getEntityStream().write( contentResult.getBytes() );
        }
    }

    public static void sendEmptyResult(ContainerResponse response)
    {
    	response.getHeaders().putSingle("Content-Type", "application/json" );
        response.setStatus( NO_CONTENT );
    }

    public static void sendCreatedResult(ContainerResponse response, Object result) throws IOException
    {
        if (result == null)
        {
        	response.setStatus( NO_CONTENT );
        }
        else
        {
        	response.getHeaders().putSingle("Content-Type", "application/json" );
            response.setStatus( CREATED );
            String contentResult = JsonConverter.toJson( result);
            response.getEntityStream().write( contentResult.getBytes() );
        }
    }

    public static void sendDeletedResult(ContainerResponse response, Object result) throws IOException
    {
        if (result == null)
        {
        	response.setStatus( NO_CONTENT );
        }
        else
        {
        	response.getHeaders().putSingle("Content-Type", "application/json" );
            response.setStatus( OK );
            String contentResult = JsonConverter.toJson( result);
            response.getEntityStream().write( contentResult.getBytes() );
        }
    }
}
