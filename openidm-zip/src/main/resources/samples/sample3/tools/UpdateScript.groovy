/*
 *
 * Copyright (c) 2010-2014 ForgeRock Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1.php or
 * OpenIDM/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at OpenIDM/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted 2010 [name of copyright owner]"
 *
 * $Id$
 */
import groovy.sql.Sql;
import groovy.sql.DataSet;

// Parameters:
// The connector sends us the following:
// connection : SQL connection
//
// action: String correponding to the action (UPDATE/ADD_ATTRIBUTE_VALUES/REMOVE_ATTRIBUTE_VALUES)
//   - UPDATE : For each input attribute, replace all of the current values of that attribute
//     in the target object with the values of that attribute.
//   - ADD_ATTRIBUTE_VALUES: For each attribute that the input set contains, add to the current values
//     of that attribute in the target object all of the values of that attribute in the input set.
//   - REMOVE_ATTRIBUTE_VALUES: For each attribute that the input set contains, remove from the current values
//     of that attribute in the target object any value that matches one of the values of the attribute from the input set.

// log: a handler to the Log facility
//
// objectClass: a String describing the Object class (__ACCOUNT__ / __GROUP__ / other)
//
// uid: a String representing the entry uid
//
// attributes: an Attribute Map, containg the <String> attribute name as a key
// and the <List> attribute value(s) as value.
//
// password: password string, clear text (only for UPDATE)
//
// options: a handler to the OperationOptions Map

log.info("Entering "+action+" Script");
def sql = new Sql(connection);


switch ( action ) {
    case "UPDATE":
    switch ( objectClass ) {
        case "__ACCOUNT__":
        sql.executeUpdate("""
            UPDATE 
                Users 
            SET 
                fullname = ?,
                firstname = ?,
                lastname = ?,
                email = ?,
                organization = ?,
                password = coalesce(sha1(?), password)
            WHERE 
                uid = ?
            """, 
            [
                attributes.get("fullname").get(0),
                attributes.get("firstname").get(0),
                attributes.get("lastname").get(0),
                attributes.get("email").get(0),
                attributes.get("organization").get(0),
                attributes.get("password") != null ? attributes.get("password").get(0) : null,
                uid
            ]
        );
        break

        case "__GROUP__":
        sql.executeUpdate("""
            UPDATE 
                Groups 
            SET
                description = ?,
                gid = ?
            WHERE 
                name = ?
            """, 
            [
                attributes.get("description").get(0), 
                attributes.get("gid").get(0),
                uid
            ]
        );
        break

        case "organization":
        sql.executeUpdate("""
            UPDATE 
                Organizations
            SET 
                description = ?
            WHERE 
                name = ?
            """, 
            [
                attributes.get("description").get(0), 
                uid
            ]
        );
        break

        default:
        uid;
    }
    break

    case "ADD_ATTRIBUTE_VALUES":
    break

    case "ADD_ATTRIBUTE_VALUES":
    break


    default:
    uid
}

return uid;