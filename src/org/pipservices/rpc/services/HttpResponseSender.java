package org.pipservices.rpc.services;

import javax.ws.rs.core.*;

import org.pipservices.commons.errors.*;

public class HttpResponseSender {
	private final static int INTERNAL_SERVER_ERROR = 500;
	private final static int NO_CONTENT = 204;
	private final static int CREATED = 201;
	private final static int OK = 200;

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
				return Response.status(ex3.getStatus())
					.type(MediaType.APPLICATION_JSON)
					.entity(errorDesc3)
					.build();
			} else {
				ErrorDescription errorDesc = ErrorDescriptionFactory.create(ex, null);
				return Response.status(INTERNAL_SERVER_ERROR)
					.type(MediaType.APPLICATION_JSON)
					.entity(errorDesc)
					.build();
			}
		} catch (Exception ex2) {
			return Response.status(INTERNAL_SERVER_ERROR).build();
		}
	}

	public static Response sendResult(Object result) {
		try {
			if (result == null) {
				return Response.status(NO_CONTENT).build();
			} else {
				return Response.status(OK)
					.type(MediaType.APPLICATION_JSON)
					.entity(result)
					.build();
			}
		} catch (Exception ex2) {
			return Response.status(INTERNAL_SERVER_ERROR).build();
		}
	}

	public static Response sendEmptyResult() {
		return Response.status(NO_CONTENT).build();
	}

	public static Response sendCreatedResult(Object result) {
		try {
			if (result == null) {
				return Response.status(NO_CONTENT).build();
			} else {
				return Response.status(CREATED)
					.type(MediaType.APPLICATION_JSON)
					.entity(result)
					.build();
			}
		} catch (Exception ex2) {
			return Response.status(INTERNAL_SERVER_ERROR).build();
		}
	}

	public static Response sendDeletedResult(Object result) {	
		try {
			if (result == null) {
				return Response.status(NO_CONTENT).build();
			} else {
				return Response.status(OK)
					.type(MediaType.APPLICATION_JSON)
					.entity(result)
					.build();
			}
		} catch (Exception ex2) {
			return Response.status(INTERNAL_SERVER_ERROR).build();
		}
	}
}
