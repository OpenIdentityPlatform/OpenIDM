/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 ForgeRock AS. All Rights Reserved
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
 * A NAME does ...
 *
 */
public interface PropertyAccessor {
    /**
     * Returns the value of the specified property. If the key is not found in
     * the Framework properties, the system properties are then searched. The
     * method returns {@code null} if the property is not found.
     *
     * @param key
     *            The name of the requested property.
     * @param defaultValue
     * @param expected
     * @param <T>
     *            The type of the expected property.
     * @return The value of the requested property, or {@code null} if the
     *         property is undefined.
     *
     * @throws SecurityException
     *             If the caller does not have the appropriate
     *             {@code PropertyPermission} to read the property, and the Java
     *             Runtime Environment supports permissions.
     */
    <T> T getProperty(String key, T defaultValue, Class<T> expected);
}
