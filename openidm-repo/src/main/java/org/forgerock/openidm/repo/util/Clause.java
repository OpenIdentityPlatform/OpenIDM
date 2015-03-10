/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2015 ForgeRock AS. All rights reserved.
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
package org.forgerock.openidm.repo.util;

/**
 * Model a where-clause or where-clause constituent.
 */
public interface Clause extends SQLRenderer<String> {
    /**
     * Model a composite clause as in 'a AND b'.
     * Helper method to construct an AND clause with a String.
     *
     * @param clause the other clause to AND with this one
     * @return the composite AND-clause
     */
    Clause and(String clause);

    /**
     * Model a composite clause as in 'a AND b'.
     *
     * @param clause the other clause to AND with this one
     * @return the composite AND-clause
     */
    Clause and(Clause clause);

    /**
     * Model a composite clause as in 'a OR b'.
     * Helper method to construct an AND clause with a String.
     *
     * @param clause the other clause to OR with this one
     * @return the composite OR-clause
     */
    Clause or(String clause);

    /**
     * Model a composite clause as in 'a OR b'.
     *
     * @param clause the other clause to OR with this one
     * @return the composite OR-clause
     */
    Clause or(Clause clause);

    /**
     * Model a composite clause as in 'NOT a'.
     *
     * @return the negated clause
     */
    Clause not();
}
