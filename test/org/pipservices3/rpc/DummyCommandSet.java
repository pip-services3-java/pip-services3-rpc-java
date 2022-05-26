package org.pipservices3.rpc;

import org.pipservices3.commons.commands.*;
import org.pipservices3.commons.data.AnyValueArray;
import org.pipservices3.commons.data.AnyValueMap;
import org.pipservices3.commons.data.FilterParams;
import org.pipservices3.commons.data.PagingParams;
import org.pipservices3.commons.run.Parameters;
import org.pipservices3.commons.validate.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DummyCommandSet extends CommandSet {

    private IDummyController _controller;

    public DummyCommandSet(IDummyController controller) {
        _controller = controller;

        addCommand(makeGetPageByFilterCommand());
        addCommand(makeGetOneByIdCommand());
        addCommand(makeCreateCommand());
        addCommand(makeUpdateCommand());
        addCommand(makeDeleteByIdCommand());
        addCommand(makeCheckCorrelationIdCommand());

        // Commands for errors
        addCommand(makeCreateWithoutValidationCommand());
        addCommand(makeRaiseCommandSetExceptionCommand());
        addCommand(makeRaiseControllerExceptionCommand());

        // V2
        addCommand(makePingCommand());
    }

    private ICommand makeGetPageByFilterCommand() {
        return new Command("get_dummies",
                new ObjectSchema()
                        .withOptionalProperty("correlation_id", String.class)
                        .withOptionalProperty("filter", new FilterParamsSchema())
                        .withOptionalProperty("paging", new PagingParamsSchema()),
                (correlationId, args) -> {
                    FilterParams filter = FilterParams.fromValue(args.get("filter"));
                    PagingParams paging = PagingParams.fromValue(args.get("paging"));

                    return _controller.getPageByFilter(correlationId, filter, paging);
                });
    }

    private ICommand makeGetOneByIdCommand() {
        return new Command("get_dummy_by_id",
                new ObjectSchema()
                        .withRequiredProperty("dummy_id", org.pipservices3.commons.convert.TypeCode.String),
                (correlationId, args) -> {
                    String dummyId = args.getAsString("dummy_id");

                    return _controller.getOneById(correlationId, dummyId);
                });
    }

    private ICommand makeCreateCommand() {
        return new Command("create_dummy", new ObjectSchema()
                .withRequiredProperty("dummy", new DummySchema()),
                (correlationId, args) -> {
                    Dummy dummy = extractDummy(args);
                    return _controller.create(correlationId, dummy);
                });
    }

    private ICommand makeUpdateCommand() {
        return new Command("update_dummy", new ObjectSchema()
                .withRequiredProperty("dummy", new DummySchema()),
                (correlationId, args) -> {
                    Dummy dummy = extractDummy(args);
                    return _controller.update(correlationId, dummy);
                });
    }

    private ICommand makeDeleteByIdCommand() {
        return new Command("delete_dummy",
                new ObjectSchema()
                        .withRequiredProperty("dummy_id", org.pipservices3.commons.convert.TypeCode.String),
                (correlationId, args) -> {
                    String dummyId = args.getAsString("dummy_id");
                    return _controller.deleteById(correlationId, dummyId);

                }
        );
    }

    private ICommand makeCreateWithoutValidationCommand() {
        return new Command("create_dummy_without_validation", null,
                (correlationId, parameters) -> {
                    return null;
                });
    }

    private ICommand makeRaiseCommandSetExceptionCommand() {
        return new Command("raise_commandset_error",
                new ObjectSchema()
                        .withRequiredProperty("dummy", new DummySchema()),
                (correlationId, parameters) -> {
                    throw new RuntimeException("Dummy error in commandset!");
                }
        );
    }

    private ICommand makeCheckCorrelationIdCommand() {
        return new Command("check_correlation_id",
                new ObjectSchema(),
                (correlationId, parameters) -> {
                    var value = this._controller.checkCorrelationId(correlationId);
                    return Map.of("correlation_id", value);
                }
        );
    }

    private ICommand makeRaiseControllerExceptionCommand() {
        return new Command("raise_exception", new ObjectSchema(),
                (correlationId, parameters) -> {
                    _controller.raiseException(correlationId);
                    return null;
                }
        );
    }

    private ICommand makePingCommand() {
        return new Command("ping_dummy", null, (correlationId, parameters) -> {
            return _controller.ping();
        });
    }


    private static Dummy extractDummy(Parameters args) {
        AnyValueMap map = args.getAsMap("dummy");

        String id = map.getAsNullableString("id");
        String key = map.getAsNullableString("key");
        String content = map.getAsNullableString("content");
        var array = map.getAsArrayWithDefault("array", new AnyValueArray());

        return new Dummy(id, key, content, new ArrayList<>());
    }

//	private static Dummy extractDummy(Parameters args) {
//		AnyValueMap map = args.getAsMap("dummy");
//
//		String id = map.getAsNullableString("id");
//		String key = map.getAsNullableString("key");
//		String content = map.getAsNullableString("content");
//		List<SubDummy> array = new ArrayList<>();
//		map.getAsArrayWithDefault("array", new AnyValueArray()).forEach(el -> array.add((SubDummy)el));
//
//		return new Dummy(id, key, content, array);
//	}
}
