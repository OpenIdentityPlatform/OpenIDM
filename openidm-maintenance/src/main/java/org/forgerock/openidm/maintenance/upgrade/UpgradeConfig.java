/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.maintenance.upgrade;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.patch.JsonPatchValueTransformer;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.PatchOperation;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServerContext;

/**
 * Config object patching utility.
 */
public class UpgradeConfig {
    /** The connection factory */
    @Reference(policy = ReferencePolicy.STATIC, target="(service.pid=org.forgerock.openidm.internal)")
    protected ConnectionFactory connectionFactory;

    /** Upgrade supports javascript value transforms for config objects */
    protected JsonPatchValueTransformer transform = new JsonPatchJavascriptValueTransformer();

    /**
     * Apply a JsonPatch to a config object on the router.
     *
     * @param resourceName the name of the resource to be patched.
     * @param patch a JsonPatch to be applied to the named config resource.
     * @throws UpgradeException
     */
    public void patchConfig(String resourceName, JsonValue patch) throws UpgradeException {
        Resource configObject;
        try {
            PatchRequest request = Requests.newPatchRequest(resourceName);
            for (PatchOperation op : PatchOperation.valueOfList(patch)) {
                request.addPatchOperation(op);
            }
            connectionFactory.getConnection().patch(new ServerContext(null), request);
        } catch (ResourceException e) {
            throw new UpgradeException("Patch request failed", e);
        }
    }
}
