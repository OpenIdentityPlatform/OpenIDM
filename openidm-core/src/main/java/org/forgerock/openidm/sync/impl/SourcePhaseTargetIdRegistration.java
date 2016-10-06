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

package org.forgerock.openidm.sync.impl;

/**
 * Encapsulates the concerns of recording the target ids correlated to a source id during the source phase. This is
 * needed to determine the set of target ids to-be-reconciled in the target phase. This interface is leveraged by the
 * ReconPhase/ReconTask/Recon abstractions common to both the clustered, and non-clustered cases.
 */
public interface SourcePhaseTargetIdRegistration {
    /**
     * Registers the target identifier as having had reconciliation attempted
     * @param targetId the target identifier
     */
    void targetIdReconciled(String targetId);
}
