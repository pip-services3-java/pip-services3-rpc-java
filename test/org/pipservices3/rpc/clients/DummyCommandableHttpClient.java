package org.pipservices3.rpc.clients;

import javax.ws.rs.core.*;

import org.pipservices3.commons.data.*;
import org.pipservices3.commons.errors.*;
import org.pipservices3.commons.run.*;
import org.pipservices3.rpc.*;

public class DummyCommandableHttpClient extends CommandableHttpClient implements IDummyClient{

	public DummyCommandableHttpClient() {
		super("dummy");
	}

	@Override
	public DataPage<Dummy> getPageByFilter(String correlationId,
		FilterParams filter, PagingParams paging) throws ApplicationException {
		
		return callCommand(
			new GenericType<DataPage<Dummy>>() {},
			"get_dummies",
			correlationId,
			Parameters.fromTuples(
				"filter", filter,
				"paging", paging
			)
		);
	}

	@Override
	public Dummy getOneById(String correlationId, String id) throws ApplicationException {
		return callCommand(
			Dummy.class,
			"get_dummy_by_id",
			correlationId,
			Parameters.fromTuples("dummy_id", id)
		);
	}

	@Override
	public Dummy create(String correlationId, Dummy entity) throws ApplicationException {
		return callCommand(
			Dummy.class,
			"create_dummy",
			correlationId,
			Parameters.fromTuples("dummy", entity)
		);
	}

	@Override
	public Dummy update(String correlationId, Dummy entity) throws ApplicationException {
		return callCommand(
			Dummy.class,
			"update_dummy",
			correlationId,
			Parameters.fromTuples("dummy", entity)
		);
	}

	@Override
	public Dummy deleteById(String correlationId, String id) throws ApplicationException {
		return callCommand(
			Dummy.class,
			"delete_dummy",
			correlationId,
			Parameters.fromTuples("dummy_id", id)
		);
	}

	@Override
	public void raiseException(String correlationId) throws ApplicationException {
		callCommand(
			Object.class,
			"raise_exception",
			correlationId,
			new Parameters()
		);
	}

}



