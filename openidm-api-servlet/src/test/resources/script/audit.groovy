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

import org.forgerock.openidm.internal.recon.ReconUtil

def auditRecord = [situation: situation, "active": "true"]

if (binding.variables.containsKey(ReconUtil.SOURCE_FIELD) && null != source) {
    auditRecord.sourceId = source.id
}
if (binding.variables.containsKey(ReconUtil.LINK_FIELD) && null != link) {
    auditRecord.link = true
}
if (binding.variables.containsKey(ReconUtil.TARGET_FIELD) && null != target) {
    auditRecord.targetId = target.id
}
if (binding.variables.containsKey(ReconUtil.ERROR_FIELD)) {
    auditRecord.error = error
}
if (binding.variables.containsKey(ReconUtil.AMBIGUOUS_FIELD)) {
    auditRecord.ambiguous = true
}

def log = openidm.create("/audit/recon", null, auditRecord)
assert log._id != null