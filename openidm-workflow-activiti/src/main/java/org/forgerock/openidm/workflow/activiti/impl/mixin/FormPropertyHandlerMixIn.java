/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright © 2012 ForgeRock Inc. All rights reserved.
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

package org.forgerock.openidm.workflow.activiti.impl.mixin;

import org.activiti.engine.delegate.Expression;
import org.activiti.engine.form.AbstractFormType;
import org.codehaus.jackson.annotate.JsonProperty;
import org.forgerock.openidm.workflow.activiti.ActivitiConstants;

/**
 *
 */
public class FormPropertyHandlerMixIn {
    
  @JsonProperty(ActivitiConstants.ID)  
  protected String id;
  protected String name;
  protected AbstractFormType type;
  protected boolean isReadable;
  protected boolean isWritable;
  protected boolean isRequired;
  protected String variableName;
  protected Expression variableExpression;
  protected Expression defaultExpression;
  
}
