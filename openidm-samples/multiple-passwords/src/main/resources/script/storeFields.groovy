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
 
import org.forgerock.openidm.managed.ManagedObjectContext;
import org.forgerock.services.context.Context;

/**
 * Stores fields of the managed object in the ManagedObjectContext's fields map.
 */

def mosc = context.getObject().asContext(ManagedObjectContext.class)
for (String field : storedFields) {
    def fieldValue = value.get(field).getObject();
    // Only store the field if it is a String, meaning is is not already hashed.
    if (fieldValue.class == String) {
        mosc.setField(field, value.get(field).getObject())
    }
}