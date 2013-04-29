package org.forgerock.openidm.salesforce.internal.data;

import java.util.Map;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.JsonResourceException;
import org.forgerock.json.resource.SimpleJsonResource;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.salesforce.internal.SalesforceConnection;
import org.forgerock.openidm.salesforce.internal.ServerContext;
import org.restlet.Response;
import org.restlet.data.Status;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A NAME does ...
 * 
 * @author Laszlo Hordos
 */
public class GenericResourceProvider extends SimpleJsonResource {

    /**
     * Setup logging for the {@link SObjectsResourceProvider}
     */
    final static Logger logger = LoggerFactory.getLogger(GenericResourceProvider.class);

    private final SalesforceConnection connection;

    public GenericResourceProvider(final SalesforceConnection connection) {
        this.connection = connection;
    }

    @Override
    protected JsonValue read(JsonValue request) throws JsonResourceException {
        final ServerContext context = ServerContext.get();
        String type = context.getMatcher().group(1);
        String id = context.getMatcher().group(2);

        ClientResource rc = getClientResource(type, id);
        handleRequest(rc, true);
        Representation body = rc.getResponse().getEntity();
        if (null != body && body instanceof EmptyRepresentation == false) {
            JacksonRepresentation<Map> rep = new JacksonRepresentation<Map>(body, Map.class);
            JsonValue result = new JsonValue(rep.getObject());
            if (result.isDefined("Id")) {
                result.put(ServerConstants.OBJECT_PROPERTY_ID, result.get("Id").required()
                        .asString());
            }
            return result;
        } else {
            throw new JsonResourceException(JsonResourceException.NOT_FOUND);
        }
    }

    private ClientResource getClientResource(String type, String id) {
        StringBuilder sb =
                new StringBuilder("services/data/").append(connection.getVersion()).append('/')
                        .append(type).append(id);
        return connection.getChild(sb.toString());
    }

    protected void handleRequest(final ClientResource resource, boolean tryReauth)
            throws JsonResourceException {
        try {
            resource.handle();
        } catch (Exception e) {
            throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR, e);
        }
        final Response response = resource.getResponse();
        if (response.getStatus().isError()) {

            JsonResourceException e = connection.getJsonResourceException(resource);

            if (tryReauth && Status.CLIENT_ERROR_UNAUTHORIZED.equals(response.getStatus())) {
                // Re authenticate
                if (connection.refreshAccessToken(resource.getRequest())) {
                    handleRequest(resource, false);
                } else {
                    throw new JsonResourceException(401, "AccessToken can not be renewed");
                }
            } else {
                throw e;
            }
            // throw new ResourceException(response.getStatus());
        }

    }
}
