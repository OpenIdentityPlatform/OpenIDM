package org.forgerock.openidm.action.impl;

import java.util.Map;

import org.forgerock.openidm.action.Action;
import org.forgerock.openidm.action.ActionException;

/**
 * A no operation {@link org.forgerock.openidm.action.Action} that does nothing. Useful
 * for {@link org.forgerock.openidm.recon.Situation} mappings where no action should be taken.
 */
public class NoOpAction implements Action {
    @Override
    public Object execute(Map<String, Object> sourceObject) throws ActionException {
        return null;
    }
}
