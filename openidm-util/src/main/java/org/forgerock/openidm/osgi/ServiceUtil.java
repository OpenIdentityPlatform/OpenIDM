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

import java.util.Arrays;
import java.util.Comparator;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * Service helper utility, * Based on apache aries
 * org/apache/aries/jndi/services/ServiceHelper.java
 */
public final class ServiceUtil {

    private ServiceUtil() {
    }

    public static Object getService(BundleContext ctx, OsgiName lookupName, String id,
            boolean requireService) {
        String interfaceName = lookupName.getInterface();
        String filter = lookupName.getFilter();
        String serviceName = lookupName.getServiceName();

        if (id != null) {
            if (filter == null) {
                filter = '(' + Constants.SERVICE_ID + '=' + id + ')';
            } else {
                filter = "(&(" + Constants.SERVICE_ID + '=' + id + ')' + filter + ')';
            }
        }

        Pair<ServiceReference<?>, Object> pair = null;

        if (!lookupName.isServiceNameBased()) {
            pair = findService(ctx, interfaceName, filter);
        }

        if (pair == null) {
            if (id == null) {
                filter = "(osgi.jndi.service.name=" + serviceName + ')';
            } else {
                filter =
                        "(&(" + Constants.SERVICE_ID + '=' + id + ")(osgi.jndi.service.name="
                                + serviceName + "))";
            }
            pair = findService(ctx, null, filter);
        }

        Object result = null;
        if (pair != null) {
            if (requireService) {
                result = pair.getRight();
            } else {
                result = pair.getLeft();
            }
        }
        return result;
    }

    private static Pair<ServiceReference<?>, Object> findService(BundleContext ctx,
            String interfaceName, String filter) {
        Pair<ServiceReference<?>, Object> p = null;

        try {
            ServiceReference<?>[] refs = ctx.getServiceReferences(interfaceName, filter);

            if (refs != null) {
                // change the service order
                Arrays.sort(refs, new Comparator<ServiceReference<?>>() {
                    public int compare(ServiceReference<?> o1, ServiceReference<?> o2) {
                        return o2.compareTo(o1);
                    }
                });

                for (ServiceReference<?> ref : refs) {
                    Object service = ctx.getService(ref);

                    if (service != null) {
                        p = new ImmutablePair<ServiceReference<?>, Object>(ref, service);
                        break;
                    }
                }
            }

        } catch (InvalidSyntaxException e) {
            // If we get an invalid syntax exception we just ignore it. Null
            // will be returned which is valid
        }
        return p;
    }
}
