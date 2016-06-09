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
 * Copyright 2011-2016 ForgeRock AS.
 */
package org.forgerock.openidm.osgi;

import org.osgi.framework.ServiceReference;

/**
 * Listener to handle state changes in the Service Tracker
 *
 * @param <S> Type of Service.
 * @param <T> The type of the tracked object.
 */
public interface ServiceTrackerListener<S, T> {

    /**
     * Notified when a service is added.
     *
     * @param reference
     *            reference of added service
     * @param service
     *            added service
     */
    void addedService(ServiceReference<S> reference, T service);

    /**
     * Notified when a service is removed.
     *
     * @param reference
     *            reference of removed service
     * @param service
     *            removed service
     */
    void removedService(ServiceReference<S> reference, T service);

    /**
     * Notified when a service is modified
     *
     * @param reference
     *            reference of modified service.
     * @param service
     *            modified service
     */
    void modifiedService(ServiceReference<S> reference, T service);
}
