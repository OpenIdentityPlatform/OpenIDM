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
package org.forgerock.openidm.idp.relyingparty;


/**
 * http://www.simplecloud.info/specs/draft-scim-core-schema-00.html#anchor8
 */
public class SocialUser {

    public static class Name {
        String formatted;
        String familyName;
        String givenName;
        String middleName;
        String honorificPrefix;
        String honorificSuffix;

        Name() { }

        public Name(String familyName, String givenName, String formatted) {
            this.familyName = familyName;
            this.givenName = givenName;
            this.formatted = formatted;
        }

        public String getFormatted() {
            return formatted;
        }

        public String getFamilyName() {
            return familyName;
        }

        public String getGivenName() {
            return givenName;
        }

        public String getMiddleName() {
            return middleName;
        }

        public String getHonorificPrefix() {
            return honorificPrefix;
        }

        public String getHonorificSuffix() {
            return honorificSuffix;
        }
    }

    public static class Email {
        String value;
        String type = "other";
        boolean primary = false;

        public Email(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public String getType() {
            return type;
        }

        public boolean isPrimary() {
            return primary;
        }
    }

    class Address {
        String type;
        String streetAddress;
        String locality;
        String region;
        String postalCode;
        String country;
        String formatted;
        boolean primary;
    }

    class PhoneNumber {
        String value;
        String type;
    }

    class Meta {
        String created;
        String lastModified;
        String version;
        String location;
    }

    final String[] schemas = new String[] { "urn:scim:schemas:core:1.0" };
    String id;
    String externalId;
    String userName;
    Name name;
    String displayName;
    String nickName;
    String profileUrl;
    Email[] emails;
    Address[] addresses;
    PhoneNumber[] phoneNumbers;
    // omitted : im, photo
    String userType;
    String title;
    String preferredLanguage;
    String locale;
    String timezone;
    boolean active;
    String password;
    // omitted : groups, x509Certificates"
    Meta meta;
    Claims[] idpData;
    String subject;

    public SocialUser() {
        name = new Name();
        emails = new Email[0];
        addresses = new Address[0];
        phoneNumbers = new PhoneNumber[0];
        idpData = new Claims[0];
    }

    public SocialUser(String userName, Name name, Email[] emails, Claims[] claims, String subject) {
        this.userName = userName;
        this.name = name;
        this.emails = emails;
        this.idpData = claims;
        this.subject = subject;
    }

    public String getUserName() {
        return userName;
    }

    public Name getName() {
        return name;
    }

    public Email[] getEmails() {
        return emails;
    }

    public Claims[] getIdpData() {
        return idpData;
    }

    public String getSubject() {
        return subject;
    }
}