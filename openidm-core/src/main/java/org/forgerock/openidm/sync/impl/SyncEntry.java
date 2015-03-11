/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
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
package org.forgerock.openidm.sync.impl;

import org.forgerock.json.resource.Context;
import org.forgerock.openidm.sync.impl.ObjectMapping.SyncOperation;
import org.forgerock.openidm.util.DateUtil;

/**
 * A sync audit log entry representation.  Contains any additional fields
 * and logic specific to sync entries.
 * 
 */
class SyncEntry extends LogEntry {

    /**
     * Construct a sync audit log entry from the given {@link SyncOperation} and mapping name.
     *
     * @param op the sync operation
     * @param mappingName the mapping name
     * @param rootContext the root context
     * @param dateUtil a date-formatting object
     */
    public SyncEntry(SyncOperation op, String mappingName, Context rootContext, DateUtil dateUtil) {
        super(op, mappingName, rootContext, dateUtil);
    }
}
