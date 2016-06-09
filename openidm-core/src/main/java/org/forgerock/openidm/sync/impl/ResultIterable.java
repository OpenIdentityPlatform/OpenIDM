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
 * Portions copyright 2014-2016 ForgeRock AS.
 */
package org.forgerock.openidm.sync.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;

import org.forgerock.json.JsonValue;

/**
 * Holds the query results and provides access to normalized ids
 * and optional full values
 */
public class ResultIterable implements Iterable<ResultEntry> {
    private final Collection<String> allIds;
    private final Iterable<JsonValue> values;

    /**
     * @param allIds all identifiers (normalized)
     * @param values optional values, must be in the same order as the ids,
     * or null if no full values available
     */
    public ResultIterable(Collection<String> allIds, Iterable<JsonValue> values) {
        this.allIds = allIds;
        this.values = values;
    }
    
    /**
     * @return all identifiers (normalized)
     */
    public Collection<String> getAllIds() {
        return allIds;
    }
    
    /**
     * Remove any entries that are not in the supplied ids
     * @param ids of entries to keep
     */
    public ResultIterable removeNotMatchingEntries(Collection<String> ids) {
        Iterator<ResultEntry> entryIter = this.iterator();
        Collection<String> newIds = Collections.synchronizedSet(new LinkedHashSet<String>());
        JsonValue newObjList = null;

        if (this.values != null) {
            newObjList = new JsonValue(new LinkedList<>());
        }

        while (entryIter.hasNext()) {
            ResultEntry entry = entryIter.next();
            if (ids.contains(entry.getId())) {
                newIds.add(entry.getId());
                if (newObjList != null) {
                    newObjList.add(entry.getValue());
                }
            }
        }
        return new ResultIterable(newIds, newObjList);
    }

    /**
     * Get an iterator over the ids and optional values
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<ResultEntry> iterator() {
        return new Iterator<ResultEntry>() {
            private final Iterator<String> idsIter;
            private final Iterator<JsonValue> valuesIter;
            
            {
                idsIter = allIds.iterator();
                if (values != null) {
                    valuesIter = values.iterator();
                } else {
                    valuesIter = null;
                }
            }

            @Override
            public boolean hasNext() {
                return idsIter.hasNext();
            }

            @Override
            public ResultEntry next() {
                return new ResultEntry(idsIter.next(), valuesIter == null ? null : valuesIter.next());
            }

            @Override
            public void remove() {
                idsIter.remove();
                if (valuesIter != null) {
                    valuesIter.remove();
                }
            }
        };
    }
}
 
