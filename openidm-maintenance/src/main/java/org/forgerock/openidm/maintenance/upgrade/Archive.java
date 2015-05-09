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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Set;

import org.forgerock.util.Function;

/**
 * Abstraction of an upgrade archive
 *
 * TODO Maybe this can be coalesced with the {@link org.forgerock.openidm.patch.Archive}
 */
public interface Archive {
    /**
     * Return the version of the archive.
     *
     * @return the product version in the archive
     */
    ProductVersion getVersion();

    /**
     * Return the set of files in the archive.
     *
     * @return a set of Path object corresponding to the relative file paths in the archive
     */
    Set<Path> getFiles();

    /**
     * Get an InputStream for a path within the archive and process it according to the provided Function.
     *
     * @param path The Path for which to retrieve an InputStream
     * @param function the function to apply to the InputStream
     * @return the return value from the function
     */
    <R, E extends Exception> R withInputStreamForPath(Path path, Function<InputStream, R, E> function)
            throws E, IOException;
}
