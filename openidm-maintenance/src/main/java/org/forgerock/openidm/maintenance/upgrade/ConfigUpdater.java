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
 *
 * Portions Copyrighted 2024 3A Systems LLC.
 */

package org.forgerock.openidm.maintenance.upgrade;

import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.json.JsonValue;
import org.forgerock.openidm.router.IDMConnectionFactory;
import org.forgerock.services.context.Context;
import org.forgerock.json.resource.PatchOperation;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.component.propertytypes.ServiceVendor;

/**
 * Config object patching utility.
 */
@Component(
        name = ConfigUpdater.PID,
        configurationPolicy = ConfigurationPolicy.IGNORE,
//        description = "OpenIDM Config Update",
        immediate = true)
@ServiceVendor(ServerConstants.SERVER_VENDOR_NAME)
@ServiceDescription("Config Update")
public class ConfigUpdater {
    /** The PID for this component. */
    public static final String PID = "org.forgerock.openidm.maintenance.update.config";

    /** The connection factory */
    @Reference(policy = ReferencePolicy.STATIC)
    protected IDMConnectionFactory connectionFactory;

    /**
     * Apply a json-patch to a config object on the router.
     *
     * @param context the context for the patch request.
     * @param resourceName the name of the resource to be patched.
     * @param patch a json-patch to be applied to the named config resource.
     * @throws UpdateException
     */
    public void patchConfig(Context context, String resourceName, JsonValue patch) throws UpdateException {
        try {
            PatchRequest request = Requests.newPatchRequest(resourceName);
            for (PatchOperation op : PatchOperation.valueOfList(patch)) {
                request.addPatchOperation(op);
            }
            connectionFactory.getConnection().patch(context, request);
        } catch (ResourceException e) {
            throw new UpdateException("Patch request failed", e);
        }
    }
}
