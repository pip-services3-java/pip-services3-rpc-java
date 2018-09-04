package org.pipservices.services;

import javax.ws.rs.core.Response;

import org.pipservices.commons.convert.*;
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
				String contentResult = JsonConverter.toJson(ErrorDescriptionFactory.create(ex3));
				return Response.status(ex3.getStatus())
					.entity(contentResult)
					.header("Content-Type", "application/json")
					.build();
			} else {
				String contentResult = JsonConverter.toJson(ErrorDescriptionFactory.create(ex, ""));
				return Response.status(INTERNAL_SERVER_ERROR)
					.entity(contentResult)
					.header("Content-Type", "application/json")
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
				String contentResult = JsonConverter.toJson(result);
				return Response.status(OK)
					.entity(contentResult)
					.header("Content-Type", "application/json")
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
				String contentResult = JsonConverter.toJson(result);
				return Response.status(CREATED)
					.entity(contentResult)
					.header("Content-Type", "application/json")
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
				String contentResult = JsonConverter.toJson(result);
				return Response.status(OK)
					.entity(contentResult)
					.header("Content-Type", "application/json")
					.build();
			}
		} catch (Exception ex2) {
			return Response.status(INTERNAL_SERVER_ERROR).build();
		}
	}
}
