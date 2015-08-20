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
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.openidm.jaspi.modules;

import org.forgerock.jaspi.modules.session.jwt.JwtSessionModule;
import org.forgerock.jaspi.modules.session.openam.OpenAMSessionModule;
import org.forgerock.jaspi.modules.iwa.IWAModule;
import org.forgerock.jaspi.modules.openid.OpenIdConnectModule;
import org.forgerock.openidm.jaspi.auth.AuthenticatorFactory;
import org.forgerock.openidm.jaspi.config.OSGiAuthnFilterHelper;

import javax.security.auth.message.module.ServerAuthModule;

/**
 * Enum that represents all the core IDM Authentication modules.
 */
public enum IDMAuthModule {

    /** JWT Session Auth Module. */
    JWT_SESSION(JwtSessionModule.class) {
        @Override
        public ServerAuthModule newInstance(OSGiAuthnFilterHelper authnFilterHelper) {
            return new JwtSessionModule();
        }
    },
    /** OpenAM Session Auth Module. */
    OPENAM_SESSION(OpenAMSessionModule.class) {
        @Override
        public ServerAuthModule newInstance(OSGiAuthnFilterHelper authnFilterHelper) {
            return new OpenAMSessionModule();
        }
    },
    /** Client-cert Auth Module. */
    CLIENT_CERT(ClientCertAuthModule.class) {
        @Override
        public ServerAuthModule newInstance(OSGiAuthnFilterHelper authnFilterHelper) {
            return new ClientCertAuthModule();
        }
    },
    /** Delegated auth module using an {@link org.forgerock.openidm.jaspi.auth.Authenticator} */
    DELEGATED(DelegatedAuthModule.class) {
        @Override
        public ServerAuthModule newInstance(OSGiAuthnFilterHelper authnFilterHelper) {
            return new DelegatedAuthModule(authnFilterHelper,
                    new AuthenticatorFactory(
                            authnFilterHelper.getConnectionFactory(),
                            authnFilterHelper.getCryptoService()));
        }
    },
    /** Managed User Auth Module. */
    MANAGED_USER(DelegatedAuthModule.class) {
        @Override
        public ServerAuthModule newInstance(OSGiAuthnFilterHelper authnFilterHelper) {
            return DELEGATED.newInstance(authnFilterHelper);
        }
    },
    /** Internal User Auth Module. */
    INTERNAL_USER(DelegatedAuthModule.class) {
        @Override
        public ServerAuthModule newInstance(OSGiAuthnFilterHelper authnFilterHelper) {
            return DELEGATED.newInstance(authnFilterHelper);
        }
    },
    /** Static User Auth Module. */
    STATIC_USER(DelegatedAuthModule.class) {
        @Override
        public ServerAuthModule newInstance(OSGiAuthnFilterHelper authnFilterHelper) {
            return DELEGATED.newInstance(authnFilterHelper);
        }
    },
    /** Passthrough to OpenICF connector Auth Module. */
    PASSTHROUGH(DelegatedAuthModule.class) {
        @Override
        public ServerAuthModule newInstance(OSGiAuthnFilterHelper authnFilterHelper) {
            return DELEGATED.newInstance(authnFilterHelper);
        }
    },
    /** IWA Auth Module. */
    IWA(IWAModule.class) {
        @Override
        public ServerAuthModule newInstance(OSGiAuthnFilterHelper authnFilterHelper) {
            return new IWAModule();
        }
    },
    /** OpenID Connect Auth Module. */
    OPENID_CONNECT(OpenIdConnectModule.class) {
        @Override
        public ServerAuthModule newInstance(OSGiAuthnFilterHelper authnFilterHelper) {
            return new OpenIdConnectModule();
        }
    };

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

    public abstract ServerAuthModule newInstance(OSGiAuthnFilterHelper authnFilterHelper);
}
