package org.pipservices.services;

import java.util.*;

import org.glassfish.jersey.internal.util.collection.MultivaluedStringMap;
import org.pipservices.commons.convert.*;
import org.pipservices.commons.data.*;

//import com.sun.jersey.core.util.*;

public class RestQueryParams extends MultivaluedStringMap {
	private static final long serialVersionUID = -903917330561942092L;

	public RestQueryParams() {
	}

	public RestQueryParams(String correlationId) {
		addCorrelationId(correlationId);
	}

	public RestQueryParams(String correlationId, FilterParams filter, PagingParams paging) {
		addCorrelationId(correlationId);
		addFilterParams(filter);
		addPagingParams(paging);
	}

	public void addCorrelationId(String correlationId) {
		if (correlationId == null)
			return;
		add("correlation_id", correlationId);
	}

	public void addFilterParams(FilterParams filter) {
		if (filter == null)
			return;

		for (Map.Entry<String, String> entry : filter.entrySet()) {
			String value = entry.getValue();
			if (value != null)
				add(entry.getKey(), value);
		}
	}

	public void addPagingParams(PagingParams paging) {
		if (paging == null)
			return;

		if (paging.getSkip() != null)
			add("skip", StringConverter.toString(paging.getSkip()));
		if (paging.getTake() != null)
			add("take", StringConverter.toString(paging.getTake()));
		add("total", StringConverter.toString(paging.hasTotal()));
	}
}
