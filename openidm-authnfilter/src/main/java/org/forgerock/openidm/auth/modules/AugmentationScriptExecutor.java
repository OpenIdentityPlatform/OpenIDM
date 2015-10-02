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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.openidm.auth.modules;

import org.forgerock.caf.authentication.api.AuthenticationException;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.openidm.util.ContextUtil;
import org.forgerock.script.Script;
import org.forgerock.script.ScriptEntry;
import org.forgerock.script.exception.ScriptThrownException;
import org.forgerock.services.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.util.HashMap;

/**
 * Responsible for executing any augment security context and providing the required parameters to the scripts.
 *
 * @since 3.0.0
 */
class AugmentationScriptExecutor {

    private static final Logger logger = LoggerFactory.getLogger(AugmentationScriptExecutor.class);

    /**
     * Executes the specified augmentation script with the given properties and SecurityContextMapper.
     *
     * @param augmentScript The augment script entry.
     * @param properties The properties to be provided to the script.
     * @param securityContextMapper A SecurityContextMapper instance.
     * @throws AuthenticationException If any problem occurs whilst executing the script.
     */
    void executeAugmentationScript(ScriptEntry augmentScript, JsonValue properties,
            SecurityContextMapper securityContextMapper) throws AuthenticationException {

        if (augmentScript == null) {
            return;
        }

        try {
            if (!augmentScript.isActive()) {
                throw new ServiceUnavailableException("Failed to execute inactive script: "
                        + augmentScript.getName().toString());
            }

            // Create internal Context chain for script-call
            Context context = ContextUtil.createInternalContext();
            final Script script = augmentScript.getScript(context);
            // Pass auth module properties and SecurityContextWrapper details to augmentation script
            script.put("properties", properties);
            JsonValue security = new JsonValue(new HashMap<String, Object>(2));
            security.put(SecurityContextMapper.AUTHENTICATION_ID, securityContextMapper.getAuthenticationId());
            security.put(SecurityContextMapper.AUTHORIZATION_ID, securityContextMapper.getAuthorizationId());
            script.put("security", security);

            // expect updated security context
            JsonValue updatedSecurityContext = new JsonValue(script.eval());

            // if security context is updated; update the SecurityContextMapper backing store
            if (!updatedSecurityContext.get(SecurityContextMapper.AUTHENTICATION_ID).isNull()) {
                securityContextMapper.setAuthenticationId(updatedSecurityContext.get(SecurityContextMapper.AUTHENTICATION_ID).asString());
            }
            if (!updatedSecurityContext.get(SecurityContextMapper.AUTHORIZATION_ID).isNull()) {
                securityContextMapper.setAuthorizationId((updatedSecurityContext.get(SecurityContextMapper.AUTHORIZATION_ID).asMap()));
            }
        } catch (ScriptThrownException e) {
            final ResourceException re = e.toResourceException(ResourceException.INTERNAL_ERROR, e.getMessage());
            logger.error("{} when attempting to execute script {}", re.toString(), augmentScript.getName(), re);
            throw new AuthenticationException(re.getMessage(), re);
        } catch (ScriptException e) {
            logger.error("{} when attempting to execute script {}", e.toString(), augmentScript.getName(), e);
            throw new AuthenticationException(e.getMessage(), e);
        } catch (ResourceException e) {
            logger.error("{} when attempting to create context", e.toString(), e);
            throw new AuthenticationException(e.getMessage(), e);
        }
    }
}
