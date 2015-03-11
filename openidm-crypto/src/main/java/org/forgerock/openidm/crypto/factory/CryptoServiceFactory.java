/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.crypto.factory;

import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.crypto.impl.CryptoServiceImpl;

/**
 * Cryptography Service Factory The cryptography service can be obtained either
 * though this factory, or the OSGi registry. This avoids bootstrap order issues
 * with decrypting configuration.
 */
public class CryptoServiceFactory {

    /**
     * A cryptography service newBuilder
     */
    private static CryptoServiceImpl instance;

    /**
     * Get a cryptography service newBuilder The cryptography service can be
     * obtained either through this factory, or preferrably the OSGi service
     * registry. This avoids bootstrap order issues with decrypting
     * configuration.
     *
     * @return a cryptography service newBuilder
     */
    public static synchronized CryptoService getInstance() {
        if (instance == null) {
            instance = new CryptoServiceImpl();
        }
        return instance;
    }
}
