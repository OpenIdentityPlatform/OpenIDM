/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock AS. All Rights Reserved
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
 */

package org.forgerock.openidm.core;

import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

/**
 * A ServicePropertyAccessor tracks services with
 * {@code ServiceTracker<Map<String, Object>, Map<String, Object>>} and gets the
 * Properties.
 *
 */
public class ServicePropertyAccessor implements PropertyAccessor {

    private final PropertyAccessor delegate;
    private final BundleContext context;
    private final ServiceTracker<Map<String, Object>, Map<String, Object>> serviceTracker;

    public ServicePropertyAccessor(final BundleContext context,
            final ServiceTracker<Map<String, Object>, Map<String, Object>> serviceTracker,
            final PropertyAccessor delegate) {
        assert null != context;
        this.delegate = delegate;
        this.context = context;
        this.serviceTracker = serviceTracker;
    }

    /**
     * Returns the value of the specified property. If the key is not found in
     * the tracked services, the {@code delegate} is then requested. The method
     * returns {@code null} if the property is not found.
     *
     * @param key
     *            The name of the requested property.
     * @param defaultValue
     *            The value to return if the property is not found.
     * @param expected
     *            The type of the expected value.
     * @param <T>
     *            The type of the expected property.
     * @return The value of the requested property, or {@code null} if the
     *         property is undefined.
     * @throws SecurityException
     *             If the caller does not have the appropriate
     *             {@code PropertyPermission} to read the property, and the Java
     *             Runtime Environment supports permissions.
     */
    @SuppressWarnings("unchecked")
    public <T> T getProperty(String key, T defaultValue, Class<T> expected) {
        T value = null;
        ServiceReference<Map<String, Object>>[] references = serviceTracker.getServiceReferences();
        if (null != references) {
            int i = 0;
            while (i < references.length) {
                Object candidateValue = context.getService(references[i]).get(key);
                if (null != expected && null != candidateValue
                        && expected.isAssignableFrom(candidateValue.getClass())) {
                    // We found the value
                    value = (T) candidateValue;
                    break;
                }
                i++;
            }
        }
        return null != value ? value : (null != delegate) ? delegate.getProperty(key, defaultValue,
                expected) : null;
    }
}
