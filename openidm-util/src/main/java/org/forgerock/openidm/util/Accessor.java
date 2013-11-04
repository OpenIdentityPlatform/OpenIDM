/*
 * Copyright © 2013 ForgeRock AS. All rights reserved.
 *
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
 */
package org.forgerock.openidm.util;

/**
 * A simple accessor of an object instance.
 *
 * @author brmiller
 */
public interface Accessor<T> {
    /**
     * Provides access to an instance of type T
     *
     * @param T the class to be accessed
     * @return an instance of type T
     */
    T access();
}
