package org.pipservices3.rpc.clients;

import jakarta.ws.rs.core.*;

import org.pipservices3.commons.data.*;
import org.pipservices3.commons.errors.*;
import org.pipservices3.commons.run.*;
import org.pipservices3.rpc.*;

import java.util.HashMap;
import java.util.Map;

public class DummyCommandableHttpClient extends CommandableHttpClient implements IDummyClient{

	public DummyCommandableHttpClient() {
		super("dummy");
	}

	@Override
	public DataPage<Dummy> getDummies(String correlationId,
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
	public Dummy getDummyById(String correlationId, String id) throws ApplicationException {
		return callCommand(
			Dummy.class,
			"get_dummy_by_id",
			correlationId,
			Parameters.fromTuples("dummy_id", id)
		);
	}

	@Override
	public Dummy createDummy(String correlationId, Dummy entity) throws ApplicationException {
		return callCommand(
			Dummy.class,
			"create_dummy",
			correlationId,
			Parameters.fromTuples("dummy", entity)
		);
	}

	@Override
	public Dummy updateDummy(String correlationId, Dummy entity) throws ApplicationException {
		return callCommand(
			Dummy.class,
			"update_dummy",
			correlationId,
			Parameters.fromTuples("dummy", entity)
		);
	}

	@Override
	public Dummy deleteDummy(String correlationId, String id) throws ApplicationException {
		return callCommand(
			Dummy.class,
			"delete_dummy",
			correlationId,
			Parameters.fromTuples("dummy_id", id)
		);
	}

	@Override
	public String checkCorrelationId(String correlationId) throws ApplicationException {
		Map<String, String> res =  callCommand(
				HashMap.class,
				"check_correlation_id",
				correlationId,
				null
		);

		return res.get("correlation_id");
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



