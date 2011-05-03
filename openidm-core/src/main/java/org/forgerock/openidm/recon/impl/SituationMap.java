/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openidm.recon.impl;

import java.util.Map;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A utility class that maps {@link Situation}s to Scripts that need to be
 * executed.
 */
public class SituationMap {

    private static final long serialVersionUID = 1L;
    final static Logger logger = LoggerFactory.getLogger(SituationMap.class);

    private Map<Situation, String> situationsToActionsMap;

    /**
     * Construct a {@link Situation} to {@link Script} mapping, with the given
     * json configuration.
     *
     * @param situationConfig json structure
     */
    public SituationMap(Map<String, Object> situationConfig) {
        situationsToActionsMap = buildMapForSituations(situationConfig);
    }

    /**
     * Convert the configuration to appropriate mappings. Situation configuration
     * text is normalized to upper case, and failed Situation conversions are logged as
     * errors.
     * <p/>
     * TODO Situations Map to Scripts not Strings, this needs to be changed in the future
     *
     * @param mappingConfig json object
     * @return aSituationMapping
     */
    private Map<Situation, String> buildMapForSituations(Map<String, Object> mappingConfig) {
        Map<Situation, String> situationMap = new HashMap<Situation, String>();
        for (Map.Entry<String, Object> entry : mappingConfig.entrySet()) {
            Situation situation = null;
            try {
                Situation.valueOf(entry.getKey().toUpperCase());
                situationMap.put(situation, ((String) entry.getValue()));
            } catch (IllegalArgumentException e) {
                logger.error(e.getLocalizedMessage());
            }
        }
        return situationMap;
    }

    /**
     * For the given situation get the {@code script} that needs to be
     * executed.
     *
     * @param situation key value
     * @return script for the given situation or null
     */
    public String getScriptForSituation(Situation situation) {
        return situationsToActionsMap.get(situation);
    }

}