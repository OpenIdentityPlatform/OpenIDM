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
 */
package org.forgerock.openidm.maintenance.impl;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.AbstractRequestHandler;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Responses;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.maintenance.upgrade.UpdateException;
import org.forgerock.openidm.maintenance.upgrade.UpdateManager;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;
import org.osgi.framework.Constants;

@Component(name = UpdateArchiveService.PID, policy = ConfigurationPolicy.IGNORE, metatype = true,
        description = "OpenIDM Product Update Archives Service", immediate = true)
@Service
@Properties({
        @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
        @Property(name = Constants.SERVICE_DESCRIPTION, value = "Product Update Archive Service"),
        @Property(name = ServerConstants.ROUTER_PREFIX, value = "/maintenance/update/archives/*")
})
public class UpdateArchiveService extends AbstractRequestHandler {
    public static final String PID = "org.forgerock.openidm.maintenance.update.archives";

    @Reference(policy= ReferencePolicy.STATIC)
    private UpdateManager updateManager;

    @Override
    public Promise<ResourceResponse, ResourceException> handleRead(Context context, ReadRequest request) {
        final String archiveName = request.getResourcePathObject().get(0);
        final Path archivePath = IdentityServer.getInstance().getInstallLocation().toPath().resolve("bin/update").resolve(archiveName);
        Path requestedFile = Paths.get("");
        Iterator<String> it = request.getResourcePathObject().tail(1).iterator();
        while (it.hasNext()) {
            requestedFile = requestedFile.resolve(it.next());
        }

        try {
            final JsonValue fileContents = updateManager.getArchiveFile(archivePath, requestedFile);
            return Responses.newResourceResponse(null, null, fileContents).asPromise();
        } catch (UpdateException e) {
            return new InternalServerErrorException(e).asPromise();
        }
    }
}
