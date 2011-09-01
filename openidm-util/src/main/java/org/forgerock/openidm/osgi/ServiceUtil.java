/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 */
package org.forgerock.openidm.osgi;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import java.util.Arrays;
import java.util.Comparator;

/**
 * @author $author$
 * @version $Revision$ $Date$
 */
public class ServiceUtil {

    private static class ServicePair {
        private ServiceReference ref;
        private Object service;
    }

    public static Object getService(BundleContext ctx, OsgiName lookupName, String id, boolean requireService) {
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

        ServicePair pair = null;

        if (!lookupName.isServiceNameBased()) {
            pair = findService(ctx, interfaceName, filter);
        }

        if (pair == null) {
            if (id == null) {
                filter = "(osgi.jndi.service.name=" + serviceName + ')';
            } else {
                filter = "(&(" + Constants.SERVICE_ID + '=' + id + ")(osgi.jndi.service.name=" + serviceName + "))";
            }
            pair = findService(ctx, null, filter);
        }

        Object result = null;
        if (pair != null) {
            if (requireService) {
                result = pair.service;
            } else {
                result = pair.ref;
            }
        }
        return result;
    }

    private static ServicePair findService(BundleContext ctx, String interfaceName, String filter) {
        ServicePair p = null;

        try {
            ServiceReference[] refs = ctx.getServiceReferences(interfaceName, filter);

            if (refs != null) {
                // change the service order
                Arrays.sort(refs, new Comparator<ServiceReference>() {
                    public int compare(ServiceReference o1, ServiceReference o2) {
                        return o2.compareTo(o1);
                    }
                });

                for (ServiceReference ref : refs) {
                    Object service = ctx.getService(ref);

                    if (service != null) {
                        p = new ServicePair();
                        p.ref = ref;
                        p.service = service;
                        break;
                    }
                }
            }

        } catch (InvalidSyntaxException e) {
            // If we get an invalid syntax exception we just ignore it. Null will be returned which is valid
        }
        return p;
    }
}
