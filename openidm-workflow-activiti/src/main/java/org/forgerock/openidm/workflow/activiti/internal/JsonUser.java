/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2012 ForgeRock AS. All rights reserved.
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
package org.forgerock.openidm.workflow.activiti.internal;

import org.activiti.engine.identity.User;
import org.forgerock.json.fluent.JsonValue;
import java.util.HashMap;
import org.forgerock.openidm.crypto.CryptoService;

import static org.forgerock.openidm.workflow.activiti.internal.SharedIdentityService.*;

/**
 * @author $author$
 * @version $Revision$ $Date$
 */
public class JsonUser extends JsonValue implements User {
    static final long serialVersionUID = 1L;
    private CryptoService cryptoService;

    /**
     * Constructs a JsonUser value object with a given userId.
     *
     * @param service OpenIDM CryptoService used for password decryption
     * @param userId the Java object representing JSON value.
     */
    public JsonUser(CryptoService service, String userId) {
        super(new HashMap());
        this.cryptoService = service;
        put(SCIM_USERNAME, userId);
    }

    public void setCryptoService(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }
    
    public JsonUser(JsonValue value) {
        super(value);
    }

    public String getId() {
        return get(SCIM_USERNAME).required().asString();
    }

    public void setId(String id) {
        put(SCIM_USERNAME, id);
    }

    public String getFirstName() {
        return get(SCIM_NAME).get(SCIM_NAME_GIVENNAME).asString();
    }

    public void setFirstName(String firstName) {
        if (get(SCIM_NAME).isNull()) {
            put(SCIM_NAME, new HashMap(6));
        }
        if (get(SCIM_NAME).isMap()) {
            get(SCIM_NAME).put(SCIM_NAME_GIVENNAME, firstName);
        }
    }

    public void setLastName(String lastName) {
        if (get(SCIM_NAME).isNull()) {
            put(SCIM_NAME, new HashMap(6));
        }
        if (get(SCIM_NAME).isMap()) {
            get(SCIM_NAME).put(SCIM_NAME_FAMILYNAME, lastName);
        }
    }

    public String getLastName() {
        return get(SCIM_NAME).get(SCIM_NAME_FAMILYNAME).asString();
    }

    public void setEmail(String email) {

    }

    public String getEmail() {
        return null;
    }

    public String getPassword() {
        JsonValue password = get(SCIM_PASSWORD);
        JsonValue decryptedPassword = cryptoService.decrypt(password);
        return decryptedPassword.asString();
    }

    public void setPassword(String password) {
        put(SCIM_PASSWORD, password);
    }
}
