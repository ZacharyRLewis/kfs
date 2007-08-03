/*
 * Copyright 2007 The Kuali Foundation.
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
package org.kuali.module.labor.web.struts.action;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.upload.FormFile;
import org.kuali.core.authorization.AuthorizationConstants;
import org.kuali.core.service.SequenceAccessorService;
import org.kuali.core.util.GlobalVariables;
import org.kuali.core.web.struts.form.KualiTableRenderFormMetadata;
import org.kuali.core.web.ui.Column;
import org.kuali.core.web.ui.KeyLabelPair;
import org.kuali.core.workflow.service.KualiWorkflowDocument;
import org.kuali.kfs.KFSConstants;
import org.kuali.kfs.KFSKeyConstants;
import org.kuali.kfs.KFSPropertyConstants;
import org.kuali.kfs.util.SpringServiceLocator;
import org.kuali.module.gl.bo.CorrectionChange;
import org.kuali.module.gl.bo.CorrectionChangeGroup;
import org.kuali.module.gl.bo.CorrectionCriteria;
import org.kuali.module.gl.bo.OriginEntry;
import org.kuali.module.gl.bo.OriginEntryGroup;
import org.kuali.module.gl.bo.OriginEntrySource;
import org.kuali.module.gl.document.CorrectionDocument;
import org.kuali.module.gl.document.CorrectionDocumentAuthorizer;
import org.kuali.module.gl.exception.LoadException;
import org.kuali.module.gl.service.CorrectionDocumentService;
import org.kuali.module.gl.util.CorrectionDocumentUtils;
import org.kuali.module.gl.web.struts.action.CorrectionAction;
import org.kuali.module.gl.web.struts.form.CorrectionForm;
import org.kuali.module.labor.LaborConstants;
import org.kuali.module.labor.bo.LaborOriginEntry;
import org.kuali.module.labor.document.LaborCorrectionDocument;
import org.kuali.module.labor.service.LaborCorrectionDocumentService;
import org.kuali.module.labor.service.LaborOriginEntryService;
import org.kuali.module.labor.web.optionfinder.LaborOriginEntryFieldFinder;
import org.kuali.module.labor.web.struts.form.LaborCorrectionForm;

public class LaborCorrectionAction extends CorrectionAction{

    LaborOriginEntryService laborOriginEntryService = SpringServiceLocator.getLaborOriginEntryService();
  
    
    
    
    
    
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        LOG.debug("execute() started");
        
        CorrectionForm correctionForm = (CorrectionForm) form;

        // Init our services once
        if (originEntryGroupService == null) {
            CorrectionAction.originEntryGroupService = SpringServiceLocator.getOriginEntryGroupService();
            CorrectionAction.originEntryService = SpringServiceLocator.getOriginEntryService();
            CorrectionAction.dateTimeService = SpringServiceLocator.getDateTimeService();
            CorrectionAction.kualiConfigurationService = SpringServiceLocator.getKualiConfigurationService();
        }

        request.setAttribute("debug", Boolean.valueOf(kualiConfigurationService.getApplicationParameterIndicator("GL.GLCP", "GL.DEBUG")));

        CorrectionForm rForm = (CorrectionForm) form;
        LOG.debug("execute() methodToCall: " + rForm.getMethodToCall());

        Collection<OriginEntry> persistedOriginEntries = null;
        
        // If we are called from the docHandler or reload, ignore the persisted origin entries because we are either creating a new document
        // or loading an old one
        if (!(KFSConstants.DOC_HANDLER_METHOD.equals(rForm.getMethodToCall()) || KFSConstants.RELOAD_METHOD_TO_CALL.equals(rForm.getMethodToCall()))) {
            restoreSystemAndEditMethod(rForm);
            restoreInputGroupSelectionForDatabaseEdits(rForm);
            if (!rForm.isRestrictedFunctionalityMode()) {
                if (StringUtils.isNotBlank(rForm.getGlcpSearchResultsSequenceNumber())) {
                    rForm.setAllEntries(SpringServiceLocator.getGlCorrectionProcessOriginEntryService().retrieveAllEntries(rForm.getGlcpSearchResultsSequenceNumber()));
                    if (rForm.getAllEntries() == null) { 
                        rForm.setDisplayEntries(null);
                    }
                    else {
                        rForm.setDisplayEntries(new ArrayList<OriginEntry> (rForm.getAllEntries()));
                    }
                
                    if ((!"showOutputGroup".equals(rForm.getMethodToCall())) && rForm.getShowOutputFlag()) {
                        // reapply the any criteria to pare down the list if the match criteria only flag is checked
                        CorrectionDocument document = rForm.getCorrectionDocument();
                        List<CorrectionChangeGroup> groups = document.getCorrectionChangeGroup();
                        updateEntriesFromCriteria(rForm, rForm.isRestrictedFunctionalityMode());
                    }
                    
                    if (!KFSConstants.TableRenderConstants.SORT_METHOD.equals(rForm.getMethodToCall())) {
                        // if sorting, we'll let the action take care of the sorting
                        KualiTableRenderFormMetadata originEntrySearchResultTableMetadata = rForm.getOriginEntrySearchResultTableMetadata();
                        if (originEntrySearchResultTableMetadata.getPreviouslySortedColumnIndex() != -1) {
                            List<Column> columns = SpringServiceLocator.getLaborCorrectionDocumentService().getTableRenderColumnMetadata(rForm.getDocument().getDocumentNumber());
                             
                            String propertyToSortName = columns.get(originEntrySearchResultTableMetadata.getPreviouslySortedColumnIndex()).getPropertyName();
                            Comparator valueComparator = columns.get(originEntrySearchResultTableMetadata.getPreviouslySortedColumnIndex()).getValueComparator();
                            sortList(rForm.getDisplayEntries(), propertyToSortName, valueComparator, originEntrySearchResultTableMetadata.isSortDescending());
                        }
                        if (rForm.getAllEntries() != null) {
                            int maxRowsPerPage = CorrectionDocumentUtils.getRecordsPerPage();
                            originEntrySearchResultTableMetadata.jumpToPage(originEntrySearchResultTableMetadata.getViewedPageNumber(), rForm.getDisplayEntries().size(), maxRowsPerPage);
                            originEntrySearchResultTableMetadata.setColumnToSortIndex(originEntrySearchResultTableMetadata.getPreviouslySortedColumnIndex());
                        }
                    }
                }
            }
        }

        ActionForward af = super.execute(mapping, form, request, response);
        return af;
    }

    
    public ActionForward save(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        LOG.debug("save() started");

        LaborCorrectionForm laborCorrectionForm = (LaborCorrectionForm) form;
        LaborCorrectionDocument document = laborCorrectionForm.getLaborCorrectionDocument();

        // Did they pick the edit method and system?
        if (!checkMainDropdown(laborCorrectionForm)) {
            return mapping.findForward(KFSConstants.MAPPING_BASIC);
        }
        if (!checkRestrictedFunctionalityModeForManualEdit(laborCorrectionForm)) {
            return mapping.findForward(KFSConstants.MAPPING_BASIC);
        }
        if (!validGroupsItemsForDocumentSave(laborCorrectionForm)) {
            return mapping.findForward(KFSConstants.MAPPING_BASIC);
        }
        if (!validChangeGroups(laborCorrectionForm)) {
            return mapping.findForward(KFSConstants.MAPPING_BASIC);
        }
        if (!checkInputGroupPersistedForDocumentSave(laborCorrectionForm)) {
            return mapping.findForward(KFSConstants.MAPPING_BASIC);
        }
        
        // Populate document
        document.setCorrectionTypeCode(laborCorrectionForm.getEditMethod());
        document.setCorrectionSelection(laborCorrectionForm.getMatchCriteriaOnly());
        document.setCorrectionFileDelete(!laborCorrectionForm.getProcessInBatch());
        document.setCorrectionInputFileName(laborCorrectionForm.getInputFileName());
        document.setCorrectionOutputFileName(null); // this field is never used
        if (laborCorrectionForm.getDataLoadedFlag() || laborCorrectionForm.isRestrictedFunctionalityMode()) {
            document.setCorrectionInputGroupId(laborCorrectionForm.getInputGroupId());
        }
        else {
            document.setCorrectionInputGroupId(null);
        }
        document.setCorrectionOutputGroupId(null);

        SpringServiceLocator.getLaborCorrectionDocumentService().persistOriginEntryGroupsForDocumentSave(document, laborCorrectionForm);
        
        LOG.debug("save() doc type name: " + laborCorrectionForm.getDocTypeName());
        return super.save(mapping, form, request, response);
    }

    
    
    /*
     * Upload a file
     */
    public ActionForward uploadFile(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws FileNotFoundException, IOException, Exception {
        LOG.debug("uploadFile() started");

        CorrectionForm correctionForm = (CorrectionForm) form;
        CorrectionDocument document = (CorrectionDocument) correctionForm.getDocument();

        java.sql.Date today = CorrectionAction.dateTimeService.getCurrentSqlDate();
        OriginEntryGroup newOriginEntryGroup = CorrectionAction.originEntryGroupService.createGroup(today, OriginEntrySource.LABOR_CORRECTION_PROCESS_EDOC, false, false, false);

        FormFile sourceFile = correctionForm.getSourceFile();

        String fullFileName = sourceFile.getFileName();

        sourceFile.getInputStream();

        BufferedReader br = new BufferedReader(new InputStreamReader(sourceFile.getInputStream()));
        int lineNumber = 0;
        int loadedCount = 0;
        boolean errorsLoading = false;
        try {
            String currentLine = br.readLine();
            while (currentLine != null) {
                lineNumber++;
                if (!StringUtils.isEmpty(currentLine)) {
                    try {
                        
                        // Check for short lines - Skip the record
                        if (currentLine.length() < LaborConstants.LLCP_MAX_LENGTH) {
                            GlobalVariables.getErrorMap().putError("systemAndEditMethod", KFSKeyConstants.Labor.LLCP_UPLOAD_FILE_INVALID_RECORD_SIZE_ERROR);                            
                            errorsLoading = true;
                            break;
                        }                        
                        LaborOriginEntry entryFromFile = new LaborOriginEntry();
                        entryFromFile.setFromTextFile(currentLine, lineNumber);
                        entryFromFile.setEntryGroupId(newOriginEntryGroup.getId());
                        laborOriginEntryService.createEntry(entryFromFile, newOriginEntryGroup);
                        loadedCount++;
                    }
                    catch (LoadException e) {
                        errorsLoading = true;
                    }
                }
                currentLine = br.readLine();
            }
        }
        finally {
            br.close();
        }

        int recordCountFunctionalityLimit = CorrectionDocumentUtils.getRecordCountFunctionalityLimit();
        if (CorrectionDocumentUtils.isRestrictedFunctionalityMode(loadedCount, recordCountFunctionalityLimit)) {
            correctionForm.setRestrictedFunctionalityMode(true);
            correctionForm.setDataLoadedFlag(false);
            document.setCorrectionInputGroupId(newOriginEntryGroup.getId());
            correctionForm.setInputFileName(fullFileName);
            
            if (CorrectionDocumentService.CORRECTION_TYPE_MANUAL.equals(correctionForm.getEditMethod())) {
                // the group size is not suitable for manual editing because it is too large
                if (recordCountFunctionalityLimit == CorrectionDocumentUtils.RECORD_COUNT_FUNCTIONALITY_LIMIT_IS_NONE) {
                    GlobalVariables.getErrorMap().putError("systemAndEditMethod",
                            KFSKeyConstants.ERROR_GL_ERROR_CORRECTION_UNABLE_TO_MANUAL_EDIT_ANY_GROUP);
                }
                else {
                    GlobalVariables.getErrorMap().putError("systemAndEditMethod",
                            KFSKeyConstants.ERROR_GL_ERROR_CORRECTION_UNABLE_TO_MANUAL_EDIT_LARGE_GROUP, String.valueOf(recordCountFunctionalityLimit));
                }
            }
        }
        else {
            correctionForm.setRestrictedFunctionalityMode(false);
            if (loadedCount > 0) {
                // Set all the data that we know
                correctionForm.setDataLoadedFlag(true);
                correctionForm.setInputFileName(fullFileName);
                document.setCorrectionInputGroupId(newOriginEntryGroup.getId());
                loadAllEntries(newOriginEntryGroup.getId(), correctionForm);
    
                if (CorrectionDocumentService.CORRECTION_TYPE_MANUAL.equals(correctionForm.getEditMethod())) {
                    correctionForm.setEditableFlag(false);
                    correctionForm.setManualEditFlag(true);
                }
            }
            else {
                GlobalVariables.getErrorMap().putError("fileUpload", KFSKeyConstants.ERROR_GL_ERROR_CORRECTION_NO_RECORDS);
            }
        }

        if (document.getCorrectionChangeGroup().isEmpty()) {
            document.addCorrectionChangeGroup(new CorrectionChangeGroup());
        }
        return mapping.findForward(KFSConstants.MAPPING_BASIC);
    }
    
    
    /**
     * Show all entries for Manual edit with groupId and persist these entries to the DB
     * The restricted functionality mode flag MUST BE SET PRIOR TO CALLING this method.
     * 
     * @param groupId
     * @param correctionForm
     * @throws Exception
     */
    protected void loadAllEntries(Integer groupId, CorrectionForm correctionForm) throws Exception {
        LOG.debug("loadAllEntries() started");

        if (!correctionForm.isRestrictedFunctionalityMode()) {
            CorrectionDocument document = correctionForm.getCorrectionDocument();
            List<LaborOriginEntry> laborSearchResults = laborOriginEntryService.getEntriesByGroupId(groupId);
            List<OriginEntry> searchResults = new ArrayList();
            searchResults.addAll(laborSearchResults);
            
            correctionForm.setAllEntries(searchResults);
            correctionForm.setDisplayEntries(new ArrayList<OriginEntry> (searchResults));

            updateDocumentSummary(document, correctionForm.getAllEntries(), correctionForm.isRestrictedFunctionalityMode());
            
            // if not in restricted functionality mode, then we can store these results temporarily in the GLCP origin entry service
            SequenceAccessorService sequenceAccessorService = SpringServiceLocator.getSequenceAccessorService();
            String glcpSearchResultsSequenceNumber = String.valueOf(sequenceAccessorService.getNextAvailableSequenceNumber(KFSConstants.LOOKUP_RESULTS_SEQUENCE));
            
            SpringServiceLocator.getGlCorrectionProcessOriginEntryService().persistAllEntries(glcpSearchResultsSequenceNumber, searchResults);
            correctionForm.setGlcpSearchResultsSequenceNumber(glcpSearchResultsSequenceNumber);
        
            int maxRowsPerPage = CorrectionDocumentUtils.getRecordsPerPage();
            KualiTableRenderFormMetadata originEntrySearchResultTableMetadata = correctionForm.getOriginEntrySearchResultTableMetadata();
            originEntrySearchResultTableMetadata.jumpToFirstPage(correctionForm.getDisplayEntries().size(), maxRowsPerPage);
            originEntrySearchResultTableMetadata.setColumnToSortIndex(-1);
        }
    }
    
    
    /**
     * Save a changed row in the group
     */
    public ActionForward saveManualEntry(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        LOG.debug("saveManualEdit() started");

        LaborCorrectionForm laborCorrectionForm = (LaborCorrectionForm) form;
        CorrectionDocument document = laborCorrectionForm.getCorrectionDocument();

        if (validLaborOriginEntry(laborCorrectionForm)) {
            int entryId = laborCorrectionForm.getLaborEntryForManualEdit().getEntryId();

            // Find it and replace it with the one from the edit spot
            for (Iterator<OriginEntry> iter = laborCorrectionForm.getAllEntries().iterator(); iter.hasNext();) {
                OriginEntry element = iter.next();
                if (element.getEntryId() == entryId) {
                    iter.remove();
                }
            }

            laborCorrectionForm.updateLaborEntryForManualEdit();
            laborCorrectionForm.getAllEntries().add(laborCorrectionForm.getLaborEntryForManualEdit());

            // we've modified the list of all entries, so repersist it
            SpringServiceLocator.getGlCorrectionProcessOriginEntryService().persistAllEntries(laborCorrectionForm.getGlcpSearchResultsSequenceNumber(), laborCorrectionForm.getAllEntries());
            //laborCorrectionForm.setDisplayEntries(null);
            laborCorrectionForm.setDisplayEntries(laborCorrectionForm.getAllEntries());
            
            if (laborCorrectionForm.getShowOutputFlag()) {
                removeNonMatchingEntries(laborCorrectionForm.getDisplayEntries(), document.getCorrectionChangeGroup());
            }
            
            // Clear out the additional row
            laborCorrectionForm.clearLaborEntryForManualEdit();
        }

        // Calculate the debit/credit/row count
        updateDocumentSummary(document, laborCorrectionForm.getAllEntries(), laborCorrectionForm.isRestrictedFunctionalityMode());

        // list has changed, we'll need to repage and resort
        applyPagingAndSortingFromPreviousPageView(laborCorrectionForm);

        return mapping.findForward(KFSConstants.MAPPING_BASIC);
    }
    
    
    /**
     * Add a new row to the group
     */
    public ActionForward addManualEntry(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        LOG.debug("addManualEdit() started");

        LaborCorrectionForm laborCorrectionForm = (LaborCorrectionForm) form;
        CorrectionDocument document = laborCorrectionForm.getCorrectionDocument();

        if (validLaborOriginEntry(laborCorrectionForm)) {
            laborCorrectionForm.updateLaborEntryForManualEdit();

            // new entryId is always 0, so give it a unique Id, SequenceAccessorService is used.
            Long newEntryId = SpringServiceLocator.getSequenceAccessorService().getNextAvailableSequenceNumber("GL_ORIGIN_ENTRY_T_SEQ");
            laborCorrectionForm.getLaborEntryForManualEdit().setEntryId(new Integer(newEntryId.intValue()));

            laborCorrectionForm.getAllEntries().add(laborCorrectionForm.getLaborEntryForManualEdit());

            // Clear out the additional row
            laborCorrectionForm.clearLaborEntryForManualEdit();
        }


        // Calculate the debit/credit/row count
        updateDocumentSummary(document, laborCorrectionForm.getAllEntries(), laborCorrectionForm.isRestrictedFunctionalityMode());

        laborCorrectionForm.setShowSummaryOutputFlag(true);

        // we've modified the list of all entries, so repersist it
        SpringServiceLocator.getGlCorrectionProcessOriginEntryService().persistAllEntries(laborCorrectionForm.getGlcpSearchResultsSequenceNumber(), laborCorrectionForm.getAllEntries());
        laborCorrectionForm.setDisplayEntries(new ArrayList<OriginEntry>(laborCorrectionForm.getAllEntries()));
        if (laborCorrectionForm.getShowOutputFlag()) {
            removeNonMatchingEntries(laborCorrectionForm.getDisplayEntries(), document.getCorrectionChangeGroup());
        }
        
        // list has changed, we'll need to repage and resort
        applyPagingAndSortingFromPreviousPageView(laborCorrectionForm);
        
        return mapping.findForward(KFSConstants.MAPPING_BASIC);
    }


    @Override
    public ActionForward manualEdit(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        // TODO Auto-generated method stub
        
        LaborCorrectionForm laborCorrectionForm = (LaborCorrectionForm) form;
        laborCorrectionForm.clearLaborEntryForManualEdit();
        
        return super.manualEdit(mapping, form, request, response);
    }


    @Override
    public ActionForward editManualEntry(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        LOG.debug("editManualEdit() started");

        LaborCorrectionForm laborCorrectionForm = (LaborCorrectionForm) form;
        CorrectionDocument document = laborCorrectionForm.getCorrectionDocument();

        int entryId = Integer.parseInt(getImageContext(request, "entryId"));

        // Find it and put it in the editing spot
        for (Iterator iter = laborCorrectionForm.getAllEntries().iterator(); iter.hasNext();) {
            LaborOriginEntry element = (LaborOriginEntry) iter.next();
            if (element.getEntryId() == entryId) {
                laborCorrectionForm.setLaborEntryForManualEdit(element);
                laborCorrectionForm.setLaborEntryFinancialDocumentReversalDate(CorrectionDocumentUtils.convertToString(element.getFinancialDocumentReversalDate(), "Date"));
                laborCorrectionForm.setLaborEntryTransactionDate(CorrectionDocumentUtils.convertToString(element.getTransactionDate(), "Date"));
                laborCorrectionForm.setLaborEntryTransactionLedgerEntryAmount(CorrectionDocumentUtils.convertToString(element.getTransactionLedgerEntryAmount(), "KualiDecimal"));
                laborCorrectionForm.setLaborEntryTransactionLedgerEntrySequenceNumber(CorrectionDocumentUtils.convertToString(element.getTransactionLedgerEntrySequenceNumber(), "Integer"));
                laborCorrectionForm.setLaborEntryUniversityFiscalYear(CorrectionDocumentUtils.convertToString(element.getUniversityFiscalYear(), "Integer"));
                break;
            }
        }

        laborCorrectionForm.setShowSummaryOutputFlag(true);

        return mapping.findForward(KFSConstants.MAPPING_BASIC);
    }
    
    
    protected boolean validLaborOriginEntry(LaborCorrectionForm laborCorrectionForm) {
        LOG.debug("validOriginEntry() started");

        LaborOriginEntry oe = laborCorrectionForm.getLaborEntryForManualEdit();

        boolean valid = true;
        LaborOriginEntryFieldFinder loeff = new LaborOriginEntryFieldFinder();
        List fields = loeff.getKeyValues();
        for (Iterator iter = fields.iterator(); iter.hasNext();) {
            KeyLabelPair lkp = (KeyLabelPair) iter.next();

            // Get field name, type, length & value on the form
            String fieldName = (String) lkp.getKey();
            String fieldDisplayName = lkp.getLabel();
            String fieldType = loeff.getFieldType(fieldName);
            int fieldLength = loeff.getFieldLength(fieldName);
            String fieldValue = null;
            if ("String".equals(fieldType)) {
                fieldValue = (String) oe.getFieldValue(fieldName);
            }
            else if (KFSPropertyConstants.FINANCIAL_DOCUMENT_REVERSAL_DATE.equals(fieldName)) {
                fieldValue = laborCorrectionForm.getLaborEntryFinancialDocumentReversalDate();
            }
            else if (KFSPropertyConstants.TRANSACTION_DATE.equals(fieldName)) {
                fieldValue = laborCorrectionForm.getLaborEntryTransactionDate();
            }
            else if (KFSPropertyConstants.TRN_ENTRY_LEDGER_SEQUENCE_NUMBER.equals(fieldName)) {
                fieldValue = laborCorrectionForm.getLaborEntryTransactionLedgerEntrySequenceNumber();
            }
            else if (KFSPropertyConstants.TRANSACTION_LEDGER_ENTRY_AMOUNT.equals(fieldName)) {
                fieldValue = laborCorrectionForm.getLaborEntryTransactionLedgerEntryAmount();
            }
            else if (KFSPropertyConstants.UNIVERSITY_FISCAL_YEAR.equals(fieldName)) {
                fieldValue = laborCorrectionForm.getLaborEntryUniversityFiscalYear();
            }

            // Now check that the data is valid
            if (!StringUtils.isEmpty(fieldValue)) {
                if (!loeff.isValidValue(fieldName, fieldValue)) {
                    GlobalVariables.getErrorMap().putError("searchResults", KFSKeyConstants.ERROR_GL_ERROR_CORRECTION_INVALID_VALUE, new String[] { fieldDisplayName, fieldValue });
                    valid = false;
                }
            }
            else if (!loeff.allowNull(fieldName)) {
                GlobalVariables.getErrorMap().putError("searchResults", KFSKeyConstants.ERROR_GL_ERROR_CORRECTION_INVALID_VALUE, new String[] { fieldDisplayName, fieldValue });
                valid = false;
            }
        }

        return valid;
    }
    
    protected void removeNonMatchingEntries(Collection<OriginEntry> entries, Collection<CorrectionChangeGroup> groups) {
        Iterator<OriginEntry> loei = entries.iterator();
        while (loei.hasNext()) {
            OriginEntry oe = loei.next();
            if (!CorrectionDocumentUtils.doesLaborEntryMatchAnyCriteriaGroups(oe, groups)) {
                loei.remove();
            }
        }
    }
    
    /**
     * Validate all the correction groups
     * 
     * @param doc
     * @return if valid, return true, false if not
     */
    protected boolean validChangeGroups(CorrectionForm form) {
        LOG.debug("validChangeGroups() started");

        CorrectionDocument doc = form.getCorrectionDocument();
        String tab = "";
        if (CorrectionDocumentService.CORRECTION_TYPE_CRITERIA.equals(form.getEditMethod())) {
            tab = "editCriteria";
        }
        else {
            tab = "manualEditCriteria";
        }

        boolean allValid = true;

        LaborOriginEntryFieldFinder loeff = new LaborOriginEntryFieldFinder();
        List fields = loeff.getKeyValues();

        List l = doc.getCorrectionChangeGroup();
        for (Iterator iter = l.iterator(); iter.hasNext();) {
            CorrectionChangeGroup ccg = (CorrectionChangeGroup) iter.next();
            for (Iterator iterator = ccg.getCorrectionCriteria().iterator(); iterator.hasNext();) {
                CorrectionCriteria cc = (CorrectionCriteria) iterator.next();
                if (!loeff.isValidValue(cc.getCorrectionFieldName(), cc.getCorrectionFieldValue())) {
                    GlobalVariables.getErrorMap().putError(tab, KFSKeyConstants.ERROR_GL_ERROR_CORRECTION_INVALID_VALUE, new String[] { loeff.getFieldDisplayName(cc.getCorrectionFieldName()), cc.getCorrectionFieldValue() });
                    allValid = false;
                }
            }
            for (Iterator iterator = ccg.getCorrectionChange().iterator(); iterator.hasNext();) {
                CorrectionChange cc = (CorrectionChange) iterator.next();
                if (!loeff.isValidValue(cc.getCorrectionFieldName(), cc.getCorrectionFieldValue())) {
                    GlobalVariables.getErrorMap().putError(tab, KFSKeyConstants.ERROR_GL_ERROR_CORRECTION_INVALID_VALUE, new String[] { loeff.getFieldDisplayName(cc.getCorrectionFieldName()), cc.getCorrectionFieldValue() });
                    allValid = false;
                }
            }
        }
        return allValid;
    }

    /**
     * Show all entries for Manual edit with groupId and persist these entries to the DB
     * 
     */
    protected void loadPersistedInputGroup(CorrectionForm correctionForm) throws Exception {

        LaborCorrectionForm laborCorrectionForm = (LaborCorrectionForm) correctionForm;
        LaborCorrectionDocument document = laborCorrectionForm.getLaborCorrectionDocument();

        int recordCountFunctionalityLimit = CorrectionDocumentUtils.getRecordCountFunctionalityLimit();
        LaborCorrectionDocumentService laborCorrectionDocumentService = SpringServiceLocator.getLaborCorrectionDocumentService();
        
        if (!laborCorrectionDocumentService.areInputOriginEntriesPersisted(document)) {
            // the input origin entry group has been purged from the system
            laborCorrectionForm.setPersistedOriginEntriesMissing(true);
            laborCorrectionForm.setRestrictedFunctionalityMode(true);
            return;
        }
        
        laborCorrectionForm.setPersistedOriginEntriesMissing(false);
        List<LaborOriginEntry> laborSearchResults = laborCorrectionDocumentService.retrievePersistedInputOriginEntries(document, recordCountFunctionalityLimit);
        List<OriginEntry> searchResults = new ArrayList();
        searchResults.addAll(laborSearchResults);
        if (searchResults == null) {
            // null when the origin entry list is too large (i.e. in restricted functionality mode)
            laborCorrectionForm.setRestrictedFunctionalityMode(true);
            updateDocumentSummary(document, null, true);
        }
        else {
            laborCorrectionForm.setAllEntries(searchResults);
            laborCorrectionForm.setDisplayEntries(new ArrayList<OriginEntry> (searchResults));

            updateDocumentSummary(document, laborCorrectionForm.getAllEntries(), false);
            
            // if not in restricted functionality mode, then we can store these results temporarily in the GLCP origin entry service
            SequenceAccessorService sequenceAccessorService = SpringServiceLocator.getSequenceAccessorService();
            String glcpSearchResultsSequenceNumber = String.valueOf(sequenceAccessorService.getNextAvailableSequenceNumber(KFSConstants.LOOKUP_RESULTS_SEQUENCE));
            
            SpringServiceLocator.getGlCorrectionProcessOriginEntryService().persistAllEntries(glcpSearchResultsSequenceNumber, searchResults);
            laborCorrectionForm.setGlcpSearchResultsSequenceNumber(glcpSearchResultsSequenceNumber);
        
            int maxRowsPerPage = CorrectionDocumentUtils.getRecordsPerPage();
            KualiTableRenderFormMetadata originEntrySearchResultTableMetadata = laborCorrectionForm.getOriginEntrySearchResultTableMetadata();
            originEntrySearchResultTableMetadata.jumpToFirstPage(laborCorrectionForm.getDisplayEntries().size(), maxRowsPerPage);
            originEntrySearchResultTableMetadata.setColumnToSortIndex(-1);
        }
    }
    
    protected void loadPersistedOutputGroup(CorrectionForm correctionForm, boolean setSequentialIds) throws Exception {
        LaborCorrectionForm laborCorrectionForm = (LaborCorrectionForm) correctionForm; 
        LaborCorrectionDocument document = laborCorrectionForm.getLaborCorrectionDocument();

        LaborCorrectionDocumentService laborCorrectionDocumentService = SpringServiceLocator.getLaborCorrectionDocumentService();
        if (!laborCorrectionDocumentService.areOutputOriginEntriesPersisted(document)) {
            // the input origin entry group has been purged from the system
            laborCorrectionForm.setPersistedOriginEntriesMissing(true);
            laborCorrectionForm.setRestrictedFunctionalityMode(true);
            return;
        }
        
        laborCorrectionForm.setPersistedOriginEntriesMissing(false);
        
        int recordCountFunctionalityLimit;
        if (CorrectionDocumentService.CORRECTION_TYPE_MANUAL.equals(laborCorrectionForm.getEditMethod())) {
            // with manual edits, rows may have been added so that the list goes would go into restricted func mode
            // so for manual edits, we ignore this limit
            recordCountFunctionalityLimit = CorrectionDocumentUtils.RECORD_COUNT_FUNCTIONALITY_LIMIT_IS_UNLIMITED;
        }
        else {
            recordCountFunctionalityLimit = CorrectionDocumentUtils.getRecordCountFunctionalityLimit();
        }
        
        List<LaborOriginEntry> laborSearchResults = laborCorrectionDocumentService.retrievePersistedOutputOriginEntries(document, recordCountFunctionalityLimit);
        List<OriginEntry> searchResults = new ArrayList();
        searchResults.addAll(laborSearchResults);
        
        if (searchResults == null) {
            // null when the origin entry list is too large (i.e. in restricted functionality mode)
            laborCorrectionForm.setRestrictedFunctionalityMode(true);
            
            KualiWorkflowDocument workflowDocument = document.getDocumentHeader().getWorkflowDocument();
            
            CorrectionDocumentAuthorizer cda = new CorrectionDocumentAuthorizer();
            Map editingMode = cda.getEditMode(document, GlobalVariables.getUserSession().getUniversalUser());
            if (editingMode.containsKey(AuthorizationConstants.TransactionalEditMode.FULL_ENTRY) || workflowDocument.stateIsCanceled()) {
                // doc in read/write mode or is cancelled, so the doc summary fields of the doc are unreliable, so clear them out
                updateDocumentSummary(document, null, true);
            }
            // else we defer to the values already in the doc, and just don't touch the values
        }
        else {
            laborCorrectionForm.setAllEntries(searchResults);
            laborCorrectionForm.setDisplayEntries(new ArrayList<OriginEntry> (searchResults));

            if (setSequentialIds) {
                CorrectionDocumentUtils.setSequentialEntryIds(laborCorrectionForm.getAllEntries());
            }
            
            // if we can display the entries (i.e. not restricted functionality mode), then recompute the summary
            updateDocumentSummary(document, laborCorrectionForm.getAllEntries(), false);
            
            // if not in restricted functionality mode, then we can store these results temporarily in the GLCP origin entry service
            SequenceAccessorService sequenceAccessorService = SpringServiceLocator.getSequenceAccessorService();
            String glcpSearchResultsSequenceNumber = String.valueOf(sequenceAccessorService.getNextAvailableSequenceNumber(KFSConstants.LOOKUP_RESULTS_SEQUENCE));
            
            SpringServiceLocator.getGlCorrectionProcessOriginEntryService().persistAllEntries(glcpSearchResultsSequenceNumber, searchResults);
            laborCorrectionForm.setGlcpSearchResultsSequenceNumber(glcpSearchResultsSequenceNumber);
        
            int maxRowsPerPage = CorrectionDocumentUtils.getRecordsPerPage();
            KualiTableRenderFormMetadata originEntrySearchResultTableMetadata = laborCorrectionForm.getOriginEntrySearchResultTableMetadata();
            originEntrySearchResultTableMetadata.jumpToFirstPage(laborCorrectionForm.getDisplayEntries().size(), maxRowsPerPage);
            originEntrySearchResultTableMetadata.setColumnToSortIndex(-1);
        }
    }
    

    protected boolean prepareForRoute(CorrectionForm correctionForm) throws Exception {
        
        LaborCorrectionForm laborCorrectionForm = (LaborCorrectionForm) correctionForm;
        LaborCorrectionDocument document = laborCorrectionForm.getLaborCorrectionDocument();

        // Is there a description?
        if (StringUtils.isEmpty(document.getDocumentHeader().getFinancialDocumentDescription())) {
            GlobalVariables.getErrorMap().putError("document.documentHeader.financialDocumentDescription", KFSKeyConstants.ERROR_DOCUMENT_NO_DESCRIPTION);
            return false;
        }

        if (laborCorrectionForm.isPersistedOriginEntriesMissing()) {
            GlobalVariables.getErrorMap().putError("searchResults", KFSKeyConstants.ERROR_GL_ERROR_CORRECTION_PERSISTED_ORIGIN_ENTRIES_MISSING);
            return false;
        }

        // Did they pick the edit method and system?
        if (!checkMainDropdown(laborCorrectionForm)) {
            return false;
        }

        if (laborCorrectionForm.getDataLoadedFlag() || laborCorrectionForm.isRestrictedFunctionalityMode()) {
            document.setCorrectionInputGroupId(laborCorrectionForm.getInputGroupId());
        }
        else {
            document.setCorrectionInputGroupId(null);
        }
        if (!checkOriginEntryGroupSelectionBeforeRouting(document)) {
            return false;
        }
        
        // were the system and edit methods inappropriately changed?
        if (GlobalVariables.getErrorMap().containsMessageKey(KFSKeyConstants.ERROR_GL_ERROR_CORRECTION_INVALID_SYSTEM_OR_EDIT_METHOD_CHANGE)) {
            return false;
        }
        
        // was the input group inappropriately changed?
        if (GlobalVariables.getErrorMap().containsMessageKey(KFSKeyConstants.ERROR_GL_ERROR_CORRECTION_INVALID_INPUT_GROUP_CHANGE)) {
            return false;
        }
        
        if (!validGroupsItemsForDocumentSave(laborCorrectionForm)) {
            return false;
        }
        
        // If it is criteria, are all the criteria valid?
        if (CorrectionDocumentService.CORRECTION_TYPE_CRITERIA.equals(laborCorrectionForm.getEditMethod())) {
            if (!validChangeGroups(laborCorrectionForm)) {
                return false;
            }
        }

        if (!checkRestrictedFunctionalityModeForManualEdit(laborCorrectionForm)) {
            return false;
        }
        
        if (!checkInputGroupPersistedForDocumentSave(laborCorrectionForm)) {
            return false;
        }
        // Get the output group if necessary
        if (CorrectionDocumentService.CORRECTION_TYPE_CRITERIA.equals(laborCorrectionForm.getEditMethod())) {
            if (!laborCorrectionForm.isRestrictedFunctionalityMode() && laborCorrectionForm.getDataLoadedFlag() && !laborCorrectionForm.getShowOutputFlag()) {
                // we're going to force the user to view the output group upon routing, so apply the criteria
                // if the user wasn't in show output mode.
                updateEntriesFromCriteria(laborCorrectionForm, false);
            }
            laborCorrectionForm.setShowOutputFlag(true);
        }
        else {
            // If it is manual edit, we don't need to save any correction groups
            document.getCorrectionChangeGroup().clear();
        }
        
        // Populate document
        document.setCorrectionTypeCode(laborCorrectionForm.getEditMethod());
        document.setCorrectionSelection(laborCorrectionForm.getMatchCriteriaOnly());
        document.setCorrectionFileDelete(!laborCorrectionForm.getProcessInBatch());
        document.setCorrectionInputFileName(laborCorrectionForm.getInputFileName());
        document.setCorrectionOutputFileName(null); // this field is never used
        
        // we'll populate the output group id when the doc has a route level change
        document.setCorrectionOutputGroupId(null);

        SpringServiceLocator.getLaborCorrectionDocumentService().persistOriginEntryGroupsForDocumentSave(document, laborCorrectionForm);
        
        return true;
    }

    public ActionForward saveToDesktop(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException {
        LOG.debug("saveToDesktop() started");

        LaborCorrectionForm laborCorrectionForm = (LaborCorrectionForm) form;

        if (checkOriginEntryGroupSelection(laborCorrectionForm)) {
            if (laborCorrectionForm.isInputGroupIdFromLastDocumentLoadIsMissing() && laborCorrectionForm.getInputGroupIdFromLastDocumentLoad() != null 
                    && laborCorrectionForm.getInputGroupIdFromLastDocumentLoad().equals(laborCorrectionForm.getInputGroupId())) {
                if (laborCorrectionForm.isPersistedOriginEntriesMissing()) {
                    GlobalVariables.getErrorMap().putError("documentsInSystem", KFSKeyConstants.Labor.ERROR_LABOR_ERROR_CORRECTION_PERSISTED_ORIGIN_ENTRIES_MISSING);
                    return mapping.findForward(KFSConstants.MAPPING_BASIC);
                }
                else {
                    String fileName = "llcp_archived_group_" + laborCorrectionForm.getInputGroupIdFromLastDocumentLoad().toString() + ".txt";
                    // set response
                    response.setContentType("application/txt");
                    response.setHeader("Content-disposition", "attachment; filename=" + fileName);
                    response.setHeader("Expires", "0");
                    response.setHeader("Cache-Control", "must-revalidate, post-check=0, pre-check=0");
                    response.setHeader("Pragma", "public");
                    
                    BufferedOutputStream bw = new BufferedOutputStream(response.getOutputStream());
                    
                    SpringServiceLocator.getCorrectionDocumentService().writePersistedInputOriginEntriesToStream((CorrectionDocument) laborCorrectionForm.getDocument(), bw);
                    
                    bw.flush();
                    bw.close();
                    
                    return null;
                }
            }
            else {
                OriginEntryGroup oeg = CorrectionAction.originEntryGroupService.getExactMatchingEntryGroup(laborCorrectionForm.getInputGroupId());
    
                String fileName = oeg.getSource().getCode() + oeg.getId().toString() + "_" + oeg.getDate().toString() + ".txt";
    
                // set response
                response.setContentType("application/txt");
                response.setHeader("Content-disposition", "attachment; filename=" + fileName);
                response.setHeader("Expires", "0");
                response.setHeader("Cache-Control", "must-revalidate, post-check=0, pre-check=0");
                response.setHeader("Pragma", "public");
    
                BufferedOutputStream bw = new BufferedOutputStream(response.getOutputStream());
    
                // write to output
                laborOriginEntryService.flatFile(laborCorrectionForm.getInputGroupId(), bw);
    
                bw.flush();
                bw.close();
    
                return null;
            }
        }
        else {
            return mapping.findForward(KFSConstants.MAPPING_BASIC);
        }
    }
    
    public ActionForward sort(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        LaborCorrectionForm correctionForm = (LaborCorrectionForm) form;
        int maxRowsPerPage = CorrectionDocumentUtils.getRecordsPerPage();

        KualiTableRenderFormMetadata originEntrySearchResultTableMetadata = correctionForm.getOriginEntrySearchResultTableMetadata();

        List<Column> columns = SpringServiceLocator.getLaborCorrectionDocumentService().getTableRenderColumnMetadata(correctionForm.getDocument().getDocumentNumber());
        
        String propertyToSortName = columns.get(originEntrySearchResultTableMetadata.getColumnToSortIndex()).getPropertyName();
        Comparator valueComparator = columns.get(originEntrySearchResultTableMetadata.getColumnToSortIndex()).getValueComparator();
        
        boolean sortDescending = false;
        if (originEntrySearchResultTableMetadata.getPreviouslySortedColumnIndex() == originEntrySearchResultTableMetadata.getColumnToSortIndex()) {
            // clicked sort on the same column that was previously sorted, so we will reverse the sort order
            sortDescending = !originEntrySearchResultTableMetadata.isSortDescending();
            originEntrySearchResultTableMetadata.setSortDescending(sortDescending);
        }
        
        originEntrySearchResultTableMetadata.setSortDescending(sortDescending);
        // sort the list now so that it will be rendered correctly
        sortList(correctionForm.getDisplayEntries(), propertyToSortName, valueComparator, sortDescending);
        
        // sorting, so go back to the first page
        originEntrySearchResultTableMetadata.jumpToFirstPage(correctionForm.getDisplayEntries().size(), maxRowsPerPage);
        
        return mapping.findForward(KFSConstants.MAPPING_BASIC);
    }

    protected void applyPagingAndSortingFromPreviousPageView(CorrectionForm correctionForm) {
        LaborCorrectionForm laborCorrectionForm = (LaborCorrectionForm) correctionForm;
        KualiTableRenderFormMetadata originEntrySearchResultTableMetadata = laborCorrectionForm.getOriginEntrySearchResultTableMetadata();
        if (originEntrySearchResultTableMetadata.getPreviouslySortedColumnIndex() != -1) {
            
            List<Column> columns = SpringServiceLocator.getLaborCorrectionDocumentService().getTableRenderColumnMetadata(laborCorrectionForm.getDocument().getDocumentNumber());
            
            String propertyToSortName = columns.get(originEntrySearchResultTableMetadata.getPreviouslySortedColumnIndex()).getPropertyName();
            Comparator valueComparator = columns.get(originEntrySearchResultTableMetadata.getPreviouslySortedColumnIndex()).getValueComparator();
            sortList(laborCorrectionForm.getDisplayEntries(), propertyToSortName, valueComparator, originEntrySearchResultTableMetadata.isSortDescending());
        }
        
        int maxRowsPerPage = CorrectionDocumentUtils.getRecordsPerPage();
        originEntrySearchResultTableMetadata.jumpToPage(originEntrySearchResultTableMetadata.getViewedPageNumber(), laborCorrectionForm.getDisplayEntries().size(), maxRowsPerPage);
    }
    
    protected boolean checkInputGroupPersistedForDocumentSave(CorrectionForm correctionForm) {
        boolean present;
        LaborCorrectionForm laborCorrectionForm = (LaborCorrectionForm) correctionForm;
        KualiWorkflowDocument workflowDocument = laborCorrectionForm.getDocument().getDocumentHeader().getWorkflowDocument(); 
        if (workflowDocument.stateIsInitiated() || (workflowDocument.stateIsSaved() && 
                (laborCorrectionForm.getInputGroupIdFromLastDocumentLoad() == null || !laborCorrectionForm.getInputGroupIdFromLastDocumentLoad().equals(laborCorrectionForm.getInputGroupId())))) {
            present = originEntryGroupService.getGroupExists(((LaborCorrectionDocument) laborCorrectionForm.getDocument()).getCorrectionInputGroupId()); 
        }
        else {
            present = SpringServiceLocator.getCorrectionDocumentService().areInputOriginEntriesPersisted((LaborCorrectionDocument) laborCorrectionForm.getDocument());
        }
        if (!present) {
            GlobalVariables.getErrorMap().putError(SYSTEM_AND_EDIT_METHOD_ERROR_KEY, KFSKeyConstants.ERROR_GL_ERROR_CORRECTION_PERSISTED_ORIGIN_ENTRIES_MISSING);
        }
        return present;
    }
}
