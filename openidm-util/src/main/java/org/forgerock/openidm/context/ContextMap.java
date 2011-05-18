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

package org.forgerock.openidm.context;

import java.util.List;
import java.util.Map;

public interface ContextMap extends Map<String, Object> {

    public final static String NESTED_ACTIVITY_IDS = "nested-activity-ids";
    public final static String REQUESTER = "requester";
    public final static String APPROVER = "approver";
    
    void pushNestedEntry(String key, Object additionalEntry);
    
    Object popNestedEntry(String key);
    
    List<Object> getNestedEntries(String key);
    
    Object getFirstNested(String key);
    
    Object getLastNested(String key);
    
    void pushActivityId(String activityId);
    
    String popActivityId();
    
    String getRequester();
    
    void putRequester(String requester);
    
    void removeRequester();
    
    String getApprover();
    
    void putApprover(String approver);
    
    void removeApprover();
}