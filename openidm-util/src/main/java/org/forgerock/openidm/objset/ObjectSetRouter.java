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
import java.util.HashMap;
import java.util.Map;

/**
 * Routes calls to other object sets based on identifier prefixes.
 * <p>
 * The {@link routes} map associates routing prefixes with object sets to route to. Any
 * request where the prefix itself is the identifier or where it begins the identifier
 * and is separated by a slash {@code /} character will be routed to the associated object
 * set.
 * <p>
 * When a call is routed to an associated object set, its routing prefix is stripped from the
 * call and from the {@code _id} property of any object that is passed to it. At the end of
 * the call, the {@code _id} property routing prefix is restored.
 * <p>
 * For example, if a routing prefix is {@code "account"} is associated with a
 * {@code AccountObjectSet} object, and a call is performed with an {@code id} of
 * {@code "account/jsmith"}, the call to the {@code AccountObjectSet} method will contain
 * an {@code id} of {@code "jsmith"}.
 * <p>
 * If an object set cannot be routed to for a call, then a {@link NotFoundException} is
 * thrown.
 * <p>
 * This class is designed to be subclassed, where the subclass is responsible for initializing
 * the {@link routes} map with prefixes and object sets to route to.
 *
 * @author Paul C. Bryan
 */
public class ObjectSetRouter implements ObjectSet {

    /** A map associating routing prefixes with object sets to route to. */
    protected Map<String, ObjectSet> routes = new HashMap<String, ObjectSet>();

    /**
     * Constructs a new object set router.
     */
    public ObjectSetRouter() {
    }

    /**
     * Splits an identifier into two parts. The first part is the routing prefix, which
     * correlates to a value in the {@link routes} map. The second part is the remaining
     * identifier, which is local to the routed {@code ObjectSet}. If the second part
     * is {@code null}, then the request is against routed the objet set itself.
     *
     * @param id the identifier to be split.
     * @param exception if {@code true} throws exception if no route could be found.
     * @return an array with two elements: prefix and identifier.
     * @throws NotFoundException if {@code exception} is true and no route could be found.
     */
    private String[] split(String id, boolean exception) throws NotFoundException {
        String[] result = new String[2];
        for (String key : routes.keySet()) {
            if (id.equals(key)) {
                result[0] = key;
                return result; // operation on object set itself
            }
            else if (id.startsWith(key + '/')) {
                result[0] = key;
                if (id.length() > key.length() + 1) {
                    result[1] = id.substring(key.length() + 1); // skip the slash
                }
            }
        }
        if (exception && result[0] == null) {
            throw new NotFoundException();
        }
        return result;
    }

    /**
     * Inserts the routing prefix in the {@code _id} property in the object, if it exists.
     *
     * @param object the object to modify the {@code _id} property in.
     * @param route the routing prefix to insert in the {@code _id} property.
     */
    private void insertRoute(Map<String, Object> object, String route) {
        if (object.containsKey("_id")) {
            Object _id = object.get("_id");
            if (_id instanceof String) { // ignore unknown _id type
                object.put("_id", route + '/' + (String)_id);
            }
        }
    }

    /**
     * Sets the {@code _id} property in the object, if it exists.
     *
     * @param object the object to set the {@code _id} property in.
     * @param id the value to set in the {@code _id} property.
     */
    private void setObjectId(Map<String, Object> object, String id) {
        if (object.containsKey("_id")) {
            object.put("_id", id); // intentionally ignores whatever value may be there
        }
    }

    @Override
    public void create(String id, Map<String, Object> object) throws ObjectSetException {
        String[] split = split(id, true); // throws NotFoundException
        setObjectId(object, split[1]); // set id without prefix
        routes.get(split[0]).create(split[1], object);
        insertRoute(object, split[0]); // restore prefix
    }

    @Override
    public Map<String, Object> read(String id) throws ObjectSetException {
        String[] split = split(id, true); // throws NotFoundException
        Map<String, Object> object = routes.get(split[0]).read(split[1]);
        insertRoute(object, split[0]); // restore prefix
        return object;
    }

    @Override
    public void update(String id, String rev, Map<String, Object> object) throws ObjectSetException {
        String[] split = split(id, true); // throws NotFoundException
        if (object.containsKey("_id")) {
            Object _id = object.get("_id");
            if (_id instanceof String) {
                String[] _split = split((String)_id, false); // do not throw NotFoundException
                if (!split[0].equals(_split[0])) { // trying to move to a different set
                    throw new BadRequestException("update cannot move object to a different set");
                }
                setObjectId(object, _split[1]); // set id without prefix
            }
        }
        routes.get(split[0]).update(split[1], rev, object);
        insertRoute(object, split[0]); // restore prefix
    }

    @Override
    public void delete(String id, String rev) throws ObjectSetException {
        String[] split = split(id, true); // throws NotFoundException
        routes.get(split[0]).delete(split[1], rev);
    }

    @Override
    public void patch(String id, String rev, Patch patch) throws ObjectSetException {
        String[] split = split(id, true); // throws NotFoundException
        routes.get(split[0]).patch(split[1], rev, patch);
    }

    @Override
    public Map<String, Object> query(String id, Map<String, Object> params) throws ObjectSetException {
        String[] split = split(id, true); // throws NotFoundException
        return routes.get(split[0]).query(split[1], params);
    }
}
