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

import org.forgerock.commons.json.schema.validator.ErrorHandler;
import org.forgerock.commons.json.schema.validator.ObjectValidatorFactory;
import org.forgerock.commons.json.schema.validator.exceptions.SchemaException;
import org.forgerock.commons.json.schema.validator.exceptions.ValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.forgerock.commons.json.schema.validator.Constants.*;

/**
 * Union Types  An array of two or more simple validators definitions.  Each
 * item in the array MUST be a simple validators definition or a schema.
 * The instance value is valid if it is of the same validators as one of
 * the simple validators definitions, or valid by one of the schemas, in
 * the array.
 * <p/>
 * <p>
 * For example, a schema that defines if an instance can be a string or
 * a number would be:</p>
 * <p/>
 * <code>{"type":["string","number"]}</code>
 *
 * @author $author$
 * @version $Revision$ $Date$
 * @see <a href="http://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.1">type</a>
 */
public class UnionTypeValidator extends Validator {

    private List<Validator> validators;

    public UnionTypeValidator(Map<String, Object> schema) {
        super(schema);
        this.validators = new ArrayList<Validator>(2);
        for (Object o : (List<Object>) schema.get(TYPE)) {
            if (o instanceof String) {
                validators.add(ObjectValidatorFactory.getTypeValidator((String) o, schema));
            } else if (o instanceof Map) {
                Validator v = ObjectValidatorFactory.getTypeValidator((Map<String, Object>) o);
                validators.add(v);
                required = v instanceof NullTypeValidator ? required = false : required;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void validate(Object node, String at, ErrorHandler handler) throws SchemaException {
        for (Validator v : validators) {
            try {
                v.validate(node, at, new ErrorHandler() {
                    @Override
                    public void error(ValidationException exception) throws SchemaException {
                        throw exception;
                    }

                    @Override
                    public void assembleException() throws ValidationException {
                        throw new UnsupportedOperationException("Not supported yet.");
                    }
                });
                return;
            } catch (ValidationException e) {
                //Only one helpers should success to be overall success.
            }
        }
        handler.error(new ValidationException("Invalid union validators.", getPath(at, null)));
    }
}
