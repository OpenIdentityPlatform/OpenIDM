package org.forgerock.openidm.quartz.impl;

import org.quartz.StatefulJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatefulSchedulerServiceJob extends SchedulerServiceJob implements StatefulJob {

    final static Logger logger = LoggerFactory.getLogger(StatefulSchedulerServiceJob.class);
    
    public StatefulSchedulerServiceJob() {
        super();
    }
}
