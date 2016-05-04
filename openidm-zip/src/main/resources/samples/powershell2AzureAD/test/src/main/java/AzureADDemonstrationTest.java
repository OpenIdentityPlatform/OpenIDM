/**
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
 * Copyright 2016 ForgeRock AS.
 */

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.http.util.Json.readJson;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.forgerock.openidm.util.JsonUtil.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.BaseRequest;
import com.mashape.unirest.request.GetRequest;
import com.mashape.unirest.request.HttpRequestWithBody;
import org.forgerock.json.JsonValue;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * A Test that demonstrates and validates the Azure AD Powershell Connector.
 */
public class AzureADDemonstrationTest {
    private static final String IDM_ENDPOINT = "http://localhost:8080/openidm";
    private static final String AZURE_ENDPOINT = IDM_ENDPOINT + "/system/azureadpowershell";
    private static final String MANAGED_ENDPOINT = IDM_ENDPOINT + "/managed";
    private static final String AZURE_AD_DOMAIN = "openidm.onmicrosoft.com";
    private static final String TEST_PREFIX = "test_";

    @BeforeClass
    public void setup() {
        Unirest.setTimeouts(1000, 5000);
    }

    /**
     * Tests all the capabilities of the Azure AD Powershell Connector.
     *
     * @throws Exception
     */
    @Test
    public void demonstrateAzureConnector() throws Exception {
        // Validate that the connector is up and running.
        JsonValue jsonValue = validateConnector();
        assertThat(jsonValue).booleanAt("ok").isTrue();

        // Clean up test users from any previous run while demoing queryFilter of User
        JsonValue testUsers = getTestUsers();
        for (JsonValue user : testUsers.get("result")) {
            String id = user.get("_id").asString();
            deleteUser(id);
        }
        testUsers = getTestUsers();
        List<JsonValue> users = testUsers.get("result").asList(JsonValue.class);
        assertThat(users).isEmpty();

        // Clean up test groups from any previous run while demoing queryFilter of Group.
        JsonValue testGroups = getTestGroupsByFilter();
        for (JsonValue group : testGroups.get("result")) {
            String id = group.get("_id").asString();
            deleteGroup(id);
        }
        testGroups = getTestGroupsByFilter();
        List<JsonValue> groups = testGroups.get("result").asList(JsonValue.class);
        assertThat(groups).isEmpty();

        // Verify creating a user.
        String lastName = "Smith";
        String firstName = "Roger";
        String userPrincipalName = TEST_PREFIX + firstName + "." + lastName + "@" + AZURE_AD_DOMAIN;
        JsonValue smithUser = createTestUser(firstName, lastName,
                userPrincipalName);
        assertThat(smithUser).stringAt("_id").isNotEmpty();
        String smithId = smithUser.get("_id").asString();

        // Verify creating a user fails if duplicated.
        JsonValue smithUserDuplicate = createTestUser(firstName, lastName, userPrincipalName);
        assertThat(smithUserDuplicate).stringAt("reason").isEqualTo("Internal Server Error");

        // Verify get user by ID.
        JsonValue userById = getUserById(smithId);
        assertThat(userById).stringAt("_id").isEqualTo(smithId);
        assertThat(userById).stringAt("LastName").isEqualTo(lastName);

        // Verify patch of user
        JsonValue patchedUser = patchUser(smithId,
                json(
                        array(
                                object(
                                        field("operation", "replace"),
                                        field("field", "FirstName"),
                                        field("value", "ModifiedFirstName")
                                )
                        )
                ));
        assertThat(patchedUser).stringAt("FirstName").isEqualTo("ModifiedFirstName");
        assertThat(getUserById(smithId)).stringAt("FirstName").isEqualTo("ModifiedFirstName");

        // Create Group
        JsonValue testGroup = createTestGroup(TEST_PREFIX + "alpha group", "Alpha Group Description");
        assertThat(testGroup).stringAt("_id").isNotEmpty();
        String groupId = testGroup.get("_id").asString();

        // Add member to group.
        JsonValue updatedGroup = addMemberToGroupViaUpdate(groupId, smithId);
        assertThat(updatedGroup).hasArray("Members").hasSize(1);

        // Get group to see that the members are populated.
        JsonValue groupById = getGroupById(groupId);
        assertThat(groupById).hasArray("Members").hasSize(1);
        assertThat(groupById.get("Members").get(0)).stringAt("ObjectId").isEqualTo(smithId);

        // Update a Group's description.
        String description = groupById.get("Description").asString();
        updatedGroup.put("Description", description + "_updated");
        JsonValue updatedDescriptionGroup = updateGroup(groupId, updatedGroup);
        assertThat(updatedDescriptionGroup).stringAt("Description").isEqualTo(description + "_updated");

        // Remove member from group via group update. (get full group, remove member from array, update group)
        JsonValue updatedGroupNoMembers = removeMemberFromGroupViaUpdate(groupId, smithId);
        assertThat(updatedGroupNoMembers).hasArray("Members").isEmpty();
    }

    /**
     * Test that demonstrates user and group reconciliation using a sample Azure AD to OpenIDM mapping configuration.
     *
     * @throws Exception
     */
    @Test
    public void testManagedObjectRecon() throws Exception {
        // Create the test user on the target system.
        String userPrincipalName = "Big.Cheese@openidm.onmicrosoft.com";
        JsonValue testUser = createTestUser("Big", "Cheese", userPrincipalName);
        assertThat(testUser).stringAt("_id").isNotEmpty();
        assertThat(testUser).stringAt("UserPrincipalName").isEqualTo(userPrincipalName);
        String testSystemUserId = testUser.get("_id").asString();

        // Get managed user to show that it doesn't exist in IDM yet.
        JsonValue managedUserByUsername = getManagedUserByUsername(userPrincipalName);
        assertThat(managedUserByUsername).hasArray("result").isEmpty();

        // Call recon.
        JsonValue reconJson = invokeRecon("systemAzureadpowershellAccount_managedUser");
        assertThat(reconJson).stringAt("state").isEqualTo("SUCCESS");

        // Get managed user to show that it now exists in IDM
        JsonValue foundUser = getManagedUserByUsername(userPrincipalName);
        assertThat(foundUser).hasArray("result").hasSize(1);

        // Delete the user on the target system.
        JsonValue deletedSystemUser = deleteUser(testSystemUserId);
        assertThat(deletedSystemUser).stringAt("_id").isEqualTo(testSystemUserId);

        // Call recon.
        JsonValue newReconJson = invokeRecon("systemAzureadpowershellAccount_managedUser");
        assertThat(reconJson).stringAt("state").isEqualTo("SUCCESS");

        // Get managed user to show that it now no longer exists in IDM
        JsonValue notFoundUser = getManagedUserByUsername(userPrincipalName);
        assertThat(notFoundUser).hasArray("result").hasSize(0);
    }

    /**
     * Demostrates the REST call to validate that the connector is up and running.
     *
     * @return the json results
     * @throws Exception
     */
    private JsonValue validateConnector() throws Exception {
        System.out.println("1. Validate that the connection with the Azure connector is OK:");
        HttpResponse<String> response = Unirest.post(AZURE_ENDPOINT + "?_action=test")
                .header("x-openidm-username", "openidm-admin")
                .header("x-openidm-password", "openidm-admin")
                .asString();
        return json(readJson(response.getBody()));
    }

    /**
     * Demonstrates the rest call to search for users with a queryFilter
     *
     * @return the json response
     * @throws Exception
     */
    private JsonValue getTestUsers() throws Exception {
        System.out.println("1. Get users by query filter: ");
        HttpResponse<String> response = Unirest.get(
                AZURE_ENDPOINT + "/account?_queryFilter=UserPrincipalName%20sw%20%22" + TEST_PREFIX + "%22")
                .header("x-openidm-username", "openidm-admin")
                .header("x-openidm-password", "openidm-admin")
                .asString();
        return json(readJson(response.getBody()));
    }

    /**
     * Demonstrates the REST call to create a user by creating a test user account with the provided first and last
     * name.
     *
     * @param first first name of the account.
     * @param last last name of the account.
     * @return the json response
     * @throws Exception
     */
    private JsonValue createTestUser(String first, String last, String userPrincipalName) throws Exception {
        System.out.println("1. Create a user: ");
        HttpResponse<String> response = Unirest.post(AZURE_ENDPOINT + "/account?_action=create")
                .header("x-openidm-username", "openidm-admin")
                .header("x-openidm-password", "openidm-admin")
                .header("content-type", "application/json")
                .body(writeValueAsString(
                        json(
                                object(
                                        field("UserPrincipalName", userPrincipalName),
                                        field("LastName", last),
                                        field("FirstName", first),
                                        field("DisplayName", "Mr. " + last),
                                        field("PasswordNeverExpires", false)
                                )
                        )
                ))
                .asString();
        return json(readJson(response.getBody()));
    }

    /**
     * Demonstrates the REST call to retrieve a user by the ID provided.
     *
     * @param id user id to use to search for the user.
     * @return the json response
     * @throws Exception
     */
    private JsonValue getUserById(String id) throws Exception {
        System.out.println("1. Get User By ID: ");
        HttpResponse<String> response = Unirest.get(AZURE_ENDPOINT + "/account/" + id)
                .header("x-openidm-username", "openidm-admin")
                .header("x-openidm-password", "openidm-admin")
                .asString();
        return json(readJson(response.getBody()));
    }

    /**
     * Demonstrates the REST call to delete a user by the user id.
     *
     * @param id id of user to delete.
     * @return the json response
     * @throws Exception
     */
    private JsonValue deleteUser(String id) throws Exception {
        System.out.println("1. Delete a user: ");
        HttpResponse<String> response = Unirest.delete(AZURE_ENDPOINT + "/account/" + id)
                .header("x-openidm-username", "openidm-admin")
                .header("x-openidm-password", "openidm-admin")
                .asString();
        return json(readJson(response.getBody()));
    }

    /**
     * Demonstrates the REST call to patch a User.
     *
     * @param id the id of the user to patch
     * @param patchJson the patch instructions in json.
     * @return the json response
     * @throws Exception
     */
    private JsonValue patchUser(String id, JsonValue patchJson) throws Exception {
        System.out.println("1. Patch a user: ");
        HttpResponse<String> response = Unirest.patch(AZURE_ENDPOINT + "/account/" + id)
                .header("x-openidm-username", "openidm-admin")
                .header("x-openidm-password", "openidm-admin")
                .header("content-type", "application/json")
                .header("if-match", "*")
                .body(writeValueAsString(patchJson))
                .asString();
        return json(readJson(response.getBody()));
    }

    /**
     * Demonstrates the REST call to create a group.
     *
     * @param name group name
     * @param description group description
     * @return the json response
     * @throws Exception
     */
    private JsonValue createTestGroup(String name, String description) throws Exception {
        System.out.println("1. Create a group: ");
        HttpResponse<String> response = Unirest.post(AZURE_ENDPOINT + "/group?_action=create")
                .header("x-openidm-username", "openidm-admin")
                .header("x-openidm-password", "openidm-admin")
                .header("content-type", "application/json")
                .body(writeValueAsString(
                        json(
                                object(
                                        field("DisplayName", name),
                                        field("Description", description)
                                )
                        )
                ))
                .asString();
        return json(readJson(response.getBody()));
    }

    /**
     * Demonstrates retrieving groups by a query filter.
     *
     * @return the json response
     * @throws Exception
     */
    private JsonValue getTestGroupsByFilter() throws Exception {
        System.out.println("1. Get groups by filter: ");
        HttpResponse<String> response = Unirest.get(
                AZURE_ENDPOINT + "/group?_queryFilter=DisplayName%20sw%20%22" + TEST_PREFIX + "%22")
                .header("x-openidm-username", "openidm-admin")
                .header("x-openidm-password", "openidm-admin")
                .asString();
        return json(readJson(response.getBody()));
    }

    /**
     * Demonstrates the REST call to delete a group by the group id.
     *
     * @param id group id to delete.
     * @return the json response
     * @throws Exception
     */
    private JsonValue deleteGroup(String id) throws Exception {
        System.out.println("1. Delete a group:");
        HttpResponse<String> response = Unirest.delete(AZURE_ENDPOINT + "/group/" + id)
                .header("x-openidm-username", "openidm-admin")
                .header("x-openidm-password", "openidm-admin")
                .asString();
        return json(readJson(response.getBody()));
    }

    /**
     * Demonstrates the REST call to retrieve a group by the group id.
     *
     * @param id groupId to search for.
     * @return the json response
     * @throws Exception
     */
    private JsonValue getGroupById(String id) throws Exception {
        System.out.println("1. Get Group by ID:");
        HttpResponse<String> response = Unirest.get(AZURE_ENDPOINT + "/group/" + id)
                .header("x-openidm-username", "openidm-admin")
                .header("x-openidm-password", "openidm-admin")
                .asString();
        return json(readJson(response.getBody()));
    }

    /**
     * Demonstrates the REST call to make to add a member to a group.
     *
     * @param groupId group to receive the new member.
     * @param userId user to be the new member.
     * @return the json response
     * @throws Exception
     */
    private JsonValue addMemberToGroupViaUpdate(String groupId, String userId) throws Exception {
        System.out.println("1. Add a member to a group by writing entire group with updated Members list:");
        HttpResponse<String> response = Unirest.put(AZURE_ENDPOINT + "/group/" + groupId)
                .header("x-openidm-username", "openidm-admin")
                .header("x-openidm-password", "openidm-admin")
                .header("content-type", "application/json")
                .header("if-match", "*")
                .body(writeValueAsString(
                        json(
                                object(
                                        field("Members", array(object(field("ObjectId", userId))))
                                )
                        )
                ))
                .asString();
        return json(readJson(response.getBody()));
    }

    /**
     * Demonstrates updating a group with the full group json.
     *
     * @param groupId id of group to update.
     * @param groupJson new json for the group.
     * @return the json response
     * @throws Exception
     */
    private JsonValue updateGroup(String groupId, JsonValue groupJson) throws Exception {
        System.out.println("1. Update a group: ");
        HttpResponse<String> response = Unirest.put(AZURE_ENDPOINT + "/group/" + groupId)
                .header("x-openidm-username", "openidm-admin")
                .header("x-openidm-password", "openidm-admin")
                .header("content-type", "application/json")
                .header("if-match", "*")  // This is critical to signal an update versus a create.
                .body(writeValueAsString(groupJson))
                .asString();
        return json(readJson(response.getBody()));
    }

    /**
     * Demonstrates updating a group's member list by using update for a group. Read the Group, remove the member you
     * are looking for, then update the group.
     *
     * @param groupId id of group to patch
     * @param userId user id to remove from the group's member list.
     * @return the json response.
     * @throws Exception
     */
    private JsonValue removeMemberFromGroupViaUpdate(String groupId, String userId) throws Exception {
        System.out.println("1. Remove member from a group with update: (get, then update with modified members list)");
        JsonValue group = getGroupById(groupId);
        List<Map> members = group.get("Members").asList(Map.class);
        List<Map> updatedMembers = new ArrayList<>(members);
        for (Map member : members) {
            if (member.get("ObjectId").toString().equals(userId)) {
                updatedMembers.remove(member);
            }
        }

        group.put("Members", updatedMembers);

        return updateGroup(groupId, group);
    }

    /**
     * Uses a query filter to find the managed user with the provided username.
     *
     * @param username username to filter on.
     * @return the user json.
     * @throws Exception
     */
    private JsonValue getManagedUserByUsername(String username) throws Exception {
        System.out.println("1. Get Managed User by username");
        HttpResponse<String> response = Unirest.get(
                MANAGED_ENDPOINT + "/user?_queryFilter=userName%20eq%20%22" + username + "%22")
                .header("x-openidm-username", "openidm-admin")
                .header("x-openidm-password", "openidm-admin")
                .asString();
        return json(readJson(response.getBody()));
    }

    /**
     * Invokes recon on IDM.
     *
     * @param mappingName mapping name to perform the recon on.
     * @return the response json of the recon.
     * @throws Exception
     */
    private JsonValue invokeRecon(String mappingName) throws Exception {
        System.out.println("1. Invoke recon on openidm");
        HttpResponse<String> response = Unirest.post(
                IDM_ENDPOINT + "/recon?_action=recon&mapping=" + mappingName + "&waitForCompletion=true")
                .header("x-openidm-username", "openidm-admin")
                .header("x-openidm-password", "openidm-admin")
                .asString();
        return json(readJson(response.getBody()));
    }

    /*
     * The below classes and methods are wrappers to Unirest to generate pseudo documentation of the requests and
     * responses.
     */

    /**
     * Wrapper of {@link com.mashape.unirest.http.Unirest} to generate pseudo documentation of what is being called and
     * returned to stdout.  Uses the same classname to allow copy-paste from postman generated code.
     */
    private static class Unirest {
        private static void setTimeouts(long connectionTimeout, long socketTimeout) {
            com.mashape.unirest.http.Unirest.setTimeouts(connectionTimeout, socketTimeout);
        }

        private static HttpRequestWithBodyWrapper post(String url) {
            System.out.println();
            System.out.println("        curl --request POST \\");
            System.out.print("         --url '" + url + "' ");
            return new HttpRequestWithBodyWrapper(com.mashape.unirest.http.Unirest.post(url));
        }

        private static GetRequestWrapper get(String url) {
            System.out.println();
            System.out.println("        curl --request GET \\");
            System.out.print("         --url '" + url + "' ");
            return new GetRequestWrapper(com.mashape.unirest.http.Unirest.get(url));
        }

        private static HttpRequestWithBodyWrapper patch(String url) {
            System.out.println();
            System.out.println("        curl --request PATCH \\");
            System.out.print("         --url '" + url + "' ");
            return new HttpRequestWithBodyWrapper(com.mashape.unirest.http.Unirest.patch(url));
        }

        private static HttpRequestWithBodyWrapper put(String url) {
            System.out.println();
            System.out.println("        curl --request PUT \\");
            System.out.print("         --url '" + url + "' ");
            return new HttpRequestWithBodyWrapper(com.mashape.unirest.http.Unirest.put(url));
        }

        private static HttpRequestWithBodyWrapper delete(String url) {
            System.out.println();
            System.out.println("        curl --request DELETE \\");
            System.out.print("         --url '" + url + "' ");
            return new HttpRequestWithBodyWrapper(com.mashape.unirest.http.Unirest.delete(url));
        }
    }

    /**
     * Wrapper class to {@link HttpRequestWithBody} that outputs the request information to stdout.
     */
    private static class HttpRequestWithBodyWrapper {
        private HttpRequestWithBody requestWithBody;

        private HttpRequestWithBodyWrapper(HttpRequestWithBody requestWithBody) {
            this.requestWithBody = requestWithBody;
        }

        private HttpRequestWithBodyWrapper header(String name, String value) {
            System.out.print("\\\n         --header '" + name + ": " + value + "' ");
            requestWithBody.header(name, value);
            return this;
        }

        private RequestBodyEntityWrapper body(String payload) throws IOException {
            System.out.println("\\\n         --data '" + writeAsStringNoEolSlash(json(readJson(payload))) + "'");
            return new RequestBodyEntityWrapper(requestWithBody.body(payload));
        }

        private HttpResponse<String> asString() throws Exception {
            return printResponseAsString(requestWithBody);
        }
    }

    /**
     * Wrapper class for RequestBodyEntity that will output the response to stdout.
     */
    private static class RequestBodyEntityWrapper {

        private BaseRequest requestBodyEntity;

        public RequestBodyEntityWrapper(BaseRequest requestBodyEntity) {
            this.requestBodyEntity = requestBodyEntity;
        }

        public HttpResponse<String> asString() throws IOException, UnirestException {
            return printResponseAsString(requestBodyEntity);
        }
    }

    /**
     * Wrapper class to {@link GetRequest} that outputs the request information to stdout.
     */
    private static class GetRequestWrapper {
        private GetRequest getRequest;

        private GetRequestWrapper(GetRequest getRequest) {
            this.getRequest = getRequest;
        }

        private GetRequestWrapper header(String name, String value) {
            System.out.print("\\\n         --header '" + name + ": " + value + "' ");
            getRequest.header(name, value);
            return this;
        }

        private HttpResponse<String> asString() throws Exception {
            return printResponseAsString(getRequest);
        }
    }

    /**
     * Invokes the request and writes the json using pretty print to stdout.
     *
     * @param request the request to invoke.
     * @return the response to the request.
     * @throws UnirestException
     * @throws IOException
     * @see org.forgerock.openidm.util.JsonUtil#writePrettyValueAsString(JsonValue)
     */
    private static HttpResponse<String> printResponseAsString(BaseRequest request)
            throws UnirestException, IOException {
        HttpResponse<String> response = request.asString();
        System.out.println();
        System.out.println("    Returns:");
        System.out.println();
        System.out.println(writePrettyValueAsString(json(readJson(response.getBody())))
                .replaceAll("\\n", "\n        ")
                .replaceFirst("\\{", "        {"));
        System.out.println();
        return response;
    }

    /**
     * Converts the json to pretty string, and then makes the string copy-paste compatible for unix cmd line.
     *
     * @param json the json to convert.
     * @return the formatted json.
     * @throws JsonProcessingException
     */
    private static String writeAsString(JsonValue json) throws JsonProcessingException {
        return writePrettyValueAsString(json).replaceAll("\\n", " \\\\\n         ");
    }

    /**
     * Converts the json to pretty string, the string will be copy-paste compatible for unix cmd line for when
     * the content is contained in quotes, ie no end-of-line slash is needed.
     *
     * @param json the json to convert.
     * @return the formatted json.
     * @throws JsonProcessingException
     */
    private static String writeAsStringNoEolSlash(JsonValue json) throws JsonProcessingException {
        return writePrettyValueAsString(json).replaceAll("\\n", " \\\n         ");
    }

}

