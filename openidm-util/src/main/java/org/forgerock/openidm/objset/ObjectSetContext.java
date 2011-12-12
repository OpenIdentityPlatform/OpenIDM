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

package org.forgerock.openidm.objset;

// Java SE
import java.util.ArrayDeque;
import java.util.Deque;

// JSON Fluent
import org.forgerock.json.fluent.JsonValue;

/**
 * TODO: Description.
 *
 * @author Paul C. Bryan
 */
public class ObjectSetContext {

    /** Stack of requests. */
    private static ThreadLocal<Deque<JsonValue>> stack = new ThreadLocal<Deque<JsonValue>>() {
        @Override protected Deque<JsonValue> initialValue() {
            return new ArrayDeque<JsonValue>();
        }
    };

    /**
     * Pushes a request onto the top of the stack.
     *
     * @param request the request to be pushed onto the top of the stack.
     */
    public static void push(JsonValue request) {
        stack.get().push(request);
    }

    /**
     * Pops the request from the top of the stack and returns it.
     *
     * @throws NoSuchElementException if there is no request on the top of the stack.
     */
    public static JsonValue pop() {
        return stack.get().pop();
    }

    /**
     * Returns the request on the top of the stack, or {@code null} if there is no request
     * on the top of the stack.
     */
    public static JsonValue get() {
        return stack.get().peekFirst();
    }

    /**
     * Removes all of the requests in the stack.
     */
    public static void clear() {
        stack.get().clear();
    }
}
