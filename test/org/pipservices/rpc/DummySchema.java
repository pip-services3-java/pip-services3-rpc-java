package org.pipservices;

import org.pipservices.commons.convert.TypeCode;
import org.pipservices.commons.validate.ObjectSchema;

public class DummySchema extends ObjectSchema{

	public DummySchema()
    {
        withOptionalProperty("id", TypeCode.String);
        withRequiredProperty("key", TypeCode.String);
        withOptionalProperty("content", TypeCode.String);
        withOptionalProperty("flag", TypeCode.Boolean);
    }
}
