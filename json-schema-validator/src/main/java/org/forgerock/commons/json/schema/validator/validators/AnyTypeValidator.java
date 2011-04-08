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
import org.forgerock.commons.json.schema.validator.exceptions.SchemaException;
import org.forgerock.commons.json.schema.validator.exceptions.ValidationException;

import java.util.Map;

import static org.forgerock.commons.json.schema.validator.Constants.*;

/**
 * AnyTypeValidator applies all the constraints of a <code>any</code> type.
 * <p/>
 * Sample JSON Schema:
 * </code>
 * {
 * "type"             : "any",
 * "required"         : false
 * }
 * </code>
 * Any object is valid unless it's required and the <code>node</code> value is null.
 *
 * @author $author$
 * @version $Revision$ $Date$
 * @see <a href="http://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.1">type</a>
 */
public class AnyTypeValidator extends Validator {

    public AnyTypeValidator(Map<String, Object> schema) {
        super(schema);
    }

    /**
     * {@inheritDoc}
     */
    public void validate(Object node, String at, ErrorHandler handler) throws SchemaException {
        if (required && null == node) {
            handler.error(new ValidationException(ERROR_MSG_REQUIRED_PROPERTY, getPath(at, null)));
        }
    }
}
