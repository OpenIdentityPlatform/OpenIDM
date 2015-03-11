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

/**
 * A SystemPropertyAccessor accesses to System properties.
 *
 */
public class SystemPropertyAccessor implements PropertyAccessor {

    private final PropertyAccessor delegate;

    public SystemPropertyAccessor(final PropertyAccessor delegate) {
        this.delegate = delegate;
    }

    /**
     * Returns the value of the specified system property. If the key is not
     * found in the System properties, the {@code delegate} is then requested.
     * The method returns {@code null} if the property is not found.
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
     *         property is not found.
     */
    @SuppressWarnings("unchecked")
    public <T> T getProperty(String key, T defaultValue, Class<T> expected) {
        T value = null;
        if (null != key
                && ((null != expected && expected.isAssignableFrom(String.class)) || defaultValue instanceof String)) {
            value = (T) System.getProperty(key, (String) defaultValue);
        }
        return null != value ? value : (null != delegate) ? delegate.getProperty(key, defaultValue,
                expected) : null;
    }
}
