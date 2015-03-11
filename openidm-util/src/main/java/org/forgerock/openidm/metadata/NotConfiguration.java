/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All Rights Reserved
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
 */
package org.forgerock.openidm.metadata;

import java.io.Serializable;

/**
 * An exception that indicates that a meta data provider knows meta-data is
 * necessary for a given configuration, but it's not a Configuration.
 * 
 */
public class NotConfiguration extends Exception implements Serializable {

    /** Serializable class a version number. */
    private static final long serialVersionUID = 1L;


    /**
     * Constructs a new exception with {@code null} as its detail message.
     */
    public NotConfiguration() {
    }

    /**
     * Constructs a new exception with the specified detail message.
     */
    public NotConfiguration(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified cause.
     */
    public NotConfiguration(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     */
    public NotConfiguration(String message, Throwable cause) {
        super(message, cause);
    }
}
