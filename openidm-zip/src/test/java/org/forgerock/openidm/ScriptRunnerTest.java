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
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.openidm;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.commonjs.module.RequireBuilder;
import org.mozilla.javascript.commonjs.module.provider.DefaultUrlConnectionExpiryCalculator;
import org.mozilla.javascript.commonjs.module.provider.ModuleSource;
import org.mozilla.javascript.commonjs.module.provider.SoftCachingModuleScriptProvider;
import org.mozilla.javascript.commonjs.module.provider.UrlModuleSourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Tests runtime scripts (Javascript-only).
 */
public class ScriptRunnerTest  {
    private static final Logger logger = LoggerFactory.getLogger(ScriptRunnerTest.class);

    /**
     * Array of directories where script modules are located.
     * Must start and end with /
     *
     * Add additional paths for the location of modules to test, or the test modules themselves.
     */
    private static final String[] moduleContainers = new String[] {
        "/",
        "/bin/defaults/script/",
        "/scriptLibs/"
    };

    /**
     * Custom ModuleSourceProvider that overrides URLModuleSourceProvider to resolve
     * script modules using the classpath.
     */
    private class ClasspathModuleSourceProvider extends UrlModuleSourceProvider {
        static final long serialVersionUID = 1L;

        public ClasspathModuleSourceProvider() {
            super(null, null, new DefaultUrlConnectionExpiryCalculator(0), null);
        }

        @Override
        protected ModuleSource loadFromPrivilegedLocations(String moduleId, Object validator)
                throws IOException, URISyntaxException {
            return loadFromPathList(moduleId, validator);
        }

        @Override
        protected ModuleSource loadFromFallbackLocations(String moduleId, Object validator)
                throws IOException, URISyntaxException {
            return loadFromPathList(moduleId, validator);
        }

        private ModuleSource loadFromPathList(String moduleId, Object validator)
                throws IOException, URISyntaxException {
            for (String container : moduleContainers) {
                // we have to add the .js to get class to resolve the module
                URL path = getClass().getResource(container + moduleId + ".js");
                if (path != null) {
                    final ModuleSource moduleSource = loadFromActualUri(path.toURI(), path.toURI(), validator);
                    if (moduleSource != null) {
                        return moduleSource;
                    }
                }
            }
            return null;
        }
    }

    private final RequireBuilder requireBuilder;

    ScriptRunnerTest() {
        requireBuilder = new RequireBuilder();
        requireBuilder.setModuleScriptProvider(new SoftCachingModuleScriptProvider(new ClasspathModuleSourceProvider()));
    }

    @Test
    public void testScripts() throws Exception {
        // always run the JS testRunner
        final InputStream stream = ScriptRunnerTest.class.getResourceAsStream("/testRunner.js");
        Context context = Context.enter();
        try {
            Scriptable scope = context.initStandardObjects();
            requireBuilder.createRequire(context, scope).install(scope);
            context.evaluateReader(scope, new InputStreamReader(stream), "testRunner", 1, null);
        } catch (JavaScriptException e) {
            String message = e.sourceName() + ":" + e.lineNumber();
            if (e.getValue() instanceof NativeObject) {
                message += ": " + ((NativeObject) e.getValue()).get("message").toString();
            }
            throw new Exception(message);
        } finally {
            Context.exit();
        }
    }
}
