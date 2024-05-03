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
 * Copyright 2016 ForgeRock AS.
 * Portions Copyrighted 2024 3A Systems LLC.
 */

package org.forgerock.openidm.script;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.patch.PatchValueTransformer;
import org.forgerock.openidm.patch.ScriptedPatchValueTransformer;
import org.forgerock.openidm.script.impl.ScriptRegistryService;
import org.forgerock.script.ScriptEntry;
import org.forgerock.script.ScriptRegistry;
import org.forgerock.script.registry.ScriptRegistryImpl;
import org.forgerock.services.context.Context;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.component.propertytypes.ServiceVendor;

import java.util.HashMap;
import java.util.Map;

/**
 * A factory API that enables callers to obtain a ScriptRegistryService based transformer given context.
 */
@Component(
        name = ScriptedPatchValueTransformerFactory.PID,
        immediate = true,
        configurationPolicy = ConfigurationPolicy.IGNORE,
        service = ScriptedPatchValueTransformerFactory.class,
        property = Constants.SERVICE_PID + "=" + ScriptedPatchValueTransformerFactory.PID
)
@ServiceVendor(ServerConstants.SERVER_VENDOR_NAME)
@ServiceDescription("OpenIDM Scripted Patch Value Transformer Factory")
public class ScriptedPatchValueTransformerFactory {
    /** The PID for this Component. */
    public static final String PID = "org.forgerock.openidm.script.scriptedpatchvaluetransformerfactory";

    /** Script Registry service. */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    private volatile ScriptRegistry scriptRegistry = null;

    /** Script Executor service. */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    private volatile ScriptExecutor scriptExecutor = null;

    /**
     * The method returns a ScriptRegistryService based transformer.
     *
     * @param context The request context
     * @return a ScriptedPatchValueTransformer instance
     */
    public PatchValueTransformer getPatchValueTransformer(final Context context) {
        return new ScriptedPatchValueTransformer() {
            @Override
            public JsonValue evalScript(JsonValue subject, JsonValue scriptConfig, JsonPointer field) throws ResourceException {
                try {
                    // get script
                    ScriptEntry scriptEntry = scriptRegistry.takeScript(scriptConfig);
                    if (scriptEntry != null) {
                        // add bindings
                        Map<String, Object> bindings = new HashMap<>();
                        bindings.put("content", subject.get(field));
                        // exec script
                        return scriptExecutor.execScript(context, scriptEntry, bindings);
                    } else {
                        throw new BadRequestException("Script is null or inactive.");
                    }
                } catch (Exception e) {
                    throw new BadRequestException("Failed to eval script " + scriptConfig.toString());
                }
            }
        };
    }

    public void bindScriptRegistry(ScriptRegistryImpl scriptRegistry) {
        this.scriptRegistry=scriptRegistry;
    }

    public void bindScriptExecutor(ScriptRegistryService scriptRegistryService) {
        this.scriptExecutor=scriptRegistryService;
    }
}