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
package org.forgerock.openidm.osgi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.naming.CompositeName;
import javax.naming.InvalidNameException;
import javax.naming.NamingException;

/**
 * A composite name for the OpenIDM namespace. This provides useful utility
 * methods for accessing the name.
 *
 * Based on apache aries org/apache/aries/jndi/url/OsgiName.java apache aries
 * org/apache/aries/jndi/url/OsgiNameParser.java
 *
 * <p/>
 * component 0: osgi:framework, osgi:service, openidm:services, osgi:servicelist
 * component 1: interface component 2: filter
 *
 */
@SuppressWarnings("serial")
public class OsgiName extends CompositeName {

    private static final long serialVersionUID = 3567653491060394677L;
    public static final String OSGI_SCHEME = "osgi";
    public static final String OPENIDM_SCHEME = "openidm";
    public static final String FRAMEWORK_PATH = "framework";
    public static final String SERVICE_PATH = "service";
    public static final String SERVICES_PATH = "services";
    public static final String SERVICE_LIST_PATH = "servicelist";

    public OsgiName(String name) {
        super(split(name));
    }

    public boolean hasFilter() {
        return size() == 3;
    }

    public boolean isServiceNameBased() {
        return size() > 3;
    }

    public String getInterface() {
        return get(1);
    }

    public String getFilter() {
        return hasFilter() ? get(2) : null;
    }

    public String getServiceName() {
        Enumeration<String> parts = getAll();
        parts.nextElement();

        StringBuilder builder = new StringBuilder();

        if (parts.hasMoreElements()) {

            while (parts.hasMoreElements()) {
                builder.append(parts.nextElement());
                builder.append('/');
            }

            builder.deleteCharAt(builder.length() - 1);
        }

        return builder.toString();
    }

    public boolean hasInterface() {
        return size() > 1;
    }

    public String getScheme() {
        String part0 = get(0);
        int index = part0.indexOf(':');

        String result;

        if (index > 0) {
            result = part0.substring(0, index);
        } else {
            result = null;
        }

        return result;
    }

    public String getSchemePath() {
        String part0 = get(0);
        int index = part0.indexOf(':');

        String result;

        if (index > 0) {
            result = part0.substring(index + 1);
        } else {
            result = null;
        }

        return result;
    }

    static Enumeration<String> split(String name) {
        List<String> elements = new ArrayList<String>();

        StringBuilder builder = new StringBuilder();

        int len = name.length();
        int count = 0;

        for (int i = 0; i < len; i++) {
            char c = name.charAt(i);

            if (c == '/' && count == 0) {
                elements.add(builder.toString());
                builder = new StringBuilder();
                continue;
            } else if (c == '(') {
                count++;
            } else if (c == ')') {
                count++;
            }

            builder.append(c);
        }

        elements.add(builder.toString());

        return Collections.enumeration(elements);
    }

    /**
     * Parse the String representation of a OSGi Object name.
     * <p/>
     * Example:
     * {@code osgi:service/javax.sql.DataSource/(osgi.jndi.service.name=jdbc/openidm)}
     *
     * @param name
     *            TODO
     * @return TODO
     * @throws NamingException
     *             if the name String has invalid syntax.
     */
    public static OsgiName parse(String name) throws NamingException {
        OsgiName result = new OsgiName(name);
        String urlScheme = result.getScheme();
        String schemePath = result.getSchemePath();

        if (OSGI_SCHEME.equals(urlScheme)
                && !(SERVICE_PATH.equals(schemePath) || SERVICE_LIST_PATH.equals(schemePath) || FRAMEWORK_PATH
                        .equals(schemePath))) {
            throw new InvalidNameException(name);
        }
        if (!(OSGI_SCHEME.equals(urlScheme) || OPENIDM_SCHEME.equals(urlScheme))) {
            throw new InvalidNameException(name);
        }

        if (OPENIDM_SCHEME.equals(urlScheme) && !SERVICES_PATH.equals(schemePath)) {
            throw new InvalidNameException(name);
        }
        return result;
    }

}
