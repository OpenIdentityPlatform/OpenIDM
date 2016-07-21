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
package org.forgerock.openidm.idp.relyingparty;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data structure used to hold details regarding OAuth Claims.
 */
public class Claims {
    private final String issuer;
    private final String subject;

    @JsonCreator
    public Claims(@JsonProperty("iss") String issuer, @JsonProperty("sub") String subject) {
        this.issuer = issuer;
        this.subject = subject;
    }

    public String getIssuer() {
        return issuer;
    }

    public String getSubject() {
        return subject;
    }
}