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
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.openidm.auth;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.http.Filter;
import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.osgi.service.component.ComponentContext;

/**
 * A CHF {@link Filter} to wrap the {@link org.forgerock.caf.authentication.framework.AuthenticationFilter},
 * so that config changes may swap out the CAF AuthenticationFilter without disturbing the CHF
 * {@link org.forgerock.json.resource.FilterChain}.
 */
@Component(name = AuthFilterWrapper.PID, policy = ConfigurationPolicy.IGNORE,
        configurationFactory = false, immediate = true)
@Service(value = { Filter.class, AuthFilterWrapper.class })
public class AuthFilterWrapper implements Filter {
    public static final String PID = "org.forgerock.openidm.auth.config";

    /** Null Object Filter to forward request on when authentication filter is not configured. */
    private static final Filter PASSTHROUGH_FILTER = new Filter() {
        @Override
        public Promise<Response, NeverThrowsException> filter(Context context, Request request, Handler next) {
            return next.handle(context, request);
        }
    };

    private volatile Filter filter;

    /**
     * Configures the commons Authentication Filter with a passthrough filter to start.
     *
     * @param context The ComponentContext.
     */
    @Activate
    protected void activate(ComponentContext context) {
        reset();
    }

    @Deactivate
    protected synchronized void deactivate(ComponentContext context) {
        reset();
    }

    /**
     * Set the wrapped filter to the given {@link Filter}.  Used when the CAF
     * {@link org.forgerock.caf.authentication.framework.AuthenticationFilter} has been initialized.
     *
     * @param filter the auth filter to wrap
     */
    synchronized void setFilter(Filter filter) {
        if (filter == null) {
            this.filter = PASSTHROUGH_FILTER;
        }
        this.filter = filter;
    }

    /**
     * Reset the wrapped filter to pass-through; i.e., when the CAF filter becomes unavailable or is in the process of
     * being reconstructed.
     */
    void reset() {
        setFilter(PASSTHROUGH_FILTER);
    }

    @Override
    public Promise<Response, NeverThrowsException> filter(Context context, Request request, Handler handler) {
        return filter.filter(context, request, handler);
    }
}
