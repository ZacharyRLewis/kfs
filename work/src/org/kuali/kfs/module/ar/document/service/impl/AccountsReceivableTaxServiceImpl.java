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
package org.kuali.kfs.module.ar.document.service.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.kuali.kfs.module.ar.ArConstants;
import org.kuali.kfs.module.ar.businessobject.CustomerAddress;
import org.kuali.kfs.module.ar.businessobject.CustomerInvoiceDetail;
import org.kuali.kfs.module.ar.businessobject.CustomerInvoiceItemCode;
import org.kuali.kfs.module.ar.businessobject.OrganizationOptions;
import org.kuali.kfs.module.ar.document.CustomerInvoiceDocument;
import org.kuali.kfs.module.ar.document.service.AccountsReceivableTaxService;
import org.kuali.kfs.module.ar.document.service.CustomerAddressService;
import org.kuali.kfs.module.ar.document.service.CustomerInvoiceDetailService;
import org.kuali.kfs.sys.context.SpringContext;
import org.kuali.kfs.sys.service.ParameterService;
import org.kuali.kfs.sys.service.impl.ParameterConstants;
import org.kuali.rice.kns.document.Document;
import org.kuali.rice.kns.service.BusinessObjectService;
import org.kuali.rice.kns.util.ObjectUtils;

public class AccountsReceivableTaxServiceImpl implements AccountsReceivableTaxService {
    
    private ParameterService parameterService;
    private BusinessObjectService businessObjectService;
    
    /**
     * @see org.kuali.kfs.module.ar.document.service.AccountsReceivableTaxService#isCustomerInvoiceDetailTaxable(org.kuali.kfs.module.ar.document.CustomerInvoiceDocument, org.kuali.kfs.module.ar.businessobject.CustomerInvoiceDetail)
     */
    public boolean isCustomerInvoiceDetailTaxable(CustomerInvoiceDocument customerInvoiceDocument, CustomerInvoiceDetail customerInvoiceDetail) {

        //check if sales tax is enabled
        if( !parameterService.getIndicatorParameter(ParameterConstants.ACCOUNTS_RECEIVABLE_DOCUMENT.class, ArConstants.ENABLE_SALES_TAX_IND)){
            return false;
        }

        //check if customer is tax exempt
        if( ObjectUtils.isNotNull(customerInvoiceDocument.getCustomer() ) ){
            if( customerInvoiceDocument.getCustomer().isCustomerTaxExemptIndicator()){
                return false;
            }
        }
        
        //check item if item is taxable
        if( StringUtils.isNotEmpty(customerInvoiceDetail.getInvoiceItemCode()) ){
            Map<String, String> criteria = new HashMap<String, String>();
            criteria.put("invoiceItemCode", customerInvoiceDetail.getInvoiceItemCode());
            criteria.put("chartOfAccountsCode", customerInvoiceDocument.getAccountsReceivableDocumentHeader().getProcessingChartOfAccountCode());
            criteria.put("organizationCode", customerInvoiceDocument.getAccountsReceivableDocumentHeader().getProcessingOrganizationCode());
            CustomerInvoiceItemCode customerInvoiceItemCode = (CustomerInvoiceItemCode) businessObjectService.findByPrimaryKey(CustomerInvoiceItemCode.class, criteria);
            
            if (ObjectUtils.isNotNull(customerInvoiceItemCode) && !customerInvoiceItemCode.isTaxableIndicator()){
                return false;
            }
        }
        
        //if address of billing org's postal code isn't the same as shipping address, return false??? 
        
        return true;
    }
    
    /**
     * @see org.kuali.kfs.module.ar.document.service.AccountsReceivableTaxService#getPostalCodeForTaxation(org.kuali.kfs.module.ar.document.CustomerInvoiceDocument)
     */
    public String getPostalCodeForTaxation(CustomerInvoiceDocument document) {
        
        String postalCode = null;
        String customerNumber = document.getAccountsReceivableDocumentHeader().getCustomerNumber();
        Integer shipToAddressIdentifier = document.getCustomerShipToAddressIdentifier();
        
        //if customer number or ship to address id isn't provided, go to org options
        if (ObjectUtils.isNotNull(shipToAddressIdentifier) && StringUtils.isNotEmpty(customerNumber) ) {
            
            CustomerAddressService customerAddressService = SpringContext.getBean(CustomerAddressService.class);
            CustomerAddress customerShipToAddress = customerAddressService.getByPrimaryKey(customerNumber, shipToAddressIdentifier);
            if( ObjectUtils.isNotNull(customerShipToAddress) ){
                postalCode = customerShipToAddress.getCustomerZipCode();
            }
        }
        else {
            Map<String, String> criteria = new HashMap<String, String>();
            criteria.put("chartOfAccountsCode", document.getBillByChartOfAccountCode());
            criteria.put("organizationCode", document.getBilledByOrganizationCode());
            OrganizationOptions organizationOptions = (OrganizationOptions) businessObjectService.findByPrimaryKey(OrganizationOptions.class, criteria);

            if (ObjectUtils.isNotNull(organizationOptions)) {
                postalCode = organizationOptions.getOrganizationPostalZipCode();
            }

           
        }
        return postalCode;
    }    

    public ParameterService getParameterService() {
        return parameterService;
    }

    public void setParameterService(ParameterService parameterService) {
        this.parameterService = parameterService;
    }

    public BusinessObjectService getBusinessObjectService() {
        return businessObjectService;
    }

    public void setBusinessObjectService(BusinessObjectService businessObjectService) {
        this.businessObjectService = businessObjectService;
    }



}
