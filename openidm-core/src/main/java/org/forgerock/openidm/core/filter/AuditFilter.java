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

package org.forgerock.openidm.core.filter;

import org.forgerock.json.resource.CrossCutFilterResultHandler;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UntypedCrossCutFilter;

/**
 * A NAME does ...
 * 
 */
public class AuditFilter implements UntypedCrossCutFilter<AuditFilter.AuditState> {

    public class AuditState {
        private final long actionTime = System.currentTimeMillis();
        private Request request;

        public AuditState(final Request request) {
            this.request = request;
        }
    }

    @Override
    public void filterGenericRequest(final ServerContext context, final Request request,
            final RequestHandler next, final CrossCutFilterResultHandler<AuditState, Object> handler) {
        handler.handleContinue(context, new AuditState(request));
    }

    @Override
    public <R> void filterGenericResult(ServerContext context, AuditState state, R result,
            ResultHandler<R> handler) {
        try {
            // TODO Write Audit log
        } finally {
            handler.handleResult(result);
        }
    }

    @Override
    public void filterQueryResource(ServerContext context, AuditState state, Resource resource,
            QueryResultHandler handler) {
        try {
            // TODO Write Audit log
        } finally {
            handler.handleResource(resource);
        }
    }

    @Override
    public void filterGenericError(ServerContext context, AuditState state,
            ResourceException error, ResultHandler<Object> handler) {
        try {
            // TODO Write Audit log
        } finally {
            handler.handleError(error);
        }
    }
}
