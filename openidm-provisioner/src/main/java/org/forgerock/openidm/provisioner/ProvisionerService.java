/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
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
 * $Id$
 */

package org.forgerock.openidm.provisioner;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.JsonResource;
import org.forgerock.openidm.sync.SynchronizationListener;

import java.util.Map;


/**
 * @author $author$
 * @version $Revision$ $Date$
 */
public interface ProvisionerService extends JsonResource {

    /**
     * Gets the unique {@link SystemIdentifier} of this instance.
     * <p/>
     * The service which refers to this service instance can distinguish between multiple instances by this value.
     *
     * @return
     */
    public SystemIdentifier getSystemIdentifier();

    /**
     * Gets a brief stats report about the current status of this service instance.
     * </p/>
     * TODO Provide a sample object
     *
     * @return
     */
    public Map<String, Object> getStatus();

    /**
     * Synchronise the changes from the end system for the given {@code objectType}.
     * <p/>
     * OpenIDM takes active role in the synchronisation process by asking the end system to get all changed object.
     * Not all system is capable to fulfill this kind of request but if the end system is capable then the implementation
     * send each changes to the {@link SynchronizationListener} and when it finished it return a new <b>stage</b> object.
     * <p/>
     * The {@code previousStage} object is the previously returned value of this method.
     * Unhandled exception will result not to update the stage object in repository.
     * <p/>
     * All exceptions must be handled to save the the new stage object.
     *
     * @param objectType
     * @param previousStage           The previously returned object. If null then it's the first execution.
     * @param synchronizationListener The listener to send the changes to.
     * @return The new updated stage object. This will be the {@code previousStage} at next call.
     */
    public JsonValue liveSynchronize(String objectType, JsonValue previousStage, final SynchronizationListener synchronizationListener);
}
