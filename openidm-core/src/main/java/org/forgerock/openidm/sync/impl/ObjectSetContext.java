package org.forgerock.openidm.sync.impl;

import org.forgerock.json.resource.ServerContext;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * A NAME does ...
 *
 */
public class ObjectSetContext {

    /** Stack of requests. */
    private static ThreadLocal<Deque<ServerContext>> stack = new ThreadLocal<Deque<ServerContext>>() {
        @Override protected Deque<ServerContext> initialValue() {
            return new ArrayDeque<ServerContext>();
        }
    };

    /**
     * Pushes a request onto the top of the stack.
     *
     * @param request the request to be pushed onto the top of the stack.
     */
    public static void push(ServerContext request) {
        stack.get().push(request);
    }

    /**
     * Pops the request from the top of the stack and returns it.
     *
     * @throws NoSuchElementException if there is no request on the top of the stack.
     */
    public static ServerContext pop() {
        return stack.get().pop();
    }

    /**
     * Returns the request on the top of the stack, or {@code null} if there is no request
     * on the top of the stack.
     */
    public static ServerContext get() {
        return stack.get().peekFirst();
    }

    /**
     * Removes all of the requests in the stack.
     */
    public static void clear() {
        stack.get().clear();
    }

}
