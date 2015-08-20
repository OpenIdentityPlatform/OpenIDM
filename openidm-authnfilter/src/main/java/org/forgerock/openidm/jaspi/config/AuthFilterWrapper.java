package org.forgerock.openidm.jaspi.config;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.http.Filter;
import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.services.context..Context;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.osgi.service.component.ComponentContext;

/**
 * Created by brmiller on 8/19/15.
 */
@Component(name = AuthFilterWrapper.PID, policy = ConfigurationPolicy.IGNORE,
        configurationFactory = false, immediate = true)
@Service(value = {Filter.class, AuthFilterWrapper.class})
public class AuthFilterWrapper implements Filter {
    public static final String PID = "org.forgerock.openidm.jaspi.config";

    static final Filter PASSTHROUGH_FILTER = new Filter() {
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
        setFilter(PASSTHROUGH_FILTER);
    }

    @Deactivate
    protected synchronized void deactivate(ComponentContext context) {
        setFilter(PASSTHROUGH_FILTER);
    }

    synchronized void setFilter(Filter filter) {
        this.filter = filter;
    }

    @Override
    public Promise<Response, NeverThrowsException> filter(Context context, Request request, Handler handler) {
        return filter.filter(context, request, handler);
    }
}
