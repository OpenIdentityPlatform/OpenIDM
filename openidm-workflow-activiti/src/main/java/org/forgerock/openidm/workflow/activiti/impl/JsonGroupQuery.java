/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2012 ForgeRock AS. All rights reserved.
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
package org.forgerock.openidm.workflow.activiti.impl;

import org.activiti.engine.identity.Group;
import org.activiti.engine.impl.GroupQueryImpl;
import org.activiti.engine.impl.Page;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.JsonResourceException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author $author$
 * @version $Revision$ $Date$
 */
public class JsonGroupQuery extends GroupQueryImpl {
    private final SharedIdentityService identityService;

    public JsonGroupQuery(SharedIdentityService identityService) {
        super();
        this.identityService = identityService;
    }

    @Override
    public List<Group> executeList(CommandContext commandContext, Page page) {
        JsonValue params = new JsonValue(new HashMap());
        if (null == getUserId()) {
            params.put("_query-id", "query-all-ids");
        } else {
            //TODO Find groups for user
            params.put("_query-id", "query-all-ids");
        }
        try {
            JsonValue result = identityService.getAccessor().query("managed/group", params);
            List<Group> userList = new ArrayList<Group>();
            for (JsonValue resultItem : result.get("result")) {
                userList.add(new JsonGroup(identityService.getAccessor().read("managed/group/" + resultItem.get("_id").asString())));
            }
            return userList;
        } catch (JsonResourceException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long executeCount(CommandContext commandContext) {
        JsonValue params = new JsonValue(new HashMap());
        if (null == getId()) {
            params.put("_query-id", "query-all-ids");
        } else {
            params.put("_query-id", "find-by-id");
            params.put("id", getId());
        }
        try {
            JsonValue result = identityService.getAccessor().query("managed/group", params);
            return result.get("result").size();
        } catch (JsonResourceException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Group executeSingleResult(CommandContext commandContext) {
        JsonValue params = new JsonValue(new HashMap());
        params.put("_query-id", "find-by-id");
        params.put("id", getId());
        try {
            JsonValue result = identityService.getAccessor().query("managed/group", params);
            JsonGroup user = new JsonGroup(result.get("result").get(0));
            return user.isNull() ? null : user;
        } catch (JsonResourceException e) {
            throw new RuntimeException(e);
        }
    }
}
