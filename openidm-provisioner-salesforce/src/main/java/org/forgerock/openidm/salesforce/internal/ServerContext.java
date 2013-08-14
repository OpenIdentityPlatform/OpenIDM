/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All Rights Reserved
 */

package org.forgerock.openidm.salesforce.internal;

import java.util.regex.Matcher;

/**
 * A NAME does ...
 * 
 * @author Laszlo Hordos
 */
public class ServerContext {

    private static ThreadLocal<ServerContext> stack = new ThreadLocal<ServerContext>();

    /**
     * Returns the request on the top of the stack, or {@code null} if there is
     * no request on the top of the stack.
     */
    public static void set(ServerContext context) {
        stack.set(context);
    }

    public static ServerContext get() {
        return stack.get();
    }

    /**
     * Removes all of the requests in the stack.
     */
    public static void clear() {
        stack.set(null);
    }

    public static ServerContext build(Matcher matcher) {
        ServerContext sc = new ServerContext(matcher);
        stack.set(sc);
        return sc;
    }

    private final Matcher matcher;

    private ServerContext(Matcher matcher) {
        this.matcher = matcher;
    }

    public Matcher getMatcher() {
        return matcher;
    }
}
