/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All Rights Reserved
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
