/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
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
 * $Id$
 */
package org.forgerock.openidm.shell.impl;

import org.forgerock.openidm.objset.ObjectSet;
import org.forgerock.openidm.shell.CustomCommandScope;

import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.ServiceLoader;

/**
 * @author $author$
 * @version $Revision$ $Date$
 */
public class Main {
    private static ObjectSet router;

    public static void main(String[] args) throws Exception {
        String[] cmd = parseCommandName(args);
        String[] parameters = Arrays.copyOfRange(args, 1, Math.max(args.length, 1));
        CustomCommandScope scope = findCommandScope(cmd[0]);
        switch (cmd.length) {
            case 2:
                Method cmdMethod = findCommandMethod(scope.getClass(), cmd[1]);
                if (null != cmdMethod) {
                    cmdMethod.invoke(scope, System.in, System.out, parameters);
                    System.exit(0);
                }
                throw new UnsupportedOperationException(cmd[1]);
            case 1:
                scope.execute(parameters);
        }
    }

    private static String[] parseCommandName(String[] args) {
        if (args.length > 0) {
            String[] cmd = null;
            String[] clauses = args[0].split(":");
            if (0 == clauses.length || clauses.length > 2) {
                throw new IllegalArgumentException("The passed command identifier has more then one ':'");
            }
            switch (clauses.length) {
                case 2: {
                    cmd = new String[2];
                    // Do blank check StringUtils.isBlank()
                    if ((null != clauses[1]) && (!"".equals(clauses[1].trim()))) {
                        cmd[1] = clauses[1].trim();
                    }
                }
                case 1: {
                    if ((null == clauses[0]) || ("".equals(clauses[0].trim()))) {
                        throw new IllegalArgumentException("The passed scope identifier has no command");
                    } else {
                        if (cmd == null) cmd = new String[1];
                        cmd[0] = clauses[0].trim();
                    }
                }
            }

            return cmd;
        } else {
            throw new IllegalArgumentException("Command required");
        }
    }

    private static CustomCommandScope findCommandScope(String groupId) throws NoSuchFieldException, IllegalAccessException {
        CustomCommandScope scope = null;
        ServiceLoader<CustomCommandScope> ldr = ServiceLoader.load(CustomCommandScope.class);
        for (CustomCommandScope cmdScope : ldr) {
            if (cmdScope.getScope().equals(groupId)) {
                if (cmdScope instanceof AbstractRemoteCommandScope) {
                    Field bind = AbstractRemoteCommandScope.class.getDeclaredField("router");
                    if (null != bind) {
                        bind.setAccessible(true);
                        bind.set(cmdScope, getRouter());
                    }
                }
                scope = cmdScope;
                break;
            }
        }
        if (null == scope) {
            throw new UnsupportedOperationException(groupId);
        }
        return scope;
    }

    private static ObjectSet getRouter() {
        if (null == router) {
            //TODO init RemoteObjectSetRouterService router
        }
        return router;
    }

    public static Method findCommandMethod(Class<? extends CustomCommandScope> commandsProvider, String methodName) {
        try {
            return commandsProvider.getMethod(methodName, InputStream.class, PrintStream.class, String[].class);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
}
