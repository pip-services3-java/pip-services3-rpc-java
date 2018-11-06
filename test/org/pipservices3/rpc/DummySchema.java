package org.pipservices3.rpc;

import org.pipservices3.commons.convert.TypeCode;
import org.pipservices3.commons.validate.ObjectSchema;

public class DummySchema extends ObjectSchema {

	public DummySchema() {
		withOptionalProperty("id", TypeCode.String);
		withRequiredProperty("key", TypeCode.String);
		withOptionalProperty("content", TypeCode.String);
		withOptionalProperty("flag", TypeCode.Boolean);
	}
}
