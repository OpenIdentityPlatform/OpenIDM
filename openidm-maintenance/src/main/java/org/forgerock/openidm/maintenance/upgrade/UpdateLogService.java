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
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.openidm.maintenance.upgrade;

import org.forgerock.json.resource.ResourceException;

/**
 * Interface for creation and update of update log entries
 */
public interface UpdateLogService {
    /**
     * Log a new update entry in the repo
     *
     * @param entry a new update entry.
     * @throws org.forgerock.json.resource.ResourceException
     */
    public void logUpdate(UpdateLogEntry entry) throws ResourceException;

    /**
     * Update an existing update entry in the repo
     *
     * @param entry the entry to update.
     * @throws ResourceException
     */
    public void updateUpdate(UpdateLogEntry entry) throws ResourceException;
}
