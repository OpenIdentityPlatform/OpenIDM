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

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.helper.StringHelpers;
import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Generates the source files from templates using a JSON configuration for the substitution variables. Templates are
 * in Handlebars format.  The filepaths are also processed as Handlebars templates.
 */
public class SourceGenerator {
    private final static SourceGenerator stub = new SourceGenerator();

    private static final List<SourceTemplate> templates = new ArrayList<SourceTemplate>();
    static {
        templates.addAll(Arrays.asList(
                new SourceTemplate("ScriptedConfiguration.template",
                        "{{packageName}}Configuration.groovy",
                        "./src/main/groovy/org/forgerock/openicf/connectors/{{lower packageName}}/"),
                new SourceTemplate("Messages.template",
                        "Messages.properties",
                        "./src/main/resources/org/forgerock/openicf/connectors/{{lower packageName}}/"),
                new SourceTemplate("pom.template", "pom.xml", "./"),
                new SourceTemplate("AuthenticateScript.groovy.template",
                        "AuthenticateScript.groovy",
                        "./src/main/resources/script/{{lower packageName}}/"),
                new SourceTemplate("TestScript.groovy.template",
                        "TestScript.groovy",
                        "./src/main/resources/script/{{lower packageName}}/"),
                new SourceTemplate("CreateScript.groovy.template",
                        "CreateScript.groovy",
                        "./src/main/resources/script/{{lower packageName}}/"),
                new SourceTemplate("DeleteScript.groovy.template",
                        "DeleteScript.groovy",
                        "./src/main/resources/script/{{lower packageName}}/"),
                new SourceTemplate("UpdateScript.groovy.template",
                        "UpdateScript.groovy",
                        "./src/main/resources/script/{{lower packageName}}/"),
                new SourceTemplate("SchemaScript.groovy.template",
                        "SchemaScript.groovy",
                        "./src/main/resources/script/{{lower packageName}}/"),
                new SourceTemplate("SearchScript.groovy.template",
                        "SearchScript.groovy",
                        "./src/main/resources/script/{{lower packageName}}/"),
                new SourceTemplate("SyncScript.groovy.template",
                        "SyncScript.groovy",
                        "./src/main/resources/script/{{lower packageName}}/"),
                new SourceTemplate("provisioner.openicf.json.template",
                        "provisioner.openicf-{{lower packageName}}.json",
                        "./src/main/resources/conf/")));
    }

    /**
     * Generates all of the source files from Handlebars templates.  Variables are substituted with
     * values found in the {@link CustomConfiguration} object.
     *
     * @param config
     * @throws IOException
     */
    public static void generateSources(CustomConfiguration config) throws IOException {
        // Add in the appropriate Connector template
        templates.add(new SourceTemplate(config.getBaseConnectorType().getConnectorTemplate(),
                "{{packageName}}Connector.java",
                "./src/main/java/org/forgerock/openicf/connectors/{{lower packageName}}/"));

        Handlebars hb = new Handlebars();
        StringHelpers.register(hb);
        hb.registerHelper("t", new Helper<String>() {
            @Override
            public CharSequence apply(String context, Options options) throws IOException {
                return "{{t \"" + context + "\"}}";
            }
        });

        for (SourceTemplate template : templates) {
            try {
                Template hbtemplate = hb.compileInline(template.getOutputName());
                String outputFilename = hbtemplate.apply(config);

                hbtemplate = hb.compileInline(template.getOutputPath());
                String outputPath = hbtemplate.apply(config);

                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        stub.getClass().getResourceAsStream("/" + template.getInputName())));
                StringBuilder out = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    out.append(line).append("\n");
                }
                hbtemplate = hb.compileInline(out.toString());
                String contents = hbtemplate.apply(config);

                FileUtils.write(new File(outputPath + outputFilename), contents);
            } catch (Exception e) {
                throw new IOException("Failed to read contents of " + template.getInputName(), e);
            }
        }

        SourceTemplate uiTemplate = new SourceTemplate(config.getBaseConnectorType().getUITemplate(),
                "org.forgerock.openicf.connectors.{{lower packageName}}.{{packageName}}Connector_1.4.html",
                "./src/main/resources/ui/");

        try {
            Template hbtemplate = hb.compileInline(uiTemplate.getOutputName());
            String outputFilename = hbtemplate.apply(config);

            hbtemplate = hb.compileInline(uiTemplate.getOutputPath());
            String outputPath = hbtemplate.apply(config);

            // Process our initial templated section of the output UI template
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    stub.getClass().getResourceAsStream("/UI_base.template")));
            StringBuilder out = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line).append("\n");
            }
            hbtemplate = hb.compileInline(out.toString());
            String contents = hbtemplate.apply(config);

            // Append non-processed content
            reader = new BufferedReader(new InputStreamReader(
                    stub.getClass().getResourceAsStream("/" + uiTemplate.getInputName())));
            out = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                out.append(line).append("\n");
            }
            contents += out.toString();

            FileUtils.write(new File(outputPath + outputFilename), contents);
        } catch (IOException e) {
            throw new IOException("Failed to read contents of " + uiTemplate.getInputName(), e);
        }
    }
}
