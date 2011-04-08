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
package org.forgerock.commons.json.schema.validator;

import org.forgerock.commons.json.schema.validator.exceptions.SchemaException;
import org.forgerock.commons.json.schema.validator.exceptions.ValidationException;
import org.forgerock.commons.json.schema.validator.validators.Validator;

/**
 * FailFastErrorHandler implements the {@link ErrorHandler} in a way it re-throws the exception
 * at first time.
 * <p/>
 * The exception prevents the validator to continue the validation of an already invalid object.
 *
 * @author $author$
 * @version $Revision$ $Date$
 */
public class FailFastErrorHandler extends ErrorHandler {
    private SchemaException ex = null;

    /**
     * {@inheritDoc}
     */
    @Override
    public void error(ValidationException exception) throws SchemaException {
        this.ex = exception;
        throw ex;
    }

    /**
     * @throws ValidationException when there is any error wrapped inside the handler.
     */
    @Override
    public void assembleException() throws ValidationException {
        if (ex instanceof ValidationException) {
            throw (ValidationException) ex;
        } else if (null != ex) {
            throw new ValidationException(ex, Validator.AT_ROOT);
        }
    }
}
