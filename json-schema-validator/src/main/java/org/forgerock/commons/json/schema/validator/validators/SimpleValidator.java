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

/**
 * SimpleValidator is a base interface for all validator implementation.
 *
 * @author $author$
 * @version $Revision$ $Date$
 */
public interface SimpleValidator<T> {
    /**
     * Validates the <code>node</code> value against the embedded schema object.
     * <p/>
     * The selected error handler defines the behaviour of the validator. The
     * {@link org.forgerock.commons.json.schema.validator.FailFastErrorHandler} throws exception at firs violation.
     * Other customised {@link ErrorHandler} can collect all exceptions and after the validation the
     * examination of the <code>handler</code> contains the final result.
     *
     * @param node      value to validate
     * @param at        JSONPath of the node. null means it's the root node
     * @param handler   customised error handler like {@link org.forgerock.commons.json.schema.validator.FailFastErrorHandler}
     * @throws SchemaException when the <code>node</code> violates with the schema
     */
    public void validate(T node, String at, ErrorHandler handler) throws SchemaException;
}
