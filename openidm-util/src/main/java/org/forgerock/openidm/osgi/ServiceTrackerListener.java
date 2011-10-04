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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 */
package org.forgerock.openidm.osgi;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Filter;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * Listener to handle state changes in the Service Tracker
 * 
 * @author aegloff
 */
public interface ServiceTrackerListener {
    
    /**
     * Notified when a service is added
     * @param reference reference of added service
     * @param service added service
     */
    public void addedService(ServiceReference reference, Object service);

    /**
     * Notified when a service is removed
     * @param reference reference of removed service
     * @param service removed service
     */
    public void removedService(ServiceReference reference, Object service);
    
    /**
     * Notified when a service is modified
     * @param reference reference of modified service
     * @param service modified service
     */
    public void modifiedService(ServiceReference reference, Object service);
}
