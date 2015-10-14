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
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.openidm.maintenance.upgrade;

/**
 * Wrapper around the product version tag (3.2.0-SNAPSHOT) and the SCM revision.
 */
public class ProductVersion {
    private final ComparableVersion version;
    private final String revision;

    /**
     * Construct a ProductVersion
     *
     * @param version the major version string
     * @param revision the minor version string
     */
    ProductVersion(String version, String revision) {
        this.version = new ComparableVersion(version);
        this.revision = revision;
    }

    /**
     * Determine whether two ProductVersions match
     *
     * @param other the ProductVersion to compare to this
     * @return true iff the major and minor strings match
     */
    public boolean isSameAs(ProductVersion other) {
        return version.compareTo(other.version) == 0
                && revision.equals(other.revision);
    }

    @Override
    public String toString() {
        return version.toString() + "-" + revision;
    }
}
