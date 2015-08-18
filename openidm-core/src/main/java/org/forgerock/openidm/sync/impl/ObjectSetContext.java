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
 * Portions copyright 2011-2015 ForgeRock AS.
 */
package org.forgerock.openidm.sync.impl;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.NoSuchElementException;

import org.forgerock.http.Context;

/**
 * A NAME does ...
 *
 */
public class ObjectSetContext {

    /** Stack of requests. */
    private static ThreadLocal<Deque<Context>> stack = new ThreadLocal<Deque<Context>>() {
        @Override protected Deque<Context> initialValue() {
            return new ArrayDeque<Context>();
        }
    };

    /**
     * Pushes a request onto the top of the stack.
     *
     * @param request the request to be pushed onto the top of the stack.
     */
    public static void push(Context request) {
        stack.get().push(request);
    }

    /**
     * Pops the request from the top of the stack and returns it.
     *
     * @throws NoSuchElementException if there is no request on the top of the stack.
     */
    public static Context pop() {
        return stack.get().pop();
    }

    /**
     * Returns the request on the top of the stack, or {@code null} if there is no request
     * on the top of the stack.
     */
    public static Context get() {
        return stack.get().peekFirst();
    }

    /**
     * Removes all of the requests in the stack.
     */
    public static void clear() {
        stack.get().clear();
    }

}
