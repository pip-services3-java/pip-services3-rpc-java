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
		_dependencyResolver.put("controller", new Descriptor("pip-services-dummies", "controller", "*", "*", "*"));
	}

	@Override
	public DataPage<Dummy> getDummies(String correlationId, FilterParams filter, PagingParams paging)
		throws ApplicationException {
		
		filter = filter != null ? filter : new FilterParams();
		paging = paging != null ? paging : new PagingParams();

		var timing = this.instrument(correlationId, "dummy.get_page_by_filter");
		try {
			return _controller.getPageByFilter(correlationId, filter, paging);
		} catch (Exception ex) {
			timing.endFailure(ex);
			throw ex;
		} finally {
			timing.endTiming();
		}
	}

	@Override
	public Dummy getDummyById(String correlationId, String id)
		throws ApplicationException {
		var timing = this.instrument(correlationId, "dummy.get_one_by_id");
		try {
			return _controller.getOneById(correlationId, id);
		} catch (Exception ex) {
			timing.endFailure(ex);
			throw ex;
		} finally {
			timing.endTiming();
		}
	}

	@Override
	public Dummy createDummy(String correlationId, Dummy entity)
		throws ApplicationException {
		var timing = this.instrument(correlationId, "dummy.create");
		try {
			return _controller.create(correlationId, entity);
		} catch (Exception ex) {
			timing.endFailure(ex);
			throw ex;
		} finally {
			timing.endTiming();
		}
	}

	@Override
	public Dummy updateDummy(String correlationId, Dummy entity)
		throws ApplicationException {
		var timing = this.instrument(correlationId, "dummy.update");
		try {
			return _controller.update(correlationId, entity);
		} catch (Exception ex) {
			timing.endFailure(ex);
			throw ex;
		} finally {
			timing.endTiming();
		}
	}

	@Override
	public Dummy deleteDummy(String correlationId, String id)
		throws ApplicationException {
		var timing = this.instrument(correlationId, "dummy.delete_by_id");
		try {
			return _controller.deleteById(correlationId, id);
		} catch (Exception ex) {
			timing.endFailure(ex);
			throw ex;
		} finally {
			timing.endTiming();
		}
	}

	@Override
	public String checkCorrelationId(String correlationId) throws ApplicationException {
		var timing = this.instrument(correlationId, "dummy.check_correlation_id");
		try {
			return this._controller.checkCorrelationId(correlationId);
		} catch (Exception ex) {
			timing.endFailure(ex);
			throw ex;
		} finally {
			timing.endTiming();
		}
	}

	@Override
	public void raiseException(String correlationId) 
		throws ApplicationException {
		var timing = this.instrument(correlationId, "dummy.raise_exception");
		try {
			_controller.raiseException(correlationId);
		} catch (Exception ex) {
			timing.endFailure(ex);
			throw ex;
		} finally {
			timing.endTiming();
		}

	}

}
