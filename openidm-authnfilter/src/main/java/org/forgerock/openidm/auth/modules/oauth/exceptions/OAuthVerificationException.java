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
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.openidm.auth.modules.oauth.exceptions;

/**
 * Problem during the verification of an OAuth module.
 */
public class OAuthVerificationException extends Exception {
    static final long serialVersionUID = 1L;

    /**
     * Construct the exception.
     */
    public OAuthVerificationException() {
        super();
    }

    /**
     * Construct the exception with the given message.
     * @param message The message.
     */
    public OAuthVerificationException(final String message) {
        super(message);
    }

    /**
     * Construct the exception with the given message and cause.
     * @param message The message.
     * @param cause The cause.
     */
    public OAuthVerificationException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Construct the exception with the given cause.
     * @param cause The cause.
     */
    public OAuthVerificationException(final Throwable cause) {
        super(cause);
    }
}
