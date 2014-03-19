/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.servlet.internal;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.SecurityContext;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.json.resource.servlet.HttpServletContextFactory;
import org.forgerock.json.resource.servlet.SecurityContextFactory;
import org.forgerock.script.Script;
import org.forgerock.script.ScriptEntry;
import org.forgerock.script.ScriptRegistry;
import org.forgerock.script.engine.Utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * An HttpServletContextFactory responsible for creating a SecurityContext for OpenIDM
 * requests.  Specifically, this class executes each of the augmentation scripts that
 * were registered with the {@link ServletComponent}.
 *
 * @author brmiller
 */
public class IDMSecurityContextFactory implements HttpServletContextFactory {

    private final static Logger logger = LoggerFactory.getLogger(IDMSecurityContextFactory.class);

    /** the Script Registry */
    private final ScriptRegistry scriptRegistry;

    /** a list of augmentation scripts to run */
    private final List<ScriptEntry> augmentationScripts;

    /**
     * Construct the IDMServletContextFactory.
     *
     * @param scriptRegistry the script registry, for registring auth module scripts
     * @param augmentationScripts the list of configured security context augmentation scripts
     */
    public IDMSecurityContextFactory(ScriptRegistry scriptRegistry, List<ScriptEntry> augmentationScripts) {
        this.scriptRegistry = scriptRegistry;
        this.augmentationScripts = augmentationScripts;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Context createContext(HttpServletRequest request) throws ResourceException {

        final SecurityContextFactory securityContextFactory = SecurityContextFactory.getHttpServletContextFactory();
        final SecurityContext securityContext = securityContextFactory.createContext(request);

        // execute global security context augmentation scripts
        for (ScriptEntry augmentScript : augmentationScripts) {
            augmentContext(augmentScript, securityContext);
        }

        if (isSecurityContextPopulated(securityContext)) {
            return securityContext;
        }

        logger.warn("Rejecting invocation as required context to allow invocation not populated");
        throw new ServiceUnavailableException(
                "Rejecting invocation as required context to allow invocation not populated");
    }

    private boolean isSecurityContextPopulated(SecurityContext context) {
        return !StringUtils.isEmpty(context.getAuthenticationId())
            && !StringUtils.isEmpty(context.getAuthorizationId().get(SecurityContext.AUTHZID_ID).toString())
            && !StringUtils.isEmpty(context.getAuthorizationId().get(SecurityContext.AUTHZID_COMPONENT).toString())
            && context.getAuthorizationId().get(SecurityContext.AUTHZID_ROLES) != null;
    }

    /**
     * Invoke an augmentation script to modify the SecurityContext as appropriate.
     *
     * @param augmentScript the script to execute
     * @param securityContext the current SecurityContext
     * @throws ResourceException on failure to execute script
     */
    private void augmentContext(ScriptEntry augmentScript, SecurityContext securityContext)
            throws ResourceException {

        if (!augmentScript.isActive()) {
            throw new ServiceUnavailableException("Failed to execute inactive script: "
                    + augmentScript.getName().toString());
        }

        ServerContext context = new ServerContext(securityContext);
        final Script script = augmentScript.getScript(context);
        script.put("security", securityContext);

        try {
            script.eval();
        } catch (Throwable t) {
            ResourceException re = Utils.adapt(t);
            logger.warn("augment script {} encountered exception with detail {} " ,
                    new Object[] { augmentScript.getName().getName(), re.getDetail(), re });
        }
    }
}
