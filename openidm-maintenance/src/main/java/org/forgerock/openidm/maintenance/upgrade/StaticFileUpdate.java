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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.EnumSet;
import java.util.Set;

import org.forgerock.util.Function;

/**
 * Updates / replaces / adds a new static file in the distribution.
 */
class StaticFileUpdate {

    static final String IDM_SUFFIX = ".idm-";

    private final FileStateChecker fileStateChecker;
    private final Path root;
    private final Archive archive;
    private final ProductVersion currentVersion;
    private final ProductVersion upgradedVersion;

    StaticFileUpdate(final FileStateChecker fileStateChecker, final Path openidmRoot, final Archive archive,
            final ProductVersion currentVersion) {
        this.fileStateChecker = fileStateChecker;
        this.root = openidmRoot;
        this.archive = archive;
        this.currentVersion = currentVersion;
        this.upgradedVersion = archive.getVersion();
    }

    /**
     * Table of what to do under what FileState circumstances.
     *
     *                     replace                 keep
     *                       old    / new       old / new
     *
     * UNEXPECTED     rename-as-old / copy          / rename-as-new
     * NONEXISTENT                    copy          / copy
     * DELETED                        copy          / copy
     * DIFFERS        rename-as-old / copy          / rename-as-new
     * UNCHANGED                      copy          / copy
     */

    private static final Set<FileState> CHANGED_STATES = EnumSet.of(FileState.UNEXPECTED, FileState.DIFFERS);

    /**
     * Replaces this static file with the new one from the archive.  If the file has been changed, copy it to
     * <em>&lt;filepath&gt;-.idm-old</em>.  Supports copying fresh file for one that is missing.
     *
     * @param path the path to replace/copy
     * @throws IOException
     */
    void replace(final Path path) throws IOException {
        if (CHANGED_STATES.contains(fileStateChecker.getCurrentFileState(path))) {
            Files.move(root.resolve(path),
                    root.resolve(path.toString() + IDM_SUFFIX + currentVersion.toString()),
                    StandardCopyOption.REPLACE_EXISTING);
        }
        archive.withInputStreamForPath(path, new Function<InputStream, Void, IOException>() {
            @Override
            public Void apply(InputStream inputStream) throws IOException {
                Files.copy(inputStream, root.resolve(path), StandardCopyOption.REPLACE_EXISTING);
                return null;
            }
        });
    }

    /**
     * Keep the static file that already exists.  Install the new file as <em>&lt;filepath&gt;-.idm-new</em>.
     *
     * @param path the path to keep/copy
     * @throws IOException
     */
    void keep(final Path path) throws IOException {
        if (CHANGED_STATES.contains(fileStateChecker.getCurrentFileState(path))) {
            archive.withInputStreamForPath(path, new Function<InputStream, Void, IOException>() {
                @Override
                public Void apply(InputStream inputStream) throws IOException {
                    Files.copy(inputStream,
                            root.resolve(path.toString() + IDM_SUFFIX + upgradedVersion.toString()),
                            StandardCopyOption.REPLACE_EXISTING);
                    return null;
                }
            });
        } else {
            throw new IOException("No such file " + path.toString() + " to keep!");
        }
    }
}
