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
 * Copyright 2013-2014 ForgeRock AS.
 */

package org.forgerock.openidm.jaspi.modules;

import org.forgerock.jaspi.modules.session.jwt.JwtSessionModule;
import org.forgerock.jaspi.modules.session.openam.OpenAMSessionModule;

import javax.security.auth.message.module.ServerAuthModule;

/**
 * Enum that represents all the core IDM Authentication modules.
 *
 * @author Phill Cunnington
 */
public enum IDMAuthModule {

    /** JWT Session Auth Module. */
    JWT_SESSION(JwtSessionModule.class),
    /** OpenAM Session Auth Module. */
    OPENAM_SESSION(OpenAMSessionModule.class),
    /** Managed User Auth Module. */
    MANAGED_USER(ManagedUserAuthModule.class),
    /** Internal User Auth Module. */
    INTERNAL_USER(InternalUserAuthModule.class),
    /** Passthrough to OpenICF connector Auth Module. */
    PASSTHROUGH(PassthroughModule.class),
    /** IWA Auth Module. */
    IWA(IWAModule.class),
    /** IWA and Passthrough Auth Module. */
    IWA_PASSTHROUGH(IWAPassthroughModule.class);

    private Class<? extends ServerAuthModule> clazz;

    /**
     * Constructs a new IDMAuthModule.
     *
     * @param clazz The corresponding class of the authentication module.
     */
    private IDMAuthModule(Class<? extends ServerAuthModule> clazz) {
        this.clazz = clazz;
    }

    /**
     * Gets the corresponding class of the authentication module.
     *
     * @return The authentication modules class.
     */
    public Class<? extends ServerAuthModule> getAuthModuleClass() {
        return clazz;
    }
}
