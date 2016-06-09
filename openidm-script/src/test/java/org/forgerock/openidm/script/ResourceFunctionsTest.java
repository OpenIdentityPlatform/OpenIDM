package org.forgerock.openidm.script;

import org.forgerock.script.Script;
import org.forgerock.script.ScriptEntry;
import org.forgerock.script.ScriptName;
import org.forgerock.script.ScriptRegistry;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.MemoryBackend;
import org.forgerock.json.resource.PatchOperation;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.Responses;
import org.forgerock.json.resource.Router;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.services.context.SecurityContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.script.registry.ScriptRegistryImpl;
import org.forgerock.script.scope.Function;
import org.forgerock.script.source.DirectoryContainer;
import org.forgerock.script.source.EmbeddedScriptSource;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Router.uriTemplate;
import static org.forgerock.openidm.script.ResourceFunctions.resourceFunctions;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * Created by brmiller on 11/2/15.
 */
public abstract class ResourceFunctionsTest {

    private ScriptRegistryImpl scriptRegistry = null;

    ConnectionFactory connectionFactory = null;

    protected abstract Map<String, Object> getConfiguration();

    protected abstract String getLanguageName();

    protected abstract URL getScriptContainer(String name);

    protected abstract ScriptRegistryImpl getScriptRegistry(Map<String, Object> configuration);

    @BeforeClass
    public void initScriptRegistry() throws Exception {
        Map<String, Object> configuration = new HashMap<>(1);
        configuration.put(getLanguageName(), getConfiguration());

        scriptRegistry = getScriptRegistry(configuration);

        RequestHandler resource = mock(RequestHandler.class);
        SingletonResourceProvider singletonProvider = mock(SingletonResourceProvider.class);

        final Router router = new Router();
        router.addRoute(uriTemplate("/Users"), new MemoryBackend());
        router.addRoute(uriTemplate("/Groups"), new MemoryBackend());
        router.addRoute(uriTemplate("mock/{id}"), singletonProvider);

        connectionFactory = Resources.newInternalConnectionFactory(router);

        doAnswer(new Answer<Promise<ResourceResponse,ResourceException>>() {
            public Promise<ResourceResponse,ResourceException> answer(InvocationOnMock invocation) throws Throwable {
                ReadRequest request = (ReadRequest) invocation.getArguments()[1];
                return Promises.newResultPromise(
                        Responses.newResourceResponse(request.getResourcePath(), "1", json(object())));
            }
        }).when(resource).handleRead(any(Context.class), any(ReadRequest.class));

        scriptRegistry.put("router", resourceFunctions(connectionFactory));

        URL container = getScriptContainer("/container/");
        Assert.assertNotNull(container);

        scriptRegistry.addSourceUnit(new DirectoryContainer("container", container));
        scriptRegistry.addSourceUnit(new EmbeddedScriptSource(ScriptEntry.Visibility.PUBLIC,
                "egy = egy + 2;egy", new ScriptName("test1", getLanguageName())));

    }

    public ScriptRegistry getScriptRegistry() {
        return scriptRegistry;
    }

    @Test
    public void testResource() throws Exception {
        ScriptName scriptName = new ScriptName("resource", getLanguageName());
        ScriptEntry scriptEntry = getScriptRegistry().takeScript(scriptName);
        Assert.assertNotNull(scriptEntry);

        Script script = scriptEntry.getScript(new RootContext());
        // Set RequestLevel Scope
        script.put("ketto", 2);
        script.putSafe("callback", mock(Function.class));

        JsonValue createContent = new JsonValue(new LinkedHashMap<String, Object>());
        createContent.put("externalId", "701984");
        createContent.put("userName", "bjensen@example.com");
        createContent.put("assignedDashboard", Arrays.asList("Salesforce", "Google", "ConstantContact"));
        createContent.put("displayName", "Babs Jensen");
        createContent.put("nickName", "Babs");

        JsonValue updateContent = createContent.copy();
        updateContent.put("_id", UUID.randomUUID().toString());
        updateContent.put("profileUrl", "https://login.example.com/bjensen");

        final Context context = new SecurityContext(new RootContext(), "bjensen@example.com", null);
        script.put("context", context);

        CreateRequest createRequest = Requests.newCreateRequest("/Users", "701984", createContent);
        script.put("createRequest", createRequest);
        ReadRequest readRequest = Requests.newReadRequest("/Users/701984");
        script.put("readRequest", readRequest);
        UpdateRequest updateRequest = Requests.newUpdateRequest("/Users/701984", updateContent);
        script.put("updateRequest", updateRequest);
        PatchRequest patchRequest = Requests.newPatchRequest("/Users/701984",
                PatchOperation.replace("userName", "ddoe"));
        script.put("patchRequest", patchRequest);
        QueryRequest queryRequest = Requests.newQueryRequest("/Users/");
        script.put("queryRequest", queryRequest);
        DeleteRequest deleteRequest = Requests.newDeleteRequest("/Users/701984");
        script.put("deleteRequest", deleteRequest);
        ActionRequest actionRequest = Requests.newActionRequest("/Users", "clear");
        script.put("actionRequest", actionRequest);
        script.eval();
    }
}
