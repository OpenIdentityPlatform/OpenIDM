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
import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import java.lang.reflect.Field;
import java.util.Properties;

import javax.mail.Session;

import com.sun.mail.util.MailSSLSocketFactory;
import org.forgerock.json.JsonValue;
import org.testng.annotations.Test;

/**
 * Tests for the STARTTLS trust settings of {@link EmailClient}.
 */
public class EmailClientTest {

    private static final String SOCKET_FACTORY = "mail.smtp.ssl.socketFactory";

    @Test
    public void startTlsValidatesTheServerCertificateByDefault() throws Exception {
        Properties props = sessionProperties(json(object(
                field("host", "smtp.example.com"),
                field("starttls", object(field("enable", true))))));

        assertThat(props.get("mail.smtp.starttls.enable")).isEqualTo("true");
        assertThat(props.get(SOCKET_FACTORY)).as("no custom trust: JSSE validation applies").isNull();
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
    }

    @Test
    public void withoutStartTlsNoSocketFactoryIsConfigured() throws Exception {
        Properties props = sessionProperties(json(object(field("host", "smtp.example.com"))));

        assertThat(props.get("mail.smtp.starttls.enable")).isNull();
        assertThat(props.get(SOCKET_FACTORY)).isNull();
    }

    private static Properties sessionProperties(JsonValue config) throws Exception {
        EmailClient client = new EmailClient(config);
        Field session = EmailClient.class.getDeclaredField("session");
        session.setAccessible(true);
        return ((Session) session.get(client)).getProperties();
    }
}
