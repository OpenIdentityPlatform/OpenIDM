package org.forgerock.openidm.scheduler;

import java.util.Calendar;
import java.util.Date;
import java.util.Set;

import org.quartz.DateIntervalTrigger;
import org.quartz.DateIntervalTrigger.IntervalUnit;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;
import org.quartz.SchedulerMetaData;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerUtils;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.spi.JobStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestRepoJobstore {

    /**
     * @param args
     */
    public static void main(String[] args) {

        System.setProperty("org.quartz.properties", "/tmp/quartz.properties");
        RepoJobStore.setRouter(new TestRouter());
        
        TestRepoJobstore test = new TestRepoJobstore();
        try {
            test.run();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public void run() throws Exception {
        Logger log = LoggerFactory.getLogger(TestRepoJobstore.class);

        log.info("------- Initializing -------------------");

        // First we must get a reference to a scheduler
        SchedulerFactory sf = new StdSchedulerFactory();
        Scheduler sched = sf.getScheduler();

        log.info("------- Initialization Complete --------");

        log.info("------- Scheduling Jobs ----------------");

        // jobs can be scheduled before sched.start() has been called

        // job 1 will run every 20 seconds
        JobDetail job = new JobDetail("job1", "group1", SimpleJob.class);
        CronTrigger trigger = new CronTrigger("trigger1", "group1", "job1",
                "group1", "0/20 * * * * ?");
        sched.addJob(job, true);
        Date ft = sched.scheduleJob(trigger);
        log.info(job.getFullName() + " has been scheduled to run at: " + ft
                + " and repeat based on expression: "
                + trigger.getCronExpression());

        // job 2 will run every other minute (at 15 seconds past the minute)
        job = new JobDetail("job2", "group1", SimpleJob.class);
        trigger = new CronTrigger("trigger2", "group1", "job2", "group1",
                "15 0/2 * * * ?");
        sched.addJob(job, true);
        ft = sched.scheduleJob(trigger);
        log.info(job.getFullName() + " has been scheduled to run at: " + ft
                + " and repeat based on expression: "
                + trigger.getCronExpression());

        // job 3 will run every other minute but only between 8am and 5pm
        job = new JobDetail("job3", "group1", SimpleJob.class);
        trigger = new CronTrigger("trigger3", "group1", "job3", "group1",
                "0 0/2 8-17 * * ?");
        sched.addJob(job, true);
        ft = sched.scheduleJob(trigger);
        log.info(job.getFullName() + " has been scheduled to run at: " + ft
                + " and repeat based on expression: "
                + trigger.getCronExpression());

        // job 4 will run every three minutes but only between 5pm and 11pm
        job = new JobDetail("job4", "group1", SimpleJob.class);
        trigger = new CronTrigger("trigger4", "group1", "job4", "group1",
                "0 0/3 17-23 * * ?");
        sched.addJob(job, true);
        ft = sched.scheduleJob(trigger);
        log.info(job.getFullName() + " has been scheduled to run at: " + ft
                + " and repeat based on expression: "
                + trigger.getCronExpression());

        // job 5 will run at 10am on the 1st and 15th days of the month
        job = new JobDetail("job5", "group1", SimpleJob.class);
        trigger = new CronTrigger("trigger5", "group1", "job5", "group1",
                "0 0 10am 1,15 * ?");
        sched.addJob(job, true);
        ft = sched.scheduleJob(trigger);
        log.info(job.getFullName() + " has been scheduled to run at: " + ft
                + " and repeat based on expression: "
                + trigger.getCronExpression());

        // job 6 will run every 30 seconds but only on Weekdays (Monday through
        // Friday)
        job = new JobDetail("job6", "group1", SimpleJob.class);
        trigger = new CronTrigger("trigger6", "group1", "job6", "group1",
                "0,30 * * ? * MON-FRI");
        sched.addJob(job, true);
        ft = sched.scheduleJob(trigger);
        log.info(job.getFullName() + " has been scheduled to run at: " + ft
                + " and repeat based on expression: "
                + trigger.getCronExpression());

        // job 7 will run every 30 seconds but only on Weekends (Saturday and
        // Sunday)
        job = new JobDetail("job7", "group1", SimpleJob.class);
        trigger = new CronTrigger("trigger7", "group1", "job7", "group1",
                "0,30 * * ? * SAT,SUN");
        sched.addJob(job, true);
        ft = sched.scheduleJob(trigger);
        log.info(job.getFullName() + " has been scheduled to run at: " + ft
                + " and repeat based on expression: "
                + trigger.getCronExpression());

        log.info("------- Starting Scheduler ----------------");

        // All of the jobs have been added to the scheduler, but none of the
        // jobs
        // will run until the scheduler has been started
        sched.start();

        log.info("------- Started Scheduler -----------------");

        log.info("------- Waiting five minutes... ------------");
        try {
            // wait five minutes to show jobs
            Thread.sleep(300L * 1000L);
            // executing...
        } catch (Exception e) {
        }

        log.info("------- Shutting Down ---------------------");

        sched.shutdown(true);

        log.info("------- Shutdown Complete -----------------");

        SchedulerMetaData metaData = sched.getMetaData();
        log.info("Executed " + metaData.getNumberOfJobsExecuted() + " jobs.");

    }
    
    
}
