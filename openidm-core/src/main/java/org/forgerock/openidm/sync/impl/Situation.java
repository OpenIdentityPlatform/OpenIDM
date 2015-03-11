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
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openidm.sync.impl;

import org.forgerock.openidm.sync.ReconAction;

/**
 * TODO: Description.
 *
 */
enum Situation {

    /**
     * The mapping qualifies for a target object and there is an existing link to a target
     * object. Detected during source object changes and reconciliation.
     * Default action: {@code UPDATE}.
     */
    CONFIRMED(ReconAction.UPDATE),

    /**
     * The mapping qualifies for a target object, there is no link to a target object, and
     * there is a single correlated target object to link. Detected during source object
     * changes and reconciliation. Default action: {@code UPDATE}.
     */
    FOUND(ReconAction.UPDATE),

    /**
     * The mapping qualifies for a target object, there is no link to a target object, and
     * there is a single correlated target object to link; however, that target already is 
     * linked to another source record.
     * Detected during source object
     * changes and reconciliation. Default action: {@code EXCEPTION}.
     */
    FOUND_ALREADY_LINKED(ReconAction.EXCEPTION),

    /**
     * The mapping qualifies for a target object, there is no link to a target object, and
     * there is no correlated target object to link. Detected during source object changes
     * and reconciliation. Default action: {@code CREATE}.
     */
    ABSENT(ReconAction.CREATE),

    /**
     * The mapping qualifies for a target object, there is no link to a target object, but
     * there is more than one correlated target object to link. Detected during source object
     * changes and reconciliation. Default action: {@code EXCEPTION}.
     */
    AMBIGUOUS(ReconAction.EXCEPTION),

    /**
     * The mapping is qualified for a target object and there is a qualified link to a target
     * object, but the target object is missing. Only detected during reconciliation and
     * source object changes in synchronous mappings. Default action: {@code EXCEPTION}.
     */
    MISSING(ReconAction.EXCEPTION),

    /**
     * The mapping is not qualified for a target object and there is either a link and/or
     * one or more matching target objects. Detected during source object changes and reconciliation.
     * Default action: {@code DELETE}.
     */
    UNQUALIFIED(ReconAction.DELETE),
    
    /**
     * The mapping is not qualified for a target object and there is no link or matching
     * target object. Detected during source object changes and reconciliation.
     * Default action: {@code REPORT}.
     */
    SOURCE_IGNORED(ReconAction.REPORT),

    /**
     * The mapping is not qualified for a source object.
     * Only detected during reconciliation.
     * Default action: {@code REPORT}.
     */
    TARGET_IGNORED(ReconAction.REPORT),
    
    /**
     * There is target object for which there is no link. Only detected during reconciliation.
     * Default action: {@code EXCEPTION}.
     */
    UNASSIGNED(ReconAction.EXCEPTION),

    /**
     * There is target object for which there is a link, but the source object does not exist. 
     * Only detected during reconciliation.
     * Default action: {@code EXCEPTION}.
     */
    SOURCE_MISSING(ReconAction.EXCEPTION),
    
    /**
     * There is only a link, but the source and target objects do not exist. 
     * Only detected during source object changes (delete) currently.
     * Default action: {@code EXCEPTION}.
     */
    LINK_ONLY(ReconAction.EXCEPTION),
    
    /**
     * There is no source, link or target left
     * Only detected during source object changes (delete)
     * Default action: {@code NOREPORT}.
     */
    ALL_GONE(ReconAction.NOREPORT);

    /** TODO: Description. */
    private ReconAction defaultAction;
    
    /**
     * TODO: Description.
     *
     * @param defaultAction TODO.
     */
    Situation(ReconAction defaultAction) {
        this.defaultAction = defaultAction;
    }
    
    /**
     * TODO: Description.
     * @return
     */
    public ReconAction getDefaultAction() {
        return defaultAction;
    }
    
}
