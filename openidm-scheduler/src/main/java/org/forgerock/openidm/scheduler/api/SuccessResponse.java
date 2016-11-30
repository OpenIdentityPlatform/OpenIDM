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
package org.forgerock.openidm.scheduler.api;

import org.forgerock.api.annotations.Description;
import org.forgerock.api.annotations.Title;
import org.forgerock.openidm.scheduler.SchedulerService;

/**
 * Resource for {@link SchedulerService} Jobs' Pause and Resume Actions.
 */
@Title("Simple single boolean response.")
public class SuccessResponse {
    private boolean success;

    /**
     * Returns true if completed successful.
     *
     * @return true if completed successful
     */
    @Description("True if successful")
    public boolean isSuccess() {
        return success;
    }
}
