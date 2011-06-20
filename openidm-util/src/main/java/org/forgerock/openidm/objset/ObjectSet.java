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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openidm.objset;

// Java Standard Edition
import java.util.List; // for Javadoc
import java.util.Map;

/**
 * An interface that provides uniform access methods on a set of objects. This interface
 * supports JSON-based structures that are composed of basic Java types: {@link Map}
 * (JSON object), {@link List} (JSON array), {@link String}, {@link Number}, {@link Boolean}.
 * <p>
 * Object identifiers are provided through the {@code id} parameter in methods, and in the
 * {@code _id} property within objects. In the case where {@code id} parameter is {@code null},
 * an operation is being requested on the entire object set.
 * <p>
 * If an object set is hierarchically organized, levels of hierarchy should be separated in
 * the {@code id} parameter using the {@code "/"} character. By convention, the {@code _id}
 * property value should be a leaf-level identifier (unqualified).
 * <p>
 * Implementations can elect to support optimistic concurrency. If so, an object's version is
 * provided through the {@code rev} parameter in methods, and in the {@code _rev} property
 * within objects. Consumers of this interface should provide object version in the {@code rev}
 * parameters if the {@code read} method yields objects that contain the {@code _rev} property.  
 * <p>
 * Implementations can reserve their own addition object property names with an underscore
 * {@code _} prefix.
 *
 * @author Paul C. Bryan
 */
public interface ObjectSet {

    /** The {@code _id} property. */
    public static final String ID = "_id";

    /** The {@code _rev} property. */
    public static final String REV = "_rev";

    /**
     * Creates a new object in the object set.
     * <p>
     * On completion, this method sets the {@code _id} property to the assigned identifier for
     * the object, and the {@code _rev} property to object version if optimistic concurrency
     * is supported.
     *
     * @param id the requested identifier to use, or {@code null} if a server-generated identifier is requested.
     * @param object the contents of the object to create in the object set.
     * @throws ForbiddenException if access to the object or object set is forbidden.
     */
    void create(String id, Map<String, Object> object) throws ObjectSetException;

    /**
     * Reads an object from the object set.
     * <p>
     * The returned object will contain metadata properties, including relative object
     * identifier {@code _id}. If optimistic concurrency is supported, the object version
     * {@code _rev} will be set in the returned object. If optimistic concurrency is not
     * supported, then {@code _rev} must be {@code null} or absent.
     *
     * @param id the identifier of the object to retrieve from the object set.
     * @throws NotFoundException if the specified object could not be found. 
     * @throws ForbiddenException if access to the object is forbidden.
     * @return the requested object.
     */
    Map<String, Object> read(String id) throws ObjectSetException;

    /**
     * Updates an existing specified object in the object set.
     * <p>
     * This method updates the {@code _rev} property to the revised object version on update
     * if optimistic concurrency is supported.
     *
     * @param id the identifier of the object to be updated.
     * @param rev the version of the object to update; or {@code null} if not provided.
     * @param object the contents of the object to updated in the object set.
     * @throws ConflictException if version is required but is {@code null}.
     * @throws ForbiddenException if access to the object is forbidden.
     * @throws NotFoundException if the specified object could not be found. 
     * @throws PreconditionFailedException if version did not match the existing object in the set.
     */
    void update(String id, String rev, Map<String, Object> object) throws ObjectSetException;

    /**
     * Deletes the specified object from the object set.
     *
     * @param id the identifier of the object to be deleted.
     * @param rev the version of the object to delete or {@code null} if not provided.
     * @throws NotFoundException if the specified object could not be found. 
     * @throws ForbiddenException if access to the object is forbidden.
     * @throws ConflictException if version is required but is {@code null}.
     * @throws PreconditionFailedException if version did not match the existing object in the set.
     */
    void delete(String id, String rev) throws ObjectSetException;

    /**
     * Applies a patch (partial change) to the specified object in the object set.
     *
     * @param id the identifier of the object to be patched.
     * @param rev the version of the object to patch or {@code null} if not provided.
     * @param patch the partial change to apply to the object.
     * @throws ConflictException if patch could not be applied for the given object state or if version is required.
     * @throws ForbiddenException if access to the object is forbidden.
     * @throws NotFoundException if the specified object could not be found. 
     * @throws PreconditionFailedException if version did not match the existing object in the set.
     */
    void patch(String id, String rev, Patch patch) throws ObjectSetException;

    /**
     * Performs a query on the specified object and returns the associated result. The
     * execution of a query should not incur side effects.
     * <p>
     * Queries are parametric; a set of named parameters is provided as the query criteria.
     * The query result is a JSON object structure composed of basic Java types; its overall
     * structure is defined by the implementation.
     *
     * @param id identifies the object to query.
     * @param params the parameters of the query to perform.
     * @return the query result object.
     * @throws NotFoundException if the specified object could not be found. 
     * @throws ForbiddenException if access to the object or the specified query is forbidden.
     */
    Map<String, Object> query(String id, Map<String, Object> params) throws ObjectSetException;

    /**
     * Performs an action on the specified object and returns the associated result. The
     * execution of an action may incur side effects.
     * <p>
     * Actions are parametric; a set of named parameters is provided as the action criteria.
     * The action result is a JSON object structure composed of basic Java types; its overall
     * structure is defined by the implementation.
     *
     * @param id identifies the object to perform the action on.
     * @param params the parameters of the action to perform.
     * @return the action result object.
     * @throws NotFoundException if the specified object could not be found. 
     * @throws ForbiddenException if access to the object or the specified action is forbidden.
     */
    Map<String, Object> action(String id, Map<String, Object> params) throws ObjectSetException;
}
