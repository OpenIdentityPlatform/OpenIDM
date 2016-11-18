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
package org.forgerock.openidm.security.impl.api;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.forgerock.api.annotations.Default;
import org.forgerock.api.annotations.Description;
import org.forgerock.api.annotations.Title;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *  Issuer Authority description used for certificates.
 */
@Title("Issuer Authority description")
class Issuer {

    @Description("State, or province name (e.g., WA)")
    @JsonProperty("ST")
    @Size(min = 1, max = 64)
    @Default("None")
    private String st;

    @Description("Country code (e.g., US)")
    @JsonProperty("C")
    @Size(min = 2, max = 2)
    @Default("None")
    private String c;

    @Description("Organizational unit name (e.g., OpenIDM)")
    @JsonProperty("OU")
    @Size(min = 1, max = 64)
    @Default("None")
    private String ou;

    @Description("Common name (e.g., forgerock.com)")
    @JsonProperty("CN")
    @Size(min = 1, max = 64)
    @NotNull
    private String cn;

    @Description("Locality name (e.g. Vancouver)")
    @JsonProperty("L")
    @Size(min = 1, max = 64)
    @Default("None")
    private String l;

    @Description("Organization (e.g. ForgeRock)")
    @JsonProperty("O")
    @Size(min = 1, max = 64)
    private String o;

    /**
     * Get state of certificate authority.
     *
     * @return state of certificate authority.
     */
    public String getSt() {
        return st;
    }

    /**
     * Get 2 character country code of certificate authority.
     *
     * @return country code of certificate authority.
     */
    public String getC() {
        return c;
    }

    /**
     * Get organizational unit name.
     *
     * @return Organizational unit name.
     */
    public String getOu() {
        return ou;
    }

    /**
     * Get common name.
     *
     * @return Common name.
     */
    public String getCn() {
        return cn;
    }

    /**
     * Get locality.
     *
     * @return locality.
     */
    public String getL() {
        return l;
    }

    /**
     * Get organization.
     *
     * @return Organization.
     */
    public String getO() {
        return o;
    }
}
