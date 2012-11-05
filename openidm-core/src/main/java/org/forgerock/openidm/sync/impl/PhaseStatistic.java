/**
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
*
* Copyright (c) 2012 ForgeRock AS. All Rights Reserved
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
*/
package org.forgerock.openidm.sync.impl;

//import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Holds the (source/target) Phase specific statistics
 *
 * @author gael
 * @author aegloff
 */
public class PhaseStatistic {

    public enum Phase {SOURCE, TARGET};
    
    private ReconciliationStatistic parentStat;
    Phase phase;
    private Map<Situation, List<String>> ids = Collections.synchronizedMap(new EnumMap<Situation, List<String>>(Situation.class));
    private AtomicLong processedEntries = new AtomicLong();
    private List<String> notValid;

    public PhaseStatistic(ReconciliationStatistic parentStat, Phase phase) {
        this.parentStat = parentStat;
        this.phase = phase;
        ids.put(Situation.CONFIRMED, Collections.synchronizedList(new ArrayList<String>()));
        ids.put(Situation.FOUND, Collections.synchronizedList(new ArrayList<String>()));
        ids.put(Situation.ABSENT, Collections.synchronizedList(new ArrayList<String>()));
        ids.put(Situation.AMBIGUOUS, Collections.synchronizedList(new ArrayList<String>()));
        ids.put(Situation.MISSING, Collections.synchronizedList(new ArrayList<String>()));
        ids.put(Situation.UNQUALIFIED, Collections.synchronizedList(new ArrayList<String>()));
        ids.put(Situation.UNASSIGNED, Collections.synchronizedList(new ArrayList<String>()));
        ids.put(Situation.SOURCE_MISSING, Collections.synchronizedList(new ArrayList<String>()));
        ids.put(Situation.SOURCE_IGNORED, Collections.synchronizedList(new ArrayList<String>()));
        ids.put(Situation.TARGET_IGNORED, Collections.synchronizedList(new ArrayList<String>()));
        notValid = Collections.synchronizedList(new ArrayList<String>());
    }

    public void processed(String sourceId, String targetId, boolean linkExisted, String linkId, Situation situation, Action action) {
        
        String id = null;
        if (phase == Phase.SOURCE) {
            id = sourceId;
        } else {
            id = targetId;
        }
        parentStat.processed(sourceId, targetId, linkExisted, linkId, situation, action);
        if (id != null) {
            processedEntries.incrementAndGet();
            if (situation != null) {
                List<String> situationIds = ids.get(situation);
                if (situationIds != null) {
                    situationIds.add(id); // TODO: option to not keep all results in memory
                }
            }
        }
    }
    
    // TODO: phase out notValid and replace with source ignored, target ignored, unqualified
    // situation processing
    public void addNotValid(String id) {
        notValid.add(id);
    }

    public long getProcessed() {
        return processedEntries.get();
    }
    
    public Map<String, Object> asMap() {
        Map<String, Object> results = new HashMap();

        results.put("processed", processedEntries);

        Map<String, Object> nv = new HashMap();
        nv.put("count", notValid.size());
        nv.put("ids", notValid);
        results.put("NOTVALID", nv);

        for (Entry<Situation, List<String>> e : ids.entrySet()) {
            Map<String, Object> res = new HashMap();
            res.put("count", e.getValue().size());
            res.put("ids", e.getValue());
            results.put(e.getKey().name(), res);
        }

        return results;
    }

    public void updateSummary(Map<String, Integer> simpleSummary) {
        for (Entry<Situation, List<String>> e : ids.entrySet()) {
            String key = e.getKey().name();
            Integer existing = simpleSummary.get(key);
            if (existing == null) {
                existing = Integer.valueOf(0);
            }
            Integer updated = existing + e.getValue().size();
            simpleSummary.put(key, updated);
        }
    }
}
