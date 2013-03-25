/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All Rights Reserved
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

import org.forgerock.openidm.recon.DataProvider

if (phase == "setup") {
    DataProvider.initDataOTOSimple(openidm)
} else if (phase == "sync") {
    ["status": "OK"]
} else if (phase == "assert") {
    def audit = openidm.query("/audit/recon", ["_filter" : "active eq \"true\""])
    for (log in audit.result){
        if ("ABSENT" == log.situation) {
            assert log.sourceId == "user2"
        } else if ("MISSING" == log.situation) {
            assert log.sourceId == "user1"
        } else if ("SOURCE_MISSING" == log.situation) {
            assert log.targetId == "user5"
        } else if ("UNASSIGNED" == log.situation) {
            assert log.targetId == "user6"
        } else if ("CONFIRMED" == log.situation) {
            assert log.sourceId == "user3"
        } else if ("LINK_ONLY" == log.situation) {
            assert false
        } else if ("FOUND" == log.situation) {
            assert log.targetId == "user4"
        }
    }
    //assert audit.result.size == 6
}
