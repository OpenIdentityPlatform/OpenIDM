/*
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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openidm.restlet;

// Restlet Framework
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.Variant;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

/**
 * TODO: Description.
 *
 * @author Paul C. Bryan
 * @see <a href="http://tools.ietf.org/html/rfc5789">HTTP PATCH</a>.
 */
public abstract class ExtendedServerResource extends ServerResource {

    /** The PATCH method. */
    public static final Method PATCH = new Method("PATCH",
     "Requests a partial modification be applied to a resource",
     "http://tools.ietf.org/html/rfc5789", false, true);

    public static final MediaType APPLICATION_PATCH_JSON =
     MediaType.register("application/patch+json", "JSON patch document");

    // not sure yet this is the best place to do this...
    static {
        Method.register(PATCH);
    }

    @Override
    protected Representation doHandle() throws ResourceException {
        Representation result = null;
        Method method = getMethod();
        if (PATCH.equals(method)) {
            result = patch(getRequestEntity());
        }
        else {
            result = super.doHandle();
        }
        return result;
    }

    @Override
    protected Representation doHandle(Variant variant) throws ResourceException {
        Representation result = null;
        Method method = getMethod();
        if (PATCH.equals(method)) {
            if (isExisting()) {
                result = patch(getRequestEntity(), variant);
            }
            else {
                setStatus(Status.CLIENT_ERROR_NOT_FOUND);
            }
        }
        else {
            result = super.doHandle(variant);
        }
        return result;
    }

    /**
     * Applies a partial modification document to the resource. This method is only invoked
     * if content negotiation has been disabled as indicated by {@code isNegotiated()},
     * otherwise the {@link #patch(Representation, Variant)} method is invoked.
     * <p>
     * This method is intended to be overridden by a subclass. The default behavior is to set
     * the response status to {@link Status#CLIENT_ERROR_METHOD_NOT_ALLOWED}.
     *
     * @param entity representation of the patch document to apply to the resource.
     * @throws ResourceException TODO.
     */
    protected Representation patch(Representation entity) throws ResourceException {
        setStatus(Status.CLIENT_ERROR_METHOD_NOT_ALLOWED);
        return null;
    }

    /**
     * Applies a partial modification document to the resource. A variant parameter is passed
     * to indicate which representation should be returned if any. This method is only invoked
     * if content negotiation has been enabled as indicated by {@code isNegotiated()},
     * otherwise the {@link #patch(Representation)} method is invoked.
     * <p>
     * This method is intended to be overridden by a subclass. The default behavior is to set
     * the response status to {@link Status#CLIENT_ERROR_METHOD_NOT_ALLOWED}.
     *
     * @param entity representation of the patch document to apply to the resource.
     * @throws ResourceException TODO.
     */
    protected Representation patch(Representation entity, Variant variant) throws ResourceException {
        setStatus(Status.CLIENT_ERROR_METHOD_NOT_ALLOWED);
        return null;
    }
}
