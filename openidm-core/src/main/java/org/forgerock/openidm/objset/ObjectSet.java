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
 * An interface that provides access methods on a set of objects. Objects are JSON object
 * structures composed of the basic Java types: {@link Map}, {@link List}, {@link String},
 * {@link Number}, {@link Boolean}.
 * <p>
 * Implementations define their own object identifier taxonomy. By convention, identifiers
 * should follow a URI relative path pattern. Identifiers are provided through the
 * {@code id} parameter in methods, and in the {@code _id} property within objects. In the
 * case where {@code id} parameter is {@code null}, the operation is being requested on the
 * object set itself.
 * <p>
 * Implementations can elect for objects in the set to be optimistically concurrent; if so,
 * an object's version is provided through the {@code rev} parameter in methods, and in
 * the {@code _rev} property within objects. Consumers of this interface should provide object
 * version in the {@code rev} parameters if the {@code read} method yields objects that
 * contain the {@code _rev} property.  
 * <p>
 * Implementations can define other reserved object property names with an underscore
 * {@code _} prefix.
 *
 * @author Paul C. Bryan
 */
public interface ObjectSet {

    /**
     * Creates a new object in the object set.
     * <p>
     * This method sets the {@code _id} property to the assigned identifier for the object,
     * and the {@code _rev} property to the revised object version if optimistic concurrency
     * is supported.
     *
     * @param id the client-generated identifier to use, or {@code null} if server-generated identifier is requested.
     * @param object the contents of the object to create in the object set.
     * @throws NotFoundException if the specified id could not be resolve. 
     * @throws ForbiddenException if access to the object or object set is forbidden.
     */
    void create(String id, Map<String, Object> object) throws ObjectSetException;

    /**
     * Reads an object from the object set.
     * <p>
     * The object will contain metadata properties, including object identifier {@code _id},
     * and object version {@code _rev} if optimistic concurrency is supported. If optimistic
     * concurrency is not supported, then {@code _rev} must be absent or {@code null}.
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
     * @throws ConflictException if patch could not be applied object state or if version is required.
     * @throws ForbiddenException if access to the object is forbidden.
     * @throws NotFoundException if the specified object could not be found. 
     * @throws PreconditionFailedException if version did not match the existing object in the set.
     */
    void patch(String id, String rev, Patch patch) throws ObjectSetException;

    /**
     * Performs a query on the specified object and returns the associated results.
     * <p>
     * Queries are parametric; a set of named parameters is provided as the query criteria.
     * The query result is a JSON object structure composed of basic Java types.
     *
     * @param id identifies the object to query.
     * @param params the parameters of the query to perform.
     * @return the query results object.
     * @throws NotFoundException if the specified object could not be found. 
     * @throws ForbiddenException if access to the object or specified query is forbidden.
     */
    Map<String, Object> query(String id, Map<String, Object> params) throws ObjectSetException;
}
