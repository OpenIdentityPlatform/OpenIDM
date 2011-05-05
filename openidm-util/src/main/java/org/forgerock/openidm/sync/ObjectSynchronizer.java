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

import java.util.Map;

/**
 * An interface for notifying that the state of a {@code source object} has changed, which results in synchronizing
 * {@code sourceObject} fields to {@code targetObjects}, through defined {@code mappings} .
 * <p/>
 * All {@code mappings} that have been defined for the given {@code sourceObject} will be evaluated and applied to
 * {@code target objects}, ordered first by synchrony and then by definition in configuration.
 * <p/>
 * Object identifiers are {@link org.forgerock.openidm.script.Script}'s in the form of uri's e.g.
 * <p/>
 * /{context_root}/{object_set}}/{object...}/{_id}
 */
public interface ObjectSynchronizer {

    /**
     * Notifies that the {@code sourceObject} has been created. This will result in changes being
     * synchronized to all {@code targetObject}s with a defined mapping.
     *
     * @param id       is the source identifier of the object that has been created and must not be {@code null}
     * @param newValue is the newly created object
     * @throws ObjectSynchronizationException if an underlying exception occurs
     */
    public void onCreate(String id, Map<String, Object> newValue) throws ObjectSynchronizationException;

    /**
     * Notifies that the {@code sourceObject} has been updated, however we do not have it's old value. In this case
     * a propagator needs to project all {@code mapped} fields for a given {@code targetObject}.
     *
     * @param id       is the source identifier of the object being updated and must not be {@code null}
     * @param newValue is the state of the {@code source object}
     * @throws ObjectSynchronizationException if an underlying exception occurs
     */
    public void onUpdate(String id, Map<String, Object> newValue) throws ObjectSynchronizationException;

    /**
     * Notifies that the {@code sourceOjbect} has been updated, providing the old and new {@code sourceObject}
     * values. By supplying old and new {@code sourceObject} values during an update, a source propagator can be
     * very smart in applying only changed {@code sourceObject} fields to {@code targetObjects}
     *
     * @param id       is the source identifier of the object being updated and must not be {@code null}
     * @param oldValue is the former state of the {@code sourceObject} and must not be {@code null}
     * @param newValue is the to be state of the {@code sourceObject} and must not be {@code null}
     * @throws ObjectSynchronizationException if an underlying exception occurs
     */
    public void onUpdate(String id, Map<String, Object> oldValue, Map<String, Object> newValue)
            throws ObjectSynchronizationException;

    /**
     * Notifies that the {@code sourceObject} has been deleted, it's {@code oldValue} may be supplied or null.
     * There is no difference in semantics if the {@code oldValue} is null. However, if the old value is not null,
     * then an implementation may chose to use the value for additional context e.g. logging and or auditing.
     *
     * @param id          is the identifier of the object that has been deleted and must not be {@code null}
     * @param objectValue is the former state of the {@code source object}
     * @throws ObjectSynchronizationException
     *          if an underlying exception occurs
     */
    public void onDelete(String id, Map<String, Object> objectValue) throws ObjectSynchronizationException;

    /**
     * Notifies that the {@code sourceObject} has been deleted, without having the {@code oldValue}
     *
     * @param id is the identifier of the object that has been deleted and must not be {@code null}
     * @throws ObjectSynchronizationException
     *          if an underlying exception occurs
     */
    public void onDelete(String id) throws ObjectSynchronizationException;

}