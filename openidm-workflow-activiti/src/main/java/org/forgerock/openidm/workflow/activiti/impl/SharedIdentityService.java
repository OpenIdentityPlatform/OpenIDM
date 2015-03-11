/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2012-2014 ForgeRock AS. All rights reserved.
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
package org.forgerock.openidm.workflow.activiti.impl;

import org.activiti.engine.IdentityService;
import org.activiti.engine.identity.Group;
import org.activiti.engine.identity.GroupQuery;
import org.activiti.engine.identity.Picture;
import org.activiti.engine.identity.User;
import org.activiti.engine.identity.UserQuery;
import org.activiti.engine.impl.identity.Authentication;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.activiti.engine.identity.NativeGroupQuery;
import org.activiti.engine.identity.NativeUserQuery;
import org.forgerock.json.resource.*;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.router.RouteService;

/**
 * @version $Revision$ $Date$
 */
public class SharedIdentityService implements IdentityService {

    public static final URI SCIM_CORE_SCHEMA = URI.create("urn:scim:schemas:core:1.0");
    //Common Schema Attributes
    public static final String SCIM_ID = "id";
    public static final String SCIM_EXTERNALID = "externalId";
    public static final String SCIM_META = "meta";
    public static final String SCIM_META_CREATED = "created";
    public static final String SCIM_META_LASTMODIFIED = "lastModified";
    public static final String SCIM_META_LOCATION = "location";
    public static final String SCIM_META_VERSION = "version";
    public static final String SCIM_META_ATTRIBUTES = "attributes";
    //SCIM User Schema
    public static final String SCIM_USERNAME = "userName";
    public static final String SCIM_NAME = "name";
    public static final String SCIM_NAME_FAMILYNAME = "familyName";
    public static final String SCIM_NAME_GIVENNAME = "givenName";
    public static final String SCIM_NAME_MIDDLENAME = "middleName";
    public static final String SCIM_NAME_HONORIFICPREFIX = "honorificPrefix";
    public static final String SCIM_NAME_HONORIFICSUFFIX = "honorificSuffix";
    public static final String SCIM_DISPLAYNAME = "displayName";
    public static final String SCIM_NICKNAME = "nickName";
    public static final String SCIM_PROFILEURL = "profileUrl";
    public static final String SCIM_TITLE = "title";
    public static final String SCIM_USERTYPE = "userType";
    public static final String SCIM_PREFERREDLANGUAGE = "preferredLanguage";
    public static final String SCIM_LOCALE = "locale";
    public static final String SCIM_TIMEZONE = "timezone";
    public static final String SCIM_ACTIVE = "active";
    public static final String SCIM_PASSWORD = "password";
    //Multi-valued Attributes
    public static final String SCIM_EMAILS = "emails";
    public static final String SCIM_PHONENUMBERS = "phoneNumbers";
    public static final String SCIM_IMS = "ims";
    public static final String SCIM_PHOTOS = "photos";
    public static final String SCIM_ADDRESSES = "addresses";
    public static final String SCIM_GROUPS = "groups";
    public static final String SCIM_ENTITLEMENTS = "entitlements";
    public static final String SCIM_ROLES = "roles";
    public static final String SCIM_X509CERTIFICATES = "x509Certificates";
    //SCIM Group Schema
    public static final String SCIM_MEMBERS = "members";
    private RouteService repositoryRoute;
    private ServerContext serverContext;
    private CryptoService cryptoService;
    private ConnectionFactory connectionFactory;
    
    public static final String USER_PATH = "managed/user/";
    public static final String GROUP_PATH = "managed/group/";

    public void setRouter(RouteService router) {
        repositoryRoute = router;
        if (router != null) {
            try {
                this.serverContext = repositoryRoute.createServerContext();
            } catch (ResourceException ex) {
                Logger.getLogger(SharedIdentityService.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public void setCryptoService(CryptoService service) {
        this.cryptoService = service;
    }

    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    private Connection getConnection() throws ResourceException {
        return connectionFactory.getConnection();
    }

    public QueryResult query(QueryRequest request, QueryResultHandler handler) throws ResourceException {
        return getConnection().query(serverContext, request, handler);
    }

    public QueryResult query(QueryRequest request, Collection<? super Resource> result) throws ResourceException {
        return getConnection().query(serverContext, request, result);
    }

    /**
     * Creates a new user. The user is transient and must be saved using
     * {@link #saveUser(org.activiti.engine.identity.User)}.
     *
     * @param userId id for the new user, cannot be null.
     */
    public User newUser(String userId) {
        return new JsonUser(cryptoService, userId);
    }

    /**
     * Saves the user. If the user already existed, the user is updated.
     *
     * @param user user to save, cannot be null.
     * @throws RuntimeException when a user with the same name already exists.
     */
    public void saveUser(User user) {
        if (user instanceof JsonUser) {
            JsonUser jsonUser = (JsonUser) user;
            JsonUserQuery query = new JsonUserQuery(this);
            query.userId(user.getId());
            User existingUser = query.executeSingleResult(null);
            try {
                if (existingUser != null) {
                    UpdateRequest request = Requests.newUpdateRequest(USER_PATH, jsonUser.getId(), jsonUser);
                    getConnection().update(serverContext, request);
                } else {
                    CreateRequest request = Requests.newCreateRequest(USER_PATH, jsonUser.getId(), jsonUser);
                    getConnection().create(serverContext, request);
                }
            } catch (ResourceException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    /**
     * Creates a {@link org.activiti.engine.identity.UserQuery} that allows to
     * programmatically query the users.
     */
    public UserQuery createUserQuery() {
        return new JsonUserQuery(this);
    }

    /**
     * @param userId id of user to delete, cannot be null. When an id is passed
     * for a not existing user, this operation is ignored.
     */
    public void deleteUser(String userId) {
        try {
            DeleteRequest request = Requests.newDeleteRequest(USER_PATH + userId);
            getConnection().delete(serverContext, request);
        } catch (ResourceException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Creates a new group. The group is transient and must be saved using
     * {@link #saveGroup(org.activiti.engine.identity.Group)}.
     *
     * @param groupId id for the new group, cannot be null.
     */
    public Group newGroup(String groupId) {
        return new JsonGroup(groupId);
    }

    /**
     * Creates a {@link org.activiti.engine.identity.GroupQuery} thats allows to
     * programmatically query the groups.
     */
    public GroupQuery createGroupQuery() {
        return new JsonGroupQuery(this);
    }

    /**
     * Saves the group. If the group already existed, the group is updated.
     *
     * @param group group to save. Cannot be null.
     * @throws RuntimeException when a group with the same name already exists.
     */
    public void saveGroup(Group group) {
        if (group instanceof JsonGroup) {
            JsonGroup jsonGroup = (JsonGroup) group;
            JsonGroupQuery query = new JsonGroupQuery(this);
            query.groupId(group.getId());
            Group existingGroup = query.executeSingleResult(null);
            try {
                if (existingGroup != null) {
                    UpdateRequest request = Requests.newUpdateRequest(GROUP_PATH, jsonGroup.getId(), jsonGroup);
                    getConnection().update(serverContext, request);
                } else {
                    CreateRequest request = Requests.newCreateRequest(GROUP_PATH, jsonGroup.getId(), jsonGroup);
                    getConnection().create(serverContext, request);
                }
            } catch (ResourceException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    /**
     * Deletes the group. When no group exists with the given id, this operation
     * is ignored.
     *
     * @param groupId id of the group that should be deleted, cannot be null.
     */
    public void deleteGroup(String groupId) {
        try {
            DeleteRequest request = Requests.newDeleteRequest(GROUP_PATH + groupId);
            getConnection().delete(serverContext, request);
        } catch (ResourceException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param userId the userId, cannot be null.
     * @param groupId the groupId, cannot be null.
     * @throws RuntimeException when the given user or group doesn't exist or
     * when the user is already member of the group.
     */
    public void createMembership(String userId, String groupId) {
    }

    /**
     * Delete the membership of the user in the group. When the group or user
     * don't exist or when the user is not a member of the group, this operation
     * is ignored.
     *
     * @param userId the user's id, cannot be null.
     * @param groupId the group's id, cannot be null.
     */
    public void deleteMembership(String userId, String groupId) {
    }

    /**
     * Checks if the password is valid for the given user. Arguments userId and
     * password are nullsafe.
     */
    public boolean checkPassword(String userId, String password) {
        JsonUserQuery query = new JsonUserQuery(this);
        query.userId(userId);
        JsonUser existingUser = (JsonUser) query.executeSingleResult(null);
        if (existingUser != null) {
            existingUser.setCryptoService(cryptoService);
            return existingUser.getPassword().equals(password);
        }
        return false;
    }

    /**
     * Passes the authenticated user id for this particular thread. All service
     * method (from any service) invocations done by the same thread will have
     * access to this authenticatedUserId.
     */
    public void setAuthenticatedUserId(String authenticatedUserId) {
        Authentication.setAuthenticatedUserId(authenticatedUserId);
    }

    /**
     * Sets the picture for a given user.
     *
     * @param picture can be null to delete the picture.
     * @throws org.activiti.engine.ActivitiException if the user doesn't exist.
     */
    public void setUserPicture(String userId, Picture picture) {
    }

    /**
     * Retrieves the picture for a given user.
     *
     * @throws org.activiti.engine.ActivitiException if the user doesn't exist.
     * @returns null if the user doesn't have a picture.
     */
    public Picture getUserPicture(String userId) {
        return null;
    }

    /**
     * Generic extensibility key-value pairs associated with a user
     */
    public void setUserInfo(String userId, String key, String value) {
    }

    /**
     * Generic extensibility key-value pairs associated with a user
     */
    public String getUserInfo(String userId, String key) {
        return null;
    }

    /**
     * Generic extensibility keys associated with a user
     */
    public List<String> getUserInfoKeys(String userId) {
        return null;
    }

    /**
     * Delete an entry of the generic extensibility key-value pairs associated
     * with a user
     */
    public void deleteUserInfo(String userId, String key) {
    }

    @Override
    public NativeUserQuery createNativeUserQuery() {
        throw new UnsupportedOperationException("Native user query not supported.");
    }

    @Override
    public NativeGroupQuery createNativeGroupQuery() {
        throw new UnsupportedOperationException("Native group query not supported.");
    }
}
