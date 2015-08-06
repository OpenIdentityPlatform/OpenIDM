/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 ForgeRock AS. All rights reserved.
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

package org.forgerock.openidm.audit.util;

/**
 * Audit logging constants.
 */
public interface AuditConstants {

    /** entryType for a "start" recon audit log entry */
    String RECON_LOG_ENTRY_TYPE_RECON_START = "start";
    /** entryType for a "summary" recon audit log entry */
    String RECON_LOG_ENTRY_TYPE_RECON_END = "summary";
    /** entryType for an "entry" recon audit log entry */
    String RECON_LOG_ENTRY_TYPE_RECON_ENTRY = "entry";

    enum ActivityAction {
        GET_CHANGED_WATCHED_FIELDS("getChangedWatchedFields"),
        GET_CHANGED_PASSWORD_FIELDS("getChangedPasswordFields");

        private String actionName;

        ActivityAction(String actionName) {
            this.actionName = actionName;
        }

        public String getActionName() {
            return actionName;
        }

        public static ActivityAction find(String actionName) {
            for (ActivityAction activityAction : ActivityAction.values()) {
                if (activityAction.getActionName().equals(actionName)) {
                    return activityAction;
                }
            }
            return null;
        }
    }

}
