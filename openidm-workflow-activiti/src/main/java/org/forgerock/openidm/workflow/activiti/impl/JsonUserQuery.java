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

import org.activiti.engine.identity.User;
import org.activiti.engine.impl.Page;
import org.activiti.engine.impl.UserQueryImpl;
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
public class JsonUserQuery extends UserQueryImpl {

    private final SharedIdentityService identityService;

    public JsonUserQuery(SharedIdentityService identityService) {
        super();
        this.identityService = identityService;
    }

    @Override
    public List<User> executeList(CommandContext commandContext, Page page) {
        JsonValue params = new JsonValue(new HashMap());
        params.put("_queryId", "query-all-ids");
        try {
            JsonValue result = identityService.getAccessor().query("managed/user", params);
            List<User> userList = new ArrayList<User>();
            for (JsonValue resultItem : result.get("result")) {
                userList.add(new JsonUser(identityService.getAccessor().read("managed/user/" + resultItem.get("_id").asString())));
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
            params.put("_queryId", "query-all-ids");
        } else {
            params.put("_queryId", "for-userName");
            params.put("uid", getId());
        }
        try {
            JsonValue result = identityService.getAccessor().query("managed/user", params);
            return result.get("result").size();
        } catch (JsonResourceException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public User executeSingleResult(CommandContext commandContext) {
        JsonValue params = new JsonValue(new HashMap());
        params.put("_queryId", "for-userName");
        params.put("uid", getId());
        try {
            JsonValue result = identityService.getAccessor().query("managed/user", params);
            if (result.get("result").size() > 0) {
                JsonUser user = new JsonUser(result.get("result").get(0));
                return user;
            }
            return null;
        } catch (JsonResourceException e) {
            throw new RuntimeException(e);
        }
    }
}
