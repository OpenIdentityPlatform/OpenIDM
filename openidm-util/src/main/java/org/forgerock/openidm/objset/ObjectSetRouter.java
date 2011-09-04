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
 * The {@link #routes} map associates routing prefixes with object sets to route to. Any
 * request where the prefix itself is the identifier or where it begins the identifier
 * and is separated by a slash {@code /} character will be routed to the associated object
 * The {@code _id} property should not be fully qualified, and is not modified during routing.
 * <p>
 * If an object set cannot be routed to for a call, then a {@link NotFoundException} is
 * thrown.
 * <p>
 * This class is designed to be subclassed, where the subclass is responsible for initializing
 * the {@link #routes} map with prefixes and object sets to route to.
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
     * correlates to a value in the {@link #routes} map. The second part is the remaining
     * identifier, which is local to the routed {@code ObjectSet}. If the second part
     * is {@code null}, then the request is against routed the objet set itself.
     *
     * @param id the identifier to be split.
     * @return an array with two elements: prefix and identifier.
     * @throws NotFoundException if no route could be found.
     */
    private String[] split(String id) throws NotFoundException {
        String[] result = new String[2];
        for (String key : routes.keySet()) {
            if (id.equals(key)) {
                result[0] = key;
            }
            else if (id.startsWith(key + '/')) {
                result[0] = key;
                if (id.length() > key.length() + 1) {
                    result[1] = id.substring(key.length() + 1); // skip the slash
                }
            }
        }
        if (result[0] == null) {
            throw new NotFoundException("no route for " + id);
        }
        return result;
    }

    @Override
    public void create(String id, Map<String, Object> object) throws ObjectSetException {
        String[] split = split(id); // throws NotFoundException
        routes.get(split[0]).create(split[1], object);
    }

    @Override
    public Map<String, Object> read(String id) throws ObjectSetException {
        String[] split = split(id); // throws NotFoundException
        return routes.get(split[0]).read(split[1]);
    }

    @Override
    public void update(String id, String rev, Map<String, Object> object) throws ObjectSetException {
        String[] split = split(id); // throws NotFoundException
        routes.get(split[0]).update(split[1], rev, object);
    }

    @Override
    public void delete(String id, String rev) throws ObjectSetException {
        String[] split = split(id); // throws NotFoundException
        routes.get(split[0]).delete(split[1], rev);
    }

    @Override
    public void patch(String id, String rev, Patch patch) throws ObjectSetException {
        String[] split = split(id); // throws NotFoundException
        routes.get(split[0]).patch(split[1], rev, patch);
    }

    @Override
    public Map<String, Object> query(String id, Map<String, Object> params) throws ObjectSetException {
        String[] split = split(id); // throws NotFoundException
        return routes.get(split[0]).query(split[1], params);
    }

    @Override
    public Map<String, Object> action(String id, Map<String, Object> params) throws ObjectSetException {
        String[] split = split(id); // throws NotFoundException
        return routes.get(split[0]).action(split[1], params);
    }
}
