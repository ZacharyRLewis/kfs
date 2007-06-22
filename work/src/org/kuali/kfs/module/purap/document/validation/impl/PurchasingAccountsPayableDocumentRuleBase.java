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
package org.kuali.module.purap.rules;

import static org.kuali.kfs.KFSConstants.BALANCE_TYPE_EXTERNAL_ENCUMBRANCE;
import static org.kuali.kfs.KFSConstants.ENCUMB_UPDT_DOCUMENT_CD;
import static org.kuali.kfs.KFSConstants.ENCUMB_UPDT_REFERENCE_DOCUMENT_CD;
import static org.kuali.kfs.KFSConstants.GL_CREDIT_CODE;
import static org.kuali.kfs.KFSConstants.GL_DEBIT_CODE;
import static org.kuali.module.purap.PurapConstants.PO_DOC_TYPE_CODE;
import static org.kuali.module.purap.PurapConstants.PURAP_ORIGIN_CODE;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.kuali.core.document.Document;
import org.kuali.core.rule.KualiParameterRule;
import org.kuali.core.rule.event.ApproveDocumentEvent;
import org.kuali.core.util.GlobalVariables;
import org.kuali.core.util.KualiDecimal;
import org.kuali.core.util.ObjectUtils;
import org.kuali.core.workflow.service.KualiWorkflowDocument;
import org.kuali.kfs.bo.AccountingLine;
import org.kuali.kfs.bo.GeneralLedgerPendingEntry;
import org.kuali.kfs.document.AccountingDocument;
import org.kuali.kfs.rules.AccountingDocumentRuleBase;
import org.kuali.kfs.util.SpringServiceLocator;
import org.kuali.module.chart.bo.ObjectCode;
import org.kuali.module.purap.PurapConstants;
import org.kuali.module.purap.PurapKeyConstants;
import org.kuali.module.purap.bo.PurApAccountingLine;
import org.kuali.module.purap.bo.PurchasingApItem;
import org.kuali.module.purap.document.CreditMemoDocument;
import org.kuali.module.purap.document.PaymentRequestDocument;
import org.kuali.module.purap.document.PurchaseOrderDocument;
import org.kuali.module.purap.document.PurchasingAccountsPayableDocument;
import org.kuali.module.purap.rule.AddPurchasingAccountsPayableItemRule;
import org.kuali.module.purap.util.PurApItemUtils;

import edu.iu.uis.eden.exception.WorkflowException;

public class PurchasingAccountsPayableDocumentRuleBase extends AccountingDocumentRuleBase implements AddPurchasingAccountsPayableItemRule {

    @Override
    protected boolean processCustomRouteDocumentBusinessRules(Document document) {
        boolean isValid = true;
        PurchasingAccountsPayableDocument purapDocument = (PurchasingAccountsPayableDocument) document;
        return isValid &= processValidation(purapDocument);
    }

    //TODO should we call our validation here?
//    @Override
//    protected boolean processCustomSaveDocumentBusinessRules(Document document) {
//        boolean isValid = true;
//        PurchasingAccountsPayableDocument purapDocument = (PurchasingAccountsPayableDocument) document;
//        return isValid &= processValidation(purapDocument);
//    }

    @Override
    protected boolean processCustomApproveDocumentBusinessRules(ApproveDocumentEvent approveEvent) {
        boolean isValid = true;
        PurchasingAccountsPayableDocument purapDocument = (PurchasingAccountsPayableDocument) approveEvent.getDocument();
        return isValid &= processValidation(purapDocument);
    }
    
    @Override
    protected boolean processCustomAddAccountingLineBusinessRules(AccountingDocument financialDocument, AccountingLine accountingLine) {
        boolean isValid = true;
        
        return isValid;
    }
    
    /**
     * This method calls each tab specific validation.  Tabs included on all PURAP docs are:
     *   DocumentOverview
     *   Vendor
     *   Item
     * 
     * @param purapDocument
     * @return
     */
    public boolean processValidation(PurchasingAccountsPayableDocument purapDocument) {
        boolean valid = true;
        valid &= processDocumentOverviewValidation(purapDocument);
        valid &= processVendorValidation(purapDocument);
        valid &= processItemValidation(purapDocument);
        return valid;
    }

    /**
     * This method performs any validation for the Document Overview tab.
     * 
     * @param purapDocument
     * @return
     */
    public boolean processDocumentOverviewValidation(PurchasingAccountsPayableDocument purapDocument) {
        boolean valid = true;
        // TODO code validation
        return valid;
    }

    /**
     * This method performs any validation for the Vendor tab.
     * 
     * @param purapDocument
     * @return
     */
    public boolean processVendorValidation(PurchasingAccountsPayableDocument purapDocument) {
        boolean valid = true;
        // TODO code validation
        return valid;
    }

    /**
     * This method performs any validation for the Item tab.
     * 
     * @param purapDocument
     * @return
     */
    public boolean processItemValidation(PurchasingAccountsPayableDocument purapDocument) {
        boolean valid = true;
        // Fetch the business rules that are common to the below the line items on all purap documents
        String documentTypeClassName = purapDocument.getClass().getName();
        String[] documentTypeArray = StringUtils.split(documentTypeClassName, ".");
        String documentType = documentTypeArray[documentTypeArray.length - 1];
        //If it's a credit memo, we'll have to append the source of the credit memo
        //whether it's created from a Vendor, a PO or a PREQ.
        if (documentType.equals("CreditMemoDocument")) {
           
        }
        String securityGroup = (String)PurapConstants.ITEM_TYPE_SYSTEM_PARAMETERS_SECURITY_MAP.get(documentType);
        KualiParameterRule allowsZeroRule = SpringServiceLocator.getKualiConfigurationService().getApplicationParameterRule(securityGroup, PurapConstants.ITEM_ALLOWS_ZERO);
        KualiParameterRule allowsPositiveRule = SpringServiceLocator.getKualiConfigurationService().getApplicationParameterRule(securityGroup, PurapConstants.ITEM_ALLOWS_POSITIVE);
        KualiParameterRule allowsNegativeRule = SpringServiceLocator.getKualiConfigurationService().getApplicationParameterRule(securityGroup, PurapConstants.ITEM_ALLOWS_NEGATIVE);
        KualiParameterRule requiresDescriptionRule = SpringServiceLocator.getKualiConfigurationService().getApplicationParameterRule(securityGroup, PurapConstants.ITEM_REQUIRES_USER_ENTERED_DESCRIPTION);

        for (PurchasingApItem item : purapDocument.getItems()) {
            //only do this check for below the line items
            item.refreshNonUpdateableReferences();
            if (!item.getItemType().isItemTypeAboveTheLineIndicator()) {
                if (ObjectUtils.isNotNull(item.getItemUnitPrice()) &&(new KualiDecimal(item.getItemUnitPrice())).isZero()) {
                    if (allowsZeroRule.getRuleActiveIndicator() &&
                        !allowsZeroRule.getParameterValueSet().contains(item.getItemTypeCode())) {
                        valid = false;
                        GlobalVariables.getErrorMap().putError("newPurchasingItemLine", PurapKeyConstants.ERROR_ITEM_BELOW_THE_LINE, item.getItemType().getItemTypeDescription(), "zero");
                    }
                }
                else if (ObjectUtils.isNotNull(item.getItemUnitPrice()) && (new KualiDecimal(item.getItemUnitPrice())).isPositive()) {
                    if (allowsPositiveRule.getRuleActiveIndicator() &&
                        !allowsPositiveRule.getParameterValueSet().contains(item.getItemTypeCode())) {
                        valid = false;
                        GlobalVariables.getErrorMap().putError("newPurchasingItemLine", PurapKeyConstants.ERROR_ITEM_BELOW_THE_LINE, item.getItemType().getItemTypeDescription(), "positive");
                    }
                }
                else if (ObjectUtils.isNotNull(item.getItemUnitPrice()) && (new KualiDecimal(item.getItemUnitPrice())).isNegative()) {
                    if (allowsNegativeRule.getRuleActiveIndicator() &&
                        !allowsNegativeRule.getParameterValueSet().contains(item.getItemTypeCode())) {
                        valid = false;
                        GlobalVariables.getErrorMap().putError("newPurchasingItemLine", PurapKeyConstants.ERROR_ITEM_BELOW_THE_LINE, item.getItemType().getItemTypeDescription(), "negative");
                    }
                }
                if (ObjectUtils.isNotNull(item.getItemUnitPrice()) && (new KualiDecimal(item.getItemUnitPrice())).isNonZero() && StringUtils.isEmpty(item.getItemDescription())) {
                    if (requiresDescriptionRule.getRuleActiveIndicator() &&
                        requiresDescriptionRule.getParameterValueSet().contains(item.getItemTypeCode())) {
                        valid = false;
                        GlobalVariables.getErrorMap().putError("newPurchasingItemLine", PurapKeyConstants.ERROR_ITEM_BELOW_THE_LINE, "The item description of " + item.getItemType().getItemTypeDescription(), "empty");
                    }
                }
            }
            
            if(PurApItemUtils.checkItemActive(item)) {
                if(item.getExtendedPrice().isNonZero()) {
                    processAccountValidation(purapDocument, item.getSourceAccountingLines(),item.getItemIdentifierString());
                }
            }
        }
        return valid;
    }

    /**
     * This method performs any validation for the Item tab when the user clicks on Save button.
     * 
     * @param purapDocument
     * @return boolean true if it passes the validation and false otherwise.
     */
    public boolean processItemValidationForSave(PurchasingAccountsPayableDocument purapDocument) {
        boolean valid = true;
        // Fetch the business rules that are common to the below the line items on all purap documents
        String documentTypeClassName = purapDocument.getClass().getName();
        String[] documentTypeArray = StringUtils.split(documentTypeClassName, ".");
        String documentType = documentTypeArray[documentTypeArray.length - 1];
        //If it's a credit memo, we'll have to append the source of the credit memo
        //whether it's created from a Vendor, a PO or a PREQ.
        if (documentType.equals("CreditMemoDocument")) {
           
        }
        String securityGroup = (String)PurapConstants.ITEM_TYPE_SYSTEM_PARAMETERS_SECURITY_MAP.get(documentType);
        KualiParameterRule allowsZeroRule = SpringServiceLocator.getKualiConfigurationService().getApplicationParameterRule(securityGroup, PurapConstants.ITEM_ALLOWS_ZERO);
        KualiParameterRule allowsPositiveRule = SpringServiceLocator.getKualiConfigurationService().getApplicationParameterRule(securityGroup, PurapConstants.ITEM_ALLOWS_POSITIVE);
        KualiParameterRule allowsNegativeRule = SpringServiceLocator.getKualiConfigurationService().getApplicationParameterRule(securityGroup, PurapConstants.ITEM_ALLOWS_NEGATIVE);
        KualiParameterRule requiresDescriptionRule = SpringServiceLocator.getKualiConfigurationService().getApplicationParameterRule(securityGroup, PurapConstants.ITEM_REQUIRES_USER_ENTERED_DESCRIPTION);

        for (PurchasingApItem item : purapDocument.getItems()) {
            //only do this check for below the line items
            item.refreshNonUpdateableReferences();
            if (!item.getItemType().isItemTypeAboveTheLineIndicator()) {
                if (ObjectUtils.isNotNull(item.getItemUnitPrice()) &&(new KualiDecimal(item.getItemUnitPrice())).isZero()) {
                    if (allowsZeroRule.getRuleActiveIndicator() &&
                        !allowsZeroRule.getParameterValueSet().contains(item.getItemTypeCode())) {
                        valid = false;
                        GlobalVariables.getErrorMap().putError("newPurchasingItemLine", PurapKeyConstants.ERROR_ITEM_BELOW_THE_LINE, item.getItemType().getItemTypeDescription(), "zero");
                    }
                }
                else if (ObjectUtils.isNotNull(item.getItemUnitPrice()) && (new KualiDecimal(item.getItemUnitPrice())).isPositive()) {
                    if (allowsPositiveRule.getRuleActiveIndicator() &&
                        !allowsPositiveRule.getParameterValueSet().contains(item.getItemTypeCode())) {
                        valid = false;
                        GlobalVariables.getErrorMap().putError("newPurchasingItemLine", PurapKeyConstants.ERROR_ITEM_BELOW_THE_LINE, item.getItemType().getItemTypeDescription(), "positive");
                    }
                }
                else if (ObjectUtils.isNotNull(item.getItemUnitPrice()) && (new KualiDecimal(item.getItemUnitPrice())).isNegative()) {
                    if (allowsNegativeRule.getRuleActiveIndicator() &&
                        !allowsNegativeRule.getParameterValueSet().contains(item.getItemTypeCode())) {
                        valid = false;
                        GlobalVariables.getErrorMap().putError("newPurchasingItemLine", PurapKeyConstants.ERROR_ITEM_BELOW_THE_LINE, item.getItemType().getItemTypeDescription(), "negative");
                    }
                }
                if (ObjectUtils.isNotNull(item.getItemUnitPrice()) && (new KualiDecimal(item.getItemUnitPrice())).isNonZero() && StringUtils.isEmpty(item.getItemDescription())) {
                    if (requiresDescriptionRule.getRuleActiveIndicator() &&
                        requiresDescriptionRule.getParameterValueSet().contains(item.getItemTypeCode())) {
                        valid = false;
                        GlobalVariables.getErrorMap().putError("newPurchasingItemLine", PurapKeyConstants.ERROR_ITEM_BELOW_THE_LINE, "The item description of " + item.getItemType().getItemTypeDescription(), "empty");
                    }
                }
            }
            valid &= verifyUniqueAccountingStrings(item.getSourceAccountingLines(), item.getItemIdentifierString());
            for (PurApAccountingLine account : item.getSourceAccountingLines()) {
                valid &= verifyAccountingStringsBetween0And100Percent(account, item.getItemIdentifierString());
            }
        }
        return valid;
    }
    
    public boolean processAddItemBusinessRules(AccountingDocument financialDocument, PurchasingApItem item) {
        // TODO Auto-generated method stub
        return true;
    }

    /**
     * A helper method for determining the route levels for a given document.
     * 
     * @param workflowDocument
     * @return List
     */
    protected static List getCurrentRouteLevels(KualiWorkflowDocument workflowDocument) {
        try {
            return Arrays.asList(workflowDocument.getNodeNames());
        }
        catch (WorkflowException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isDebit(AccountingDocument financialDocument, AccountingLine accountingLine) {
        // TODO Auto-generated method stub
        return false;
    }

//    /**      
//     * @see org.kuali.kfs.rules.GeneralLedgerPostingDocumentRuleBase#populateOffsetGeneralLedgerPendingEntry(java.lang.Integer, org.kuali.kfs.bo.GeneralLedgerPendingEntry, org.kuali.core.util.GeneralLedgerPendingEntrySequenceHelper, org.kuali.kfs.bo.GeneralLedgerPendingEntry)      
//     */     
//    @Override     
//    protected boolean populateOffsetGeneralLedgerPendingEntry(Integer universityFiscalYear, GeneralLedgerPendingEntry explicitEntry, GeneralLedgerPendingEntrySequenceHelper sequenceHelper, GeneralLedgerPendingEntry offsetEntry) {
//        return true;     
//    } 
    
    /**
     * @see org.kuali.kfs.rules.AccountingDocumentRuleBase#isAmountValid(org.kuali.kfs.document.AccountingDocument, org.kuali.kfs.bo.AccountingLine)
     */
    @Override
    public boolean isAmountValid(AccountingDocument document, AccountingLine accountingLine) {
        // TODO Auto-generated method stub
        return true;
    }


    /**
     * This method performs any additional document level validation for the accounts
     * 
     * @param purapDocument
     * @return
     */
    public boolean processAccountValidation(PurchasingAccountsPayableDocument purapDocument, List<PurApAccountingLine> purAccounts, String itemLineNumber) {
        boolean valid = true;
        valid = valid & verifyHasAccounts(purAccounts,itemLineNumber);
        valid = valid & verifyAccountPercent(purAccounts,itemLineNumber);
        //We can't invoke the verifyUniqueAccountingStrings in here because otherwise it would be invoking it more than once, if we're also
        //calling it upon Save.
        return valid;
    }

    protected boolean verifyHasAccounts(List<PurApAccountingLine>purAccounts,String itemLineNumber) {
        boolean valid = true;
        
        if(purAccounts.isEmpty()) {
            valid=false;
            GlobalVariables.getErrorMap().putError("newPurchasingItemLine", PurapKeyConstants.ERROR_ITEM_ACCOUNTING_INCOMPLETE, itemLineNumber);
        }
        
        return valid;
    }
    
    /**
     * This method verifies account percent
     * @param purAccounts
     * @return
     */
    protected boolean verifyAccountPercent(List<PurApAccountingLine> purAccounts,String itemLineNumber) {
        boolean valid = true;
        
        //validate that the percents total 100 for each item
        BigDecimal totalPercent = BigDecimal.ZERO;
        BigDecimal desiredPercent = new BigDecimal("100");
        for (PurApAccountingLine account : purAccounts) {
            totalPercent = totalPercent.add(account.getAccountLinePercent());
        }
        
        if(desiredPercent.compareTo(totalPercent)!=0) {
            GlobalVariables.getErrorMap().putError("newPurchasingItemLine", PurapKeyConstants.ERROR_ITEM_ACCOUNTING_TOTAL, itemLineNumber);
            valid = false;
        }
        return valid;
    }

    
    
    // (hjs) this could probably be done in a more generic way with a better method name, but this works for now
    public String entryDescription(String description) {
        if (description != null && description.length() > 40) {
            return description.toString().substring(0, 39);
        }
        else {
            return description;
        }
    }


    protected void purapCustomizeGeneralLedgerPendingEntry(PurchasingAccountsPayableDocument purapDocument, AccountingLine accountingLine, GeneralLedgerPendingEntry explicitEntry,
            Integer referenceDocumentNumber, String debitCreditCode, boolean isEncumbrance) {
        
        explicitEntry.setFinancialSystemOriginationCode(PURAP_ORIGIN_CODE);
        explicitEntry.setReferenceFinancialSystemOriginationCode(PURAP_ORIGIN_CODE);
        explicitEntry.setReferenceFinancialDocumentTypeCode(PO_DOC_TYPE_CODE);
        if (ObjectUtils.isNotNull(referenceDocumentNumber)) {
            explicitEntry.setReferenceFinancialDocumentNumber(referenceDocumentNumber.toString());
        }

        //TODO should we be doing it like this or storing the FY in the acct table in which case we wouldn't need this at all we'd inherit it from the accountingdocument
        ObjectCode objectCode = SpringServiceLocator.getObjectCodeService().getByPrimaryId(explicitEntry.getUniversityFiscalYear(), explicitEntry.getChartOfAccountsCode(), explicitEntry.getFinancialObjectCode());
        explicitEntry.setFinancialObjectTypeCode(objectCode.getFinancialObjectTypeCode());

        if (isEncumbrance) {
            explicitEntry.setFinancialBalanceTypeCode(BALANCE_TYPE_EXTERNAL_ENCUMBRANCE);

            // D - means the encumbrance is based on the document number
            // R - means the encumbrance is based on the referring document number
            // Encumbrances are created on the PO. They are updated by PREQ's and CM's.
            // So PO encumbrances are D, PREQ & CM's are R.
            if (purapDocument instanceof PurchaseOrderDocument) {
                explicitEntry.setTransactionEncumbranceUpdateCode(ENCUMB_UPDT_DOCUMENT_CD);
            }
            else if (purapDocument instanceof PaymentRequestDocument) {
                explicitEntry.setTransactionEncumbranceUpdateCode(ENCUMB_UPDT_REFERENCE_DOCUMENT_CD);
            }
            else if (purapDocument instanceof CreditMemoDocument) {
                explicitEntry.setTransactionEncumbranceUpdateCode(ENCUMB_UPDT_REFERENCE_DOCUMENT_CD);
            }
        }
        
        // if the amount is negative, flip the D/C indicator
        if (accountingLine.getAmount().doubleValue() < 0) {
            if (GL_CREDIT_CODE.equals(debitCreditCode)) {
                explicitEntry.setTransactionDebitCreditCode(GL_DEBIT_CODE);
            }
            else {
                explicitEntry.setTransactionDebitCreditCode(GL_CREDIT_CODE);
            }
        } else {
            explicitEntry.setTransactionDebitCreditCode(debitCreditCode);
        }

    }//end purapCustomizeGeneralLedgerPendingEntry()

    /**
     * 
     * This method verifies that the accounting strings entered are unique for each item. 
     * 
     * @param purAccounts
     * @param itemLineNumber
     * @return
     */
    protected boolean verifyUniqueAccountingStrings(List<PurApAccountingLine> purAccounts, String itemLineNumber) {
        Set existingAccounts = new HashSet();
        for (PurApAccountingLine acct : purAccounts) {
            if (!existingAccounts.contains(acct.toString())) {
                existingAccounts.add(acct.toString());
            }
            else {
                GlobalVariables.getErrorMap().putError(itemLineNumber, PurapKeyConstants.ERROR_ITEM_ACCOUNTING_NOT_UNIQUE, itemLineNumber);
                return false;
            }
        }
        return true;
    }
    
    protected boolean verifyAccountingStringsBetween0And100Percent(PurApAccountingLine account, String itemLineNumber) {
        double pct = account.getAccountLinePercent().doubleValue();
        if (pct <= 0 || pct > 100) {
            GlobalVariables.getErrorMap().putError(itemLineNumber, PurapKeyConstants.ERROR_ITEM_PERCENT, "%", itemLineNumber);
            return false;
        }
        return true;
    }
}
