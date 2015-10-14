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
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.openidm.maintenance.upgrade;

/**
 * General exception during archive handling
 */
public class ArchiveException extends UpdateException {
    private static final long serialVersionUID = 1L;

    /**
     * Construct an ArchiveException with a String
     *
     * @param message the exception message string
     */
    ArchiveException(String message) {
        super(message);
    }

    /**
     * Construct an ArchiveException with a String and a cause
     *
     * @param message the exception message string
     * @param cause the cause exception this ArchiveException wraps
     */
    ArchiveException(String message, Exception cause) {
        super(message, cause);
    }
}
