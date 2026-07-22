/**
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
*
* Copyright (c) 2012 ForgeRock AS. All Rights Reserved
* Portions Copyrighted 2026 3A Systems, LLC.
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
*
*/

package org.forgerock.openidm.quartz.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.forgerock.util.encode.Base64;
import org.quartz.JobPersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepoJobStoreUtils {

    private static final Logger logger = LoggerFactory.getLogger(RepoJobStoreUtils.class);

    /**
     * Package name prefixes that are considered safe to deserialize from the
     * scheduler repository. The persisted objects are always Quartz
     * jobs/triggers/calendars whose state is composed of Quartz types and a
     * bounded set of JDK types (collections, dates, time zones, numbers and
     * strings) originating from the schedule configuration. A persisted
     * {@code JobDetail} also carries its job implementation class, so the
     * package containing OpenIDM's scheduler jobs is allowed. Restricting
     * deserialization to these types prevents arbitrary gadget-chain classes
     * on the classpath from being instantiated (CWE-502).
     */
    private static final String[] ALLOWED_PREFIXES = {
        "org.quartz.",
        "org.forgerock.openidm.quartz.impl.",
        "java.lang.",
        "java.util.",
        "java.math.",
        "java.time.",
        // concrete TimeZone implementation returned by TimeZone.getTimeZone()
        "sun.util.calendar."
    };

    /**
     * Subpackages of the allowed prefixes that never appear in legitimate
     * scheduler state and are rejected outright to keep the reachable type
     * graph small — notably {@code java.lang.invoke.SerializedLambda}, whose
     * {@code readResolve()} reflectively invokes methods on its capturing
     * class, the {@code java.lang.reflect} proxy/reflection types, and Quartz's
     * utility jobs that can execute commands or invoke remote services from
     * values in a {@code JobDataMap}.
     */
    private static final String[] REJECTED_PREFIXES = {
        "java.lang.invoke.",
        "java.lang.reflect.",
        "org.quartz.jobs."
    };

    /**
     * An {@link ObjectInputFilter} that only permits the Quartz and JDK types
     * that legitimately appear in serialized scheduler state, rejecting any
     * other class before it can be resolved or instantiated.
     */
    private static final ObjectInputFilter SCHEDULER_FILTER = RepoJobStoreUtils::checkInput;

    /**
     * Resource limits applied alongside the type allowlist so that a crafted
     * payload cannot exhaust memory or the stack during deserialization (for
     * example by declaring an oversized array or a deeply nested graph). These
     * bounds are far above any legitimate serialized job/trigger/calendar while
     * still preventing trivial denial of service (JEP 290).
     */
    private static final ObjectInputFilter LIMIT_FILTER = ObjectInputFilter.Config.createFilter(
            "maxbytes=10000000;maxarray=1000000;maxrefs=1000000;maxdepth=100");

    /**
     * The type allowlist combined with the resource limits. Reused across
     * invocations; the per-stream JVM-wide filter (if any) is merged in at
     * deserialization time.
     */
    private static final ObjectInputFilter BASE_FILTER =
            ObjectInputFilter.merge(SCHEDULER_FILTER, LIMIT_FILTER);

    private static ObjectInputFilter.Status checkInput(ObjectInputFilter.FilterInfo info) {
        Class<?> clazz = info.serialClass();
        if (clazz == null) {
            // Not a class check (e.g. array length / reference count limits);
            // leave the decision to any subsequent checks.
            return ObjectInputFilter.Status.UNDECIDED;
        }
        // Unwrap array types down to their base component type.
        while (clazz.isArray()) {
            clazz = clazz.getComponentType();
        }
        if (clazz.isPrimitive()) {
            return ObjectInputFilter.Status.ALLOWED;
        }
        final String name = clazz.getName();
        if (isAllowed(name)) {
            return ObjectInputFilter.Status.ALLOWED;
        }
        logger.warn("Rejected deserialization of class {} from the scheduler repository", name);
        return ObjectInputFilter.Status.REJECTED;
    }

    private static boolean isAllowed(String name) {
        for (String prefix : REJECTED_PREFIXES) {
            if (name.startsWith(prefix)) {
                return false;
            }
        }
        for (String prefix : ALLOWED_PREFIXES) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Converts a serializable object into a String.
     * 
     * @param object the object to serialize.
     * @return a string representation of the serialized object.
     */
    public static String serialize(Serializable object) throws JobPersistenceException {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(object);
            oos.flush();
            oos.close();
            return Base64.encode(baos.toByteArray());
        } catch (Exception e) {
            logger.warn("Failed to serialize scheduler state", e);
            throw new JobPersistenceException(e.getMessage(), e);
        }
    }
    
    /**
     * Converts a String representation of a serialized object back
     * into an object.
     * 
     * @param str the representation of the serialized object
     * @return the deserialized object
     */
    public static Object deserialize(String str) throws JobPersistenceException {
        try {
            byte[] bytes = Base64.decode(str);
            if (bytes == null) {
                bytes = new byte[0];
            }
            try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
                // Restrict deserialization to the expected Quartz/JDK types and
                // bound the resources it may consume, so that a crafted payload
                // cannot trigger a gadget-chain remote code execution (CWE-502)
                // or exhaust memory. Setting a stream filter replaces the
                // JVM-wide jdk.serialFilter for this stream (JEP 415), so any
                // operator-configured filter is merged in rather than dropped.
                final ObjectInputFilter jvmWide = ois.getObjectInputFilter();
                ois.setObjectInputFilter(jvmWide == null
                        ? BASE_FILTER
                        : ObjectInputFilter.merge(BASE_FILTER, jvmWide));
                return ois.readObject();
            }
        } catch (Exception e) {
            logger.warn("Failed to deserialize scheduler state", e);
            throw new JobPersistenceException(e.getMessage(), e);
        }
    }
}
