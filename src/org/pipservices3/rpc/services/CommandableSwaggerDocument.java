package org.pipservices3.rpc.services;

import org.pipservices3.commons.commands.ICommand;
import org.pipservices3.commons.config.ConfigParams;
import org.pipservices3.commons.convert.TypeCode;
import org.pipservices3.commons.convert.TypeConverter;
import org.pipservices3.commons.reflect.PropertyReflector;
import org.pipservices3.commons.validate.ArraySchema;
import org.pipservices3.commons.validate.ObjectSchema;

import java.util.*;

public class CommandableSwaggerDocument {

    private String content = "";

    public List<ICommand> commands;

    public String version = "3.0.2";
    public String baseRoute;

    public String infoTitle;
    public String infoDescription;
    public String infoVersion = "1";
    public String infoTermsOfService;

    public String infoContactName;
    public String infoContactUrl;
    public String infoContactEmail;

    public String infoLicenseName;
    public String infoLicenseUrl;
    protected final Map<String, Object> objectType = Map.of("type", "object");


    public CommandableSwaggerDocument(String baseRoute, ConfigParams config, List<ICommand> commands) {
        this.baseRoute = baseRoute;
        this.commands = commands != null ? commands : new ArrayList<>();

        config = config != null ? config : new ConfigParams();

        this.infoTitle = config.getAsStringWithDefault("name", "CommandableHttpService");
        this.infoDescription = config.getAsStringWithDefault("description", "Commandable microservice");
    }

    public String toString() {
        this.content = "";
        Map<String, Object> data = new LinkedHashMap<>();

        data.put("openapi", version);

        data.put("info", Map.of(
                "title", infoTitle != null ? infoTitle : "null",
                "description", infoDescription != null ? infoDescription : "null",
                "version", infoVersion != null ? infoVersion : "null",
                "termsOfService", infoTermsOfService != null ? infoTermsOfService : "null",
                "contact", Map.of(
                        "name", infoContactName != null ? infoContactName : "null",
                        "url", infoContactUrl != null ? infoContactUrl : "null",
                        "email", infoContactEmail != null ? infoContactEmail : "null"
                ),
                "license", Map.of(
                        "name", infoLicenseName != null ? infoLicenseName : "null",
                        "url", infoLicenseUrl != null ? infoLicenseUrl : "null"
                )));

        data.put("paths", this.createPathsData());

        this.writeData(0, data);

        return this.content;
    }

    private Map<String, Object> createPathsData() {
        Map<String, Object> data = new HashMap<>();

        for (var command : this.commands) {
            var path = this.baseRoute + "/" + command.getName();
            if (!path.startsWith("/")) path = "/" + path;

            var bodyData = this.createRequestBodyData(command);
            var responseData = this.createResponsesData();
            data.put(path, Map.of(
                    "post", Map.of(
                            "tags", List.of(this.baseRoute),
                            "operationId", command.getName() != null ? command.getName() : "null",
                            "requestBody", bodyData != null ? bodyData : "null",
                            "responses", responseData
                    )
            ));
        }

        return data;
    }

    private Map<String, Object> createRequestBodyData(ICommand command) {
        var schemaData = this.createSchemaData(command);
        return schemaData == null ? null : Map.of(
                "content", Map.of(
                        "application/json", Map.of(
                                "schema", schemaData
                        )
                )
        );
    }

    private Map<String, Object> createSchemaData(ICommand command) {
        var schema = (ObjectSchema) PropertyReflector.getProperty(command, "_schema");

        if (schema == null || schema.getProperties() == null)
            return null;

        return this.createPropertyData(schema, true);
    }

    private Map<String, Object> createPropertyData(ObjectSchema schema, boolean includeRequired) {
        Map<String, Object> properties = new HashMap<>();
        List<String> required = new ArrayList<>();
        Map<String, Object> data = new HashMap<>();

        schema.getProperties().forEach(property -> {

            if (property.getType() == null) {
                properties.put(property.getName(), this.objectType);
            } else {
                var propertyName = property.getName();
                var propertyType = property.getType();

                if (propertyType instanceof ArraySchema) {
                    properties.put(propertyName, Map.of(
                            "type", "array",
                            "items", this.createPropertyTypeData(((ArraySchema) propertyType).getValueType())
                    ));
                } else {
                    properties.put(propertyName, this.createPropertyTypeData(propertyType));
                }

                if (includeRequired && property.isRequired()) required.add(propertyName);
            }
        });

        data.put("properties", properties);

        if (required.size() > 0) {
            data.put("required", required);
        }

        return data;
    }

    private Map<String, Object> createPropertyTypeData(Object propertyType) {
        if (propertyType instanceof ObjectSchema) {
            var objectMap = this.createPropertyData((ObjectSchema) propertyType, false);
            var newMap = new HashMap<String, Object>();

            newMap.putAll(objectType);
            newMap.putAll(objectMap);

            return newMap;
        } else {
            TypeCode typeCode;

            if (Arrays.stream(TypeCode.values()).toList().contains(propertyType)) {
                typeCode = (TypeCode) propertyType;
            } else {
                typeCode = TypeConverter.toTypeCode(propertyType);
            }

            if (typeCode == TypeCode.Unknown || typeCode == TypeCode.Map) {
                typeCode = TypeCode.Object;
            }

            switch (typeCode) {
                case Integer:
                    return Map.of(
                            "type", "integer",
                            "format", "int32"
                    );
                case Long:
                    return Map.of(
                            "type", "number",
                            "format", "int64"
                    );
                case Float:
                    return Map.of(
                            "type", "number",
                            "format", "float"
                    );
                case Double:
                    return Map.of(
                            "type", "number",
                            "format", "double"
                    );
                case DateTime:
                    return Map.of(
                            "type", "string",
                            "format", "date-time"
                    );
                case Boolean:
                    return Map.of(
                            "type", "boolean"
                    );
                default:
                    return Map.of("type", TypeConverter.toString(typeCode));
            }
        }
    }

    private Map<String, Object> createResponsesData() {
        return Map.of(
                "200", Map.of(
                        "description", "Successful response",
                        "content", Map.of(
                                "application/json", Map.of(
                                        "schema", Map.of(
                                                "type", "object"
                                        )
                                )
                        )
                )
        );
    }

    protected void writeData(int indent, Map<String, Object> data) {
        for (var entry : data.entrySet()) {
            var key = entry.getKey();
            var value = entry.getValue();

            if (value == null
                    || (value instanceof String && value.equals("null"))
                    || value instanceof Map && ((Map<?, ?>) value).values().stream().allMatch(v -> v.equals("null"))) {
                // Skip...
            } else if (value instanceof String) {
                this.writeAsString(indent, key, (String) value);
            } else if (value instanceof List) {
                if (((List<?>) value).size() > 0) {
                    this.writeName(indent, key);
                    for (var index = 0; index < ((List<?>) value).size(); index++) {
                        final var item = ((List<?>) value).get(index);
                        this.writeArrayItem(indent + 1, (String) item, null);
                    }
                }
            } else if (value instanceof Map) {
                var props = PropertyReflector.getProperties(value).entrySet().stream().filter(Objects::nonNull);
                if (props.toArray().length >= 0) {
                    this.writeName(indent, key);
                    this.writeData(indent + 1, (Map<String, Object>) value);
                }
            } else {
                this.writeAsObject(indent, key, value);
            }
        }
    }

    protected void writeName(int indent, String name) {
        var spaces = this.getSpaces(indent);
        this.content += spaces + name + ":\n";
    }

    protected void writeArrayItem(int indent, String name, Boolean isObjectItem) {
        isObjectItem = isObjectItem != null ? isObjectItem : false;
        var spaces = this.getSpaces(indent);

        this.content += spaces + "- ";

        if (isObjectItem)
            this.content += name + ":\n";
        else
            this.content += name + "\n";
    }

    protected void writeAsObject(int indent, String name, Object value) {
        if (value == null) return;

        var spaces = this.getSpaces(indent);
        this.content += spaces + name + ": " + value + "\n";
    }

    protected void writeAsString(int indent, String name, String value) {
        if (value == null) return;

        var spaces = this.getSpaces(indent);

        this.content += spaces + name + ": '" + value + "'\n";
    }

    protected String getSpaces(int length) {
        return " ".repeat(Math.max(0, length * 2));
    }
}
