package org.forgerock.openidm.salesforce.internal.async;

import static org.fest.assertions.api.Assertions.*;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.RootContext;
import org.forgerock.json.resource.Router;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.salesforce.internal.SalesforceConfiguration;
import org.forgerock.openidm.salesforce.internal.SalesforceConnection;
import org.forgerock.openidm.salesforce.internal.data.SalesforceRequestHandler;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sforce.async.BatchStateEnum;
import com.sforce.async.BulkConnection;
import com.sforce.async.JobInfo;
import com.sforce.async.JobStateEnum;
import com.sforce.async.OperationEnum;
import com.sforce.ws.parser.XmlInputStream;

/**
 * A NAME does ...
 * 
 * @author Laszlo Hordos
 */
public class AsyncJobResourceProviderTest {

    private SalesforceConnection connection = null;
    private Connection router = null;

    @BeforeClass
    public void beforeClass() throws Exception {
        File configFile = new File(System.getProperty("user.home"), "salesforce.json");
        if (configFile.exists()) {
            JsonValue config = new JsonValue((new ObjectMapper()).readValue(configFile, Map.class));
            SalesforceConfiguration configuration =
                    SalesforceRequestHandler.parseConfiguration(config
                            .get("configurationProperties"));
            connection = new SalesforceConnection(configuration);

            Router r = new Router();

            r.addRoute("/async/job", new AsyncJobResourceProvider(connection));
            r.addRoute("/async/job/{jobId}/batch", new AsyncBatchResourceProvider(connection));
            r.addRoute("/async/job/{jobId}/batch/{batchId}/result",
                    new AsyncBatchResultResourceProvider(connection));

            router = Resources.newInternalConnection(r);
        }
    }

    @AfterClass
    public void afterClass() throws Exception {
        if (null != connection) {
            connection.dispose();
        }
    }

    @Test
    public void testFromJobInfo() throws Exception {
        InputStream in =
                AsyncJobResourceProviderTest.class
                        .getResourceAsStream("/async/jobinfo_response-01.xml");
        Assert.assertNotNull(in, "Failed to load 'jobinfo_response-01.xml'");

        XmlInputStream xin = new XmlInputStream();
        xin.setInput(in, "UTF-8");
        JobInfo jobInfo = new JobInfo();
        jobInfo.load(xin, BulkConnection.typeMapper);

        JsonValue result = AsyncJobResourceProvider.fromJobInfo(jobInfo);

        assertThat(result.asMap()).contains(entry("operation", "insert"),
                entry("_id", "750x0000000005LAAQ"));
    }

    @Test(enabled = false)
    public void testCreateInstance() throws Exception {
        if (null == connection) {
            throw new SkipException("Salesforce connection is not available");
        }

        // / JOB
        JsonValue jobRequest = new JsonValue(new HashMap<String, Object>());
        jobRequest.put("operation", OperationEnum.upsert.name());
        jobRequest.put("externalIdFieldName", "Id");

        CreateRequest createRequest = Requests.newCreateRequest("async/job", jobRequest);

        try {
            router.create(new RootContext(), createRequest);
            failBecauseExceptionWasNotThrown(ResourceException.class);
        } catch (BadRequestException e) {
            /* expected because required attribute is missing */
        }
        jobRequest.put("object", "Account");

        Resource job = router.create(new RootContext(), createRequest);
        System.out.println(job.getContent());
        Assert.assertNotNull(job.getId());
        assertThat(job.getContent().asMap()).containsKey("id");

        ReadRequest readRequest = Requests.newReadRequest("async/job", job.getId());
        job = router.read(new RootContext(), readRequest);
        assertThat(job.getContent().asMap()).containsKey("id");

        // / BATCH #1
        JsonValue batch = new JsonValue(new ArrayList<Map<String, Object>>());

        Map<String, Object> account = new HashMap<String, Object>();
        account.put("Name", "Bulk - Account1");
        account.put("Type", "Customer - Channel");
        account.put("BillingStreet", "345 Shoreline Park Mountain View, CA 94043 USA");
        account.put("AccountNumber", "CC978213");
        account.put("Description",
                "Self-described as \"\"the top\"\" branding guru on the West Coast");
        batch.add(account);

        createRequest = Requests.newCreateRequest("async/job/" + job.getId() + "/batch", batch);
        Resource batch1 = router.create(new RootContext(), createRequest);
        assertThat(batch1.getContent().asMap()).contains(
                entry("state", BatchStateEnum.Queued.name()));
        readRequest =
                Requests.newReadRequest("async/job/" + job.getId() + "/batch", batch1.getId());
        batch1 = router.read(new RootContext(), readRequest);

        // / BATCH #2
        batch = new JsonValue(new ArrayList<Map<String, Object>>());
        account = new HashMap<String, Object>();
        account.put("Name", "Bulk - Account2");
        account.put("Type", "Customer - Channel");
        account.put("BillingStreet", "345 Shoreline Park Mountain View, CA 94043 USA");
        account.put("AccountNumber", "CC978213");
        account.put("Description",
                "World-renowned expert in fuzzy logic design. Influential in technology purchases.");
        batch.add(account);

        createRequest = Requests.newCreateRequest("async/job/" + job.getId() + "/batch", batch);
        Resource batch2 = router.create(new RootContext(), createRequest);

        readRequest =
                Requests.newReadRequest("async/job/" + job.getId() + "/batch", batch2.getId());
        batch2 = router.read(new RootContext(), readRequest);

        QueryRequest queryRequest = Requests.newQueryRequest("async/job/" + job.getId() + "/batch");
        queryRequest.setQueryId(ServerConstants.QUERY_ALL_IDS);

        Collection<Resource> result = new ArrayList<Resource>();

        QueryResult qr = router.query(new RootContext(), queryRequest, result);

        // / Start JOB
        ActionRequest actionRequest = Requests.newActionRequest("async/job", job.getId(), "close");
        JsonValue response = router.action(new RootContext(), actionRequest);
        assertThat(response.asMap()).contains(entry("state", JobStateEnum.Closed.name()));

        awaitCompletion(router, job, batch1, batch2);

        queryRequest =
                Requests.newQueryRequest("async/job/" + job.getId() + "/batch/" + batch2.getId()
                        + "/result");
        queryRequest.setQueryId(ServerConstants.QUERY_ALL_IDS);

        result = new ArrayList<Resource>();

        qr = router.query(new RootContext(), queryRequest, result);

        /*
         * for (Resource batchResult : result) { readRequest =
         * Requests.newReadRequest("async/job/" + job.getId() + "/batch/" +
         * batch2.getId() + "/result", batchResult.getId()); Resource r =
         * router.read(new RootContext(), readRequest);
         * assertThat(r.getContent().asMap()).containsKey("_id"); }
         */

        // / Abort JOB
        actionRequest = Requests.newActionRequest("async/job", job.getId(), "abort");

        //response = router.action(new RootContext(), actionRequest);

        //assertThat(response.asMap()).contains(entry("state", JobStateEnum.Aborted.name()));

        // Close the Job

        actionRequest = Requests.newActionRequest("async/job", job.getId(), "close");
        try {
            router.action(new RootContext(), actionRequest);
            failBecauseExceptionWasNotThrown(ConflictException.class);
        } catch (ConflictException e) {
            //assertThat(e).hasMessage("InvalidJobState : Closing already aborted Job not allowed");
            assertThat(e).hasMessage("InvalidJobState : Closing already closed Job not allowed");
        }
    }

    private void awaitCompletion(Connection router, Resource job, Resource... batchInfoList)
            throws Exception {
        long sleepTime = 0L;
        Set<String> incomplete = new HashSet<String>();
        for (Resource bi : batchInfoList) {
            incomplete.add(bi.getId());
        }

        QueryRequest queryRequest = Requests.newQueryRequest("async/job/" + job.getId() + "/batch");
        queryRequest.setQueryId(ServerConstants.QUERY_ALL_IDS);

        int max = 10;

        while (!incomplete.isEmpty()) {
            if (max < 0) {
                throw new RuntimeException("Timed out");
            }
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
            }
            System.out.println("Awaiting results..." + incomplete.size());
            sleepTime = 2000L;

            Collection<Resource> statusList = new ArrayList<Resource>(2);
            router.query(new RootContext(), queryRequest, statusList);

            for (Resource b : statusList) {
                BatchStateEnum state = b.getContent().get("state").asEnum(BatchStateEnum.class);

                if (state == BatchStateEnum.Completed || state == BatchStateEnum.Failed) {
                    if (incomplete.remove(b.getId())) {
                        System.out.println("BATCH STATUS:\n" + b);
                    }
                }
            }
            max--;
        }
    }
}
