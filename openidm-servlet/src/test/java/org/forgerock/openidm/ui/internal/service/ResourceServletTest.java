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
package org.forgerock.openidm.ui.internal.service;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.mockito.InOrder;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests for the static-resource lookup in {@link ResourceServlet}.
 *
 * <p>The servlet is driven through its real {@link ResourceServlet#doGet} with a mocked
 * request/response pair. {@code defaultDir} / {@code extensionDir} are plain absolute paths
 * (no {@code &{...}} variables), so {@code PropertyUtil.substVars} returns them untouched and
 * no {@code IdentityServer} set-up is needed.
 *
 * <p>The fixture is a real directory tree, because the lookup resolves paths against the
 * live filesystem:
 * <pre>
 *   tmpDir/
 *     secret.txt                ← above the UI root, must never be reachable
 *     ui/
 *       default/
 *         index.html
 *         shared.txt
 *         js/app.js
 *       default-old/            ← sibling sharing the "default" name prefix
 *         leak.txt
 *       extension/
 *         shared.txt            ← overrides default/shared.txt
 *       extension.bak/          ← sibling sharing the "extension" name prefix
 *         leak.txt
 * </pre>
 */
public class ResourceServletTest {

    private static final String INDEX_HTML =
            "<html><head><title>t</title></head><body>index</body></html>";

    /** Captures everything the servlet writes to the response body. */
    private static final class CapturingOutputStream extends ServletOutputStream {
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        @Override
        public void write(int b) {
            buffer.write(b);
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
            // not used
        }

        String asString() {
            return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private Path tmpDir;
    private Path defaultDir;
    private Path extensionDir;

    private ResourceServlet servlet;
    private HttpServletResponse response;
    private CapturingOutputStream body;

    @BeforeMethod
    public void setUp() throws Exception {
        tmpDir = Files.createTempDirectory("openidm-ui-test-");
        Files.writeString(tmpDir.resolve("secret.txt"), "top-secret");

        Path ui = tmpDir.resolve("ui");
        defaultDir = ui.resolve("default");
        extensionDir = ui.resolve("extension");

        Files.createDirectories(defaultDir.resolve("js"));
        Files.writeString(defaultDir.resolve("index.html"), INDEX_HTML);
        Files.writeString(defaultDir.resolve("shared.txt"), "from-default");
        Files.writeString(defaultDir.resolve("js/app.js"), "default-js");

        Files.createDirectories(ui.resolve("default-old"));
        Files.writeString(ui.resolve("default-old/leak.txt"), "LEAKED");

        Files.createDirectories(extensionDir);
        Files.writeString(extensionDir.resolve("shared.txt"), "from-extension");

        Files.createDirectories(ui.resolve("extension.bak"));
        Files.writeString(ui.resolve("extension.bak/leak.txt"), "LEAKED");

        servlet = newServlet(defaultDir.toString(), extensionDir.toString());

        body = new CapturingOutputStream();
        response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(body);
    }

    @AfterMethod
    public void tearDown() {
        deleteRecursively(tmpDir.toFile());
    }

    // -----------------------------------------------------------------------
    // Regular lookups
    // -----------------------------------------------------------------------

    @Test(description = "A file below the default dir is served with its MIME type")
    public void testServesFileFromDefaultDir() throws Exception {
        servlet.doGet(request("/js/app.js"), response);

        assertEquals(body.asString(), "default-js");
        verify(response).setContentType("application/javascript");
        verify(response, never()).sendError(anyInt());
    }

    @Test(description = "A file present in both dirs is taken from the extension dir")
    public void testExtensionDirTakesPrecedence() throws Exception {
        servlet.doGet(request("/shared.txt"), response);

        assertEquals(body.asString(), "from-extension");
        verify(response, never()).sendError(anyInt());
    }

    @Test(description = "index.html is served with the context path injected before </head>")
    public void testIndexHtmlInjectsContextPath() throws Exception {
        servlet.doGet(request("/index.html"), response);

        String html = body.asString();
        assertTrue(html.contains("window.__openidm_context_path="), html);
        assertTrue(html.contains("</script>\n</head>"), html);
        verify(response).setContentType("text/html");
        // set once in doGet() and once more in handleIndexHtml()
        verify(response, atLeastOnce()).setHeader("Cache-Control", "no-cache");
    }

    @Test(description = "The context root '/' is answered with index.html")
    public void testRootPathServesIndexHtml() throws Exception {
        servlet.doGet(request("/"), response);

        assertTrue(body.asString().contains("<body>index</body>"), body.asString());
        verify(response, never()).sendError(anyInt());
    }

    @Test(description = "A request without path info is redirected to the configured context root")
    public void testMissingPathInfoRedirectsToContextRoot() throws Exception {
        injectField(servlet, "contextRoot", "/admin");
        HttpServletRequest request = request(null);
        when(request.getServletPath()).thenReturn("/somewhere-else");

        servlet.doGet(request, response);

        verify(response).sendRedirect("/admin/");
        verify(response, never()).sendError(anyInt());
    }

    @Test(description = "The redirect for the root context root does not double the slash")
    public void testMissingPathInfoRedirectsForRootContext() throws Exception {
        injectField(servlet, "contextRoot", "/");

        servlet.doGet(request(null), response);

        verify(response).sendRedirect("/");
    }

    @Test(description = "An empty path info is redirected like a missing one")
    public void testEmptyPathInfoRedirectsToContextRoot() throws Exception {
        injectField(servlet, "contextRoot", "/admin");

        servlet.doGet(request(""), response);

        verify(response).sendRedirect("/admin/");
        verify(response, never()).sendError(anyInt());
    }

    @Test(description = "A non-existent extension dir is skipped and the default dir is used")
    public void testMissingExtensionDirFallsBackToDefaultDir() throws Exception {
        servlet = newServlet(defaultDir.toString(), tmpDir.resolve("ui/no-such-dir").toString());

        servlet.doGet(request("/js/app.js"), response);

        assertEquals(body.asString(), "default-js");
        verify(response, never()).sendError(anyInt());
    }

    @Test(description = "An unchanged resource is answered with 304 and no body")
    public void testNotModifiedSinceIsHonoured() throws Exception {
        HttpServletRequest request = request("/js/app.js");
        when(request.getDateHeader("If-Modified-Since"))
                .thenReturn(System.currentTimeMillis() + 60_000L);

        servlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_NOT_MODIFIED);
        assertEquals(body.asString(), "");
    }

    @Test(description = "Last-Modified carries the file's modification time in milliseconds")
    public void testLastModifiedIsTheFileTimeInMillis() throws Exception {
        long millis = 1_500_000_000_000L;
        Files.setLastModifiedTime(defaultDir.resolve("js/app.js"), FileTime.fromMillis(millis));

        servlet.doGet(request("/js/app.js"), response);

        verify(response).setDateHeader("Last-Modified", millis);
    }

    @Test(description = "Content-Length is declared from the file size before the body is written")
    public void testContentLengthIsSetBeforeTheBody() throws Exception {
        servlet.doGet(request("/js/app.js"), response);

        InOrder order = inOrder(response);
        order.verify(response).setContentLengthLong("default-js".length());
        order.verify(response).getOutputStream();
        assertEquals(body.asString(), "default-js");
    }

    @Test(description = "A file renamed over after Content-Length is declared still yields a matching body")
    public void testContentLengthAndBodyComeFromTheSameFile() throws Exception {
        Path file = defaultDir.resolve("js/app.js");
        Path replacement = defaultDir.resolve("js/app.js.tmp");
        Files.writeString(replacement, "replaced-js-of-another-size");
        long[] declared = new long[1];
        doAnswer(invocation -> {
            declared[0] = (Long) invocation.getArguments()[0];
            try {
                Files.move(replacement, file, REPLACE_EXISTING, ATOMIC_MOVE);
            } catch (IOException e) {
                throw new SkipException("cannot rename over an open file here: " + e);
            }
            return null;
        }).when(response).setContentLengthLong(anyLong());

        servlet.doGet(request("/js/app.js"), response);

        assertEquals(body.asString(), "default-js");
        assertEquals(declared[0], "default-js".length());
    }

    // -----------------------------------------------------------------------
    // Rejections
    // -----------------------------------------------------------------------

    @Test(description = "A directory is never served")
    public void testDirectoryIsNotServed() throws Exception {
        servlet.doGet(request("/js"), response);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
        assertEquals(body.asString(), "");
    }

    @Test(description = "An unknown file yields 404")
    public void testUnknownFileYields404() throws Exception {
        servlet.doGet(request("/nope.txt"), response);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
        assertEquals(body.asString(), "");
    }

    @Test(description = "A symlink inside the dir that points outside it is rejected")
    public void testSymlinkEscapingTheDirIsRejected() throws Exception {
        createSymbolicLink(defaultDir.resolve("link.txt"), tmpDir.resolve("secret.txt"));

        servlet.doGet(request("/link.txt"), response);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
        assertEquals(body.asString(), "");
    }

    @Test(description = "A configured dir that is itself a symlink still serves its files")
    public void testSymlinkedDirIsServed() throws Exception {
        Path link = tmpDir.resolve("ui/extension-link");
        createSymbolicLink(link, extensionDir);
        servlet = newServlet(defaultDir.toString(), link.toString());

        servlet.doGet(request("/shared.txt"), response);

        assertEquals(body.asString(), "from-extension");
        verify(response, never()).sendError(anyInt());
    }

    @DataProvider(name = "traversalPaths")
    public Object[][] traversalPaths() {
        return new Object[][] {
                { "/../default-old/leak.txt", "sibling of the default dir sharing its name prefix" },
                { "/js/../../default-old/leak.txt", "dot-dot climbing out of a real sub-directory" },
                { "/../extension.bak/leak.txt", "sibling of the extension dir sharing its name prefix" },
                { "/../../secret.txt", "file above the UI root" },
        };
    }

    @Test(dataProvider = "traversalPaths",
            description = "Paths escaping the configured dirs are rejected with 404")
    public void testPathTraversalIsRejected(String target, String scenario) throws Exception {
        servlet.doGet(request(target), response);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
        assertEquals(body.asString(), "", scenario);
    }

    @Test(description = "A path that leaves the configured dir lexically is rejected even if its real path lands back inside")
    public void testLexicalEscapeIsRejectedBeforeSymlinkResolution() throws Exception {
        // extension-link/../extension/shared.txt normalizes to extension/shared.txt, whose real
        // path is inside the link's real target: only the lexical check can reject it
        Path link = tmpDir.resolve("ui/extension-link");
        createSymbolicLink(link, extensionDir);
        servlet = newServlet(defaultDir.toString(), link.toString());

        servlet.doGet(request("/../extension/shared.txt"), response);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
        assertEquals(body.asString(), "");
    }

    @Test(description = "A request path the filesystem cannot parse is a 404, not a 500")
    public void testUnparsablePathIsNotFound() throws Exception {
        servlet.doGet(request("/bad\0name.txt"), response);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
        assertEquals(body.asString(), "");
    }

    @Test(description = "A configured dir that is a regular file serves nothing")
    public void testConfiguredDirThatIsAFileServesNothing() throws Exception {
        servlet = newServlet(defaultDir.toString(), defaultDir.resolve("shared.txt").toString());

        servlet.doGet(request("/."), response);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
        assertEquals(body.asString(), "");
    }

    @Test(description = "An empty configured dir serves nothing, not the working directory")
    public void testEmptyConfiguredDirServesNothing() throws Exception {
        // surefire runs in the module dir, so pom.xml is reachable if "" resolves to the working dir
        assertTrue(Files.isRegularFile(Path.of("pom.xml")), "pom.xml in the working directory");
        servlet = newServlet(defaultDir.toString(), "");

        servlet.doGet(request("/pom.xml"), response);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
        assertEquals(body.asString(), "");
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static ResourceServlet newServlet(String defaultDir, String extensionDir) throws Exception {
        ResourceServlet servlet = new ResourceServlet();
        injectField(servlet, "defaultDir", defaultDir);
        injectField(servlet, "extensionDir", extensionDir);

        ServletContext servletContext = mock(ServletContext.class);
        when(servletContext.getMimeType(anyString())).thenReturn(null);
        ServletConfig servletConfig = mock(ServletConfig.class);
        when(servletConfig.getServletContext()).thenReturn(servletContext);
        servlet.init(servletConfig);
        return servlet;
    }

    private static HttpServletRequest request(String pathInfo) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getServletPath()).thenReturn("/ui");
        when(request.getPathInfo()).thenReturn(pathInfo);
        when(request.getDateHeader("If-Modified-Since")).thenReturn(-1L);
        return request;
    }

    private static void createSymbolicLink(Path link, Path target) throws IOException {
        try {
            Files.createSymbolicLink(link, target);
        } catch (UnsupportedOperationException | SecurityException e) {
            throw new SkipException("symbolic links not supported here: " + e);
        } catch (FileSystemException e) {
            // Windows without SeCreateSymbolicLinkPrivilege reports ERROR_PRIVILEGE_NOT_HELD as a
            // plain FileSystemException; its subclasses (exists, no such file, ...) are real errors
            if (File.separatorChar != '\\' || e.getClass() != FileSystemException.class) {
                throw e;
            }
            throw new SkipException("symbolic links need SeCreateSymbolicLinkPrivilege here: " + e);
        }
    }

    private static void injectField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void deleteRecursively(File file) {
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        file.delete();
    }
}
