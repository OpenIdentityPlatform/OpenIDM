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
 * Copyright 2017 ForgeRock AS.
 */

package org.forgerock.openidm.policy.api;

import javax.validation.constraints.NotNull;

import java.util.List;

import org.forgerock.api.annotations.Description;
import org.forgerock.api.annotations.Title;

/**
 * Property-policy definition.
 */
@Title("Property Policies")
public class PropertyPolicy {

    private PropertyPolicy() {
        // hidden constructor
    }

    @Description("Property Name")
    @NotNull
    public String name;

    @Description("Requirements")
    @NotNull
    public List<String> policyRequirements;

    @Description("Policies")
    @NotNull
    public List<Policy> policies;

    @Description("Fallback Policies")
    public List<Policy> fallbackPolicies;

    @Description("Conditional Policies")
    public List<ConditionalPolicy> conditionalPolicies;

}
