/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.maintenance.upgrade;

/**
 * Wrapper around the product version tag (3.2.0-SNAPSHOT) and the SCM revision.
 */
public class ProductVersion {
    private final ComparableVersion version;
    private final String revision;

    ProductVersion(String version, String revision) {
        this.version = new ComparableVersion(version);
        this.revision = revision;
    }

    public boolean isSameAs(ProductVersion other) {
        return version.compareTo(other.version) == 0
                && revision.equals(other.revision);
    }

    @Override
    public String toString() {
        return version.toString() + "-" + revision;
    }
}
