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

package org.forgerock.openidm.shell.impl;

import org.apache.felix.gogo.runtime.CommandProcessorImpl;
import org.apache.felix.gogo.runtime.threadio.ThreadIOImpl;
import org.apache.felix.gogo.shell.Console;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Converter;
import org.apache.felix.service.command.Function;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.shell.CustomCommandScope;

import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Main entry point for command-line interface.
 */
public final class Main {
    /** the command processor. */
    protected static CommandProcessorImpl processor;

    /**
     * Program main!
     *
     * @param args the command-line arguments
     * @throws Exception on failure to execute shell/command-line
     */
    public static void main(String[] args) throws Exception {
        initProcessor();
        CommandSession session = processor.createSession(System.in, System.out, System.err);
        session.put("prompt", "openidm# ");
        session.put("_cwd", IdentityServer.getFileForPath("."));
        // start shell
        if (args.length == 0) {
            Thread thread = new Thread(new Console(session), "OpenIDM shell");
            thread.start();
        } else {
            processor.eval(session, args);
        }
    }

    private static void initProcessor() {
        processor = new CommandProcessorImpl(new ThreadIOImpl());


        // Setup the variables and commands exposed in an OSGi environment.
        //processor.addConstant(CONTEXT, context);
        processor.addCommand("osgi", processor, "addCommand");
        processor.addCommand("osgi", processor, "removeCommand");
        processor.addCommand("osgi", processor, "eval");
        ServiceLoader<CustomCommandScope> ldr = ServiceLoader.load(CustomCommandScope.class);
        for (CustomCommandScope cmdScope : ldr) {
            if (null != cmdScope.getScope() && null != cmdScope.getFunctionMap()) {
                for (Map.Entry<String, String> entry : cmdScope.getFunctionMap().entrySet()) {
                    Function target = new CommandProxy(cmdScope,
                            entry.getKey());
                    processor.addCommand(cmdScope.getScope(), target, entry.getKey());
                }
            }
        }

        ServiceLoader<Converter> cldr = ServiceLoader.load(Converter.class);
        for (Converter converter : cldr) {
            processor.addConverter(converter);
        }
    }

    /**
     * Locate the command method.
     *
     * @param commandsProvider the command provider (scope)
     * @param methodName the method name
     * @return the method
     */
    public static Method findCommandMethod(Class<? extends CustomCommandScope> commandsProvider, String methodName) {
        try {
            return commandsProvider.getMethod(methodName, InputStream.class, PrintStream.class, String[].class);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    // do not instantiate
    private Main() {
    }
}
