/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openidm.patch.exception;

/**
 * A server side error occurred during the upgrade.
 *
 */
public class PostPatchException extends PatchException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new exception with {@code null} as its detail message.
     */
    public PostPatchException() {
    }

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message The detail message
     */
    public PostPatchException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail message and nested throwable.
     *
     * @param message The detail message
     * @param ex The throwable which caused the exception
     */
    public PostPatchException(String message, Throwable ex) {
        super(message, ex);
    }
}
