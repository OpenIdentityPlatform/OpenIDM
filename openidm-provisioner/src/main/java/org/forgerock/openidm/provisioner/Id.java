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
 *
 * $Id$
 */
package org.forgerock.openidm.provisioner;

import org.apache.commons.lang.StringUtils;
import org.forgerock.openidm.objset.ObjectSetException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * Id is a util class to work with the {@code id} property in the {@link org.forgerock.openidm.objset.ObjectSet}
 * interface.
 * <p/>
 * A valid ID MAY start with {@code system} and followed by the name of the end system, type of the
 * object. The last token is always the local identifier of the object instance.
 * <p/>
 * Valid identifiers:
 * {@code
 * system/LDAP/account
 * LDAP/account
 * system/LDAP/account/
 * LDAP/account/
 * system/LDAP/account/ead738c0-7641-11e0-a1f0-0800200c9a66
 * LDAP/account/ead738c0-7641-11e0-a1f0-0800200c9a66
 * system/Another+OpenIDM/account/http%3a%2f%2fopenidm%2fopenidm%2fmanaged%2fuser%2fead738c0-7641-11e0-a1f0-0800200c9a66
 * Another+OpenIDM/account/http%3a%2f%2fopenidm%2fopenidm%2fmanaged%2fuser%2fead738c0-7641-11e0-a1f0-0800200c9a66
 * }
 * Invalid identifiers:
 * {@code
 * /system/LDAP
 * system/account
 * }
 *
 * @author $author$
 * @version $Revision$ $Date$
 */
public class Id {

    private final static Logger TRACE = LoggerFactory.getLogger(Id.class);
    public static final String SYSTEM_BASE = "system/";
    public static final String CHARACTER_ENCODING_UTF_8 = "UTF-8";
    private URI baseURI;
    private String systemName;
    private String objectType;
    private String localId = null;

    public Id(String systemName, String objectType) throws ObjectSetException {
        if (StringUtils.isBlank(systemName)) {
            throw new ObjectSetException("System name can not be blank");
        }
        if (StringUtils.isBlank(objectType)) {
            throw new ObjectSetException("Object type can not be blank");
        }
        try {
            this.baseURI = new URI("");
            this.systemName = URLDecoder.decode(systemName, CHARACTER_ENCODING_UTF_8);
            this.objectType = URLDecoder.decode(objectType, CHARACTER_ENCODING_UTF_8);
        } catch (URISyntaxException e) {
            // Should never happen.
            throw new UndeclaredThrowableException(e);
        } catch (UnsupportedEncodingException e) {
            // Should never happen.
            throw new UndeclaredThrowableException(e);
        }

    }

    public Id(String systemName, String objectType, String localId) throws ObjectSetException {
        if (StringUtils.isBlank(systemName)) {
            throw new ObjectSetException("System name can not be blank");
        }
        if (StringUtils.isBlank(objectType)) {
            throw new ObjectSetException("Object type can not be blank");
        }
        if (StringUtils.isBlank(localId)) {
            throw new ObjectSetException("Object id can not be blank");
        }
        try {
            this.baseURI = new URI("");
            this.systemName = URLDecoder.decode(systemName, CHARACTER_ENCODING_UTF_8);
            this.objectType = URLDecoder.decode(objectType, CHARACTER_ENCODING_UTF_8);
            this.localId = URLDecoder.decode(localId, CHARACTER_ENCODING_UTF_8);
        } catch (URISyntaxException e) {
            // Should never happen.
            throw new UndeclaredThrowableException(e);
        } catch (UnsupportedEncodingException e) {
            // Should never happen.
            throw new UndeclaredThrowableException(e);
        }
    }

    public Id(String id) throws ObjectSetException {
        if (StringUtils.isBlank(id)) {
            throw new ObjectSetException("Id can not be blank");
        }
        int index = id.indexOf(SYSTEM_BASE);
        String relativeId = id;
        if (index > -1) {
            relativeId = id.substring(index + 7);
        }
        try {
            String[] segments = relativeId.split("\\/");
            if (2 <= segments.length && segments.length <= 3) {
                switch (segments.length > 2 ? 3 : segments.length) {
                    case 3:
                        localId = URLDecoder.decode(segments[2], CHARACTER_ENCODING_UTF_8);
                    case 2:
                        objectType = URLDecoder.decode(segments[1], CHARACTER_ENCODING_UTF_8);
                    case 1:
                        systemName = URLDecoder.decode(segments[0], CHARACTER_ENCODING_UTF_8);
                }
            } else {
                throw new ObjectSetException("Invalid number of tokens in ID " + id);
            }
            this.baseURI = new URI("");
        } catch (UnsupportedEncodingException e) {
            // Should never happen.
            throw new UndeclaredThrowableException(e);
        } catch (URISyntaxException e) {
            // Should never happen.
            throw new UndeclaredThrowableException(e);
        }
    }

    public String getLocalId() {
        return localId;
    }

    public String getObjectType() {
        return objectType;
    }

    public String getSystemName() {
        return systemName;
    }

    public Id expectObjectId() throws ObjectSetException {
        if (StringUtils.isBlank(localId)) {
            ObjectSetException ex = new ObjectSetException("This id instance does not qualified to identify a single unique object");
            TRACE.error("Unqualified id: systemName={}, objectType={}, localId={}", new Object[]{systemName, objectType, localId}, ex);
            throw ex;
        }
        return this;
    }

    public Id resolveLocalId(String uid) throws ObjectSetException {
        if (null == uid) {
            return new Id(systemName, objectType);
        } else {
            return new Id(systemName, objectType, uid);
        }
    }

    public URI getId() {
        try {
            URI id = getObjectSetId();
            if (null != localId) {
                id = id.resolve(URLEncoder.encode(localId, CHARACTER_ENCODING_UTF_8));
            }
            return id;
        } catch (UnsupportedEncodingException e) {
            // Should never happen.
            throw new UndeclaredThrowableException(e);
        }
    }

    public URI getQualifiedId() {
        try {
            URI id = getObjectSetId();
            if (null != localId) {
                id = id.resolve(URLEncoder.encode(localId, CHARACTER_ENCODING_UTF_8));
            }
            return baseURI.resolve(SYSTEM_BASE).resolve(id);
        } catch (UnsupportedEncodingException e) {
            // Should never happen.
            throw new UndeclaredThrowableException(e);
        }
    }


    private URI getObjectSetId() throws UnsupportedEncodingException {
        return baseURI.resolve(URLEncoder.encode(systemName, CHARACTER_ENCODING_UTF_8) + "/").resolve(URLEncoder.encode(objectType, CHARACTER_ENCODING_UTF_8) + "/");
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(SYSTEM_BASE);
        sb.append(systemName).append("/").append(objectType);
        if (null != localId) {
            sb.append("/").append(localId);
        }
        return sb.toString();
    }

    /**
     * Safely escapes the {@code uid} value
     *
     * @param uid
     * @return
     * @throws IllegalArgumentException if the {@code uid} is blank
     * @throws NullPointerException     if the {@code uid} is null
     */
    public static String escapeUid(String uid) {
        if (StringUtils.isBlank(uid)) {
            throw new IllegalArgumentException("UID can not be blank");
        }
        try {
            return URLEncoder.encode(uid, CHARACTER_ENCODING_UTF_8);
        } catch (UnsupportedEncodingException e) {
            // Should never happen.
            throw new UndeclaredThrowableException(e);
        }
    }

    /**
     * Safely unescapes the {@code uid} value
     *
     * @param uid
     * @return
     * @throws IllegalArgumentException if the {@code uid} is blank
     * @throws NullPointerException     if the {@code uid} is null
     */
    public static String unescapeUid(String uid) {
        if (StringUtils.isBlank(uid)) {
            throw new IllegalArgumentException("UID can not be blank");
        }
        try {
            return URLDecoder.decode(uid, CHARACTER_ENCODING_UTF_8);
        } catch (UnsupportedEncodingException e) {
            // Should never happen.
            throw new UndeclaredThrowableException(e);
        }
    }
}
