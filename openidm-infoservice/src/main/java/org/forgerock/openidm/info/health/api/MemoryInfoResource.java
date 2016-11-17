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

import org.forgerock.api.annotations.Description;
import org.forgerock.api.annotations.ReadOnly;

/**
 * Api POJO for {@link org.forgerock.openidm.info.health.MemoryInfoResourceProvider}.
 */
public class MemoryInfoResource {
    private int objectPendingFinalization;
    private HeapMemoryUsage heapMemoryUsage;
    private NonHeapMemoryUsage nonHeapMemoryUsage;

    @Description("Count of objects pending finalization")
    @ReadOnly
    public int getObjectPendingFinalization() {
        return objectPendingFinalization;
    }

    @Description("Stats of heap memory usage of this single node")
    @ReadOnly
    public HeapMemoryUsage getHeapMemoryUsage() {
        return heapMemoryUsage;
    }

    @Description("Stats of non-heap memory usage of this single node")
    @ReadOnly
    public NonHeapMemoryUsage getNonHeapMemoryUsage() {
        return nonHeapMemoryUsage;
    }

    private static class HeapMemoryUsage extends MemoryUsage {
    }

    private static class NonHeapMemoryUsage extends MemoryUsage {
    }
}
