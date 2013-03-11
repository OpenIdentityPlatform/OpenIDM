/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.config;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.QueryFilter;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;

/**
 * A NAME does ...
 * 
 * @author Laszlo Hordos
 */
public interface ConfigurationManager {

    public class PID {

        /**
         * Default prefix for OpenIDM OSGi services
         */
        public static final String DEFAULT_SERVICE_RDN_PREFIX = "org.forgerock.openidm.";
        public static final String OBJECT_PROPERTY_ROLES = "_roles";

        private final List<String> roles;

        private final String serviceName;

        private final String instanceAlias;

        private PID(String serviceName, String instanceAlias, List<String> roles) {
            this.serviceName = serviceName;
            this.instanceAlias = instanceAlias;
            this.roles = roles;
        }

        public static PID serviceName(final String serviceName) {
            return serviceName(serviceName, (List<String>) null);
        }

        public static PID serviceName(final String serviceName, final List<String> roles) {
            String pid =
                    null != serviceName ? serviceName.trim()
                            .replace(DEFAULT_SERVICE_RDN_PREFIX, "") : null;
            if (null == pid || pid.isEmpty()) {
                throw new IllegalArgumentException("ServiceName can not be blank");
            }
            return new PID(pid, null, null != roles ? Collections.unmodifiableList(roles) : null);
        }

        public static PID serviceName(final String serviceName, final String instanceAlias) {
            return serviceName(serviceName, instanceAlias, null);
        }

        public static PID serviceName(final String serviceName, final String instanceAlias,
                final List<String> roles) {
            String pid =
                    null != serviceName ? serviceName.trim()
                            .replace(DEFAULT_SERVICE_RDN_PREFIX, "") : null;
            if (null == pid || pid.isEmpty()) {
                throw new IllegalArgumentException("ServiceName can not be blank");
            }
            String alias =
                    null != instanceAlias ? instanceAlias.trim() : UUID.randomUUID().toString();
            return new PID(pid, alias.isEmpty() ? UUID.randomUUID().toString() : alias,
                    null != roles ? Collections.unmodifiableList(roles) : null);
        }

        public List<String> getRoles() {
            return roles;
        }

        public String getQualifiedServiceName() {
            return PID.DEFAULT_SERVICE_RDN_PREFIX + serviceName;
        }

        public String getServiceName() {
            return serviceName;
        }

        public String getInstanceAlias() {
            return instanceAlias;
        }

        public String getShortCanonicalName() {
            if (null != instanceAlias) {
                return (serviceName + "/" + instanceAlias).toLowerCase();
            } else {
                return serviceName.toLowerCase();
            }
        }

        public String getLongCanonicalName() {
            if (null != instanceAlias) {
                return (DEFAULT_SERVICE_RDN_PREFIX + serviceName + "/" + instanceAlias)
                        .toLowerCase();
            } else {
                return (DEFAULT_SERVICE_RDN_PREFIX + serviceName).toLowerCase();
            }
        }

        @Override
        public String toString() {
            return getShortCanonicalName();
        }
    }

    // Properties to set configuration file handling behavior
    // TODO Write JavaDoc
    public static final String OPENIDM_FILEINSTALL_BUNDLES_NEW_START =
            "openidm.fileinstall.bundles.new.start";
    public static final String OPENIDM_FILEINSTALL_FILTER = "openidm.fileinstall.filter";
    public static final String OPENIDM_FILEINSTALL_DIR = "openidm.fileinstall.dir";
    public static final String OPENIDM_FILEINSTALL_POLL = "openidm.fileinstall.poll";
    public static final String OPENIDM_FILEINSTALL_ENABLED = "openidm.fileinstall.enabled";

    public static final String FELIX_FILEINSTALL_PID = "org.apache.felix.fileinstall";

    public Resource createConfiguration(PID persistentIdentifier, JsonValue configuration)
            throws ResourceException;

    public Resource readConfiguration(PID persistentIdentifier) throws ResourceException;

    public Resource updateConfiguration(PID persistentIdentifier, String revision,
            JsonValue configuration) throws ResourceException;

    public Resource deleteConfiguration(PID persistentIdentifier, String revision)
            throws ResourceException;

    public void queryConfigurations(QueryFilter filter, QueryResultHandler handler)
            throws ResourceException;
}
