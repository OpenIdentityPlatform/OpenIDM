/*
 *
 * Copyright (c) 2010 ForgeRock Inc. All Rights Reserved
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

import groovy.sql.Sql
import groovy.sql.DataSet
import org.forgerock.openicf.misc.scriptedcommon.OperationType
import org.identityconnectors.common.logging.Log

import java.sql.Connection

// Parameters:
// The connector sends us the following:
// connection : SQL connection
// action: String correponding to the action ("RUNSCRIPTONCONNECTOR" here)
// log: a handler to the Log facility
// options: a handler to the OperationOptions Map
//
// Arguments can be passed to the script in the REST call, e.g.:
//
// curl -k --header "X-OpenIDM-Username: openidm-admin" \
// --header "X-OpenIDM-Password: openidm-admin" \
// --header "Content-Type: application/json" \
// --request POST "https://localhost:8443/openidm/system/hrdb?_action=script&scriptId=ResetDatabase" \
// -d "{\"arg1\":\"foo\",\"arg2\":\"bar\"}"
//
// These arguments can be accessed here by name, e.g.
//
// def firstArg = arg1 as String;
//
// Note that these can be complex types; Arguments are passed in as Object type.

def operation = operation as OperationType
def connection = connection as Connection
def sql = new Sql(connection);
def log = log as Log

log.info("Entering " + operation + " Script");

// Create and use the db if it's not present and clear out old tables if they exist
try {
    try {
        sql.execute("CREATE DATABASE IF NOT EXISTS hrdb CHARACTER SET utf8 COLLATE utf8_bin;")
        sql.execute("USE hrdb;")
    } catch(Exception e){}

    sql.execute("DROP TABLE IF EXISTS car;")
    sql.execute("DROP TABLE IF EXISTS groups_users;")
    sql.execute("DROP TABLE IF EXISTS users;")
    sql.execute("DROP TABLE IF EXISTS groups;")
    sql.execute("DROP TABLE IF EXISTS organizations;")
} catch(Exception e){}

// Create our tables
sql.execute("""
CREATE TABLE users(
        id int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,
        uid char(32) NOT NULL,
        password char(128),
        firstname varchar(32) NOT NULL DEFAULT '',
        lastname varchar(32) NOT NULL DEFAULT '',
        fullname varchar(32),
        email varchar(128),
        organization varchar(32),
        timestamp timestamp DEFAULT now()
);
""")

sql.execute("""
CREATE TABLE car(
        users_id int(11) NOT NULL,
        year varchar(4) NOT NULL,
        make varchar(32) NOT NULL,
        model varchar(32) NOT NULL,
        FOREIGN KEY (users_id) REFERENCES users(id) ON DELETE CASCADE
);
""")

sql.execute("""
CREATE TABLE groups(
        id int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,
        gid char(32) NOT NULL,
        name varchar(32) NOT NULL DEFAULT '',
        description varchar(32),
        timestamp timestamp DEFAULT now()
);
""")

sql.execute("""
CREATE TABLE groups_users(
        users_id int(11) NOT NULL,
        groups_id int(11) NOT NULL,
        FOREIGN KEY (users_id) REFERENCES users(id) ON DELETE CASCADE,
        FOREIGN KEY (groups_id) REFERENCES groups(id) ON DELETE CASCADE
);
""")

sql.execute("""
CREATE TABLE organizations(
        id int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,
        name varchar(32) NOT NULL DEFAULT '',
        description varchar(32),
        timestamp timestamp DEFAULT now()
);
""")


// Now populate the tables
sql.execute("""
INSERT INTO users
( uid, password, firstname, lastname, fullname, email, organization, timestamp )
VALUES
("bob",sha1("password1"),"Bob", "Fleming","Bob Fleming","Bob.Fleming@example.com","HR",CURRENT_TIMESTAMP),
("rowley",sha1("password2"),"Rowley","Birkin","Rowley Birkin","Rowley.Birkin@example.com","SALES",CURRENT_TIMESTAMP),
("louis",sha1("password3"),"Louis", "Balfour","Louis Balfour","Louis.Balfor@example.com","SALES",CURRENT_TIMESTAMP),
("john",sha1("password4"),"John", "Smith","John Smith","John.Smith@example.com","SUPPORT",CURRENT_TIMESTAMP),
("jdoe",sha1("password5"),"John", "Doe","John Doe","John.Doe@example.com","ENG",CURRENT_TIMESTAMP);
""")

sql.execute("""
INSERT INTO car (users_id,year,make,model) VALUES
(1,"1979","Ford","Pinto"),
(2,"2013","BMW","328ci"),
(2,"2010","Lexus","ES300"),
(3,"2001","Chevrolet","Venture"),
(4,"2009","Buick","LeSabre"),
(4,"2011","Honda","Accord"),
(5,"1987","Schwinn","Bicycle");
""")

sql.execute("""
INSERT INTO groups VALUES ("0","100","admin","Admin group",CURRENT_TIMESTAMP);
""")

sql.execute("""
INSERT INTO groups VALUES ("0","101","users","Users group",CURRENT_TIMESTAMP);
""")

sql.execute("""
INSERT INTO groups_users (users_id, groups_id) SELECT id, 1 FROM users where organization='HR';
""")

sql.execute("""
INSERT INTO groups_users (users_id, groups_id) SELECT id, 2 FROM users where organization <> 'HR';
""")

sql.execute("""
INSERT INTO organizations VALUES ("0","HR","HR organization",CURRENT_TIMESTAMP);
""")

sql.execute("""
INSERT INTO organizations VALUES ("0","SALES","Sales organization",CURRENT_TIMESTAMP);
""")

sql.execute("""
INSERT INTO organizations VALUES ("0","SUPPORT","Support organization",CURRENT_TIMESTAMP);
""")

sql.execute("""
INSERT INTO organizations VALUES ("0","ENG","Engineering organization",CURRENT_TIMESTAMP);
""")

sql.execute("grant all on hrdb.* to root@'%' IDENTIFIED BY 'password';")

// do a query to check it all worked ok
def results = sql.firstRow("select firstname, lastname from users where id=1").firstname
def expected = "Bob"
assert results == expected

return "Database reset successful."