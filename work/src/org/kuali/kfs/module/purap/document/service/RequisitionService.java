/*
 * Copyright 2006-2007 The Kuali Foundation.
 * 
 * Licensed under the Educational Community License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.opensource.org/licenses/ecl1.php
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kuali.module.purap.service;

import org.kuali.core.exceptions.ValidationException;
import org.kuali.module.purap.document.RequisitionDocument;

import edu.iu.uis.eden.exception.WorkflowException;

public interface RequisitionService {

    public void saveDocumentWithoutValidation(RequisitionDocument requisitionDocument);

    public RequisitionDocument getRequisitionById(Integer id);

    public boolean isAutomaticPurchaseOrderAllowed(RequisitionDocument requisition);
}
