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
package org.forgerock.commons.json.schema.validator.exceptions;

/**
 * Encapsulate a JSON validator error.
 * <p/>
 * <p>This exception may include information for locating the error
 * in the original JSON document object.  Note that although the application
 * will receive a ValidationException as the argument to the handlers
 * in the {@link org.forgerock.commons.json.schema.validator.ErrorHandler ErrorHandler} interface,
 * the application is not actually required to throw the exception;
 * instead, it can simply read the information in it and take a
 * different action.</p>
 * <p/>
 * <p>Since this exception is a subclass of {@link SchemaException
 * SchemaException}, it inherits the ability to wrap another exception.</p>
 *
 * @author $author$
 * @version $Revision$ $Date$
 * @see SchemaException
 */
public class ValidationException extends SchemaException {

    public ValidationException(String string) {
        super(string);
        this.path = null;
    }

    public ValidationException(String string, Throwable throwable) {
        super(string, throwable);
        this.path = null;
    }

    public ValidationException(String string, Throwable throwable, String path) {
        super(string, throwable);
        this.path = path;
    }

    public ValidationException(String message, String path) {
        super(message);
        this.path = path;
    }

    public ValidationException(Exception e, String path) {
        super(e);
        this.path = path;
    }

    public ValidationException(String message, Exception e, String path) {
        super(message, e);
        this.path = path;
    }

    //////////////////////////////////////////////////////////////////////
    // Internal state.
    //////////////////////////////////////////////////////////////////////


    /**
     * The dot–notated JSONPath to the violent node
     */
    private String path;
}
