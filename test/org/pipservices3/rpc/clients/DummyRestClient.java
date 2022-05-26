package org.pipservices3.rpc.clients;

import jakarta.ws.rs.HttpMethod;
import org.pipservices3.commons.data.DataPage;
import org.pipservices3.commons.data.FilterParams;
import org.pipservices3.commons.data.PagingParams;
import org.pipservices3.commons.errors.ApplicationException;
import org.pipservices3.commons.errors.ApplicationExceptionFactory;
import org.pipservices3.commons.errors.ErrorDescription;
import org.pipservices3.rpc.Dummy;

import java.util.HashMap;

public class DummyRestClient extends RestClient implements IDummyClient{
    @Override
    public DataPage<Dummy> getDummies(String correlationId, FilterParams filter, PagingParams paging) throws ApplicationException {
        String route = "/dummies";

        route = this.addFilterParams(route, filter);
        route = this.addPagingParams(route, paging);

        var timing = this.instrument(correlationId, "dummy.get_page_by_filter");
        try {
            return this.call(DataPage.class, correlationId,HttpMethod.GET, route, null);
        } catch (Exception ex) {
            timing.endFailure(ex);
            throw ex;
        } finally {
            timing.endTiming();
        }
    }

    @Override
    public Dummy getDummyById(String correlationId, String id) throws ApplicationException {
        var timing = this.instrument(correlationId, "dummy.get_one_by_id");
        try {
            return this.call(Dummy.class, correlationId,HttpMethod.GET, "/dummies/" + id, null);
        } catch (Exception ex) {
            timing.endFailure(ex);
            throw ex;
        } finally {
            timing.endTiming();
        }
    }

    @Override
    public Dummy createDummy(String correlationId, Dummy entity) throws ApplicationException {
        var timing = this.instrument(correlationId, "dummy.create");
        try {
            return this.call(Dummy.class, correlationId, HttpMethod.POST, "/dummies", entity);
        } catch (Exception ex) {
            timing.endFailure(ex);
            throw ex;
        } finally {
            timing.endTiming();
        }
    }

    @Override
    public Dummy updateDummy(String correlationId, Dummy entity) throws ApplicationException {
        var timing = this.instrument(correlationId, "dummy.update");
        try {
            return this.call(Dummy.class,correlationId , HttpMethod.PUT, "/dummies", entity);
        } catch (Exception ex) {
            timing.endFailure(ex);
            throw ex;
        } finally {
            timing.endTiming();
        }
    }

    @Override
    public Dummy deleteDummy(String correlationId, String id) throws ApplicationException {
        var timing = this.instrument(correlationId, "dummy.delete_by_id");
        try {
            return this.call(Dummy.class, correlationId, HttpMethod.DELETE, "/dummies/" + id, null);
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
            HashMap<String, String> result = this.call(HashMap.class, correlationId, HttpMethod.GET, "/dummies/check/correlation_id", null);
            return result.getOrDefault("correlation_id", null);
        } catch (Exception ex) {
            timing.endFailure(ex);
            throw ex;
        } finally {
            timing.endTiming();
        }
    }

    @Override
    public void raiseException(String correlationId) throws ApplicationException {
        var timing = this.instrument(correlationId, "dummy.raise_exception");
        try {
            throw this.call(ApplicationException.class, correlationId, HttpMethod.POST, "/dummies/raise_exception", null);
        } catch (Exception ex) {
            timing.endFailure(ex);
            throw ex;
        } finally {
            timing.endTiming();
        }
    }
}
