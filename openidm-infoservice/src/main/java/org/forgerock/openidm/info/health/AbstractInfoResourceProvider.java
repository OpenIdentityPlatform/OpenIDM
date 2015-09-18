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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openidm.info.health;

import org.forgerock.json.resource.*;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

/**
 * Base Health Info class.
 */
public abstract class AbstractInfoResourceProvider implements SingletonResourceProvider {

    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(Context context, ActionRequest request) {
        return new NotSupportedException("Action is not supported").asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> patchInstance(Context context, PatchRequest request) {
        return new NotSupportedException("Patch is not supported").asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, ReadRequest request) {
        return new NotSupportedException("Read is not supported").asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> updateInstance(Context context, UpdateRequest request) {
        return new NotSupportedException("Update is not supported").asPromise();
    }
}
