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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Set;

import org.forgerock.util.Function;

/**
 * Abstraction of an upgrade archive
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
