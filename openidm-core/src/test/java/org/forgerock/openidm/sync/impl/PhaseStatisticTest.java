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
 * Copyright 2017 ForgeRock AS.
 */

package org.forgerock.openidm.sync.impl;

import static org.mockito.Mockito.mock;
import static org.assertj.core.api.Assertions.assertThat;

import org.forgerock.json.JsonValue;
import org.forgerock.openidm.sync.ReconAction;
import org.testng.annotations.Test;

public class PhaseStatisticTest {
    @Test
    public void testAggregation() {
        //given
        final ReconciliationStatistic firstParentStat = mock(ReconciliationStatistic.class);
        final ReconciliationStatistic secondParentStat = mock(ReconciliationStatistic.class);
        final PhaseStatistic firstPhaseStat = new PhaseStatistic(firstParentStat, PhaseStatistic.Phase.SOURCE, "firstInstance");
        final PhaseStatistic secondPhaseStat = new PhaseStatistic(secondParentStat, PhaseStatistic.Phase.SOURCE, "secondInstance");
        firstPhaseStat.processed("sourceId", "targetId", false, "linkId", true, Situation.CONFIRMED, ReconAction.CREATE );
        secondPhaseStat.processed("sourceId", "targetId", false, "linkId", true, Situation.CONFIRMED, ReconAction.CREATE );
        firstPhaseStat.processed("sourceId", "targetId", false, "linkId", true, Situation.ABSENT, ReconAction.CREATE );
        firstPhaseStat.processed("sourceId", "targetId", false, "linkId", true, Situation.AMBIGUOUS, ReconAction.CREATE );
        final String notValidId = "foo";
        secondPhaseStat.addNotValid(notValidId);
        //when
        firstPhaseStat.aggregateValues(secondPhaseStat);
        final JsonValue resultsJson = new JsonValue(firstPhaseStat.asMap());
        //then
        assertThat(firstPhaseStat.getProcessed()).isEqualTo(4);
        assertThat(resultsJson.get(Situation.CONFIRMED.name()).asMap().get("count")).isEqualTo(2);
        assertThat(resultsJson.get(Situation.ABSENT.name()).asMap().get("count")).isEqualTo(1);
        assertThat(resultsJson.get(Situation.AMBIGUOUS.name()).asMap().get("count")).isEqualTo(1);
        assertThat(new JsonValue(resultsJson.get("NOTVALID").asMap().get("ids")).asList().size()).isEqualTo(1);
        assertThat(new JsonValue(resultsJson.get("NOTVALID").asMap().get("ids")).asList().contains(notValidId));
    }
}
