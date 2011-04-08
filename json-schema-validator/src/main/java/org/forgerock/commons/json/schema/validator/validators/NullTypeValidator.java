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
 * null  Value MUST be null.  Note this is mainly for purpose of
 * being able use union types to define nullability.  If this validators
 * is not included in a union, null values are not allowed (the
 * primitives do not allow nulls on their own.
 *
 * @author $author$
 * @version $Revision$ $Date$
 * @see <a href="http://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.1">type</a>
 */
public class NullTypeValidator extends Validator {

    public NullTypeValidator(Map<String, Object> schema) {
        super(schema);
    }

    public void validate(Object node, String at, ErrorHandler handler) throws SchemaException {
        if (null != node) {
            handler.error(new ValidationException(ERROR_MSG_NULL_TYPE, getPath(at, null)));
        }
    }
}
