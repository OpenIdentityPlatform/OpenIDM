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
package org.forgerock.openidm.script.impl;

import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.json.fluent.JsonNode;
import org.forgerock.json.fluent.JsonNodeException;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.scheduler.ExecutionException;
import org.forgerock.openidm.scheduler.ScheduledService;
import org.forgerock.openidm.scope.ScopeFactory;
import org.forgerock.openidm.script.Script;
import org.forgerock.openidm.script.ScriptException;
import org.forgerock.openidm.script.Scripts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: Description.
 *
 * @author nati
 */
@Component(name = "org.forgerock.openidm.script",
        policy = ConfigurationPolicy.IGNORE,
        immediate = true)
@Properties({
        @Property(name = "service.description", value = "OpenIDM script service"),
        @Property(name = "service.vendor", value = ServerConstants.SERVER_VENDOR_NAME)
})
@Service
public class SchedulableScriptService implements ScheduledService {

    private final static Logger LOGGER = LoggerFactory.getLogger(SchedulableScriptService.class);
    @Reference
    public ScopeFactory scopeFactory;

    @Override
    public void execute(Map<String, Object> context) throws ExecutionException {
        try {
            String name = (String) context.get(INVOKER_NAME);
            JsonNode params = new JsonNode(context).get(CONFIGURED_INVOKE_CONTEXT);
            JsonNode scriptNode = params.get("script").expect(Map.class);
            if (!scriptNode.isNull()) {
                JsonNode input = params.get("input");
                Script script = Scripts.newInstance(name, scriptNode);
                execScript(name, script, input);
            } else {
                throw new ExecutionException("Unknown script '" + name + "' configured in schedule. ");
            }
        } catch (JsonNodeException jne) {
            throw new ExecutionException(jne);
        }
    }

    private void execScript(String name, Script script, JsonNode input) throws ExecutionException {
        if (script != null) {
            Map<String, Object> scope = scopeFactory.newInstance();
            scope.put("input", input.getValue());
            try {
                script.exec(scope);
            } catch (ScriptException se) {
                String msg = name + " script encountered exception";
                LOGGER.debug(msg, se);
                throw new ExecutionException(msg, se);
            }
        }
    }
}
