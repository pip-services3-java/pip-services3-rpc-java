package org.pipservices3.rpc.services;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.pipservices3.commons.errors.ApplicationException;
import org.pipservices3.commons.errors.ErrorDescription;
import org.pipservices3.commons.errors.ErrorDescriptionFactory;

/**
 * Helper class that handles HTTP-based responses.
 */
public class HttpResponseSender {
	private final static int INTERNAL_SERVER_ERROR = 500;
	private final static int NO_CONTENT = 204;
	private final static int CREATED = 201;
	private final static int OK = 200;

	/**
	 * Sends error serialized as ErrorDescription object and appropriate HTTP status
	 * code. If status code is not defined, it uses 500 status code.
	 * 
	 * @param ex an error object to be sent.
	 * @return HTTP response status
	 */
	public static Response sendError(Exception ex) {
		// Unwrap exception
		if (ex instanceof RuntimeException) {
			if (ex.getStackTrace().length > 0) {
				ex.setStackTrace(new StackTraceElement[] { ex.getStackTrace()[0] });
			}
		}

		try {
			if (ex instanceof ApplicationException) {
				ApplicationException ex3 = (ApplicationException) ex;
				ErrorDescription errorDesc3 = ErrorDescriptionFactory.create(ex3);
				return Response.status(ex3.getStatus()).type(MediaType.APPLICATION_JSON).entity(errorDesc3).build();
			} else {
				ErrorDescription errorDesc = ErrorDescriptionFactory.create(ex, null);
				return Response.status(INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON).entity(errorDesc)
						.build();
			}
		} catch (Exception ex2) {
			return Response.status(INTERNAL_SERVER_ERROR).build();
		}
	}

	/**
	 * Creates a callback function that sends result as JSON object. That callack
	 * function call be called directly or passed as a parameter to business logic
	 * components.
	 * 
	 * If object is not null it returns 200 status code. For null results it returns
	 * 204 status code. If error occur it sends ErrorDescription with approproate
	 * status code.
	 * 
	 * @param result a body object to result.
	 * @return execution result.
	 */
	public static Response sendResult(Object result) {
		try {
			if (result == null) {
				return Response.status(NO_CONTENT).build();
			} else {
				return Response.status(OK).type(MediaType.APPLICATION_JSON).entity(result).build();
			}
		} catch (Exception ex2) {
			return Response.status(INTERNAL_SERVER_ERROR).build();
		}
	}

	/**
	 * Creates a callback function that sends an empty result with 204 status code.
	 * If error occur it sends ErrorDescription with approproate status code.
	 * 
	 * @return HTTP response status with no content.
	 */
	public static Response sendEmptyResult() {
		return Response.status(NO_CONTENT).build();
	}

	/**
	 * Creates a callback function that sends newly created object as JSON. That
	 * callack function call be called directly or passed as a parameter to business
	 * logic components.
	 * 
	 * If object is not null it returns 201 status code. For null results it returns
	 * 204 status code. If error occur it sends ErrorDescription with approproate
	 * status code.
	 * 
	 * @param result a body object to created result
	 * @return execution result.
	 */
	public static Response sendCreatedResult(Object result) {
		try {
			if (result == null) {
				return Response.status(NO_CONTENT).build();
			} else {
				return Response.status(CREATED).type(MediaType.APPLICATION_JSON).entity(result).build();
			}
		} catch (Exception ex2) {
			return Response.status(INTERNAL_SERVER_ERROR).build();
		}
	}

	/**
	 * Creates a callback function that sends deleted object as JSON. That callack
	 * function call be called directly or passed as a parameter to business logic
	 * components.
	 * 
	 * If object is not null it returns 200 status code. For null results it returns
	 * 204 status code. If error occur it sends ErrorDescription with approproate
	 * status code.
	 * 
	 * @param result a body object to deleted result
	 * @return execution result.
	 */
	public static Response sendDeletedResult(Object result) {
		try {
			if (result == null) {
				return Response.status(NO_CONTENT).build();
			} else {
				return Response.status(OK).type(MediaType.APPLICATION_JSON).entity(result).build();
			}
		} catch (Exception ex2) {
			return Response.status(INTERNAL_SERVER_ERROR).build();
		}
	}
}
