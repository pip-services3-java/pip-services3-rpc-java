package org.pipservices3.rpc;

import org.pipservices3.commons.convert.TypeCode;
import org.pipservices3.commons.validate.ObjectSchema;

public class SubDummySchema extends ObjectSchema {
    public SubDummySchema() {
        super();
        this.withRequiredProperty("key", TypeCode.String);
        this.withOptionalProperty("content", TypeCode.String);
    }
}
