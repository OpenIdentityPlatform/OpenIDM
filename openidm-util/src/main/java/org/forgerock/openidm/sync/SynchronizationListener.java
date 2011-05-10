/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openidm.sync;

// Java Standard Edition
import java.util.Map;

/**
 * Interface for implementations that wish to receive notifications of object changes,
 * for the purpose of performing synchronization.
 * <p>
 * The {@code id} parameter is a fully-qualified object identitifer, relative to the root
 * of the internal object set router. Example: {@code "managed/user/jsmith"}. Implementations
 * should ignore object identifiers that they do not recognize.
 *
 * @author Paul C. Bryan
 */
public interface SynchronizationListener {
    
    /**
     * Called when a source object has been created.
     *
     * @param id the fully-qualified identifier of the object that was created.
     * @param object the value of the object that was created.
     * @throws SynchronizationException if an exception occurs processing the notification.
     */
    void onCreate(String id, Map<String, Object> object) throws SynchronizationException;

    /**
     * Called when a source object has been updated.
     *
     * @param id the fully-qualified identifier of the object that was updated.
     * @param newValue the new value of the object after the update.
     * @throws SynchronizationException if an exception occurs processing the notification.
     */
    void onUpdate(String id, Map<String, Object> newValue) throws SynchronizationException;

    /**
     * Called when a source object has been updated.
     *
     * @param id the fully-qualified identifier of the object that was updated.
     * @param oldValue the old value of the object prior to the update.
     * @param newValue the new value of the object after the update.
     * @throws SynchronizationException if an exception occurs processing the notification.
     */
    void onUpdate(String id, Map<String, Object> oldValue, Map<String, Object> newValue)
    throws SynchronizationException;

    /**
     * Called when a source object has been deleted.
     *
     * @param id the fully-qualified identifier of the object that was deleted.
     * @throws SynchronizationException if an exception occurs processing the notification.
     */
    void onDelete(String id) throws SynchronizationException;
}
