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

import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openidm.workflow.activiti.ActivitiConstants;
import org.activiti.engine.identity.User;
import org.activiti.engine.impl.Page;
import org.activiti.engine.impl.UserQueryImpl;
import org.activiti.engine.impl.interceptor.CommandContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @version $Revision$ $Date$
 */
public class JsonUserQuery extends UserQueryImpl {

    static final long serialVersionUID = 1L;
    private final SharedIdentityService identityService;

    public JsonUserQuery(SharedIdentityService identityService) {
        super();
        this.identityService = identityService;
    }

    @Override
    public List<User> executeList(CommandContext commandContext, Page page) {
        try {
            QueryRequest request = Requests.newQueryRequest(SharedIdentityService.USER_PATH);
            request.setQueryId(ActivitiConstants.QUERY_ALL_IDS);
            List<User> userList = new ArrayList<User>();
            QueryResultHandler handler = new QueryResultHandlerImpl(userList);
            identityService.query(request, handler);
            return userList;
        } catch (ResourceException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long executeCount(CommandContext commandContext) {
        try {
            QueryRequest request = Requests.newQueryRequest(SharedIdentityService.USER_PATH);
            if (null == getId()) {
                request.setQueryId(ActivitiConstants.QUERY_ALL_IDS);
            } else {
                request.setQueryId("for-userName");
                request.setAdditionalParameter("uid", getId());
            }
            Collection<Resource> result = new ArrayList<Resource>();
            identityService.query(request, result);
            return result.size();
        } catch (ResourceException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public User executeSingleResult(CommandContext commandContext) {
        return readUser(getId());
    }
    
    User readUser(String id) {
        try {
            QueryRequest request = Requests.newQueryRequest(SharedIdentityService.USER_PATH);
            request.setQueryId("for-userName");
            request.setAdditionalParameter("uid", id);
            List<Resource> result = new ArrayList<Resource>();
            identityService.query(request, result);
            if (result.size() > 0) {
                JsonUser user = new JsonUser(result.get(0).getContent());
                return user;
            }
            return null;
        } catch (ResourceException e) {
            throw new RuntimeException(e);
        }

    }

    private class QueryResultHandlerImpl implements QueryResultHandler {

        private final List<User> userList;

        public QueryResultHandlerImpl(List<User> userList) {
            this.userList = userList;
        }

        @Override
        public void handleError(ResourceException error) {
            throw new RuntimeException(error);
        }

        @Override
        public boolean handleResource(Resource resource) {
            return userList.add(readUser(resource.getContent().get(ActivitiConstants.ID).asString()));
        }

        @Override
        public void handleResult(QueryResult result) {
        }
    }

}
