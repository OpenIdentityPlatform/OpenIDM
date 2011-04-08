/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * $Id$
 */
package org.forgerock.commons.json.schema.validator.validators;

import org.apache.oro.text.regex.Perl5Matcher;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Pattern;
import org.forgerock.commons.json.schema.validator.ErrorHandler;
import org.forgerock.commons.json.schema.validator.ObjectValidatorFactory;
import org.forgerock.commons.json.schema.validator.exceptions.SchemaException;
import org.forgerock.commons.json.schema.validator.exceptions.ValidationException;
import org.forgerock.commons.json.schema.validator.helpers.EnumHelper;

import java.util.*;

import static org.forgerock.commons.json.schema.validator.Constants.*;

/**
 * ObjectTypeValidator applies all the constraints of a <code>object</code> type.
 * <p/>
 * Sample JSON Schema:
 * </code>
 * {
 * "type"        : "object"
 * }
 * </code>
 *
 * @author $author$
 * @version $Revision$ $Date$
 * @see <a href="http://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.1">type</a>
 */
public class ObjectTypeValidator extends Validator {

    /**
     * This attribute is an object with property definitions that define the
     * valid values of instance object property values.  When the instance
     * value is an object, the property values of the instance object MUST
     * conform to the property definitions in this object.  In this object,
     * each property definition's value MUST be a schema, and the property's
     * name MUST be the name of the instance property that it defines.  The
     * instance property value MUST be valid according to the schema from
     * the property definition.  Properties are considered unordered, the
     * order of the instance properties MAY be in any order.
     */
    private Map<String, PropertyValidatorBag> propertyValidators;
    private Set<String> propertyNames;

    /**
     * This attribute is an object that defines the requirements of a
     * property on an instance object.  If an object instance has a property
     * with the same name as a property in this attribute's object, then the
     * instance must be valid against the attribute's property value
     * (hereafter referred to as the "dependency value").
     * <p/>
     * The dependency value can take one of two forms:
     * <p/>
     * Simple Dependency  If the dependency value is a string, then the
     * instance object MUST have a property with the same name as the
     * dependency value.  If the dependency value is an array of strings,
     * then the instance object MUST have a property with the same name
     * as each string in the dependency value's array.
     * <p/>
     * Schema Dependency  If the dependency value is a schema, then the
     * instance object MUST be valid against the schema.
     */
    private Map<String, Validator> dependenciesValidators;
    private Map<String, Set<String>> dependencyValues;

    /**
     * This attribute is an object that defines the schema for a set of
     * property names of an object instance.  The name of each property of
     * this attribute's object is a regular expression pattern in the ECMA
     * 262/Perl 5 format, while the value is a schema.  If the pattern
     * matches the name of a property on the instance object, the value of
     * the instance's property MUST be valid against the pattern name's
     * schema value.
     */
    private Map<Pattern, Validator> patternPropertyValidators;

    /**
     * This attribute defines a schema for all properties that are not
     * explicitly defined in an object type definition.  If specified, the
     * value MUST be a schema or a boolean.  If false is provided, no
     * additional properties are allowed beyond the properties defined in
     * the schema.  The default value is an empty schema which allows any
     * value for additional properties.
     */
    private boolean allowAdditionalProperties = true;
    private Validator additionalPropertyValidator = null;
    /**
     * This provides an enumeration of all possible values that are valid
     * for the instance property.  This MUST be an array, and each item in
     * the array represents a possible value for the instance value.  If
     * this attribute is defined, the instance value MUST be one of the
     * values in the array in order for the schema to be valid.
     */
    private EnumHelper enumHelper = null;
    /**
     * The value of this property MUST be another schema which will provide
     * a base schema which the current schema will inherit from.  The
     * inheritance rules are such that any instance that is valid according
     * to the current schema MUST be valid according to the referenced
     * schema.  This MAY also be an array, in which case, the instance MUST
     * be valid for all the schemas in the array.  A schema that extends
     * another schema MAY define additional attributes, constrain existing
     * attributes, or add other constraints.
     * <p/>
     * Conceptually, the behavior of extends can be seen as validating an
     * instance against all constraints in the extending schema as well as
     * the extended schema(s).  More optimized implementations that merge
     * schemas are possible, but are not required.
     */
    private List<Validator> extendsValidators = null;
    private Perl5Matcher matcher = new Perl5Matcher();

    public ObjectTypeValidator(Map<String, Object> schema) {
        super(schema);
        Map<String, Object> objectProperties = (Map<String, Object>) schema.get(PROPERTIES);
        if (null != objectProperties) {
            //Map<String, Object> properties = (Map<String, Object>) e.getValue();
            propertyValidators = new HashMap<String, PropertyValidatorBag>(objectProperties.size());

            for (Map.Entry<String, Object> entry : objectProperties.entrySet()) {
                Validator validator = ObjectValidatorFactory.getTypeValidator((Map<String, Object>) entry.getValue());
                propertyValidators.put(entry.getKey(), new PropertyValidatorBag(validator));
            }
            propertyNames = Collections.unmodifiableSet(propertyValidators.keySet());
        } else {
            propertyValidators = Collections.emptyMap();
            propertyNames = Collections.emptySet();
        }
        for (Map.Entry<String, Object> e : schema.entrySet()) {
            if (ADDITIONALPROPERTIES.equals(e.getKey())) {
                if (e.getValue() instanceof Boolean && !((Boolean) e.getValue()) || (e.getValue() instanceof String && e.getValue().equals("false"))) {
                    allowAdditionalProperties = false;
                } else if (e.getValue() != null && e.getValue() instanceof Map) {
                    additionalPropertyValidator = ObjectValidatorFactory.getTypeValidator((Map<String, Object>) e.getValue());
                }
            } else if (PATTERNPROPERTIES.equals(e.getKey())) {
                if (e.getValue() instanceof Map) {

                    Map<String, Object> properties = (Map<String, Object>) e.getValue();
                    patternPropertyValidators = new HashMap<Pattern, Validator>(properties.size());

                    for (Map.Entry<String, Object> entry : properties.entrySet()) {
                        try {
                            Perl5Compiler compiler = new Perl5Compiler();
                            Pattern p = compiler.compile(entry.getKey());

                            Validator validator = ObjectValidatorFactory.getTypeValidator((Map<String, Object>) entry.getValue());
                            patternPropertyValidators.put(p, validator);
                            for (Map.Entry<String, PropertyValidatorBag> schemaProperty : propertyValidators.entrySet()) {
                                if (matcher.matches(schemaProperty.getKey(), p)) {
                                    schemaProperty.getValue().addPatternValidator(validator);
                                }
                            }

                        } catch (MalformedPatternException ex) {
                            //LOG.error("Failed to apply pattern on " + at + ": Invalid RE syntax [" + p.getPattern() + "]", pse);
                        }

                        /*try {
                            Pattern p = Pattern.compile(entry.getKey());
                            Validator validator = ObjectValidatorFactory.getTypeValidator((Map<String, Object>) entry.getValue());
                            patternPropertyValidators.put(p, validator);
                            for (Map.Entry<String, PropertyValidatorBag> schemaProperty : propertyValidators.entrySet()) {
                                if (p.matcher(schemaProperty.getKey()).matches()) {
                                    schemaProperty.getValue().addPatternValidator(validator);
                                }
                            }
                        } catch (PatternSyntaxException pse) {
                            //LOG.error("Failed to apply pattern on " + at + ": Invalid RE syntax [" + pattern + "]", pse);
                        }     */
                    }
                }
            } else if (DEPENDENCIES.equals(e.getKey())) {

                if (e.getValue() instanceof Map) {
                    for (Map.Entry<String, Object> d : ((Map<String, Object>) e.getValue()).entrySet()) {
                        PropertyValidatorBag validator = propertyValidators != null ? propertyValidators.get(d.getKey()) : null;
                        if (null != validator) {
                            if (d.getValue() instanceof Map) {
                                validator.setDependencyValidator(ObjectValidatorFactory.getTypeValidator((Map<String, Object>) d.getValue()));
                            } else {
                                validator.setDependencyValue(d.getValue());
                            }
                        } else {
                            if (null == dependencyValues) {
                                dependencyValues = new HashMap<String, Set<String>>(1);
                            }
                            if (d.getValue() instanceof Map) {
                                // @TODO: Validate additional properties
                                //validator.setDependencyValidator(ObjectValidatorFactory.getTypeValidator((Map<String, Object>) d.getValue()));
                            } else if (d.getValue() instanceof String) {
                                Set<String> requiredAttributeSet = new HashSet<String>(1);
                                requiredAttributeSet.add((String) d.getValue());
                                dependencyValues.put(d.getKey(), requiredAttributeSet);
                            } else if (d.getValue() instanceof Collection) {
                                dependencyValues.put(d.getKey(), new HashSet<String>((Collection<String>) d.getValue()));
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void validate(Object node, String at, ErrorHandler handler) throws SchemaException {

        if (node instanceof Map) {
            Set<String> additionalPropertyNames = new HashSet<String>(((Map<String, Object>) node).keySet());
            additionalPropertyNames.removeAll(propertyNames);

            if (!allowAdditionalProperties && !additionalPropertyNames.isEmpty()) {
                // @TODO: Add exception message: Additional Properties not allowed
                handler.error(new ValidationException("Error: Additional Properties not allowed", getPath(at, null)));
            }

            Set<String> instancePropertyKeySet = Collections.unmodifiableSet(((Map<String, Object>) node).keySet());

            for (Map.Entry<String, PropertyValidatorBag> schemaProperty : propertyValidators.entrySet()) {
                Map.Entry<String, Object> entry = null;
                //null == entry.getValue() can not used for Null type
                for (Map.Entry<String, Object> instanceProperty : ((Map<String, Object>) node).entrySet()) {
                    if (schemaProperty.getKey().equals(instanceProperty.getKey())) {
                        entry = instanceProperty;
                        break;
                    }
                }
                if (null != entry) {
                    schemaProperty.getValue().validate(entry.getValue(), instancePropertyKeySet, getPath(at, schemaProperty.getKey()), handler);
                } else if (schemaProperty.getValue().isRequired()) {
                    // @TODO: Add exception message: Required property value is null
                    handler.error(new ValidationException("", getPath(at, schemaProperty.getKey())));
                }
            }

            for (String additionalPropertyName : additionalPropertyNames) {
                Object propertyNode = ((Map<String, Object>) node).get(additionalPropertyName);

                if (null != additionalPropertyValidator) {
                    additionalPropertyValidator.validate(propertyNode, getPath(at, additionalPropertyName), handler);
                }

                // @TODO: Implement Dependency check
                Validator dependencyPropertyValidator = null != dependenciesValidators ? dependenciesValidators.get(additionalPropertyName) : null;
                if (null != dependencyPropertyValidator) {
                    dependencyPropertyValidator.validate(propertyNode, getPath(at, additionalPropertyName), handler);
                }

                if (null != patternPropertyValidators) {
                    for (Map.Entry<Pattern, Validator> v : patternPropertyValidators.entrySet()) {
                        if (matcher.matches(additionalPropertyName, v.getKey())) {
                            v.getValue().validate(propertyNode, getPath(at, additionalPropertyName), handler);
                        }
                    }
                }
            }

//            Map<String, Object> instanceProperties = (Map<String, Object>) node;
//
//            if (dependenciesList != null && !dependenciesList.isEmpty()) {
//                for (String dependency : dependenciesList) {
//                    if (instanceProperties.get(dependency) == null) {
//                        handler.error(new ValidationException("", getPath(at, dependency))); // @TODO: Add exception message to contants
//                    }
//                }
//            }
//
//            if (dependenciesValidators != null) {
//                for (Map.Entry<String, Validator> validator : dependenciesValidators.entrySet()) {
//                    Validator dependencyValidator = validator.getValue();
//
//                    dependencyValidator.validate(instanceProperties.get(validator.getKey()), at, handler);
//                }
//            }
//
//            if (patternPropertisValidators != null) {
//                for (Map.Entry<String, Validator> validator : patternPropertisValidators.entrySet()) {
//                    for (Map.Entry<String, Object> property : instanceProperties.entrySet()) {
//                        if (property.getKey().matches(validator.getKey())) {
//                            Validator patternValidator = validator.getValue();
//
//                            patternValidator.validate(instanceProperties.get(property.getKey()), getPath(at, property.getKey()), handler);
//                        }
//                    }
//                }
//            }
//
//            for (Map.Entry<String, Validator> validator : properties.entrySet()) {
//                Validator propertyValidator = validator.getValue();
//
//                propertyValidator.validate(instanceProperties.get(validator.getKey()), getPath(at, validator.getKey()), handler);
//
//                instanceProperties.remove(validator.getKey());
//            }
//
//            if (allowAdditionalProperties && additionalPropertyValidators != null && instanceProperties.size() > 0) {
//
//                for (Map.Entry<String, Object> instanceProperty : instanceProperties.entrySet()) {
//                    if (additionalPropertyValidators.containsKey(instanceProperty.getKey())) {
//
//                        Validator propertyValidator = additionalPropertyValidators.get(instanceProperty.getKey());
//
//                        propertyValidator.validate(instanceProperty.getValue(), getPath(at, instanceProperty.getKey()), handler);
//                    } else {
//                        handler.error(new ValidationException("", getPath(at, instanceProperty.getKey()))); // @TODO: Add exception message to contants
//                    }
//                }
//            } else if (!allowAdditionalProperties && instanceProperties.size() > 0) {
//                // additional properties exist but is not allowed
//                handler.error(new ValidationException(ERROR_MSG_ADDITIONAL_PROPERTIES)); // @TODO: Add exception message to contants
//            }
        } else if (null != node) {
            handler.error(new ValidationException(ERROR_MSG_TYPE_MISMATCH, getPath(at, null)));
        } else if (required) {
            handler.error(new ValidationException(ERROR_MSG_REQUIRED_PROPERTY, getPath(at, null)));
        }
    }

    private class PropertyValidatorBag implements SimpleValidator<Object> {
        private Validator propertyValidator;
        private Set<Validator> patternValidators = null;
        private Validator dependencyValidator = null;
        private Set<String> requiredProperties = null;

        private PropertyValidatorBag(Validator propertyValidator) {
            this.propertyValidator = propertyValidator;
        }

        private void addPatternValidator(final Validator v) {
            if (null == patternValidators) {
                patternValidators = new HashSet<Validator>(1);
            }
            patternValidators.add(v);
        }

        private void setDependencyValidator(Validator dependencyValidator) {
            this.dependencyValidator = dependencyValidator;
        }

        private void setDependencyValue(Object dependencyValue) {
            if (dependencyValue instanceof String) {
                requiredProperties = new HashSet<String>(1);
                requiredProperties.add((String) dependencyValue);
            } else if (dependencyValue instanceof Collection) {
                requiredProperties = new HashSet<String>((Collection<String>) dependencyValue);
            }
        }


        private boolean isRequired() {
            return propertyValidator.isRequired();
        }

        public void validate(Object node, String at, ErrorHandler handler) throws SchemaException {
            propertyValidator.validate(node, at, handler);
            if (null != patternValidators) {
                for (Validator v : patternValidators) {
                    v.validate(node, at, handler);
                }
            }
            if (null != dependencyValidator) {
                dependencyValidator.validate(node, at, handler);
            }
        }


        public void validate(Object node, Set<String> propertyKeySet, String at, ErrorHandler handler) throws SchemaException {
            if (null != requiredProperties && !propertyKeySet.containsAll(requiredProperties)) {
                handler.error(new ValidationException("Dependency ERROR: Missiong properties", at));
            }
            validate(node, at, handler);
        }
    }
}