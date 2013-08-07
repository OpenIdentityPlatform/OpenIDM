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

package org.forgerock.openidm.quartz.impl;

import java.util.ArrayList;
import java.util.List;

import org.quartz.Trigger;
import org.quartz.spi.SchedulerSignaler;

public class SimpleSignaler implements SchedulerSignaler {
    
    private List<String> finalized = new ArrayList<String>();
    private List<String> misfired = new ArrayList<String>();

    @Override
    public void notifySchedulerListenersFinalized(Trigger trigger) {
        finalized.add(trigger.getFullName());
    }

    @Override
    public void notifyTriggerListenersMisfired(Trigger trigger) {
        misfired.add(trigger.getFullName());
    }

    @Override
    public void signalSchedulingChange(long time) {
        
    }
    
    public boolean isFinalized(Trigger trigger) {
        return finalized.contains(trigger.getFullJobName());
    }
    
    public boolean isMisfired(Trigger trigger) {
        return misfired.contains(trigger.getFullJobName());
    }
    
    public int getMisfiredCount() {
        return misfired.size();
    }
    
    public int getFinalizedCount() {
        return finalized.size();
    }
    
    public void clear() {
        finalized = new ArrayList<String>();
        misfired = new ArrayList<String>();
    }

}
