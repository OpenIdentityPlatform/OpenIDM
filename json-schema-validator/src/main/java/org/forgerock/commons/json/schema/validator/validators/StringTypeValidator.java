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

import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;
import org.forgerock.commons.json.schema.validator.ErrorHandler;
import org.forgerock.commons.json.schema.validator.exceptions.SchemaException;
import org.forgerock.commons.json.schema.validator.exceptions.ValidationException;
import org.forgerock.commons.json.schema.validator.helpers.EnumHelper;
import org.forgerock.commons.json.schema.validator.helpers.FormatHelper;

import java.util.List;
import java.util.Map;

import static org.forgerock.commons.json.schema.validator.Constants.*;

/**
 * StringTypeValidator applies all the constraints of a <code>string</code> type.
 * <p/>
 * Sample JSON Schema:
 * </code>
 * {
 * "type"        : "string",
 * "required"    : true,
 * "minLength"   : 1,
 * "maxLength"   : 8,
 * "enum" : [
 * " ",
 * "number1",
 * "number2",
 * "123456789"
 * ],
 * "pattern-fix" : ".*",
 * "format-fix"  : "date"
 * }
 * </code>
 *
 * @author $author$
 * @version $Revision$ $Date$
 * @see <a href="http://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.1">type</a>
 */
public class StringTypeValidator extends Validator {

    /**
     * When the instance value is a string, this provides a regular
     * expression that a string instance MUST match in order to be valid.
     * Regular expressions SHOULD follow the regular expression
     * specification from ECMA 262/Perl 5
     */
    private Pattern p = null;
    /**
     * When the instance value is a string, this defines the minimum length
     * of the string.
     */
    private int minLength = -1;
    /**
     * When the instance value is a string, this defines the maximum length
     * of the string.
     */
    private int maxLength = -1;
    /**
     * This provides an enumeration of all possible values that are valid
     * for the instance property.  This MUST be an array, and each item in
     * the array represents a possible value for the instance value.  If
     * this attribute is defined, the instance value MUST be one of the
     * values in the array in order for the schema to be valid.
     */
    private EnumHelper enumHelper = null;
    /**
     * This property defines the validators of data, content validators, or microformat
     * to be expected in the instance property values.  A format attribute
     * MAY be one of the values listed below, and if so, SHOULD adhere to
     * the semantics describing for the format.  A format SHOULD only be
     * used to give meaning to primitive types (string, integer, number, or
     * boolean).  Validators MAY (but are not required to) validate that the
     * instance values conform to a format
     */
    private FormatHelper formatHelper = null;

    public StringTypeValidator(Map<String, Object> schema) {
        super(schema);
        for (Map.Entry<String, Object> e : schema.entrySet()) {
            if (PATTERN.equals(e.getKey())) {
                if (e.getValue() instanceof String) {
                    String pattern = (String) e.getValue();

                    try {
                        Perl5Compiler compiler = new Perl5Compiler();
                        p = compiler.compile(pattern);
                    } catch (MalformedPatternException e1) {
                        //LOG.error("Failed to apply pattern on " + at + ": Invalid RE syntax [" + pattern + "]", pse);
                    }

                    //try {

                    //    Pattern  p = null;

                    //} catch (PatternSyntaxException pse) {
                    //LOG.error("Failed to apply pattern on " + at + ": Invalid RE syntax [" + pattern + "]", pse);
                    //}
                }
            } else if (REQUIRED.equals(e.getKey())) {
                if (e.getValue() instanceof Boolean) {
                    required = ((Boolean) e.getValue());
                } else if (e.getValue() instanceof String) {
                    required = Boolean.parseBoolean((String) e.getValue());
                }
            } else if (MINLENGTH.equals(e.getKey())) {
                if (e.getValue() instanceof Number) {
                    minLength = ((Number) e.getValue()).intValue();
                }
            } else if (MAXLENGTH.equals(e.getKey())) {
                if (e.getValue() instanceof Number) {
                    maxLength = ((Number) e.getValue()).intValue();
                }
            } else if (ENUM.equals(e.getKey())) {
                if (e.getValue() instanceof List) {
                    enumHelper = new EnumHelper((List<Object>) e.getValue());
                }
            } else if (FORMAT.equals(e.getKey())) {
                if (e.getValue() instanceof String) {
                    formatHelper = new FormatHelper((String) e.getValue());
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void validate(Object node, String at, ErrorHandler handler) throws SchemaException {
        if (node instanceof String) {
            String nodeValue = (String) node;
            if (minLength > -1 && nodeValue.length() < minLength) {
                handler.error(new ValidationException("minLength error", getPath(at, null)));
            }
            if (maxLength > -1 && nodeValue.length() > maxLength) {
                handler.error(new ValidationException("maxLength error", getPath(at, null)));
            }
            if (null != p) {
                Perl5Matcher matcher = new Perl5Matcher();
                //Matcher m = p.matcher(nodeValue);
                //if (!m.matches()) {
                //    handler.error(new ValidationException(getPath(at, null) + ": does not match the regex pattern " + p.pattern(), getPath(at, null)));
                //}

                if (!matcher.matches(nodeValue, p)) {
                    System.out.println("PATTERN: " + p.getPattern());
                    handler.error(new ValidationException(getPath(at, null) + ": does not match the regex pattern " + p.getPattern(), getPath(at, null)));

                }

            }
            if (null != enumHelper) {
                enumHelper.validate(node, at, handler);
            }
            if (null != formatHelper) {
                formatHelper.validate(node, at, handler);
            }
        } else if (null != node) {
            handler.error(new ValidationException(ERROR_MSG_TYPE_MISMATCH, getPath(at, null)));
        } else if (required) {
            handler.error(new ValidationException(ERROR_MSG_REQUIRED_PROPERTY, getPath(at, null)));
        }
    }
}
