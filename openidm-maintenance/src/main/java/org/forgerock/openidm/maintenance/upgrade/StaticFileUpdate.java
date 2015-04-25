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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Updates / replaces / adds a new file in the distribution.
 */
public class StaticFileUpdate {

    private static final String OLD_SUFFIX = ".idm-old";
    private static final String NEW_SUFFIX = ".idm-new";

    private final Path path;
    private final FileState fileState;
    private final Archive archive;

    StaticFileUpdate(Path path,  FileState fileState,  Archive archive) {
        this.path = path;
        this.fileState = fileState;
        this.archive = archive;
    }

    /**
     * @return whether the static file exists
     */
    boolean exists() {
        return Files.exists(path);
    }

    /**
     * @return whether the static file has been changed
     */
    boolean isChanged() throws IOException {
        return exists()
                && FileState.State.DIFFERS.equals(fileState.getCurrentFileState(path));
    }

    /**
     * Replaces this static file with the new one from the archive.  If the file has been changed, copy it to
     * <em>&lt;filepath&gt;-.idm-old</em>.
     *
     * @throws IOException
     */
    void replace() throws IOException {
        if (isChanged()) {
            Files.move(path, Paths.get(path.toString() + OLD_SUFFIX));
        }
        Files.copy(archive.getInputStream(path), path);
    }

    /**
     * Keep the static file that already exists.  Install the new file as <em>&lt;filepath&gt;-.idm-new</em>.
     *
     * @throws IOException
     */
    void keep() throws IOException {
        if (isChanged()) {
            Files.copy(archive.getInputStream(path), Paths.get(path.toString() + NEW_SUFFIX));
        }
        throw new IOException("The file " + path.toString() + " does not exist - cannot \"keep\" it");
    }
}
