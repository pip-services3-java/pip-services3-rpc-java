package org.pipservices;

import org.pipservices.commons.commands.*;
import org.pipservices.commons.data.AnyValueMap;
import org.pipservices.commons.data.FilterParams;
import org.pipservices.commons.data.PagingParams;
import org.pipservices.commons.run.Parameters;
import org.pipservices.commons.validate.*;

public class DummyCommandSet extends CommandSet{
	
	private IDummyController _controller;
	
	public DummyCommandSet(IDummyController controller)
    {
        _controller = controller;

        addCommand(makeGetPageByFilterCommand());
        addCommand(makeGetOneByIdCommand());
        addCommand(makeCreateCommand());
        addCommand(makeUpdateCommand());
        addCommand(makeDeleteByIdCommand());
        // Commands for errors
        //addCommand(makeCreateWithoutValidationCommand());
        //addCommand(makeRaiseCommandSetExceptionCommand());
        //addCommand(makeRaiseControllerExceptionCommand());

        // V2
        //addCommand(makePingCommand());
    }
	
	
	private ICommand makeGetPageByFilterCommand()
    {
        return new Command(
            "get_dummies",
            new ObjectSchema()
                .withOptionalProperty("correlation_id", String.class)
                .withOptionalProperty("filter", new FilterParamsSchema())
                .withOptionalProperty("paging", new PagingParamsSchema()), 
                (corelationId, args) -> getDummies(corelationId, args) 
                );
    }
	
	private ICommand makeGetOneByIdCommand()
    {
        return new Command(
            "get_dummy_by_id",
            new ObjectSchema()
                .withRequiredProperty("dummy_id", org.pipservices.commons.convert.TypeCode.String),
                (corelationId, args) -> getOneDummy(corelationId, args)
                );
    }
	
	private ICommand makeCreateCommand()
    {
        return new Command(
            "create_dummy",
            new ObjectSchema()
                .withRequiredProperty("dummy", new DummySchema()),
                (corelationId, args) -> createDummy(corelationId, args)
            );
    }
	
	private ICommand makeUpdateCommand()
    {
        return new Command(
            "update_dummy",
            new ObjectSchema()
                .withRequiredProperty("dummy", new DummySchema()),
                (corelationId, args) -> updateDummy(corelationId, args)
            );
    }
	
	private ICommand makeDeleteByIdCommand()
    {
        return new Command(
            "delete_dummy",
            new ObjectSchema()
                .withRequiredProperty("dummy_id", org.pipservices.commons.convert.TypeCode.String),
                (corelationId, args) -> deleteDummy(corelationId, args)
            );
    }
	
		
    private Object getDummies(String correlationId, Parameters args)
    {
    	FilterParams filter = FilterParams.fromValue(args.get("filter"));
    	PagingParams paging = PagingParams.fromValue(args.get("paging"));

        return _controller.getPageByFilter(correlationId, filter, paging);
    }
    
    
    private Object getOneDummy(String correlationId, Parameters args)
    {
        String dummyId = args.getAsString("dummy_id");

        return _controller.getOneById(correlationId, dummyId);
    }

    private Object createDummy(String correlationId, Parameters args)
    {
    	Dummy dummy = extractDummy(args);

        return _controller.create(correlationId, dummy);
    }

    private Object updateDummy(String correlationId, Parameters args)
    {
    	Dummy dummy = extractDummy(args);

        return _controller.update(correlationId, dummy);
    }

    private Object deleteDummy(String correlationId, Parameters args)
    {
        String dummyId = args.getAsString("dummy_id");

        return _controller.deleteById(correlationId, dummyId);
    }

    private static Dummy extractDummy(Parameters args)
    {
        AnyValueMap map = args.getAsMap("dummy");

        String id = map.getAsStringWithDefault("id", "");
        String key = map.getAsStringWithDefault("key", "");
        String content = map.getAsStringWithDefault("content", "");
        boolean flag = map.getAsBooleanWithDefault("flag", false);

        Dummy dummy = new Dummy(id, key, content, flag);
        return dummy;
    }
    
}
