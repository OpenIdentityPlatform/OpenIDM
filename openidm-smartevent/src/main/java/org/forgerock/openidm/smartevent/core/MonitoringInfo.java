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
 * Copyright © 2012 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openidm.smartevent.core;

/**
 * Holds monitoring and statistics info
 * 
 */
public class MonitoringInfo {

    public long totalInvokes;
    public long totalTime;

    /**
     * Reset the statistics
     */
    public void reset() {
        totalInvokes = 0;
        totalTime = 0;
    }

    public String toString() {
        return "Invocations: " + totalInvokes + " total time: "
                + StatisticsHandler.formatNsAsMs(totalTime) + " mean: "
                + StatisticsHandler.formatNsAsMs(totalInvokes > 0 ? totalTime / totalInvokes : -1);
    }
}
