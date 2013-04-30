/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All Rights Reserved
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
 */

package org.forgerock.openidm.repo.dynamodb.internal;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.config.EnhancedConfig;
import org.forgerock.openidm.config.JSONEnhancedConfig;
import org.forgerock.openidm.core.ServerConstants;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.internal.StaticCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeAction;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest;
import com.amazonaws.services.dynamodbv2.model.DeleteItemResult;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.ExpectedAttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.InternalServerErrorException;
import com.amazonaws.services.dynamodbv2.model.ItemCollectionSizeLimitExceededException;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.LimitExceededException;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughputExceededException;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.model.TableStatus;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateItemResult;

/**
 * A NAME does ...
 * 
 * @author Laszlo Hordos
 * @see <a
 *      href="http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/LowLevelJavaItemCRUD.html">AWS
 *      SDK for Java Low-Level API</a>
 */
@Component(name = DynamoDBRepoService.PID, immediate = true, policy = ConfigurationPolicy.REQUIRE,
        enabled = true)
@Service
@Properties({ @Property(name = Constants.SERVICE_DESCRIPTION, value = DynamoDBRepoService.NAME),
    @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
    @Property(name = ServerConstants.ROUTER_PREFIX, value = "/repo/*"),
    @Property(name = "db.type", value = "DynamoDB") })
public class DynamoDBRepoService implements RequestHandler {

    public static final String PID = "org.forgerock.openidm.repo.dynamodb";
    public static final String NAME = "Repository Service using DynamoDB";

    /**
     * Setup logging for the {@link DynamoDBRepoService}.
     */
    final static Logger logger = LoggerFactory.getLogger(DynamoDBRepoService.class);

    private AmazonDynamoDBClient client = null;

    @Activate
    void activate(ComponentContext context) throws Exception {
        logger.debug("Activating {}", context);

        EnhancedConfig config = JSONEnhancedConfig.newInstance();

        String factoryPid = config.getConfigurationFactoryPid(context);
        if (StringUtils.isNotBlank(factoryPid)) {
            throw new IllegalArgumentException("Configuration must have property: "
                    + ServerConstants.CONFIG_FACTORY_PID);
        }

        JsonValue configuration = config.getConfigurationAsJson(context);

        AWSCredentialsProvider credentials =
                new AWSCredentialsProviderChain(new StaticCredentialsProvider(
                        new BasicAWSCredentials(configuration.get("accessKey").asString(),
                                configuration.get("secretKey").asString())),
                        new InstanceProfileCredentialsProvider());

        ObjectMapper mapper = new ObjectMapper();
        mapper.getDeserializationConfig().set(
                DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        client =
                new AmazonDynamoDBClient(credentials, mapper.convertValue(configuration.asMap(),
                        ClientConfiguration.class));
        // client.setEndpoint("https://dynamodb.us-east-1.amazonaws.com");

        Region usWest2 = Region.getRegion(Regions.US_WEST_2);
        client.setRegion(usWest2);

        logger.info("Repository started.");
    }

    /**
     * Handle an existing activated service getting changed; e.g. configuration
     * changes or dependency changes
     * 
     * @param context
     *            THe OSGi component context
     * @throws Exception
     *             if handling the modified event failed
     */
    @Modified
    void modified(ComponentContext context) throws Exception {

    }

    @Deactivate
    void deactivate(ComponentContext context) throws Exception {
        logger.debug("Deactivating {}", NAME);
        client.shutdown();
    }

    @Override
    public void handleAction(final ServerContext context, final ActionRequest request,
            final ResultHandler<JsonValue> handler) {
        try {

        } catch (Throwable t) {
            handler.handleError(adapt(t));
        }
    }

    @Override
    public void handleCreate(final ServerContext context, final CreateRequest request,
            final ResultHandler<Resource> handler) {
        try {

        } catch (Throwable t) {
            handler.handleError(adapt(t));
        }
    }

    @Override
    public void handleDelete(final ServerContext context, final DeleteRequest request,
            final ResultHandler<Resource> handler) {
        try {

            String tableName = null;

            Map<String, ExpectedAttributeValue> expectedValues =
                    new HashMap<String, ExpectedAttributeValue>();
            Map<String, AttributeValue> key = new HashMap<String, AttributeValue>();
            key.put("Id", new AttributeValue().withS(request.getResourceName()));

            expectedValues.put("Version", new ExpectedAttributeValue()
                    .withValue(new AttributeValue().withN(request.getRevision())));

            ReturnValue returnValues = ReturnValue.ALL_OLD;

            DeleteItemRequest deleteItemRequest =
                    new DeleteItemRequest().withTableName(tableName).withKey(key).withExpected(
                            expectedValues).withReturnValues(returnValues);

            DeleteItemResult result = client.deleteItem(deleteItemRequest);

            //Build response resource
            handler.handleResult(fromResultItem(result.getAttributes()));

        } catch (Throwable t) {
            handler.handleError(adapt(t));
        }
    }

    @Override
    public void handlePatch(final ServerContext context, final PatchRequest request,
            final ResultHandler<Resource> handler) {
        try {

            String tableName = null;

            Map<String, AttributeValueUpdate> updateItems =
                    new HashMap<String, AttributeValueUpdate>();

            HashMap<String, AttributeValue> key = new HashMap<String, AttributeValue>();
            key.put("Id", new AttributeValue().withN("101"));

            // Add two new authors to the list.
            updateItems.put("Authors", new AttributeValueUpdate().withAction(AttributeAction.ADD)
                    .withValue(new AttributeValue().withSS("AuthorYY", "AuthorZZ")));

            // Reduce the price. To add or subtract a value,
            // use ADD with a positive or negative number.
            updateItems.put("Price", new AttributeValueUpdate().withAction(AttributeAction.ADD)
                    .withValue(new AttributeValue().withN("-1")));

            // Delete the ISBN attribute.
            updateItems.put("ISBN", new AttributeValueUpdate().withAction(AttributeAction.DELETE));

            UpdateItemRequest updateItemRequest =
                    new UpdateItemRequest().withTableName(tableName).withKey(key).withReturnValues(
                            ReturnValue.UPDATED_NEW).withAttributeUpdates(updateItems);

            UpdateItemResult result = client.updateItem(updateItemRequest);

            //Build response resource
            handler.handleResult(fromResultItem(result.getAttributes()));

        } catch (Throwable t) {
            handler.handleError(adapt(t));
        }
    }

    @Override
    public void handleQuery(final ServerContext context, final QueryRequest request,
            final QueryResultHandler handler) {
        try {

            String replyId = null;

            long twoWeeksAgoMilli = (new Date()).getTime() -
            (15L*24L*60L*60L*1000L);
            Date twoWeeksAgo = new Date();
            twoWeeksAgo.setTime(twoWeeksAgoMilli);
            SimpleDateFormat df = new
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            String twoWeeksAgoStr = df.format(twoWeeksAgo);

            Map<String, AttributeValue> lastEvaluatedKey = null;
            String tableName = null;
            do {

                Condition hashKeyCondition =
                        new Condition().withComparisonOperator(ComparisonOperator.EQ.toString())
                                .withAttributeValueList(new AttributeValue().withS(replyId));

                Condition rangeKeyCondition =
                        new Condition().withComparisonOperator(ComparisonOperator.GT.toString())
                                .withAttributeValueList(new AttributeValue().withS(twoWeeksAgoStr));

                Map<String, Condition> keyConditions = new HashMap<String, Condition>();
                keyConditions.put("Id", hashKeyCondition);
                keyConditions.put("ReplyDateTime", rangeKeyCondition);

                com.amazonaws.services.dynamodbv2.model.QueryRequest queryRequest =
                        new com.amazonaws.services.dynamodbv2.model.QueryRequest().withTableName(
                                tableName).withKeyConditions(keyConditions).withAttributesToGet(
                                Arrays.asList("Message", "ReplyDateTime", "PostedBy")).withLimit(1)
                                .withExclusiveStartKey(lastEvaluatedKey);

                com.amazonaws.services.dynamodbv2.model.QueryResult result =
                        client.query(queryRequest);
                for (Map<String, AttributeValue> item : result.getItems()) {
                    // printItem(item);

                    //Build response resource
                    handler.handleResource(fromResultItem(item));
                }

                lastEvaluatedKey = result.getLastEvaluatedKey();
            } while (lastEvaluatedKey != null);

            handler.handleResult(new QueryResult());

        } catch (Throwable t) {
            handler.handleError(adapt(t));
        }
    }

    @Override
    public void handleRead(final ServerContext context, final ReadRequest request,
            final ResultHandler<Resource> handler) {
        try {

            String id = null;
            String tableName = null;

            Map<String, AttributeValue> key = new HashMap<String, AttributeValue>();
            key.put("Id", new AttributeValue().withN(id));

            GetItemRequest getItemRequest =
                    new GetItemRequest().withTableName(tableName).withKey(key).withAttributesToGet(
                            Arrays.asList("Id", "ISBN", "Title", "Authors"));

            GetItemResult result = client.getItem(getItemRequest);

            //Build response resource
            handler.handleResult(fromResultItem(result.getItem()));

        } catch (Throwable t) {
            handler.handleError(adapt(t));
        }
    }

    @Override
    public void handleUpdate(final ServerContext context, final UpdateRequest request,
            final ResultHandler<Resource> handler) {
        try {

        } catch (Throwable t) {
            handler.handleError(adapt(t));
        }
    }

    // /////

    private void createTable(String tableName) {
        // Create a table with a primary hash key named 'name', which holds a
        // string
        CreateTableRequest createTableRequest =
                new CreateTableRequest().withTableName(tableName).withKeySchema(
                        new KeySchemaElement().withAttributeName("name").withKeyType(KeyType.HASH))
                        .withAttributeDefinitions(
                                new AttributeDefinition().withAttributeName("name")
                                        .withAttributeType(ScalarAttributeType.S))
                        .withProvisionedThroughput(
                                new ProvisionedThroughput().withReadCapacityUnits(10L)
                                        .withWriteCapacityUnits(5L));
        TableDescription createdTableDescription =
                client.createTable(createTableRequest).getTableDescription();
        logger.debug("Created Table: {}", createdTableDescription);

        // Wait for it to become active
        waitForTableToBecomeAvailable(tableName);

        // Describe our new table
        DescribeTableRequest describeTableRequest =
                new DescribeTableRequest().withTableName(tableName);
        TableDescription tableDescription = client.describeTable(describeTableRequest).getTable();
        logger.debug("Table Description: {}", tableDescription);
    }

    private void waitForTableToBecomeAvailable(String tableName) {
        logger.debug("Waiting for {} to become ACTIVE...",tableName);

        long startTime = System.currentTimeMillis();
        long endTime = startTime + (10 * 60 * 1000);
        while (System.currentTimeMillis() < endTime) {
            try {
                Thread.sleep(1000 * 20);
            } catch (Exception e) {
            }
            try {
                DescribeTableRequest request = new DescribeTableRequest().withTableName(tableName);
                TableDescription tableDescription = client.describeTable(request).getTable();
                String tableStatus = tableDescription.getTableStatus();
                logger.trace("  - current state: {}", tableStatus);
                if (tableStatus.equals(TableStatus.ACTIVE.toString()))
                    return;
            } catch (AmazonServiceException ase) {
                if (ase.getErrorCode().equalsIgnoreCase("ResourceNotFoundException") == false)
                    throw ase;
            }
        }

        throw new RuntimeException("Table " + tableName + " never went active");
    }

    public static ResourceException adapt(final Throwable t) {
        int resourceResultCode;
        JsonValue detail = null;
        try {
            throw t;
        } catch (final AmazonServiceException ase) {
            detail = new JsonValue(new LinkedHashMap<String, Object>(5));
            detail.put("errorType", ase.getErrorType().name());
            detail.put("errorCode", ase.getErrorCode());
            detail.put("statusCode", ase.getStatusCode());
            detail.put("serviceName", ase.getServiceName());
            detail.put("requestId", ase.getRequestId());

            if (ase instanceof ResourceNotFoundException) {
            }
            if (ase instanceof ResourceInUseException) {
            }
            if (ase instanceof ProvisionedThroughputExceededException) {
            }
            if (ase instanceof LimitExceededException) {
            }
            if (ase instanceof ItemCollectionSizeLimitExceededException) {
            }
            if (ase instanceof InternalServerErrorException) {
            }
            if (ase instanceof ConditionalCheckFailedException) {
            }

            resourceResultCode = ase.getStatusCode();

            System.out
                    .println("Caught an AmazonServiceException, which means your request made it "
                            + "to AWS, but was rejected with an error response for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());

        } catch (final AmazonClientException ace) {
            System.out
                    .println("Caught an AmazonClientException, which means the client encountered "
                            + "a serious internal problem while trying to communicate with AWS, "
                            + "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());

            resourceResultCode = ResourceException.INTERNAL_ERROR;
        } catch (final ResourceException e) {
            return e;
        } catch (final JsonValueException e) {
            resourceResultCode = ResourceException.BAD_REQUEST;
        } catch (final Throwable tmp) {
            resourceResultCode = ResourceException.INTERNAL_ERROR;
        }
        return ResourceException.getException(resourceResultCode, t.getMessage(), t).setDetail(
                detail);
    }


    private Resource fromResultItem(Map<String,AttributeValue> resultItem) {
        return null;
    }
}
