package org.pipservices.rpc.clients;

import org.junit.*;
import org.pipservices.commons.errors.ApplicationException;
import org.pipservices.commons.refer.Descriptor;
import org.pipservices.commons.refer.References;
import org.pipservices.rpc.DummyController;

public class DummyDirectClientTest {	
	private DummyController _ctrl;
    private DummyDirectClient _client;
    private DummyClientFixture _fixture;
	
    @Before
    public void setUp() throws ApplicationException {
        _ctrl = new DummyController();
        _client = new DummyDirectClient();

        References references = References.fromTuples(
            new Descriptor("pip-services-dummies", "controller", "default", "default", "1.0"), _ctrl
        );
        _client.setReferences(references);

        _fixture = new DummyClientFixture(_client);

        _client.open(null);
    }

    @After
    public void tearDown() {
    	_client.close(null);
    }    
    
    @Test
    public void testCrudOperations() throws ApplicationException {
    	_fixture.testCrudOperations();
	}
    
}
