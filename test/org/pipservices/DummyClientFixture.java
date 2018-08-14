package org.pipservices;

import static org.junit.Assert.*;

import org.pipservices.commons.data.*;
import org.pipservices.commons.errors.*;
import org.pipservices.example.IDummyClient;

public class DummyClientFixture {
    private final Dummy DUMMY1 = new Dummy(null, "Key 1", "Content 1", true);
    private final Dummy DUMMY2 = new Dummy(null, "Key 2", "Content 2", true);

    private IDummyClient _client;

    public DummyClientFixture(IDummyClient client) {
        assertNotNull(client);
        _client = client;
    }

    public void testCrudOperations() throws ApplicationException {
        // Create one dummy
        Dummy dummy1 = _client.create(null, DUMMY1);

        assertNotNull(dummy1);
        assertNotNull(dummy1.getId());
        assertEquals(DUMMY1.getKey(), dummy1.getKey());
        assertEquals(DUMMY1.getContent(), dummy1.getContent());

        // Create another dummy
        Dummy dummy2 = _client.create(null, DUMMY2);

        assertNotNull(dummy2);
        assertNotNull(dummy2.getId());
        assertEquals(DUMMY2.getKey(), dummy2.getKey());
        assertEquals(DUMMY2.getContent(), dummy2.getContent());

        // Get all dummies
        DataPage<Dummy> dummies = _client.getPageByFilter(null, null, null);
        assertNotNull(dummies);
        assertEquals(2, dummies.getData().size());

        // Update the dummy
        dummy1.setContent("Updated Content 1");
        Dummy dummy = _client.update(null, dummy1);

        assertNotNull(dummy);
        assertEquals(dummy1.getId(), dummy.getId());
        assertEquals(dummy1.getKey(), dummy.getKey());
        assertEquals("Updated Content 1", dummy.getContent());

        // Delete the dummy
        _client.deleteById(null, dummy1.getId());

        // Try to get deleted dummy
        dummy = _client.getOneById(null, dummy1.getId());
        assertNull(dummy);
    }
}
