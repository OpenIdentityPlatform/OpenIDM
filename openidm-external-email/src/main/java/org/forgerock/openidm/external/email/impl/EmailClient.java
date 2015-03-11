/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2014 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.external.email.impl;

import com.sun.mail.util.MailSSLSocketFactory;

import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.BadRequestException;

/**
 * Email client.
 */
public class EmailClient {

    private String host = "localhost";
    private String port = "25";
    private String username = null;
    private String password = null;
    private String fromAddr = null;
    private boolean smtpAuth = false;
    private Properties props = new Properties();
    private Session session;

    public EmailClient(JsonValue config) throws RuntimeException {
        if (config.get("host").asString() != null) {
            host = config.get("host").asString();
            props.put("mail.smtp.host", host);
        }
        if (config.get("port").asString() != null) {
            port = config.get("port").asString();
            props.put("mail.smtp.port", port);
        }
        if (config.get("from").asString() != null) {
            fromAddr = config.get("from").asString();
        }

        if (config.get("mail.smtp.auth").asString().equalsIgnoreCase("true")) {
            if (config.get("username").asString() != null) {
                username = config.get("username").asString();
            } else {
                throw new RuntimeException("No username provided for SMTP auth");
            }
            if (config.get("password").asString() != null) {
                password = config.get("password").asString();
            } else {
                throw new RuntimeException("No password provided for SMTP auth");
            }
            props.put("mail.smtp.auth", "true");
            smtpAuth = true;
        }

        if (config.get("mail.smtp.starttls.enable").asString().equalsIgnoreCase("true")) {
            props.put("mail.smtp.starttls.enable", "true");

            // temporary hack to avoid cert check
            try {
                MailSSLSocketFactory sf = new MailSSLSocketFactory();
                sf.setTrustAllHosts(true);
                props.put("mail.smtp.ssl.socketFactory", sf);
            } catch (Exception e) {
            }
        }

        session = Session.getInstance(props);
    }

    /**
     * Send the email according to the parameters in <em>params</em>:
     *
     * from : the From: address
     * to : The To: recipients - a comma separated email address strings
     * cc: The Cc: recipients - a comma separated email address strings
     * bcc: The Bcc: recipients - a comma separated email address strings
     * subject: The subject
     * body : the message body
     *
     * @param params a JsonValue containing the from, to, cc, bcc, subject, and body parameters
     * @throws BadRequestException
     */
    public void send(JsonValue params) throws BadRequestException {
        InternetAddress from = null;
        InternetAddress[] to = null;
        InternetAddress[] cc = null;
        InternetAddress[] bcc = null;

        String subject = params.get("subject")
                .defaultTo(params.get("_subject"))
                .defaultTo("<no subject>")
                .asString();

        try {
            if (!params.get("from").isNull()) {
                from = new InternetAddress(params.get("from").asString());
            } else if (!params.get("_from").isNull()) {
                from = new InternetAddress(params.get("_from").asString());
            } else if (fromAddr != null) {
                from = new InternetAddress(fromAddr);
            } else { // we don't have a from, need to throw
                throw new BadRequestException("From: email address is absent");
            }
        } catch (AddressException ae) {
            throw new BadRequestException("Bad From: email address");
        }

        try {
            to = InternetAddress.parse(params.get("to").defaultTo(params.get("_to")).asString());
        } catch (AddressException ae) {
            throw new BadRequestException("Bad To: email address");
        }

        try {
            if (!params.get("cc").isNull()) {
                cc = InternetAddress.parse(params.get("cc").asString());
            } else if (!params.get("cc").isNull()) {
                cc = InternetAddress.parse(params.get("_cc").asString());
            }
        } catch (AddressException ae) {
            throw new BadRequestException("Bad Cc: email address");
        }

        try {
            if (!params.get("bcc").isNull()) {
                bcc = InternetAddress.parse(params.get("bcc").asString());
            } else if (!params.get("_bcc").isNull()) {
                bcc = InternetAddress.parse(params.get("_bcc").asString());
            }
        } catch (AddressException ae) {
            throw new BadRequestException("Bad Bcc: email address");
        }

        try {
            Message message = new MimeMessage(session);
            message.setFrom(from);
            message.setRecipients(Message.RecipientType.TO, to);
            if (cc != null) {
                message.setRecipients(Message.RecipientType.CC, cc);
            }
            if (bcc != null) {
                message.setRecipients(Message.RecipientType.BCC, bcc);
            }
            message.setSubject(subject);

            String type = params.get("type")
                    .defaultTo(params.get("_type"))
                    .defaultTo("text/plain")
                    .asString();
            Object body = params.get("body")
                    .defaultTo(params.get("_body"))
                    .getObject();

            if (type.equalsIgnoreCase("text/plain") || type.equalsIgnoreCase("text/html") || type.equalsIgnoreCase("text/xml")) {
                if (body != null && body instanceof String) {
                    message.setContent(body, type);
                } else {
                    message.setText("<empty message>");
                }
            } else {
                // no idea what this is... let's throw
                throw new BadRequestException("Email type: " + type + " is not handled");
            }

            Transport transport = session.getTransport("smtp");

            if (smtpAuth) {
                transport.connect(username, password);
            } else {
                transport.connect();
            }
            message.saveChanges();
            transport.sendMessage(message, message.getAllRecipients());
            transport.close();

        } catch (MessagingException e) {
            throw new BadRequestException(e);
        }
    }

    public void format() {
    }
}
