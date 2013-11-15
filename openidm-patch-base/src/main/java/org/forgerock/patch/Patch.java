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
package org.forgerock.patch;

import java.io.File;
import java.net.URL;
import java.util.Map;

import org.forgerock.patch.exception.PatchException;
import org.forgerock.patch.exception.PostPatchException;
import org.forgerock.patch.exception.PrePatchException;

/**
 *
 * @author cgdrake
 */
public interface Patch {

    public void initialize(URL patchUrl, String originalUrlString, File workingDir,
            File installDir, Map<String, Object> params);

    public void apply() throws PatchException, PrePatchException, PostPatchException;

    public void abort() throws PatchException;
    
    public void rollback() throws PatchException;

    public void cleanup() throws PatchException;
}
