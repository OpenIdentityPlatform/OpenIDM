/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openidm.sync.impl;

/**
 * A bean like wrapper for {@code propertyMappings} json object entries in the
 * {@code objectSynchronization} {@mappings} configuration.
 */
public class PropertyEntry {

    private String sourcePath;
    private String targetPath;
    private String script;

    /**
     * Get the sourcePath identifier for this entry.
     *
     * @return sourceObject
     */
    public String getSourcePath() {
        return sourcePath;
    }

    /**
     * Set the sourcePath identifier for this entry.
     *
     * @param sourcePath
     */
    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    /**
     * Get the targetPath identifier for this entry.
     *
     * @return targetObject
     */
    public String getTargetPath() {
        return targetPath;
    }

    /**
     * Set the targetPath identifier for this entry.
     *
     * @param targetPath
     */
    public void setTargetPath(String targetPath) {
        this.targetPath = targetPath;
    }

    /**
     * Get the script identifier for this entry.
     *
     * @return
     */
    public String getScript() {
        return script;
    }

    /**
     * Set the script identifier for this entry.
     *
     * @param script
     */
    public void setScript(String script) {
        this.script = script;
    }

    /**
     * Debugging toString.
     *
     * @return nested toString values
     */
    @Override
    public String toString() {
        return "PropertyEntry{" +
                "sourcePath='" + sourcePath + '\'' +
                ", targetPath='" + targetPath + '\'' +
                ", script='" + script + '\'' +
                '}';

    }

}
