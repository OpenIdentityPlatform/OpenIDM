package org.forgerock.openidm.filter;

import java.util.Map;

import org.forgerock.openidm.filter.AuthFilter.AuthData;

public interface AuthFilterService {

    /**
     * Method for re-authenticating requests.  The request will need to include
     * a "X-OpenIDM-Reauth-Password" header.
     * 
     * @param request   The request object.
     * @return          The AuthData response object.
     * @throws AuthException
     */
    public AuthData reauthenticate(Map<String,Object> request) throws AuthException;
}
