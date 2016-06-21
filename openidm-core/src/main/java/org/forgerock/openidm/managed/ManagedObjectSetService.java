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

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openidm.sync.impl.SynchronizationService;
import org.forgerock.services.context.Context;

/**
 * An interface for performing actions on a set of managed objects. This interface is consumed by the RelationshipProvider
 * when functioning as the relationship endpoint in order to trigger functionality on the Managed Object 'owning' the
 * relationship.
 */
public interface ManagedObjectSetService {

    /**
     * Sends a sync action request to the synchronization service
     *
     * @param context the Context of the request
     * @param request the Request being processed
     * @param resourceId the additional resourceId parameter telling the synchronization service which object
     *                   is being synchronized
     * @param action the {@link org.forgerock.openidm.sync.impl.SynchronizationService.SyncServiceAction}
     * @param oldValue the previous object value before the change (if applicable, or null if not)
     * @param newValue the object value to sync
     * @throws ResourceException in case of a failure that was not handled by the ResultHandler
     */
    void performSyncAction(final Context context, final Request request, final String resourceId,
                           final SynchronizationService.SyncServiceAction action, final JsonValue oldValue,
                           final JsonValue newValue) throws ResourceException;

    /**
     * Executes a postUpdate script, if configured, for a managed object.
     *
     * @param context the Context of the request
     * @param request the Request being processed
     * @param resourceId the resource ID of the managed object
     * @param oldValue the previous object value
     * @param newValue the object value
     * @throws ResourceException in case of a failure that was not handled by the ResultHandler
     */
    void executePostUpdate(final Context context, final Request request, final String resourceId,
                                  final JsonValue oldValue, final JsonValue newValue) throws ResourceException;

    /**
     *
     * @param context the Context of the request
     * @param request the Request being processed
     * @param resourceId the resource ID of the managed object
     * @param oldValue the previous object value
     * @param newValue the updated object value
     * @throws ResourceException in case the onUpdate script throws an exception
     */
    void executeOnUpdateScript(Context context, Request request, String resourceId, JsonValue oldValue,
                                      JsonValue newValue) throws ResourceException;


    }

