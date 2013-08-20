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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 */
package org.forgerock.openidm.config.persistence;

/**
 * Interface implemented by OpenIDM 
 * PersistenceManager extension/implementation.
 * 
 * Allows the system to recognize when the extension is ready
 * to process configuration.
 */
public interface ConfigPersisterMarker {
    
    /**
     * Notifies the extension when the system determined that 
     * all necessary services are ready, and checks if the 
     * extension itself is ready
     * @throws an exception if the extension could not initiate
     */
    void checkReady() throws BootstrapFailure;
}
