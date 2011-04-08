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
package org.forgerock.commons.json.schema.validator.helpers;

import org.forgerock.commons.json.schema.validator.ErrorHandler;
import org.forgerock.commons.json.schema.validator.exceptions.SchemaException;
import org.forgerock.commons.json.schema.validator.exceptions.ValidationException;
import org.forgerock.commons.json.schema.validator.validators.SimpleValidator;

import java.util.List;

import static org.forgerock.commons.json.schema.validator.Constants.ERROR_MSG_ENUM_VIOLATION;

/**
 * This class implements "enum" validation on all types of objects as defined in
 * the paragraph 5.19 of the JSON Schema specification.
 * <p/>
 * This implementation relies on the <code>equals</code> method.
 *
 * @author $author$
 * @version $Revision$ $Date$
 * @see <a href="http://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.19">enum</a>
 */
public class EnumHelper implements SimpleValidator<Object> {

    private List<Object> enumValues;

    public EnumHelper(List<Object> enumValues) {
        this.enumValues = enumValues;
    }

    public void validate(Object node, String at, ErrorHandler handler) throws SchemaException {
        if (!enumValues.contains(node)) {
            handler.error(new ValidationException(ERROR_MSG_ENUM_VIOLATION, at));
        }
    }
}
