/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All Rights Reserved
 */

package org.forgerock.openidm.salesforce.internal;

import org.restlet.Request;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Parameter;
import org.restlet.engine.security.AuthenticatorHelper;
import org.restlet.util.Series;

/**
 * A NAME does ...
 * 
 * @author Laszlo Hordos
 */
public class RawAuthenticatorHelper extends AuthenticatorHelper {

    public static final ChallengeScheme HTTP_Token = new ChallengeScheme(
            "HTTP_Token", "Token", "HTTP Token authentication");

    public RawAuthenticatorHelper() {
        super(ChallengeScheme.HTTP_OAUTH, true, false);
    }

    @Override
    public String formatResponse(ChallengeResponse challenge, Request request,
            Series<Parameter> httpHeaders) {
        return challenge.getRawValue();
    }
}
