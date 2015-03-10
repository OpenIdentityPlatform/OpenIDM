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

package org.forgerock.openidm.tools.scriptedbundler;

/**
 * Template bean for an individual source template.
 */
public class SourceTemplate {
    private String inputName;
    private String outputName;
    private String outputPath;

    /**
     * Constructor.
     */
    public SourceTemplate() {}

    /**
     * Constructor with initial data.
     *
     * @param inputName
     * @param outputName
     * @param outputPath
     */
    public SourceTemplate(String inputName, String outputName, String outputPath) {
        this.inputName = inputName;
        this.outputName = outputName;
        this.outputPath = outputPath;
    }

    /**
     * Return the input name.
     *
     * @return
     */
    public String getInputName() {
        return inputName;
    }

    /**
     * Set the input name.
     *
     * @param inputName
     */
    public void setInputName(String inputName) {
        this.inputName = inputName;
    }

    /**
     * Return the output name.
     *
     * @return
     */
    public String getOutputName() {
        return outputName;
    }

    /**
     * Set the output name.
     *
     * @param outputName
     */
    public void setOutputName(String outputName) {
        this.outputName = outputName;
    }

    /**
     * Return the output path.
     *
     * @return
     */
    public String getOutputPath() {
        return outputPath;
    }

    /**
     * Set the output path.
     *
     * @param outputPath
     */
    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }
}
