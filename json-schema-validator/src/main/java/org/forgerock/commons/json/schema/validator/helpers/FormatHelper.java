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
import org.forgerock.commons.json.schema.validator.validators.SimpleValidator;

import static org.forgerock.commons.json.schema.validator.Constants.FORMAT_DATE;
import static org.forgerock.commons.json.schema.validator.Constants.FORMAT_DATE_TIME;

/**
 * This class implements "format" validation on primitive types of objects as defined in
 * the paragraph 5.23 of the JSON Schema specification.
 * <p/>
 * Additional custom formats MAY be created.  These custom formats MAY
 * be expressed as an URI, and this URI MAY reference a schema of that
 * format.
 *
 * @author $author$
 * @version $Revision$ $Date$
 * @see <a href="http://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.23">format</a>
 */
public class FormatHelper implements SimpleValidator<Object> {

    public FormatHelper(String format) {
        if (FORMAT_DATE_TIME.equals(format)) {
        } else if (FORMAT_DATE.equals(format)) {
        }
    }

    public void validate(Object node, String at, ErrorHandler handler) throws SchemaException {
        //TODO: implements
    }
}
