package org.pipservices3.rpc.clients;

import org.pipservices3.commons.data.DataPage;
import org.pipservices3.commons.data.FilterParams;
import org.pipservices3.commons.data.PagingParams;
import org.pipservices3.commons.errors.ApplicationException;
import org.pipservices3.commons.refer.Descriptor;
import org.pipservices3.rpc.Dummy;
import org.pipservices3.rpc.IDummyController;
import org.pipservices3.rpc.clients.DirectClient;

public class DummyDirectClient extends DirectClient<IDummyController> implements IDummyClient {

	public DummyDirectClient() {
		_dependencyResolver.put("controller", new Descriptor("pip-services3-dummies", "controller", "*", "*", "*"));
	}

	@Override
	public boolean isOpen() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public DataPage<Dummy> getPageByFilter(String correlationId, FilterParams filter, PagingParams paging)
		throws ApplicationException {
		
		filter = filter != null ? filter : new FilterParams();
		paging = paging != null ? paging : new PagingParams();

//        Timing timing = instrument(correlationId, "dummy.get_page_by_filter");
		{
			return _controller.getPageByFilter(correlationId, filter, paging);
		}
	}

	@Override
	public Dummy getOneById(String correlationId, String id)
		throws ApplicationException {
		return _controller.getOneById(correlationId, id);
	}

	@Override
	public Dummy create(String correlationId, Dummy entity) 
		throws ApplicationException {
		return _controller.create(correlationId, entity);
	}

	@Override
	public Dummy update(String correlationId, Dummy entity) 
		throws ApplicationException {
		return _controller.update(correlationId, entity);
	}

	@Override
	public Dummy deleteById(String correlationId, String id)  
		throws ApplicationException {
		return _controller.deleteById(correlationId, id);
	}

	@Override
	public void raiseException(String correlationId) 
		throws ApplicationException {
		_controller.raiseException(correlationId);

	}

}
