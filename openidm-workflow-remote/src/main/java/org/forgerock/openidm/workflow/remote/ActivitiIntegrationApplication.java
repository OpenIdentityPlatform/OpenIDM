/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright © 2012 ForgeRock Inc. All rights reserved.
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
package org.forgerock.openidm.workflow.remote;

import java.util.logging.Level;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngines;
import org.forgerock.json.resource.restlet.JsonResourceRestlet;
import org.forgerock.openidm.workflow.activiti.impl.ActivitiResource;
import org.restlet.Application;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Form;
import org.restlet.data.Parameter;
import org.restlet.security.ChallengeAuthenticator;
import org.restlet.security.SecretVerifier;
import org.restlet.security.Verifier;
// SLF4J
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Remote client for OpenIDM-Activiti integration
 */
public class ActivitiIntegrationApplication extends Application {

    private final static Logger LOGGER = LoggerFactory.getLogger(ActivitiIntegrationApplication.class);

    private ProcessEngine engine;
    private ChallengeAuthenticator authenticator;

    /**
     * Creates a root Restlet that will receive all incoming calls.
     */
    @Override
    public synchronized Restlet createInboundRoot() {
        try {
            engine = ProcessEngines.getDefaultProcessEngine();
        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(ActivitiIntegrationApplication.class.getName()).log(Level.SEVERE, null, ex);
        }
        Verifier verifier = new SecretVerifier() {

            @Override
            public boolean verify(String username, char[] password) {
                boolean verified = engine.getIdentityService().checkPassword(username, new String(password));
                return verified;
            }
        };

        authenticator = new ChallengeAuthenticator(null, true, ChallengeScheme.HTTP_BASIC,
                "Activiti Realm") {

            @Override
            protected boolean authenticate(Request request, Response response) {
                if (request.getChallengeResponse() == null) {
                    return false;
                } else {
                    boolean authenticated = super.authenticate(request, response);
                    if (authenticated) {
                        Parameter user = ((Form) request.getAttributes().get("org.restlet.http.headers")).getFirst("X-OpenIDM-Username", true);
                        if (user != null) {
                            engine.getIdentityService().setAuthenticatedUserId(user.getValue());
                        }
                    }
                    return authenticated;
                }
            }
        };
        authenticator.setVerifier(verifier);

        JsonResourceRestlet root = new JsonResourceRestlet(new ActivitiResource(engine));
        authenticator.setNext(root);
        return authenticator;
    }
}
