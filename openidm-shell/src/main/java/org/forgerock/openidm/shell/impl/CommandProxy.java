/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.forgerock.openidm.shell.impl;

import org.apache.felix.gogo.runtime.Reflective;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Function;
import org.forgerock.openidm.shell.CustomCommandScope;

import java.util.ArrayList;
import java.util.List;

/**
 * Based on gogo shell command proxy.
 */
public class CommandProxy implements Function {

    private CustomCommandScope tgt;
    private String function;

    /**
     * Construct teh command proxy.
     *
     * @param tgt the command scope
     * @param function the name of the function to execute
     */
    public CommandProxy(CustomCommandScope tgt, String function) {
        this.tgt = tgt;
        this.function = function;
    }

    /**
     * Execute the command.
     *
     * @param session the command session
     * @param arguments the command arguments
     * @return the result of the command execution
     * @throws Exception from command execution / invocation
     */
    public Object execute(CommandSession session, List<Object> arguments) throws Exception {
        try {
            if (tgt instanceof Function) {
                return ((Function) tgt).execute(session, arguments);
            } else {
                return Reflective.invoke(session, tgt, function, arguments);
            }
        } catch (IllegalArgumentException e) {
            List<Object> method = new ArrayList<Object>();
            method.add(function);
            session.getConsole().println(Reflective.invoke(session, tgt, "getUsage", method));
            return null;
        }
    }
}
