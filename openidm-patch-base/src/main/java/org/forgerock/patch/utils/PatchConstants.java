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

package org.forgerock.patch.utils;

/**
 *
 * @author cgdrake
 */
public final class PatchConstants {
    
    private PatchConstants() {};
    
    public static final String CONFIG_PROPERTIES_FILE = "/org/forgerock/patch/impl/patch.properties";
    public static final String CONFIG_PATCH_IMPL_CLASS = "org.forgerock.patch.impl";

    public static final String PATCH_RELEASE = "org.forgerock.patch.release";
    public static final String MIN_TARGET_RELEASE = "org.forgerock.patch.minimum.target.release";
    public static final String PATCH_DESCRIPTION = "org.forgerock.patch.description";
    
    public static final String PATCH_BACKUP_ARCHIVE = "org.forgerock.patch.backup.archive";
}
