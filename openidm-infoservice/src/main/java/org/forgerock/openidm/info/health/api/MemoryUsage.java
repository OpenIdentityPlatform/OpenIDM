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
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.openidm.info.health.api;

import org.forgerock.api.annotations.Default;
import org.forgerock.api.annotations.Description;
import org.forgerock.api.annotations.ReadOnly;

/**
 * Api pojo for {@link MemoryInfoResource}
 *
 * @see java.lang.management.MemoryUsage
 */
public class MemoryUsage {

    private long init;
    private long used;
    private long committed;
    private long max;

    /**
     * Returns the amount of memory in bytes that the Java virtual machine
     * initially requests from the operating system for memory management.
     *
     * @return The initial size of memory in bytes.
     */
    @Description("The initial size of memory in bytes")
    @ReadOnly
    public long getInit() {
        return init;
    }

    /**
     * Returns the amount of used memory in bytes.
     *
     * @return the amount of used memory in bytes.
     */
    @Description("The amount of used memory in bytes")
    @ReadOnly
    public long getUsed() {
        return used;
    }

    /**
     * Returns the amount of memory in bytes that is committed for
     * the Java virtual machine to use.  This amount of memory is
     * guaranteed for the Java virtual machine to use.
     *
     * @return the amount of committed memory in bytes.
     */
    @Description("The amount of committed memory in bytes")
    @ReadOnly
    public long getCommitted() {
        return committed;
    }

    /**
     * Returns the maximum amount of memory in bytes that can be
     * used for memory management.
     * @return The maximum amount of memory in bytes.
     */
    @Description("The maximum amount of memory in bytes")
    @Default("-1")
    @ReadOnly
    public long getMax() {
        return max;
    }
}

