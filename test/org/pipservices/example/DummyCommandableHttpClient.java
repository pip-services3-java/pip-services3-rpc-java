package org.pipservices.example;

import java.io.IOException;

import org.pipservices.commons.data.DataPage;
import org.pipservices.commons.data.FilterParams;
import org.pipservices.commons.data.PagingParams;
import org.pipservices.commons.errors.ApplicationException;
import org.pipservices.Dummy;
import org.pipservices.rest.CommandableHttpClient;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class DummyCommandableHttpClient extends CommandableHttpClient implements IDummyClient{

	public DummyCommandableHttpClient() {
		super("dummy");
	}

	@SuppressWarnings("unchecked")
	@Override
	public DataPage<Dummy> getPageByFilter(String correlationId, FilterParams filter, PagingParams paging) {
		
		filter = filter != null ? filter : new FilterParams();
        paging = paging != null ? paging : new PagingParams();
     
        try {
			return (DataPage<Dummy>)callCommand("get_dummies", correlationId, filter, paging);
		} catch (ApplicationException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Dummy getOneById(String correlationId, String id) {
		try {
			return (Dummy)callCommand("get_dummy_by_id", correlationId, id);
		} catch (ApplicationException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Dummy create(String correlationId, Dummy entity) {
		try {
			return (Dummy)callCommand("create_dummy", correlationId, entity);
		} catch (ApplicationException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Dummy update(String correlationId, Dummy entity) {
		try {
			return (Dummy)callCommand("update_dummy", correlationId, entity);
		} catch (ApplicationException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Dummy deleteById(String correlationId, String id) {
		try {
			return (Dummy)callCommand("delete_dummy", correlationId, id);
		} catch (ApplicationException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void raiseException(String correlationId) {
		try {
			callCommand("raise_exception", correlationId, new FilterParams(), new PagingParams());
		} catch (ApplicationException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}



