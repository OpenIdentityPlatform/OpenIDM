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
package org.forgerock.openidm.shell.felixgogo;

import org.apache.felix.service.command.CommandSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Abstract class of Felix GoGo Commands Service
 *
 * @author $author$
 * @version $Revision$ $Date$
 */
public class AbstractFelixCommandsService {
    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(AbstractFelixCommandsService.class);

    private Object service;

    public AbstractFelixCommandsService(Object service) {
        this.service = service;
    }

    /**
     * Run command method
     *
     * @param commandName command name
     * @param params      command parameters
     */
    protected void runCommand(String commandName, CommandSession session, String[] params) {
        try {
            Method method = service.getClass().getMethod(commandName, InputStream.class, PrintStream.class, String[].class);
            method.invoke(service, session.getKeyboard(), session.getConsole(), params);
        } catch (NoSuchMethodException e) {
            session.getConsole().println("No such command: " + commandName);
        } catch (Exception e) {
            logger.warn("Unable to execute command: {} with args: {}", new Object[]{commandName, Arrays.toString(params)}, e);
            e.printStackTrace(session.getConsole());
        }
    }

}

