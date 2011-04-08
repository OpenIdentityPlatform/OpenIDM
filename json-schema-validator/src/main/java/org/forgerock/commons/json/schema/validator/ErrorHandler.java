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

/**
 * ErrorHandler is the abstract base class for Validators.
 * <p>
 * If a Validator application needs to implementation of customized error
 * handling, it must implement this class.
 * <p/>
 * Use this handler when call the
 * {@link org.forgerock.commons.json.schema.validator.validators.SimpleValidator#validate(Object, String, ErrorHandler)}}
 * method.  The helpers will then report all errors.</p>
 *
 * @author $author$
 * @version $Revision$ $Date$
 */
public abstract class ErrorHandler {

    private boolean hasError = false;

    /**
     * Process the <code>exception</code> of the Validator.
     * <p/>
     * None of the implementations of {Validator#validate} method throws exception. They call
     * this method to decide what to do if an exception occurs.
     *
     * @param exception the exception that the validator wants to handled
     * @throws SchemaException when the implementation re-throws the <code>exception</code>
     */
    final void handleError(ValidationException exception) throws SchemaException {
        this.hasError = true;
        error(exception);
    }

    /**
     * Receive notification of an error.
     * <p/>
     * <p>For example, a validator would use this callback to
     * report the violation of a validity constraint.
     * The default behaviour is to take no action.</p>
     * <p/>
     * <p>The validator must continue to provide normal validation
     * after invoking this method: it should still be possible
     * for the application to process the document through to the end.
     * If the application cannot do so, then the parser should report
     * a fatal error.</p>
     * <p/>
     * <p>Filters may use this method to report other, non-JSON errors
     * as well.</p>
     *
     * @param exception The error information encapsulated in a
     *                  validation exception.
     * @throws ValidationException Any JSON exception, possibly
     *                             wrapping another exception.
     */
    public abstract void error(ValidationException exception)
            throws SchemaException;

    /**
     * Get the final result of the validation.
     * <p/>
     * The default value is <code>false</code>. If the validator has called the {#handleError} method
     * then it return <code>true</code>.
     *
     * @return true if there was an error during the validation process.
     */
    public boolean hasError() {
        return hasError;
    }

    ///////////////////////////////////////////////////////////////////////////////

    /**
     * @deprecated
     */
    final void throwException() throws ValidationException {
        if (hasError) {
            assembleException();
        }
    }

    /**
     * Throws an assembled exception after the validator finished the processing.
     * <p/>
     * Implementation of this method MUST throw an Exception if the {#error()} method
     * was called on this instance before.
     *
     * @throws ValidationException when this instance wraps an error message(s).
     * @deprecated
     */
    public abstract void assembleException()
            throws ValidationException;
}
