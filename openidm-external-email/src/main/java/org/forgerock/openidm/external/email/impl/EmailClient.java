/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
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
import java.util.Map;

import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.forgerock.json.fluent.JsonValue;

/**
 *
 * @author gael
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
            try{
                MailSSLSocketFactory sf = new MailSSLSocketFactory();
                sf.setTrustAllHosts(true);
                props.put("mail.smtp.ssl.socketFactory", sf);
            }
            catch (Exception e){
            }
        }

        session = Session.getInstance(props);
    }

    public void send(Map<String, Object> params) throws RuntimeException {
        // _from : the From: address
        // _to : The To: recipients - a comma separated email address strings 
        // _cc: The Cc: recipients - a comma separated email address strings 
        // _bcc: The Bcc: recipients - a comma separated email address strings 
        // _subject: The subject
        // _body : the message body
        InternetAddress from = null;
        InternetAddress[] to = null;
        InternetAddress[] cc = null;
        InternetAddress[] bcc = null;

        String subject = (String) params.get("_subject");
        if (subject == null) {
            subject = "<no subject>";
        }

        try {
            if (params.get("_from") != null) {
                from = new InternetAddress((String) params.get("_from"));
            } else {
                from = new InternetAddress(fromAddr);
            }
        } catch (AddressException ae) {
            throw new RuntimeException("Bad From: header");
        }

        try {
            to = InternetAddress.parse((String) params.get("_to"));
        } catch (AddressException ae) {
            throw new RuntimeException("Bad To: header");
        }

        try {
            if (params.get("_cc") != null) {
                cc = InternetAddress.parse((String) params.get("_cc"));
            }
        } catch (AddressException ae) {
            throw new RuntimeException("Bad Cc: header");
        }

        try {
            if (params.get("_bcc") != null) {
                bcc = InternetAddress.parse((String) params.get("_bcc"));
            }
        } catch (AddressException ae) {
            throw new RuntimeException("Bad Bcc: header");
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

            String type = (String) params.get("_type");
            if (type == null) {
                type = "text/plain";
            }
            Object body = params.get("_body");

            if (type.equalsIgnoreCase("text/plain") || type.equalsIgnoreCase("text/html") || type.equalsIgnoreCase("text/xml")) {
                if (body != null && body instanceof String) {
                    message.setContent(body, type);
                } else {
                    message.setText("<empty message>");
                }
            } else {
                // no idea what this is... let's throw
                throw new RuntimeException("Unknown email type: " + type);
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
            throw new RuntimeException(e);
        }
    }

    public void format() {
    }
}
