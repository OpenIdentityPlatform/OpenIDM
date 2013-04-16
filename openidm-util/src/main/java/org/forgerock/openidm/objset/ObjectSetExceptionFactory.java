/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2013 ForgeRock AS. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */
package org.forgerock.openidm.objset;

import java.util.Map;

import org.forgerock.json.resource.JsonResourceException;

/**
 * A factory for generating ObjectSetExceptions based on a supplied exception
 * code and other parameters.  A subclass of ObjectSetException will be used
 * if it corresponds to the exception code.
 * 
 * @author ckienle
 */
public class ObjectSetExceptionFactory {

    /**
     * Instantiates and returns a new ObjectSetException with the specified code.
     * 
     * @param code the exception code
     * @return the new ObjectSetException
     */
    public static ObjectSetException getInstance(int code) {
        String message = null;
        Throwable cause = null;
        return getInstance(code, message, cause);
    }

    /**
     * Instantiates and returns a new ObjectSetException with the specified code, 
     * and message.
     * 
     * @param code the exception code
     * @param message a failure message
     * @return the new ObjectSetException
     */
    public static ObjectSetException getInstance(int code, String message) {
        Throwable cause = null;
        return getInstance(code, message, cause);
    }

    /**
     * Instantiates and returns a new ObjectSetException with the specified code, 
     * and cause.
     * 
     * @param code the exception code
     * @param cause the cause of the exception
     * @return the new ObjectSetException
     */
    public static ObjectSetException getInstance(int code, Throwable cause) {
        String message = null;
        return getInstance(code, message, cause);
    }

    /**
     * Instantiates and returns a new ObjectSetException with the specified code, 
     * message, and cause.
     * 
     * @param code the exception code
     * @param message a failure message
     * @param cause the cause of the exception
     * @return the new ObjectSetException
     */
    public static ObjectSetException getInstance(int code, String message, Throwable cause) {
        switch (code) {
        case JsonResourceException.BAD_REQUEST: 
            return new BadRequestException(message, cause);
        case JsonResourceException.FORBIDDEN: 
            return new ForbiddenException(message, cause);
        case JsonResourceException.NOT_FOUND: 
            return new NotFoundException(message, cause);
        case JsonResourceException.CONFLICT: 
            return new ConflictException(message, cause);
        case JsonResourceException.VERSION_MISMATCH: 
            return new PreconditionFailedException(message, cause); 
        case JsonResourceException.INTERNAL_ERROR: 
            return new InternalServerErrorException(message, cause);
        case JsonResourceException.UNAVAILABLE: 
            return new ServiceUnavailableException(message, cause);
        default: 
            return new ObjectSetException(code, message, cause);
        }
    }

    /**
     * Instantiates and returns a new ObjectSetException with the specified code, 
     * message, and detail map.
     * 
     * @param code the exception code
     * @param message a failure message
     * @param failureDetail a Map of failure details
     * @return the new ObjectSetException
     */
    public static ObjectSetException getInstance(int code, String message, Map<String, Object> failureDetail) {
        ObjectSetException result = getInstance(code, message);
        result.setDetail(failureDetail);
        return result;
    }

    /**
     * Instantiates and returns a new ObjectSetException with the specified code, 
     * message, cause, and detail map.
     * 
     * @param code the exception code
     * @param message a failure message
     * @param failureDetail a Map of failure details
     * @param cause the cause of the exception
     * @return the new ObjectSetException
     */
    public static ObjectSetException getInstance(int code, String message, Map<String, Object> failureDetail, Throwable cause) {
        ObjectSetException result = getInstance(code, message, cause);
        result.setDetail(failureDetail);
        return result;
    }
    
    
}
