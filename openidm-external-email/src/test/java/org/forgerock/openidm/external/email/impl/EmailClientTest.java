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
package org.forgerock.openidm.external.email.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.mail.Session;
import javax.net.ssl.SSLContext;

import com.sun.mail.util.MailSSLSocketFactory;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.testng.annotations.Test;

/**
 * Tests for the STARTTLS settings of {@link EmailClient}.
 */
public class EmailClientTest {

    private static final String SOCKET_FACTORY = "mail.smtp.ssl.socketFactory";
    private static final String CHECK_SERVER_IDENTITY = "mail.smtp.ssl.checkserveridentity";
    private static final String STARTTLS_REQUIRED = "mail.smtp.starttls.required";

    @Test
    public void startTlsValidatesTheServerCertificateByDefault() throws Exception {
        Properties props = sessionProperties(json(object(
                field("host", "smtp.example.com"),
                field("starttls", object(field("enable", true))))));

        assertThat(props.get("mail.smtp.starttls.enable")).isEqualTo("true");
        assertThat(props.get(SOCKET_FACTORY)).as("no custom trust: JSSE validation applies").isNull();
        assertThat(props.get(CHECK_SERVER_IDENTITY)).isEqualTo("true");
    }

    @Test
    public void startTlsTrustAllIsOptIn() throws Exception {
        Properties props = sessionProperties(json(object(
                field("host", "smtp.example.com"),
                field("starttls", object(field("enable", true), field("trustAll", true))))));

        MailSSLSocketFactory sf = (MailSSLSocketFactory) props.get(SOCKET_FACTORY);
        assertThat(sf).isNotNull();
        assertThat(sf.isTrustAllHosts()).isTrue();
    }

    @Test
    public void startTlsTrustedHostsAreLimitedToTheConfiguredList() throws Exception {
        Properties props = sessionProperties(json(object(
                field("host", "smtp.example.com"),
                field("starttls", object(field("enable", true),
                        field("trustedHosts", array("smtp.example.com", "mail.internal")))))));

        MailSSLSocketFactory sf = (MailSSLSocketFactory) props.get(SOCKET_FACTORY);
        assertThat(sf).isNotNull();
        assertThat(sf.isTrustAllHosts()).isFalse();
        assertThat(sf.getTrustedHosts()).containsExactly("smtp.example.com", "mail.internal");
        assertThat(props.get(CHECK_SERVER_IDENTITY)).isNull();
    }

    @Test
    public void startTlsTrustAllWinsOverTrustedHosts() throws Exception {
        Properties props = sessionProperties(json(object(
                field("host", "smtp.example.com"),
                field("starttls", object(field("enable", true), field("trustAll", true),
                        field("trustedHosts", array("mail.internal")))))));

        MailSSLSocketFactory sf = (MailSSLSocketFactory) props.get(SOCKET_FACTORY);
        assertThat(sf.isTrustAllHosts()).isTrue();
    }

    @Test
    public void startTlsUsesTheJvmDefaultProtocols() throws Exception {
        Properties props = sessionProperties(json(object(
                field("host", "smtp.example.com"),
                field("starttls", object(field("enable", true))))));

        // JavaMail 1.4.7 falls back to TLSv1 alone, which current JDKs disable
        assertThat(props.getProperty("mail.smtp.ssl.protocols").split(" "))
                .containsExactly(SSLContext.getDefault().getDefaultSSLParameters().getProtocols());
    }

    @Test
    public void socketFactoryComesFromTheJavaMailInUse() {
        // JavaMail checks trustedHosts only for its own MailSSLSocketFactory class; a copy from
        // another mail jar (jakarta.mail) is not recognised and then trusts every host
        assertThat(MailSSLSocketFactory.class.getProtectionDomain().getCodeSource().getLocation())
                .isEqualTo(Session.class.getProtectionDomain().getCodeSource().getLocation());
    }

    @Test
    public void startTlsIsOpportunisticByDefault() throws Exception {
        Properties props = sessionProperties(json(object(
                field("host", "smtp.example.com"),
                field("starttls", object(field("enable", true))))));

        assertThat(props.get(STARTTLS_REQUIRED)).isEqualTo("false");
    }

    @Test
    public void startTlsRequiredRejectsServersWithoutStartTls() throws Exception {
        Properties props = sessionProperties(json(object(
                field("host", "smtp.example.com"),
                field("starttls", object(field("enable", true), field("required", true))))));

        assertThat(props.get(STARTTLS_REQUIRED)).isEqualTo("true");
        assertThat(props.get(CHECK_SERVER_IDENTITY)).isEqualTo("true");
    }

    @Test
    public void startTlsRequiredImpliesStartTls() throws Exception {
        Properties props = sessionProperties(json(object(
                field("host", "smtp.example.com"),
                field("starttls", object(field("enable", false), field("required", true))))));

        // JavaMail issues STARTTLS for required alone, so the trust settings must apply as well
        assertThat(props.get("mail.smtp.starttls.enable")).isEqualTo("true");
        assertThat(props.get(STARTTLS_REQUIRED)).isEqualTo("true");
        assertThat(props.get(CHECK_SERVER_IDENTITY)).isEqualTo("true");
    }

    @Test
    public void startTlsRequiredStopsBeforeMailWhenTheServerDoesNotOfferIt() throws Exception {
        try (StartTlsStrippingServer server = new StartTlsStrippingServer()) {
            EmailClient client = new EmailClient(server.config(true));
            try {
                client.send(message());
                fail("sent over a connection without STARTTLS");
            } catch (BadRequestException e) {
                assertThat(e.getCause()).hasMessageContaining("STARTTLS is required");
            }
            assertThat(server.commands()).as("connected and read the EHLO reply").anyMatch(c -> c.startsWith("EHLO"));
            assertThat(server.commands()).noneMatch(c -> c.startsWith("MAIL FROM"));
        }
    }

    @Test
    public void startTlsWithoutRequiredSendsInClearWhenTheServerDoesNotOfferIt() throws Exception {
        try (StartTlsStrippingServer server = new StartTlsStrippingServer()) {
            new EmailClient(server.config(false)).send(message());

            assertThat(server.commands()).anyMatch(c -> c.startsWith("MAIL FROM"));
        }
    }

    @Test
    public void trustSettingsApplyOnlyWithStartTls() throws Exception {
        Properties props = sessionProperties(json(object(
                field("host", "smtp.example.com"),
                field("starttls", object(field("enable", false), field("trustAll", true))))));

        assertThat(props.get("mail.smtp.starttls.enable")).isNull();
        assertThat(props.get(SOCKET_FACTORY)).isNull();
        assertThat(props.get(CHECK_SERVER_IDENTITY)).isNull();
    }

    private static JsonValue message() {
        return json(object(
                field("from", "idm@example.com"),
                field("to", "user@example.com"),
                field("subject", "test"),
                field("body", "test")));
    }

    /**
     * An SMTP server whose EHLO reply does not offer STARTTLS, as seen by a client whose
     * connection is tampered with on path; it accepts every message in clear.
     */
    private static final class StartTlsStrippingServer implements AutoCloseable {

        private final ServerSocket serverSocket;
        private final List<String> commands = new CopyOnWriteArrayList<>();
        private final Thread thread;

        StartTlsStrippingServer() throws IOException {
            serverSocket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
            thread = new Thread(this::serve, "fake-smtp");
            thread.setDaemon(true);
            thread.start();
        }

        JsonValue config(boolean required) {
            return json(object(
                    field("host", serverSocket.getInetAddress().getHostAddress()),
                    field("port", String.valueOf(serverSocket.getLocalPort())),
                    field("starttls", object(field("enable", true), field("required", required)))));
        }

        List<String> commands() throws InterruptedException {
            thread.join(10_000);
            return commands;
        }

        private void serve() {
            try (Socket socket = serverSocket.accept();
                 BufferedReader in = new BufferedReader(
                         new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
                 Writer out = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.US_ASCII)) {
                reply(out, "220 localhost ESMTP");
                String line;
                while ((line = in.readLine()) != null) {
                    commands.add(line);
                    if (line.startsWith("EHLO")) {
                        reply(out, "250-localhost\r\n250 8BITMIME");
                    } else if (line.equals("DATA")) {
                        reply(out, "354 end with <CRLF>.<CRLF>");
                        while ((line = in.readLine()) != null && !line.equals(".")) {
                            // message content
                        }
                        reply(out, "250 OK");
                    } else if (line.equals("QUIT")) {
                        reply(out, "221 bye");
                        return;
                    } else {
                        reply(out, "250 OK");
                    }
                }
            } catch (IOException e) {
                // the client closed the connection
            }
        }

        private static void reply(Writer out, String reply) throws IOException {
            out.write(reply + "\r\n");
            out.flush();
        }

        @Override
        public void close() throws IOException {
            serverSocket.close();
        }
    }

    private static Properties sessionProperties(JsonValue config) throws Exception {
        EmailClient client = new EmailClient(config);
        Field session = EmailClient.class.getDeclaredField("session");
        session.setAccessible(true);
        return ((Session) session.get(client)).getProperties();
    }
}
