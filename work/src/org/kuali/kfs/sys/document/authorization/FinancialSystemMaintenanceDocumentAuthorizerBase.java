/*
 * Copyright 2008 The Kuali Foundation.
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
package org.kuali.kfs.sys.document.authorization;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kuali.kfs.sys.businessobject.ChartOrgHolder;
import org.kuali.kfs.sys.context.SpringContext;
import org.kuali.kfs.sys.identity.KfsKimAttributes;
import org.kuali.kfs.sys.service.FinancialSystemUserService;
import org.kuali.rice.kim.bo.Person;
import org.kuali.rice.kns.bo.BusinessObject;
import org.kuali.rice.kns.document.Document;
import org.kuali.rice.kns.document.authorization.MaintenanceDocumentAuthorizerBase;

/**
 * This class is the custom KFS maintenance document authorizer base class
 */
public class FinancialSystemMaintenanceDocumentAuthorizerBase extends MaintenanceDocumentAuthorizerBase {
    private static Log LOG = LogFactory.getLog(FinancialSystemMaintenanceDocumentAuthorizerBase.class);

    private static FinancialSystemUserService financialSystemUserService;

    @Override
    protected void addRoleQualification(BusinessObject businessObject, Map<String, String> attributes) {
        super.addRoleQualification(businessObject, attributes);
        if (businessObject instanceof Document) {
            Document document = (Document) businessObject;

            String initiatorPrincipalId = document.getDocumentHeader().getWorkflowDocument().getInitiatorPrincipalId();
            Person initiator = getPersonService().getPerson(initiatorPrincipalId);
            
            ChartOrgHolder initiatorPrimaryOrganization = getFinancialSystemUserService().getOrganizationByNamespaceCode(initiator, attributes.get(KfsKimAttributes.NAMESPACE_CODE));
            if (initiatorPrimaryOrganization != null) {
                attributes.put(KfsKimAttributes.CHART_OF_ACCOUNTS_CODE, initiatorPrimaryOrganization.getChartOfAccountsCode());
                attributes.put(KfsKimAttributes.ORGANIZATION_CODE, initiatorPrimaryOrganization.getOrganizationCode());
            }
            
            attributes.put(KfsKimAttributes.CAMPUS_CODE, initiator.getCampusCode());
        }
    }

    public static final FinancialSystemUserService getFinancialSystemUserService() {
        if (financialSystemUserService == null) {
            financialSystemUserService = SpringContext.getBean(FinancialSystemUserService.class);
        }
        return financialSystemUserService;
    }
}
