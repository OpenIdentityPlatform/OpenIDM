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

package org.forgerock.openidm.managed;

import java.util.Set;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.services.context.Context;

/**
 * An interface for performing actions on a set of managed objects.
 */
public interface ManagedObjectSetService {
    
    /** 
     * Performs and update on a managed object.  The update will include executing onUpdate and postUpdate scripts as
     * well as performing a sync operation on the managed object.
     *
     * @param context the current Context
     * @param request the source Request
     * @param resourceId the resource id of the object being modified
     * @param rev the revision of the object being modified
     * @param oldValue the old value of the object
     * @param newValue the new value of the object
     * @param relationshipFields a set of relationship fields.
     * @return a {@link ResourceResponse} object representing the updated resource
     * @throws ResourceException
     */
    public ResourceResponse update(final Context context, Request request, String resourceId, String rev,
            JsonValue oldValue, JsonValue newValue, Set<JsonPointer> relationshipFields)
            throws ResourceException;
}
