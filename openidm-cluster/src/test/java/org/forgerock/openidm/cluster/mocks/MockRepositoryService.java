package org.forgerock.openidm.cluster.mocks;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.openidm.util.ResourceUtil.notSupported;

import java.util.ArrayList;
import java.util.List;

import org.forgerock.http.Context;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Responses;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.cluster.ClusterManager;
import org.forgerock.openidm.cluster.InstanceState;
import org.forgerock.openidm.repo.RepositoryService;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;

/**
 * A mock {@link RepositoryService} and {@link RequestHandler} used for testing.
 */
public class MockRepositoryService implements RepositoryService, RequestHandler {

	private JsonValue repoMap = null;

	public MockRepositoryService() {
		this.repoMap = json(object(
                field("cluster", object(
                        field("states", object())))));
	}

	@Override
	public ResourceResponse create(CreateRequest request)
			throws ResourceException {
		JsonValue newContent = request.getContent().copy();
		newContent.put("_id", request.getNewResourceId());
		newContent.put("_rev", null);
		repoMap.put(new JsonPointer(request.getResourcePath() + "/" + request.getNewResourceId()), newContent.getObject());
		return newResourceResponse(request.getResourcePath(), null, newContent);
	}

	@Override
	public ResourceResponse read(ReadRequest request)
			throws ResourceException {
		JsonValue value = repoMap.get(new JsonPointer(request.getResourcePath()));
		if (value != null) {
			return newResourceResponse(request.getResourcePath(), null, value);
		} else {
			throw new NotFoundException("Object not found");
		}
	}

	@Override
	public ResourceResponse update(UpdateRequest request)
			throws ResourceException {
		repoMap.put(new JsonPointer(request.getResourcePath()), request.getContent().getObject());
		return newResourceResponse(request.getResourcePath(), null, repoMap.get(new JsonPointer(request.getResourcePath())));
	}

	@Override
	public ResourceResponse delete(DeleteRequest request)
			throws ResourceException {
		JsonValue content = repoMap.get(new JsonPointer(request.getResourcePath()));
		repoMap.remove(new JsonPointer(request.getResourcePath()));
		return newResourceResponse(request.getResourcePath(), null, content);
	}

	@Override
	public List<ResourceResponse> query(QueryRequest request)
			throws ResourceException {
		String queryId = request.getQueryId();
		if (queryId.equals(ClusterManager.QUERY_INSTANCES)) {
			List<ResourceResponse> instances = new ArrayList<ResourceResponse>();
			JsonValue states = repoMap.get(new JsonPointer("cluster/states"));
			for (String key : states.keys()) {
				instances.add(newResourceResponse(request.getResourcePath() + "/" + key, null, states.get(key)));
			}
			return instances;
		} else if (queryId.equals(ClusterManager.QUERY_FAILED_INSTANCE)) {
			List<ResourceResponse> failedInstances = new ArrayList<ResourceResponse>();
			JsonValue states = repoMap.get(new JsonPointer("cluster/states"));
			for (String key : states.keys()) {
				JsonValue instance = states.get(key);
				InstanceState instanceState = new InstanceState(key, instance.asMap());
				if (instanceState.hasFailed(3000)) {
					failedInstances.add(newResourceResponse(request.getResourcePath() + "/" + key, null, instance));
				}
			}
			return failedInstances;
		} else if (queryId.equals(ClusterManager.QUERY_EVENTS)) {
			return new ArrayList<ResourceResponse>();
		}
		return null;
	}
	
	// RequestHandler methods

	@Override
	public Promise<ResourceResponse, ResourceException> handleRead(
			Context context, ReadRequest request) {
		String path = request.getResourcePath();
		String newPath = path.substring(path.indexOf("/") + 1);
		ResourceResponse response;
		try {
			response = read(Requests.copyOfReadRequest(request).setResourcePath(newPath));
		} catch (ResourceException e) {
			return e.asPromise();
		}
		return Promises.newResultPromise(response);
	}

	@Override
	public Promise<QueryResponse, ResourceException> handleQuery(
			Context context, QueryRequest request, QueryResourceHandler handler) {
		String path = request.getResourcePath();
		String newPath = path.substring(path.indexOf("/") + 1);
		List<ResourceResponse> responses;
		try {
			responses = query(Requests.copyOfQueryRequest(request).setResourcePath(newPath));
			for (ResourceResponse response : responses) {
				handler.handleResource(response);
			}
		} catch (ResourceException e) {
			return e.asPromise();
		}
		return Promises.newResultPromise(Responses.newQueryResponse());
	}

	@Override
	public Promise<ActionResponse, ResourceException> handleAction(
			Context context, ActionRequest request) {
		return notSupported(request).asPromise();
	}

	@Override
	public Promise<ResourceResponse, ResourceException> handleCreate(
			Context context, CreateRequest request) {
		return notSupported(request).asPromise();
	}

	@Override
	public Promise<ResourceResponse, ResourceException> handleDelete(
			Context context, DeleteRequest request) {
		return notSupported(request).asPromise();
	}

	@Override
	public Promise<ResourceResponse, ResourceException> handlePatch(
			Context context, PatchRequest request) {
		return notSupported(request).asPromise();
	}

	@Override
	public Promise<ResourceResponse, ResourceException> handleUpdate(
			Context context, UpdateRequest request) {
		return notSupported(request).asPromise();
	}
}
