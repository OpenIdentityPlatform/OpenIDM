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

package org.forgerock.openidm.jetty;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;

/**
 * Custom {@code org.eclipse.jetty.server.handler.ErrorHandler} implementation that removes sensitive information
 * from HTTP error-responses.
 */
public class JettyErrorHandler extends org.eclipse.jetty.server.handler.ErrorHandler {

    /**
     * Handles Jetty errors originating from HTTP requests. Add the following entry to {@code jetty.xml} or directly
     * to an {@code org.eclipse.jetty.server.Server} instance.
     * <p>
     * <pre>
     * &lt;Configure id="Server" class="org.eclipse.jetty.server.Server">
     *   ...
     *   &lt;Call name="addBean">
     *     &lt;Arg>
     *       &lt;New class="org.forgerock.openidm.jetty.JettyErrorHandler"/>
     *     &lt;/Arg>
     *   &lt;/Call>
     *   ...
     * &lt;/Configure>
     * </pre>
     *
     * @param target The target of the request, which is either a URI, name, or {@code null}.
     * @param baseRequest Jetty HTTP request
     * @param request Servlet HTTP request
     * @param response Servlet HTTP response
     * @throws IOException I/O error
     */
    @Override
    public void handle(final String target, final Request baseRequest, final HttpServletRequest request,
            final HttpServletResponse response) throws IOException {
        baseRequest.setHandled(true);
        outputErrorPageResponse(request, response);
    }

    /**
     * Outputs an error page, corresponding to pages stored in the {@code ui/errors/} directory within OpenIDM's root
     * directory. Status codes map to HTML file-names, or to the default HTML page.
     * <p>For example,</p>
     * <ul>
     * <li>ui/errors/404.html</li>
     * <li>ui/errors/default.html</li>
     * </ul>
     *
     * @param request Servlet HTTP request
     * @param response Servlet HTTP response
     * @throws IOException I/O error
     */
    public static void outputErrorPageResponse(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        Integer status = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (status == null) {
            status = 500;
        }

        response.setContentType(MimeTypes.Type.TEXT_HTML_UTF_8.asString());
        response.setHeader(HttpHeader.CACHE_CONTROL.asString(), "must-revalidate,no-cache,no-store");

        // clear any error-reasons from the response
        ((Response) response).setStatusWithReason(status, null);

        Path path = Paths.get(String.format("ui/errors/%1$d.html", status));
        if (!Files.exists(path)) {
            path = Paths.get("ui/errors/default.html");
            if (Files.notExists(path)) {
                // no error page exists, so return a blank page (has HTTP status code)
                response.setContentLength(0);
                return;
            }
        }

        final ServletOutputStream output = response.getOutputStream();
        try (final FileInputStream input = new FileInputStream(path.toFile())) {
            final FileChannel channel = input.getChannel();
            final byte[] buffer = new byte[8 * 1024];
            final ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
            int length;
            while ((length = channel.read(byteBuffer)) != -1) {
                output.write(buffer, 0, length);
                byteBuffer.clear();
            }
        }
        response.flushBuffer();
    }

}
