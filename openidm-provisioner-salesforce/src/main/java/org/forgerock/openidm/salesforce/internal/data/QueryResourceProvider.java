package org.forgerock.openidm.salesforce.internal.data;

import java.util.Map;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.JsonResourceException;
import org.forgerock.json.resource.SimpleJsonResource;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.salesforce.internal.ResultHandler;
import org.forgerock.openidm.salesforce.internal.SalesforceConnection;
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
public class QueryResourceProvider extends SimpleJsonResource {

    /**
     * Setup logging for the {@link SObjectsResourceProvider}
     */
    final static Logger logger = LoggerFactory.getLogger(GenericResourceProvider.class);

    private final SalesforceConnection connection;

    public QueryResourceProvider(SalesforceConnection connection) {
        this.connection = connection;
    }

    @Override
    protected JsonValue query(JsonValue request) throws JsonResourceException {
        String queryExpression =
                request.get("params").get("_queryExpression").required().asString();

        StringBuilder sb =
                new StringBuilder("services/data/").append(connection.getVersion()).append('/')
                        .append("query");

        ClientResource rc = connection.getChild(sb.toString());

        rc.getReference().addQueryParameter("q", queryExpression);
        rc.setMethod(org.restlet.data.Method.GET);
        logger.debug("Attempt to execute query: {}?{}", rc.getReference(), rc.getReference()
                .getQuery());
        handleRequest(rc, true);
        Representation body = rc.getResponse().getEntity();

        ResultHandler handler = new ResultHandler();

        if (null != body && body instanceof EmptyRepresentation == false) {
            JacksonRepresentation<Map> rep = new JacksonRepresentation<Map>(body, Map.class);
            JsonValue result = new JsonValue(rep.getObject());
            for (JsonValue record : result.get("records")) {
                if (record.isDefined("Id")) {
                    record.put(ServerConstants.OBJECT_PROPERTY_ID, record.get("Id").asString());
                }
                handler.handleResource(record);
            }
        }

        return handler.getResult();
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
