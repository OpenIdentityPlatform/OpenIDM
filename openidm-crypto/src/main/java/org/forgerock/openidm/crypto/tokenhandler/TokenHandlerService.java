/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for
 * the specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file
 * and include the License file at legal/CDDLv1.0.txt. If applicable, add the following
 * below the CDDL Header, with the fields enclosed by brackets [] replaced by your
 * own identifying information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2017 ForgeRock AS.
 */

package org.forgerock.openidm.crypto.tokenhandler;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.json.jose.jwe.EncryptionMethod;
import org.forgerock.json.jose.jwe.JweAlgorithm;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SigningManager;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.json.jose.tokenhandler.JwtTokenHandler;
import org.forgerock.openidm.keystore.SharedKeyService;
import org.forgerock.tokenhandler.TokenHandler;
import org.forgerock.util.encode.Base64;

import java.security.Key;

/**
 * Provides JWT token handlers.
 */
@Component(name = TokenHandlerService.PID, immediate = true)
@Service(value = TokenHandlerService.class)
@Properties({
        @Property(name = "service.description", value = "OpenIDM Token Handler Service"),
        @Property(name = "service.vendor", value = "ForgeRock AS"),
})
public class TokenHandlerService {
    static final String PID = "org.forgerock.openidm.crypto.TokenHandlerService";

    /** The shared key service. Used to get the shared key for self service. */
    @Reference
    private SharedKeyService sharedKeyService;

    /**
     * Return a JWT token handler that uses default algorithms and encryption method.
     *
     * @return a token handler
     */
    public TokenHandler getJwtTokenHandler(String sharedKeyAlias, String certAlias) {
        return getJwtTokenHandler(
                sharedKeyAlias,
                JweAlgorithm.RSAES_PKCS1_V1_5,
                EncryptionMethod.A128CBC_HS256,
                certAlias,
                JwsAlgorithm.HS256,
                1800L);
    }

    /**
     * Construct and return a JWT token handler with a specific set of properties.
     *
     * @param jweAlgorithm the encryption algorithm
     * @param encryptionMethod the encryption method
     * @param jwsAlgorithm the signing algorithm
     * @param tokenLifespan lifespan of the token in seconds
     * @return the specialized token handler
     */
    public TokenHandler getJwtTokenHandler(String sharedKeyAlias, JweAlgorithm jweAlgorithm,
            EncryptionMethod encryptionMethod, String certAlias, JwsAlgorithm jwsAlgorithm, Long tokenLifespan) {
        try {
            // pull the shared key in from the keystore
            final Key key = sharedKeyService.getSharedKey(sharedKeyAlias);
            final String sharedKey = Base64.encode(key.getEncoded());
            final SigningHandler signingHandler = new SigningManager().newHmacSigningHandler(sharedKey.getBytes());

            return new JwtTokenHandler(
                    jweAlgorithm,
                    encryptionMethod,
                    sharedKeyService.getKeyPair(certAlias),
                    jwsAlgorithm,
                    signingHandler,
                    tokenLifespan);
        } catch (Exception e) {
            throw new RuntimeException("Unable to read shared key or create key pair for encryption", e);
        }
    }
}