package org.pipservices;

import org.junit.Test;
import org.pipservices.commons.errors.ApplicationException;
import org.pipservices.commons.refer.Descriptor;
import org.pipservices.commons.refer.References;
import org.pipservices.example.DummyDirectClient;

public class DummyDirectClientTest {
	
	private final DummyController _ctrl;
    private final DummyDirectClient _client;
    private final DummyClientFixture _fixture;
	
    public DummyDirectClientTest() throws ApplicationException {
        _ctrl = new DummyController();
        _client = new DummyDirectClient();

        References references = References.fromTuples(
            new Descriptor("pip-services-dummies", "controller", "default", "default", "1.0"), _ctrl
        );
        _client.setReferences(references);

        _fixture = new DummyClientFixture(_client);

        _client.open(null);
    }
    
    @Test
    public void testCrudOperations() throws ApplicationException {
    	_fixture.testCrudOperations();
    	}
    
    public void dispose() {
    	_client.close(null);
    }    
}
