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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openidm.jaspi.modules;

import org.forgerock.jaspi.exceptions.JaspiAuthException;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.openidm.jaspi.config.OSGiAuthnFilterHelper;
import org.forgerock.script.Script;
import org.forgerock.script.ScriptEntry;
import org.forgerock.script.exception.ScriptThrownException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import javax.security.auth.message.AuthException;

/**
 * Responsible for executing any augment security context and providing the required parameters to the scripts.
 *
 * @since 3.0.0
 */
class AugmentationScriptExecutor {

    private static final Logger logger = LoggerFactory.getLogger(AugmentationScriptExecutor.class);

    private final OSGiAuthnFilterHelper authnFilterHelper;

    AugmentationScriptExecutor(OSGiAuthnFilterHelper authnFilterHelper) {
        this.authnFilterHelper = authnFilterHelper;
    }

    /**
     * Executes the specified augmentation script with the given properties and SecurityContextMapper.
     *
     * @param augmentScript The augment script entry.
     * @param properties The properties to be provided to the script.
     * @param securityContextMapper A SecurityContextMapper instance.
     * @throws AuthException If any problem occurs whilst executing the script.
     */
    void executeAugmentationScript(ScriptEntry augmentScript, JsonValue properties,
            SecurityContextMapper securityContextMapper) throws AuthException {

        if (augmentScript == null) {
            return;
        }

        try {
            if (!augmentScript.isActive()) {
                throw new ServiceUnavailableException("Failed to execute inactive script: "
                        + augmentScript.getName().toString());
            }

            // Create internal ServerContext chain for script-call
            ServerContext context = authnFilterHelper.getRouter().createServerContext();
            final Script script = augmentScript.getScript(context);
            // Pass auth module properties and SecurityContextWrapper details to augmentation script
            script.put("properties", properties);
            script.put("security", securityContextMapper.asJsonValue());
            script.eval();
        } catch (ScriptThrownException e) {
            final ResourceException re = e.toResourceException(ResourceException.INTERNAL_ERROR, e.getMessage());
            logger.error("{} when attempting to execute script {}", re.toString(), augmentScript.getName(), re);
            throw new JaspiAuthException(re.getMessage(), re);
        } catch (ScriptException e) {
            logger.error("{} when attempting to execute script {}", e.toString(), augmentScript.getName(), e);
            throw new JaspiAuthException(e.getMessage(), e);
        } catch (ResourceException e) {
            logger.error("{} when attempting to create server context", e.toString(), e);
            throw new JaspiAuthException(e.getMessage(), e);
        }
    }
}
