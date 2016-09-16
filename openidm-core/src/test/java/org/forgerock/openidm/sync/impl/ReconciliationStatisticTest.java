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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.forgerock.guava.common.base.Optional;
import org.forgerock.openidm.sync.ReconAction;
import org.testng.annotations.Test;

public class ReconciliationStatisticTest {
    @Test
    public void testAggregation() {
        //given
        final ObjectMapping mockObjectMapping = mock(ObjectMapping.class);
        final ReconciliationContext mockContext = mock(ReconciliationContext.class);
        when(mockContext.getObjectMapping()).thenReturn(mockObjectMapping);
        final ReconciliationStatistic firstStat = new ReconciliationStatistic(mockContext);
        final ReconciliationStatistic secondStat = new ReconciliationStatistic(mockContext);
        //when
        firstStat.reconStart();
        secondStat.reconStart();
        firstStat.processed("sourceId", null, false, null, false, Situation.CONFIRMED, ReconAction.CREATE );
        secondStat.processed("sourceId2", null, false, null, false, Situation.CONFIRMED, ReconAction.CREATE );
        firstStat.processed(null, null, true, "linkId", false, Situation.ABSENT, ReconAction.CREATE );
        firstStat.processed(null, null, false, "linkId2", true, Situation.AMBIGUOUS, ReconAction.CREATE );
        secondStat.processed(null, "targetId", false, null, false, Situation.CONFIRMED, ReconAction.CREATE );
        secondStat.processed(null, "targetId2", false, null, false, Situation.CONFIRMED, ReconAction.IGNORE );
        secondStat.reconEnd();
        firstStat.reconEnd();
        final List<ReconciliationStatistic> instances = new ArrayList<>();
        instances.add(firstStat);
        instances.add(secondStat);
        final Optional<ReconciliationStatistic> aggregatedStat = ReconciliationStatistic.getAggregatedInstance(instances);
        //then
        assertThat(aggregatedStat.isPresent());
        assertThat(aggregatedStat.get().getSourceProcessed()).isEqualTo(2);
        assertThat(aggregatedStat.get().getTargetCreated()).isEqualTo(1);
        assertThat(aggregatedStat.get().getTargetProcessed()).isEqualTo(1);
        assertThat(aggregatedStat.get().getLinkProcessed()).isEqualTo(1);
        assertThat(aggregatedStat.get().getLinkCreated()).isEqualTo(1);
        assertThat(aggregatedStat.get().getStarted()).isEqualTo(firstStat.getStarted());
        assertThat(aggregatedStat.get().getEnded()).isEqualTo(firstStat.getEnded());
    }
}
