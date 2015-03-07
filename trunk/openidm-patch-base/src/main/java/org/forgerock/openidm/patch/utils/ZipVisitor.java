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

import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Zip file entry visitor.
 */
public interface ZipVisitor {

    /**
     * Visit ZipEntry within the ZipInputStream.
     *
     * @param zipEntry The ZipEntry to visit
     * @param zipInputStream The ZipInputStream being read
     *
     * @return boolean true if the ZipEntry was found.  False otherwise.
     * @throws IOException if operating on the zip file fails
     */
    boolean visitEntry(ZipEntry zipEntry, ZipInputStream zipInputStream)
            throws IOException;
}
