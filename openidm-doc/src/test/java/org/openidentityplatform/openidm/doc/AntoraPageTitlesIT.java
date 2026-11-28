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
 * Copyright 2026 3A Systems, LLC.
 */
package org.openidentityplatform.openidm.doc;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.IncludeProcessor;
import org.asciidoctor.extension.PreprocessorReader;
import org.testng.annotations.Test;

public class AntoraPageTitlesIT {
    private static final Options SITE_HEADER = Options.builder()
            .safe(SafeMode.SAFE).attributes(Map.of("env-site", "")).parseHeaderOnly(true).build();
    private static final Options SITE_HEADER_WITHOUT_TITLE_ATTRIBUTE = Options.builder()
            .safe(SafeMode.SAFE).attributes(Map.of("env-site", "", "title!", "")).parseHeaderOnly(true).build();

    @Test
    public void allGeneratedPagesHaveDocumentTitles() throws IOException {
        final Path modules = Path.of(System.getProperty("antoraModulesDirectory"));
        final List<Path> pages;
        try (Stream<Path> paths = Files.walk(modules)) {
            pages = paths.filter(Files::isRegularFile)
                    .map(modules::relativize)
                    .filter(path -> path.getNameCount() >= 3 && path.getName(1).toString().equals("pages"))
                    .filter(path -> path.getFileName().toString().endsWith(".adoc"))
                    .sorted()
                    .toList();
        }
        assertFalse(pages.isEmpty(), "No generated Antora pages in " + modules);

        try (Asciidoctor asciidoctor = Asciidoctor.Factory.create()) {
            asciidoctor.javaExtensionRegistry().includeProcessor(new RootPartialIncludeProcessor(modules));
            final List<Path> untitled = pages.stream()
                    .filter(page -> !hasDocumentTitle(asciidoctor, modules.resolve(page)))
                    .toList();
            assertTrue(untitled.isEmpty(), "Generated Antora pages without a document title: " + untitled);
        }
    }

    private static boolean hasDocumentTitle(Asciidoctor asciidoctor, Path page) {
        // A full parse can fall back to the first section heading. Antora reads page metadata from the header.
        final Document document = asciidoctor.loadFile(page.toFile(), SITE_HEADER);
        final String title = document.getDoctitle();
        if (title == null || title.isBlank()) {
            return false;
        }
        if (document.hasAttribute("title")) {
            // A :title: attribute can override a title, but Antora still needs a document header.
            final String headerTitle = asciidoctor.loadFile(page.toFile(), SITE_HEADER_WITHOUT_TITLE_ATTRIBUTE)
                    .getDoctitle();
            return headerTitle != null && !headerTitle.isBlank();
        }
        return true;
    }

    private static final class RootPartialIncludeProcessor extends IncludeProcessor {
        private static final String PREFIX = "ROOT:partial$";
        private final Path partials;

        private RootPartialIncludeProcessor(Path modules) {
            partials = modules.resolve("ROOT/partials").toAbsolutePath().normalize();
        }

        @Override
        public boolean handles(String target) {
            return target.startsWith(PREFIX);
        }

        @Override
        public void process(Document document, PreprocessorReader reader, String target, Map<String, Object> attributes) {
            final Path partial = partials.resolve(target.substring(PREFIX.length())).normalize();
            if (!partial.startsWith(partials)) {
                throw new IllegalArgumentException("Invalid Antora ROOT partial: " + target);
            }
            final String path = partial.toString();
            try {
                reader.pushInclude(Files.readString(partial), path, path, 1, attributes);
            } catch (IOException e) {
                throw new UncheckedIOException("Cannot read Antora partial " + partial, e);
            }
        }
    }
}
