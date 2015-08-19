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
 * Copyright 2012-2015 ForgeRock AS.
 */
package org.forgerock.openidm.workflow.activiti.impl;

import org.forgerock.openidm.workflow.activiti.ActivitiConstants;
import org.activiti.engine.identity.Group;
import org.activiti.engine.impl.GroupQueryImpl;
import org.activiti.engine.impl.Page;
import org.activiti.engine.impl.interceptor.CommandContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.forgerock.json.resource.*;

/**
 * @version $Revision$ $Date$
 */
public class JsonGroupQuery extends GroupQueryImpl {

    static final long serialVersionUID = 1L;
    private final SharedIdentityService identityService;

    public JsonGroupQuery(SharedIdentityService identityService) {
        super();
        this.identityService = identityService;
    }

    @Override
    public List<Group> executeList(CommandContext commandContext, Page page) {
        QueryRequest request = Requests.newQueryRequest(SharedIdentityService.GROUP_PATH);
        request.setQueryId(ActivitiConstants.QUERY_ALL_IDS);
        List<Group> groupList = new ArrayList<Group>();
        QueryResourceHandler handler = new QueryResultHandlerImpl(groupList);
        try {
            identityService.query(request, handler);
            return groupList;
        } catch (ResourceException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long executeCount(CommandContext commandContext) {
        try {
            QueryRequest request = Requests.newQueryRequest(SharedIdentityService.GROUP_PATH);
            if (null == getId()) {
                request.setQueryId(ActivitiConstants.QUERY_ALL_IDS);
            } else {
                request.setQueryId("get-by-field-value");
                request.setAdditionalParameter("value", getId());
                request.setAdditionalParameter("field", "id");
            }
            Collection<ResourceResponse> result = new ArrayList<>();
            identityService.query(request, result);
            return result.size();
        } catch (ResourceException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Group executeSingleResult(CommandContext commandContext) {
        return readGroup(getId());
    }

    private Group readGroup(String id) {
        try {
            QueryRequest request = Requests.newQueryRequest(SharedIdentityService.GROUP_PATH);
            request.setQueryId("get-by-field-value");
            request.setAdditionalParameter("value", id);
            request.setAdditionalParameter("field", "id");
            List<ResourceResponse> result = new ArrayList<>();
            identityService.query(request, result);
            if (result.size() > 0) {
                JsonGroup group = new JsonGroup(result.get(0).getContent());
                return group;
            }
            return null;
        } catch (ResourceException e) {
            throw new RuntimeException(e);
        }
    }

    private class QueryResultHandlerImpl implements QueryResourceHandler {

        private final List<Group> groupList;

        public QueryResultHandlerImpl(List<Group> groupList) {
            this.groupList = groupList;
        }

        @Override
        public boolean handleResource(ResourceResponse resource) {
            return groupList.add(readGroup(resource.getContent().get(ActivitiConstants.ID).asString()));
        }

    }
}
