/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
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
 * Abstract class of Felix GoGo Commands Service.
 *
 * Based on Apache License 2.0 licensed osgilab org.knowhowlab.osgi.shell.felixgogo
 */
public class AbstractFelixCommandsService {
    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(AbstractFelixCommandsService.class);

    private Object service;

    /**
     * Construct the Felix command service.
     *
     * @param service the command service
     */
    public AbstractFelixCommandsService(Object service) {
        this.service = service;
    }

    /**
     * Run command method.
     *
     * @param commandName command name
     * @param session     command session
     * @param params      command parameters
     */
    protected void runCommand(String commandName, CommandSession session, String[] params) {
        try {
            Method method = service.getClass().getMethod(
                    commandName, InputStream.class, PrintStream.class, String[].class);
            method.invoke(service, session.getKeyboard(), session.getConsole(), params);
        } catch (NoSuchMethodException e) {
            session.getConsole().println("No such command: " + commandName);
        } catch (Exception e) {
            logger.warn("Unable to execute command: {} with args: {}", commandName, Arrays.toString(params), e);
            e.printStackTrace(session.getConsole());
        }
    }

}

