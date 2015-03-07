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
package org.forgerock.openidm.patch;

import java.io.File;
import java.net.URL;
import java.util.Map;

import org.forgerock.openidm.patch.exception.PatchException;
import org.forgerock.openidm.patch.exception.PostPatchException;
import org.forgerock.openidm.patch.exception.PrePatchException;

/**
 * Basic patch interface.
 */
public interface Patch {

    /**
     * Initializes the patch bundle.
     *
     * @param patchUrl          A URL specifying the location of the patch bundle
     * @param originalUrlString The String representation of the patch bundle location
     * @param workingDir        The working directory in which store logs and temporary files
     * @param installDir        The target directory against which the patch is to be applied
     * @param params            Additional patch specific parameters
     */
    public void initialize(URL patchUrl, String originalUrlString, File workingDir,
            File installDir, Map<String, Object> params);

    /**
     * Applies the patch bundle to the target installation directory.
     *
     * @throws PatchException       Thrown during application of the patch if a error occurs.
     * @throws PrePatchException    Thrown during the pre-patch phase if a error occurs.
     * @throws PostPatchException   Thrown during the post-patch phase if a error occurs.
     */
    public void apply() throws PatchException, PrePatchException, PostPatchException;

    /**
     * Abort the application of the patch bundle to the target installation directory.
     *
     * @throws PatchException   Thrown if an error occurs while attempting to Abort
     */
    public void abort() throws PatchException;

    /**
     * Rollback the application of the patch bundle to the target installation directory.
     *
     * @throws PatchException   Thrown if an error occurs while attempt to Rollback
     */
    public void rollback() throws PatchException;

    /**
     * Cleanup after the application of the patch bundle to the target installation directory.
     *
     * @throws PatchException   Thrown if an error occurs while attempt to Cleanup
     */
    public void cleanup() throws PatchException;
}
