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
import org.apache.felix.service.command.Parameter;
import org.forgerock.openidm.shell.CustomCommandScope;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

/**
 * @author $author$
 * @version $Revision$ $Date$
 */
public class BasicCommandScope implements CustomCommandScope {

    /**
     * {@inheritDoc}
     */
    public Map<String, String> getFunctionMap() {
        Map<String, String> help = new HashMap<String, String>();
        help.put("help", "displays available commands");
        help.put("exit", "exit from the console");
        return help;

    }

    /**
     * {@inheritDoc}
     */
    public String getScope() {
        return "basic";
    }


    @Descriptor("displays available commands")
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

    @Descriptor("exit from the console")
    public void exit() {
        System.exit(0);
    }


    @Descriptor("displays information about a specific command")
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
            for (Method m : methods) {
                Descriptor d = m.getAnnotation(Descriptor.class);
                if (d == null) {
                    session.getConsole().println("\n" + m.getName());
                } else {
                    session.getConsole().println("\n" + m.getName() + " - " + d.value());
                }

                session.getConsole().println("   scope: " + name.substring(0, name.indexOf(':')));

                // Get flags and options.
                Class[] paramTypes = m.getParameterTypes();
                Map<String, Parameter> flags = new TreeMap();
                Map<String, String> flagDescs = new TreeMap();
                Map<String, Parameter> options = new TreeMap();
                Map<String, String> optionDescs = new TreeMap();
                List<String> params = new ArrayList();
                Annotation[][] anns = m.getParameterAnnotations();
                for (int paramIdx = 0; paramIdx < anns.length; paramIdx++) {
                    Parameter p = findAnnotation(anns[paramIdx], Parameter.class);
                    d = findAnnotation(anns[paramIdx], Descriptor.class);
                    if (p != null) {
                        if (p.presentValue().equals(Parameter.UNSPECIFIED)) {
                            options.put(p.names()[0], p);
                            if (d != null) {
                                optionDescs.put(p.names()[0], d.value());
                            }
                        } else {
                            flags.put(p.names()[0], p);
                            if (d != null) {
                                flagDescs.put(p.names()[0], d.value());
                            }
                        }
                    } else if (d != null) {
                        params.add(paramTypes[paramIdx].getSimpleName());
                        params.add(d.value());
                    } else {
                        params.add(paramTypes[paramIdx].getSimpleName());
                        params.add("");
                    }
                }

                // Print flags and options.
                if (flags.size() > 0) {
                    session.getConsole().println("   flags:");
                    for (Map.Entry<String, Parameter> entry : flags.entrySet()) {
                        // Print all aliases.
                        String[] names = entry.getValue().names();
                        session.getConsole().print("      " + names[0]);
                        for (int aliasIdx = 1; aliasIdx < names.length; aliasIdx++) {
                            session.getConsole().print(", " + names[aliasIdx]);
                        }
                        session.getConsole().println("   " + flagDescs.get(entry.getKey()));
                    }
                }
                if (options.size() > 0) {
                    session.getConsole().println("   options:");
                    for (Map.Entry<String, Parameter> entry : options.entrySet()) {
                        // Print all aliases.
                        String[] names = entry.getValue().names();
                        session.getConsole().print("      " + names[0]);
                        for (int aliasIdx = 1; aliasIdx < names.length; aliasIdx++) {
                            session.getConsole().print(", " + names[aliasIdx]);
                        }
                        session.getConsole().println("   "
                                + optionDescs.get(entry.getKey())
                                + ((entry.getValue().absentValue() == null) ? ""
                                : " [optional]"));
                    }
                }
                if (params.size() > 0) {
                    session.getConsole().println("   parameters:");
                    for (Iterator<String> it = params.iterator(); it.hasNext(); ) {
                        session.getConsole().println("      " + it.next() + "   " + it.next());
                    }
                }
            }
        }
    }

    private static <T extends Annotation> T findAnnotation(Annotation[] annotations, Class<T> clazz) {
        for (int i = 0; (annotations != null) && (i < annotations.length); i++) {
            if (clazz.isInstance(annotations[i])) {
                return clazz.cast(annotations[i]);
            }
        }
        return null;
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
