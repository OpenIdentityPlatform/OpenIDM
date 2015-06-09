    /**
     * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
     *
     * Copyright 2015 ForgeRock AS. All rights reserved.
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

Roles Samples: All you ever wanted to know about Roles in OpenIDM
==================================================================

The samples available in the sub-directories provide all the information
you need to manage Roles in OpenIDM, via either REST or via the Administrative
UI. The following use cases are covered:
* Create a role
* Update a role
* Create a role with an assignment (entitlement) or update an existing
  role to add an assignment to that role.
* Query all roles and their assignments
* Query all user and their roles (+assignments)
* Sync a user who has a role with an attribute bearing entitlement to
  an external system (OpenDJ, based on sample 2b)
* Delete a role


CRUD operations for Roles
-------------------------

Available as part of the "crudops" sample. Provides a list of operations that
can be performed via REST or via the UI to manage roles in OpenIDM.

Use Case: the "Employee" and "Contractor" roles (common in most companies) will
be created, searched, updated, assigned to a user via the REST API; the
"Contractor" role will be deallocated from the user and deleted. That same
Contractor role will be (re)created via the Admin UI, updated and finally
deleted again.

Provisioning with Roles
-----------------------

Available as part of the "provroles" sample. Provides a list of operations
and configurations necessary for the provisioning of a set of attributes based
on role membership. The set of attributes will be pushed with the rest of the
user information to OpenDJ (based on sample2b).

Use Case: all regular (full-time) employees of the company must have their
employee type set and they must all be a member of the _Employees_ and
the _Chat Users_ groups in the corporate directory (OpenDJ). In turn,
contractors must also have their employee type set but they will only be part
of the _Contractors_ group (no chatting for contractors!). Roles will be used
to set the required properties on the external resource (OpenDJ).