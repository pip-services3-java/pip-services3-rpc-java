package org.pipservices;

import java.util.*;

import org.pipservices.commons.commands.CommandSet;
import org.pipservices.commons.commands.ICommandable;
import org.pipservices.commons.data.*;
import org.pipservices.commons.errors.*;
import org.pipservices.example.DummyCommandSet;
import org.pipservices.example.IDummyController;

public class DummyController implements IDummyController, ICommandable {
	private Object _lock = new Object();
	private List<Dummy> _entities = new ArrayList<Dummy>();
	
	private DummyCommandSet _commandSet;
	
	public DummyController() {}

	@Override
	public CommandSet getCommandSet() {
		if (_commandSet == null)
        {
            _commandSet = new DummyCommandSet(this);
        }

	    return _commandSet;
	}

	@Override
	public DataPage<Dummy> getPageByFilter(String correlationId, FilterParams filter, PagingParams paging) {
		filter = filter != null ? filter : new FilterParams();
		String key = filter.getAsNullableString("key");
		
		paging = paging != null ? paging : new PagingParams();
		long skip = paging.getSkip(0);
		long take = paging.getTake(100);
		
		List<Dummy> result = new ArrayList<Dummy>();
		synchronized (_lock) {
			for (Dummy entity: _entities) {
				if (key != null && !key.equals(entity.getKey()))
					continue;
				
				skip--;
				if (skip >= 0) continue; 
				
				take--;
				if (take < 0) break;
				
				result.add(entity);
			}
		}
		return new DataPage<Dummy>(result);
	}

	@Override
	public Dummy getOneById(String correlationId, String id) {
		synchronized (_lock) {
			for (Dummy entity : _entities) {
				if (entity.getId().equals(id))
					return entity;
			}
		}
		return null;
	}

	@Override
	public Dummy create(String correlationId, Dummy entity) {
		synchronized (_lock) {
			if (entity.getId() == null)
			entity.setId(IdGenerator.nextLong());

		_entities.add(entity);
		}
		return entity;
	}

	@Override
	public Dummy update(String correlationId, Dummy newEntity) {
		synchronized (_lock) {
			for (int index = 0; index < _entities.size(); index++) {
				Dummy entity = _entities.get(index);
				if (entity.getId().equals(newEntity.getId())) {
					_entities.set(index, newEntity);
					return newEntity;
				}
			}
		}		
		return null;
	}

	@Override
	public Dummy deleteById(String correlationId, String id) {
		synchronized (_lock) {
			for (int index = 0; index < _entities.size(); index++) {
				Dummy entity = _entities.get(index);
				if (entity.getId().equals(id)) {
					_entities.remove(index);
					return entity;
				}
			}
		}
		return null;
	}

	@Override
	public void raiseException(String correlationId) {
		try {
			throw new NotFoundException(correlationId, "TEST_ERROR", "Dummy error in controller!");
		} catch (NotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	@Override
	public boolean ping() {
		return true;
	}
}
