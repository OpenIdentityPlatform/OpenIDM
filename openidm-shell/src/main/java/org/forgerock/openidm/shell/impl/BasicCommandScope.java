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

import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Descriptor;
import org.forgerock.openidm.shell.CustomCommandScope;

import java.lang.reflect.Method;
import java.util.*;

/**
 * @author $author$
 * @version $Revision$ $Date$
 */
public class BasicCommandScope extends CustomCommandScope {

    /**
     * {@inheritDoc}
     */
    public Map<String, String> getFunctionMap() {
        Map<String, String> help = new HashMap<String, String>();
        help.put("help", getShortHeader("help"));
        help.put("exit", getShortHeader("exit"));
        return help;

    }

    /**
     * {@inheritDoc}
     */
    public String getScope() {
        return "basic";
    }


    @Descriptor("Displays available commands.")
    public void help(CommandSession session) {
        ServiceLoader<CustomCommandScope> ldr = ServiceLoader.load(CustomCommandScope.class);
        for (CustomCommandScope cmdScope : ldr) {
            if (null != cmdScope.getScope() && null != cmdScope.getFunctionMap()) {
                for (Map.Entry<String, String> entry : cmdScope.getFunctionMap().entrySet()) {
                    session.getConsole().append("\t").append(cmdScope.getScope()).append(":").append(entry.getKey()).append("\t").println(entry.getValue());
                }
            }
        }
    }

    @Descriptor("Exit from the console.")
    public void exit() {
        System.exit(0);
    }

    @Descriptor("Displays information about a specific command.")
    public void help(CommandSession session, @Descriptor("target command") String name) {
        Map<String, List<Method>> commands = getCommands();

        List<Method> methods = null;

        // If the specified command doesn't have a scope, then
        // search for matching methods by ignoring the scope.
        int scopeIdx = name.indexOf(':');
        if (scopeIdx < 0) {
            for (Map.Entry<String, List<Method>> entry : commands.entrySet()) {
                String k = entry.getKey().substring(entry.getKey().indexOf(':') + 1);
                if (name.equals(k)) {
                    name = entry.getKey();
                    methods = entry.getValue();
                    break;
                }
            }
        }
        // Otherwise directly look up matching methods.
        else {
            methods = commands.get(name);
        }

        if ((methods != null) && (methods.size() > 0)) {
            StringBuilder help = new StringBuilder();
            for (int i = 0; i < methods.size(); i++) {
                Method m = methods.get(i);
                if (i > 0) {
                    help.append("\n");
                }
                help.append("Info: ").append(getHeader(m)).append("\n");
                help.append(getUsage(m)).append("\n");
            }
            session.getConsole().print(help.toString());
        }
    }

    private Map<String, List<Method>> getCommands() {
        Map<String, List<Method>> commands = new TreeMap();

        ServiceLoader<CustomCommandScope> ldr = ServiceLoader.load(CustomCommandScope.class);
        for (CustomCommandScope cmdScope : ldr) {
            if (null != cmdScope.getScope() && null != cmdScope.getFunctionMap()) {

                for (String func : cmdScope.getFunctionMap().keySet()) {
                    commands.put(cmdScope.getScope() + ":" + func, new ArrayList());
                }

                if (!commands.isEmpty()) {
                    Method[] methods = cmdScope.getClass().getMethods();
                    for (Method method : methods) {
                        List<Method> commandMethods = commands.get(cmdScope.getScope() + ":"
                                + method.getName());
                        if (commandMethods != null) {
                            commandMethods.add(method);
                        }
                    }
                }

                // Remove any missing commands.
                Iterator<Map.Entry<String, List<Method>>> it = commands.entrySet().iterator();
                while (it.hasNext()) {
                    if (it.next().getValue().size() == 0) {
                        it.remove();
                    }
                }
            }
        }

        return commands;
    }
}
