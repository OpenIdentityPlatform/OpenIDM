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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openidm.patch.utils;

/**
 * Constants use by the base patch framework.
 */
public final class PatchConstants {

    private PatchConstants() {
        //donothing
    };

    /**
     * Location of the patch.properties file within the patch bundle.
     */
    public static final String CONFIG_PROPERTIES_FILE = "/org/forgerock/patch/impl/patch.properties";

    /**
     * Property which specifies the Patch Implementation class file.
     */
    public static final String CONFIG_PATCH_IMPL_CLASS = "org.forgerock.patch.impl";

    /**
     * Property which specifies the software release associate with the patch to be applied.
     */
    public static final String PATCH_RELEASE = "org.forgerock.patch.release";

    /**
     * Property which specifies the minimum require target release in order to be able to
     * apply the patch bundle to the target.
     */
    public static final String MIN_TARGET_RELEASE = "org.forgerock.patch.minimum.target.release";

    /**
     * Property whcih specifies the description of the patch being applied.
     */
    public static final String PATCH_DESCRIPTION = "org.forgerock.patch.description";

    /**
     * Property which specifies the name of the patch archive bundle to be created.
     */
    public static final String PATCH_BACKUP_ARCHIVE = "org.forgerock.patch.backup.archive";
}
