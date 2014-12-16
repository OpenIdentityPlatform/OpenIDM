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

package org.forgerock.openidm.shell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Parameter;
import org.forgerock.openidm.shell.felixgogo.MetaVar;

/**
 * An abstract command scope for the Felix Gogo command processor.
 */
public abstract class CustomCommandScope {
    /** formatting whitespace constant to insert before the option. */
    protected static final String LEAD_OPTION_SPACE = "  ";
    /** formatting whitespace constant used to pad the option's description. */
    protected static final String OPTIONS_SPACE = "                                  ";

    /**
     * Get the {@link org.apache.felix.service.command.CommandProcessor#COMMAND_FUNCTION} value.
     * <p/>
     * TODO add description
     *
     * @return retrun a new map where the key is the command name and the value is the description.
     */
    public abstract Map<String, String> getFunctionMap();

    /**
     * Get the {@link org.apache.felix.service.command.CommandProcessor#COMMAND_SCOPE} value.
     * <p/>
     * TODO add description
     *
     * @return the scope value
     */
    public abstract String getScope();

    /**
     * Gets usage information for method by name. Fetches the method with the longest list of arguments.
     *
     * @param name Name of the method to search for
     * @return String containing usage information
     * @throws NoSuchMethodException if no such method can be found
     */
    public String getUsage(String name) throws NoSuchMethodException {
        Method m = getLongestMethodByName(name);
        return getUsage(m);
    }

    /**
     * Gets usage information for a specified method.
     *
     * @param method Method object to pull information from.
     * @return String containing usage information.
     */
    public String getUsage(Method method) {
        StringBuilder usage = new StringBuilder("Usage: ").append(method.getName());
        List<String> args = getArguments(method);
        List<String> opts = getParameters(method);

        if (opts.size() > 0) {
            usage.append(" [options]");
        }

        for (String arg : args) {
            usage.append(" <").append(arg).append(">");
        }

        usage.append("\nScope: ").append(getScope());

        if (!opts.isEmpty()) {
            usage.append("\nOptions:");
        }

        for (String opt : opts) {
            usage.append("\n").append(opt);
        }

        return usage.toString();
    }

    /**
     * Fetches the header for the method with the longest argument list.
     *
     * @param name Name of the method to search for
     * @return String containing header information for the method
     */
    protected String getLongHeader(String name) {
        String header;
        try {
            Method method = getLongestMethodByName(name);
            header = getHeader(method);
        } catch (NoSuchMethodException e) {
            header = "[WARNING] No such method exists.";
        }
        return header;
    }

    /**
     * Fetches the header for the method with the shortest argument list.
     *
     * @param name Name of the method to search for
     * @return String containing header information for the method
     */
    protected String getShortHeader(String name) {
        String header;
        try {
            Method method = getShortestMethodByName(name);
            header = getHeader(method);
        } catch (NoSuchMethodException e) {
            header = "[WARNING] No such method exists.";
        }
        return header;
    }

    /**
     * Fetches the header from a specified method.
     *
     * @param method method to pull a header from
     * @return String containing the header for the specified method
     */
    protected String getHeader(Method method) {
        Descriptor desc = method.getAnnotation(Descriptor.class);
        return desc.value();
    }

    /**
     * Fetches the list of arguments from a specified method.
     * Arguments are defined by annotated method arguments with only a @Descriptor.
     * @param method method to fetch the list of arguments from
     * @return a list of Strings containing argument descriptions
     */
    protected List<String> getArguments(Method method) {
        List<String> args = new ArrayList<String>();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();

        for (Annotation[] annotations : parameterAnnotations) {
            String desc = null;
            boolean foundParam = false;

            for (Annotation a : annotations) {
                if (a instanceof Parameter) {
                    foundParam = true;
                } else if (a instanceof Descriptor) {
                    Descriptor d = (Descriptor) a;
                    desc = d.value();
                }
            }

            if (desc != null && !foundParam) {
                args.add(desc);
            }
        }
        return args;
    }

    /**
     * Fetches the list of parameters from a specified method.
     * <p>
     * Parameters are defined as by method arguments annotated with @Parameter
     *
     * @param method method to fetch the list of parameters from
     * @return a list of Strings containing parameter descriptions
     */
    protected List<String> getParameters(Method method) {
        List<String> opts = new ArrayList<String>();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();

        for (Annotation[] annotations : parameterAnnotations) {
            String names = null;
            String desc = "";
            String metaVar = null;

            for (Annotation a : annotations) {
                if (a instanceof Parameter) {
                    Parameter param = (Parameter) a;
                    names = StringUtils.join(param.names(), ", ");
                } else if (a instanceof MetaVar) {
                    MetaVar m = (MetaVar) a;
                    metaVar = m.value();
                } else if (a instanceof Descriptor) {
                    Descriptor d = (Descriptor) a;
                    desc = d.value();
                }
            }

            String namesWithMeta = StringUtils.isBlank(metaVar) ? names : names + " " + metaVar;

            if (names != null) {
                String str = LEAD_OPTION_SPACE
                        + namesWithMeta
                        + OPTIONS_SPACE.substring(Math.min(namesWithMeta.length(), OPTIONS_SPACE.length()))
                        + desc;
                opts.add(str);
            }
        }
        return opts;
    }

    /**
     * Fetches a sorted list of methods all sharing a specified name.
     *
     * This list is returned in increasing order of parameters.
     * @param name the name of the method list to retrieve
     * @return a sorted list of methods
     */
    protected List<Method> getAllMethodsByName(String name) {
        Method[] allMethods = this.getClass().getDeclaredMethods();
        List<Method> allNamedMethods = new ArrayList<Method>();
        for (Method m : allMethods) {
            if (m.getName().equals(name)) {
                allNamedMethods.add(m);
            }
        }

        Collections.sort(allNamedMethods, new Comparator<Method>() {
            public int compare(Method o1, Method o2) {
                Integer l1 = o1.getParameterTypes().length;
                Integer l2 = o2.getParameterTypes().length;
                return l1.compareTo(l2);
            }
        });

        return allNamedMethods;
    }

    /**
     * Fetch a method specified by name (gets the method with the largest number of parameters).
     *
     * @param name of the method to fetch
     * @return the method with the largest number of parameters by name
     * @throws NoSuchMethodException if no such method exists
     */
    protected Method getLongestMethodByName(String name) throws NoSuchMethodException {
        List<Method> methods = getAllMethodsByName(name);
        if (methods.isEmpty()) {
            throw new NoSuchMethodException();
        }
        return methods.get(methods.size() - 1);
    }

    /**
     * Fetch a method specified by name (gets the method with the shortest number of parameters).
     *
     * @param name of the method to fetch
     * @return the method with the largest number of parameters by name
     * @throws NoSuchMethodException if no such method exists
     */
    protected Method getShortestMethodByName(String name) throws NoSuchMethodException {
        List<Method> methods = getAllMethodsByName(name);
        if (methods.isEmpty()) {
            throw new NoSuchMethodException();
        }
        return methods.get(0);
    }
}
