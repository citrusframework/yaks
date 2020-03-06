/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.citrusframework.yaks.swagger;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import io.swagger.models.ArrayModel;
import io.swagger.models.Model;
import io.swagger.models.RefModel;
import io.swagger.models.parameters.AbstractSerializableParameter;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.BooleanProperty;
import io.swagger.models.properties.DateProperty;
import io.swagger.models.properties.DateTimeProperty;
import io.swagger.models.properties.DoubleProperty;
import io.swagger.models.properties.FloatProperty;
import io.swagger.models.properties.IntegerProperty;
import io.swagger.models.properties.LongProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.properties.StringProperty;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Generates proper payloads and validation expressions based on Swagger Open API specification rules. Creates outbound payloads
 * with generated random test data according to specification and creates inbound payloads with proper validation expressions to
 * enforce the specification rules.
 *
 * @author Christoph Deppisch
 */
public class SwaggerTestDataGenerator {

    /**
     * Creates payload from schema for outbound message.
     * @param model
     * @param definitions
     * @return
     */
    public static String createOutboundPayload(Model model, Map<String, Model> definitions) {
        StringBuilder payload = new StringBuilder();

        if (model instanceof RefModel) {
            model = definitions.get(((RefModel) model).getSimpleRef());
        }

        if (model instanceof ArrayModel) {
            payload.append(createOutboundPayload(((ArrayModel) model).getItems(), definitions));
        } else {
            payload.append("{");

            if (model.getProperties() != null) {
                for (Map.Entry<String, Property> entry : model.getProperties().entrySet()) {
                    payload.append("\"").append(entry.getKey()).append("\": ").append(createOutboundPayload(entry.getValue(), definitions)).append(",");
                }
            }

            if (payload.toString().endsWith(",")) {
                payload.replace(payload.length() - 1, payload.length(), "");
            }

            payload.append("}");
        }

        return payload.toString();
    }

    /**
     * Creates payload from property for outbound message.
     * @param property
     * @param definitions
     * @return
     */
    public static String createOutboundPayload(Property property, Map<String, Model> definitions) {
        StringBuilder payload = new StringBuilder();

        if (property instanceof RefProperty) {
            Model model = definitions.get(((RefProperty) property).getSimpleRef());
            payload.append("{");

            if (model.getProperties() != null) {
                for (Map.Entry<String, Property> entry : model.getProperties().entrySet()) {
                    payload.append("\"")
                            .append(entry.getKey())
                            .append("\": ")
                            .append(createRandomValueExpression(entry.getValue(), definitions, true))
                            .append(",");
                }
            }

            if (payload.toString().endsWith(",")) {
                payload.replace(payload.length() - 1, payload.length(), "");
            }

            payload.append("}");
        } else if (property instanceof ArrayProperty) {
            payload.append("[");
            payload.append(createRandomValueExpression(((ArrayProperty) property).getItems(), definitions, true));
            payload.append("]");
        } else {
            payload.append(createRandomValueExpression(property, definitions, true));
        }

        return payload.toString();
    }

    /**
     * Create payload from schema with random values.
     * @param property
     * @param definitions
     * @param quotes
     * @return
     */
    public static String createRandomValueExpression(Property property, Map<String, Model> definitions, boolean quotes) {
        StringBuilder payload = new StringBuilder();

        if (property instanceof RefProperty) {
            payload.append(createOutboundPayload(property, definitions));
        } else if (property instanceof ArrayProperty) {
            payload.append(createOutboundPayload(property, definitions));
        } else if (property instanceof StringProperty || property instanceof DateProperty || property instanceof DateTimeProperty) {
            if (quotes) {
                payload.append("\"");
            }

            if (property instanceof DateProperty) {
                payload.append("citrus:currentDate()");
            } else if (property instanceof DateTimeProperty) {
                payload.append("citrus:currentDate('yyyy-MM-dd'T'hh:mm:ss')");
            } else if (!CollectionUtils.isEmpty(((StringProperty) property).getEnum())) {
                payload.append("citrus:randomEnumValue(").append(((StringProperty) property).getEnum().stream().map(value -> "'" + value + "'").collect(Collectors.joining(","))).append(")");
            } else if (Optional.ofNullable(property.getFormat()).orElse("").equalsIgnoreCase("uuid")) {
                payload.append("citrus:randomUUID()");
            } else {
                payload.append("citrus:randomString(").append(((StringProperty) property).getMaxLength() != null && ((StringProperty) property).getMaxLength() > 0 ? ((StringProperty) property).getMaxLength() : (((StringProperty) property).getMinLength() != null && ((StringProperty) property).getMinLength() > 0 ? ((StringProperty) property).getMinLength() : 10)).append(")");
            }

            if (quotes) {
                payload.append("\"");
            }
        } else if (property instanceof IntegerProperty || property instanceof LongProperty) {
            payload.append("citrus:randomNumber(8)");
        } else if (property instanceof FloatProperty || property instanceof DoubleProperty) {
            payload.append("citrus:randomNumber(8)");
        } else if (property instanceof BooleanProperty) {
            payload.append("citrus:randomEnumValue('true', 'false')");
        } else if (quotes) {
            payload.append("\"\"");
        }

        return payload.toString();
    }

    /**
     * Creates control payload from property for validation.
     * @param property
     * @param definitions
     * @return
     */
    public static String createInboundPayload(Property property, Map<String, Model> definitions) {
        StringBuilder payload = new StringBuilder();

        if (property instanceof RefProperty) {
            Model model = definitions.get(((RefProperty) property).getSimpleRef());
            payload.append("{");

            if (model.getProperties() != null) {
                for (Map.Entry<String, Property> entry : model.getProperties().entrySet()) {
                    payload.append("\"").append(entry.getKey()).append("\": ").append(createValidationExpression(entry.getValue(), definitions, true)).append(",");
                }
            }

            if (payload.toString().endsWith(",")) {
                payload.replace(payload.length() - 1, payload.length(), "");
            }

            payload.append("}");
        } else if (property instanceof ArrayProperty) {
            payload.append("[");
            payload.append(createValidationExpression(((ArrayProperty) property).getItems(), definitions, true));
            payload.append("]");
        } else {
            payload.append(createValidationExpression(property, definitions, false));
        }

        return payload.toString();
    }

    /**
     * Creates control payload from schema for validation.
     * @param model
     * @param definitions
     * @return
     */
    public static String createInboundPayload(Model model, Map<String, Model> definitions) {
        StringBuilder payload = new StringBuilder();

        if (model instanceof RefModel) {
            model = definitions.get(((RefModel) model).getSimpleRef());
        }

        if (model instanceof ArrayModel) {
            payload.append("[");
            payload.append(createValidationExpression(((ArrayModel) model).getItems(), definitions, true));
            payload.append("]");
        } else {
            payload.append("{");

            if (model.getProperties() != null) {
                for (Map.Entry<String, Property> entry : model.getProperties().entrySet()) {
                    payload.append("\"").append(entry.getKey()).append("\": ").append(createValidationExpression(entry.getValue(), definitions, true)).append(",");
                }
            }

            if (payload.toString().endsWith(",")) {
                payload.replace(payload.length() - 1, payload.length(), "");
            }

            payload.append("}");
        }

        return payload.toString();
    }

    /**
     * Create validation expression using functions according to parameter type and format.
     * @param property
     * @param definitions
     * @param quotes
     * @return
     */
    public static String createValidationExpression(Property property, Map<String, Model> definitions, boolean quotes) {
        StringBuilder payload = new StringBuilder();
        if (property instanceof RefProperty) {
            Model model = definitions.get(((RefProperty) property).getSimpleRef());
            payload.append("{");

            if (model.getProperties() != null) {
                for (Map.Entry<String, Property> entry : model.getProperties().entrySet()) {
                    payload.append("\"").append(entry.getKey()).append("\": ").append(createValidationExpression(entry.getValue(), definitions, quotes)).append(",");
                }
            }

            if (payload.toString().endsWith(",")) {
                payload.replace(payload.length() - 1, payload.length(), "");
            }

            payload.append("}");
        } else if (property instanceof ArrayProperty) {
            if (quotes) {
                payload.append("\"");
            }

            payload.append("@ignore@");

            if (quotes) {
                payload.append("\"");
            }
        } else if (property instanceof StringProperty) {
            if (quotes) {
                payload.append("\"");
            }

            if (StringUtils.hasText(((StringProperty) property).getPattern())) {
                payload.append("@matches(").append(((StringProperty) property).getPattern()).append(")@");
            } else if (!CollectionUtils.isEmpty(((StringProperty) property).getEnum())) {
                payload.append("@matches(").append(((StringProperty) property).getEnum().stream().collect(Collectors.joining("|"))).append(")@");
            } else {
                payload.append("@notEmpty()@");
            }

            if (quotes) {
                payload.append("\"");
            }
        } else if (property instanceof DateProperty) {
            if (quotes) {
                payload.append("\"");
            }

            payload.append("@matchesDatePattern('yyyy-MM-dd')@");

            if (quotes) {
                payload.append("\"");
            }
        } else if (property instanceof DateTimeProperty) {
            if (quotes) {
                payload.append("\"");
            }

            payload.append("@matchesDatePattern('yyyy-MM-dd'T'hh:mm:ss')@");

            if (quotes) {
                payload.append("\"");
            }
        } else if (property instanceof IntegerProperty || property instanceof LongProperty) {
            if (quotes) {
                payload.append("\"");
            }

            payload.append("@isNumber()@");

            if (quotes) {
                payload.append("\"");
            }
        } else if (property instanceof FloatProperty || property instanceof DoubleProperty) {
            if (quotes) {
                payload.append("\"");
            }

            payload.append("@isNumber()@");

            if (quotes) {
                payload.append("\"");
            }
        } else if (property instanceof BooleanProperty) {
            if (quotes) {
                payload.append("\"");
            }

            payload.append("@matches(true|false)@");

            if (quotes) {
                payload.append("\"");
            }
        } else {
            if (quotes) {
                payload.append("\"");
            }

            payload.append("@ignore@");

            if (quotes) {
                payload.append("\"");
            }
        }

        return payload.toString();
    }

    /**
     * Create validation expression using functions according to parameter type and format.
     * @param parameter
     * @return
     */
    public static String createValidationExpression(AbstractSerializableParameter parameter) {
        switch (parameter.getType()) {
            case "integer":
                return "@isNumber()@";
            case "string":
                if (parameter.getFormat() != null && parameter.getFormat().equals("date")) {
                    return "\"@matchesDatePattern('yyyy-MM-dd')@\"";
                } else if (parameter.getFormat() != null && parameter.getFormat().equals("date-time")) {
                    return "\"@matchesDatePattern('yyyy-MM-dd'T'hh:mm:ss')@\"";
                } else if (StringUtils.hasText(parameter.getPattern())) {
                    return "\"@matches(" + parameter.getPattern() + ")@\"";
                } else if (!CollectionUtils.isEmpty(parameter.getEnum())) {
                    return "\"@matches(" + (parameter.getEnum().stream().collect(Collectors.joining("|"))) + ")@\"";
                } else {
                    return "@notEmpty()@";
                }
            case "boolean":
                return "@matches(true|false)@";
            default:
                return "@ignore@";
        }
    }

    /**
     * Create random value expression using functions according to parameter type and format.
     * @param parameter
     * @return
     */
    public static String createRandomValueExpression(AbstractSerializableParameter parameter) {
        switch (parameter.getType()) {
            case "integer":
                return "citrus:randomNumber(8)";
            case "string":
                if (parameter.getFormat() != null && parameter.getFormat().equals("date")) {
                    return "\"citrus:currentDate('yyyy-MM-dd')\"";
                } else if (parameter.getFormat() != null && parameter.getFormat().equals("date-time")) {
                    return "\"citrus:currentDate('yyyy-MM-dd'T'hh:mm:ss')\"";
                } else if (StringUtils.hasText(parameter.getPattern())) {
                    return "\"citrus:randomValue(" + parameter.getPattern() + ")\"";
                } else if (!CollectionUtils.isEmpty(parameter.getEnum())) {
                    return "\"citrus:randomEnumValue(" + (parameter.getEnum().stream().collect(Collectors.joining(","))) + ")\"";
                } else if (Optional.ofNullable(parameter.getFormat()).orElse("").equalsIgnoreCase("uuid")){
                    return "citrus:randomUUID()";
                } else {
                    return "citrus:randomString(10)";
                }
            case "boolean":
                return "true";
            default:
                return "";
        }
    }

}
