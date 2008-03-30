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
package org.kuali.module.budget.web.struts.action;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.jasperreports.engine.JRParameter;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.kuali.core.service.BusinessObjectService;
import org.kuali.core.util.GlobalVariables;
import org.kuali.core.util.WebUtils;
import org.kuali.core.web.struts.action.KualiAction;
import org.kuali.kfs.KFSConstants;
import org.kuali.kfs.KFSPropertyConstants;
import org.kuali.kfs.KFSConstants.ReportGeneration;
import org.kuali.kfs.context.SpringContext;
import org.kuali.kfs.service.ReportGenerationService;
import org.kuali.module.budget.BCConstants;
import org.kuali.module.budget.BCKeyConstants;
import org.kuali.module.budget.BudgetConstructionReportMode;
import org.kuali.module.budget.BCConstants.Report.ReportSelectMode;
import org.kuali.module.budget.bo.BudgetConstructionObjectPick;
import org.kuali.module.budget.bo.BudgetConstructionOrgAccountSummaryReport;
import org.kuali.module.budget.bo.BudgetConstructionOrgSubFundSummaryReport;
import org.kuali.module.budget.bo.BudgetConstructionPullup;
import org.kuali.module.budget.bo.BudgetConstructionReasonCodePick;
import org.kuali.module.budget.bo.BudgetConstructionReportThresholdSettings;
import org.kuali.module.budget.bo.BudgetConstructionSubFundPick;
import org.kuali.module.budget.service.BudgetConstructionAccountSummaryReportService;
import org.kuali.module.budget.service.BudgetConstructionOrganizationReportsService;
import org.kuali.module.budget.service.BudgetConstructionSubFundSummaryReportService;
import org.kuali.module.budget.service.BudgetReportsControlListService;
import org.kuali.module.budget.util.ReportControlListBuildHelper;
import org.kuali.module.budget.web.struts.form.OrganizationReportSelectionForm;

/**
 * Struts Action Class for the Organization Report Selection Screen.
 */
public class OrganizationReportSelectionAction extends KualiAction {
    private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(OrganizationReportSelectionAction.class);

    /**
     * Return to previous screen, Organization Selection screen.
     */
    public ActionForward returnToCaller(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        OrganizationReportSelectionForm organizationReportSelectionForm = (OrganizationReportSelectionForm) form;
        String backUrl = organizationReportSelectionForm.getBackLocation() + "?methodToCall=refresh&docFormKey=" + organizationReportSelectionForm.getDocFormKey();

        return new ActionForward(backUrl, true);
    }

    /**
     * Called from org select or account listing. Checks for needed control list build, makes call to build control list if
     * necessary, and forwards to subfund or object code select page.
     */
    public ActionForward start(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        OrganizationReportSelectionForm organizationReportSelectionForm = (OrganizationReportSelectionForm) form;
        String personUserIdentifier = GlobalVariables.getUserSession().getUniversalUser().getPersonUniversalIdentifier();

        // retrieve report mode to determine how control list should be built and what select screen should be rendered
        BudgetConstructionReportMode reportMode = BudgetConstructionReportMode.getBudgetConstructionReportModeByName(organizationReportSelectionForm.getReportMode());
        if (reportMode == null) {
            LOG.error("Invalid report mode passed to report select action: " + organizationReportSelectionForm.getReportMode());
            throw new RuntimeException("Invalid report mode passed to report select action: " + organizationReportSelectionForm.getReportMode());
        }

        // retrieve build helper to determine if a control list rebuild is needed
        ReportControlListBuildHelper buildHelper = (ReportControlListBuildHelper) GlobalVariables.getUserSession().retrieveObject(BCConstants.Report.CONTROL_BUILD_HELPER_SESSION_NAME);
        if (buildHelper == null) {
            // session timeout, need to rebuild build request
            buildHelper = new ReportControlListBuildHelper();

            Collection<BudgetConstructionPullup> selectedOrganizations = SpringContext.getBean(BudgetReportsControlListService.class).retrieveSelectedOrganziations(personUserIdentifier);
            buildHelper.addBuildRequest(organizationReportSelectionForm.getCurrentPointOfViewKeyCode(), selectedOrganizations, reportMode.reportBuildMode);
        }

        // do list builds
        buildControlLists(personUserIdentifier, organizationReportSelectionForm.getUniversityFiscalYear(), buildHelper, reportMode.reportSelectMode);

        // a few reports go just against the account control table, therefore we are ready to run the report
        if (ReportSelectMode.ACCOUNT.equals(reportMode.reportSelectMode)) {
            return performReport(mapping, form, request, response);
        }

        // setup action form
        if (ReportSelectMode.SUBFUND.equals(reportMode.reportSelectMode)) {
            organizationReportSelectionForm.setSubFundPickList((List) SpringContext.getBean(BudgetReportsControlListService.class).retrieveSubFundList(personUserIdentifier));
            organizationReportSelectionForm.setOperatingModeTitle(BCConstants.Report.SUB_FUND_SELECTION_TITLE);
        }
        else if (ReportSelectMode.OBJECT_CODE.equals(reportMode.reportSelectMode) || ReportSelectMode.REASON.equals(reportMode.reportSelectMode)) {
            organizationReportSelectionForm.setObjectCodePickList((List) SpringContext.getBean(BudgetReportsControlListService.class).retrieveObjectCodeList(personUserIdentifier));
            organizationReportSelectionForm.setOperatingModeTitle(BCConstants.Report.OBJECT_CODE_SELECTION_TITLE);
            organizationReportSelectionForm.getBudgetConstructionReportThresholdSettings().setLockThreshold(reportMode.lockThreshold);
        }

        return mapping.findForward(KFSConstants.MAPPING_BASIC);
    }

    /**
     * Makes service calls to rebuild the report control and sub-fund or object code select lists if needed.
     * 
     * @param personUserIdentifier - current user requesting the report
     * @param buildHelper - contains the current and requested build states
     * @param reportSelectMode - indicates whether the report takes a sub-fund or object code select list
     */
    private void buildControlLists(String personUserIdentifier, Integer universityFiscalYear, ReportControlListBuildHelper buildHelper, ReportSelectMode reportSelectMode) {
        BudgetReportsControlListService budgetReportsControlListService = SpringContext.getBean(BudgetReportsControlListService.class);

        String[] pointOfViewFields = buildHelper.getRequestedState().getPointOfView().split("[-]");
        if (buildHelper.isBuildNeeded()) {
            budgetReportsControlListService.updateReportsControlList(personUserIdentifier, universityFiscalYear, pointOfViewFields[0], pointOfViewFields[1], buildHelper.getRequestedState().getBuildMode());

            if (ReportSelectMode.SUBFUND.equals(reportSelectMode)) {
                budgetReportsControlListService.updateReportSubFundGroupSelectList(personUserIdentifier);
            }
            else if (ReportSelectMode.OBJECT_CODE.equals(reportSelectMode) || ReportSelectMode.REASON.equals(reportSelectMode)) {
                budgetReportsControlListService.updateReportObjectCodeSelectList(personUserIdentifier);
            }

            buildHelper.requestBuildComplete();
        }
    }

    /**
     * Generates the Budget Report and returns pdf.
     */
    public ActionForward performReport(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        OrganizationReportSelectionForm organizationReportSelectionForm = (OrganizationReportSelectionForm) form;
        String personUserIdentifier = GlobalVariables.getUserSession().getUniversalUser().getPersonUniversalIdentifier();

        BudgetConstructionReportMode reportMode = BudgetConstructionReportMode.getBudgetConstructionReportModeByName(organizationReportSelectionForm.getReportMode());
        if (!storeCodeSelections(organizationReportSelectionForm, reportMode, personUserIdentifier)) {
            return mapping.findForward(KFSConstants.MAPPING_BASIC);
        }

        // build report data and populate report objects for rendering
        Collection reportData = buildReportData(reportMode, organizationReportSelectionForm.getUniversityFiscalYear(), personUserIdentifier, organizationReportSelectionForm.isReportConsolidation(), organizationReportSelectionForm.getBudgetConstructionReportThresholdSettings());

        // build pdf and stream back
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
       
        ResourceBundle resourceBundle = ResourceBundle.getBundle(BCConstants.Report.REPORT_MESSAGES_CLASSPATH, Locale.getDefault());
        Map<String, Object> reportConfig = new HashMap<String, Object>();
        reportConfig.put(JRParameter.REPORT_RESOURCE_BUNDLE, resourceBundle);
        
        SpringContext.getBean(ReportGenerationService.class).generateReportToOutputStream(null, reportData, BCConstants.Report.REPORT_TEMPLATE_CLASSPATH + reportMode.jasperFileName, baos);
        WebUtils.saveMimeOutputStreamAsFile(response, ReportGeneration.PDF_MIME_TYPE, baos, reportMode.jasperFileName + ReportGeneration.PDF_FILE_EXTENSION);

        return null;
    }


    /**
     * Checks and stores sub-fund, object code, or reason code list depenending on the report mode and which screen we are on.
     * 
     * @param organizationReportSelectionForm - OrganizationReportSelectionForm containing the select lists
     * @param reportMode - BudgetConstructionReportMode for the report being ran
     * @return - true if a selection was found and the list was stored or if we need to show reason code select screen, false
     *         otherwise
     */
    private boolean storeCodeSelections(OrganizationReportSelectionForm organizationReportSelectionForm, BudgetConstructionReportMode reportMode, String personUserIdentifier) {
        boolean codeSelected = true;

        if (ReportSelectMode.SUBFUND.equals(reportMode.reportSelectMode)) {
            // came from sub fund select screen so need to store sub fund selection settings
            if (!storedSelectedSubFunds(organizationReportSelectionForm.getSubFundPickList())) {
                codeSelected = false;
            }
        }
        else if (organizationReportSelectionForm.getOperatingModeTitle().equals(BCConstants.Report.OBJECT_CODE_SELECTION_TITLE)) {
            // came from object code select screen so need to store object code selection settings
            if (!storedSelectedObjectCodes(organizationReportSelectionForm.getObjectCodePickList())) {
                codeSelected = false;
            }

            // determine if we need to setup reason code select
            if (ReportSelectMode.REASON.equals(reportMode.reportSelectMode) && !organizationReportSelectionForm.getBudgetConstructionReportThresholdSettings().isUseThreshold()) {
                BudgetReportsControlListService budgetReportsControlListService = SpringContext.getBean(BudgetReportsControlListService.class);

                // rebuild reason code control list
                budgetReportsControlListService.updateReportReasonCodeSelectList(personUserIdentifier);

                // setup form
                organizationReportSelectionForm.setReasonCodePickList((List) budgetReportsControlListService.retrieveReasonCodeList(personUserIdentifier));
                organizationReportSelectionForm.setOperatingModeTitle(BCConstants.Report.REASON_CODE_SELECTION_TITLE);
                codeSelected = false;
            }
        }
        else if (organizationReportSelectionForm.getOperatingModeTitle().equals(BCConstants.Report.REASON_CODE_SELECTION_TITLE)) {
            // came from reason code select screen so need to store reason code selection settings
            if (!storedSelectedReasonCodes(organizationReportSelectionForm.getReasonCodePickList())) {
                codeSelected = false;
            }
        }

        return codeSelected;
    }

    /**
     * Calls the report service for the given reportMode to build the report data in the db then populate the reports objects for
     * rendering to pdf.
     * 
     * @param reportMode - BudgetConstructionReportMode indicates which report we are running
     * @param universityFiscalYear - budget fiscal year
     * @param personUserIdentifier - user running report
     * @param runConsolidated - indicates whether the report should be ran consolidated (if it has a consolidated option)
     * @param budgetConstructionReportThresholdSettings - contains threshold setting options
     * @return Collection - Reports objects that contain built data
     */
    private Collection buildReportData(BudgetConstructionReportMode reportMode, Integer universityFiscalYear, String personUserIdentifier, boolean runConsolidated, BudgetConstructionReportThresholdSettings budgetConstructionReportThresholdSettings) {
        Collection reportData = new ArrayList();

        switch (reportMode) {
            case ACCOUNT_SUMMARY_REPORT:
                SpringContext.getBean(BudgetConstructionAccountSummaryReportService.class).updateReportsAccountSummaryTable(personUserIdentifier, runConsolidated);
                reportData = SpringContext.getBean(BudgetConstructionAccountSummaryReportService.class).buildReports(universityFiscalYear, personUserIdentifier);
                break;
            case SUBFUND_SUMMARY_REPORT:
                SpringContext.getBean(BudgetConstructionSubFundSummaryReportService.class).updateSubFundSummaryReport(personUserIdentifier);
                reportData = SpringContext.getBean(BudgetConstructionSubFundSummaryReportService.class).buildReports(universityFiscalYear, personUserIdentifier);
                break;
        }

        return reportData;
    }

    /**
     * Selects all sub-fund codes.
     */
    public ActionForward selectAllSubFunds(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        OrganizationReportSelectionForm organizationReportSelectionForm = (OrganizationReportSelectionForm) form;
        for (BudgetConstructionSubFundPick budgetConstructionSubFundPick : organizationReportSelectionForm.getSubFundPickList()) {
            budgetConstructionSubFundPick.setReportFlag(new Integer(1));
        }

        return mapping.findForward(KFSConstants.MAPPING_BASIC);
    }

    /**
     * Selects all object codes.
     */
    public ActionForward selectAllObjectCodes(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        OrganizationReportSelectionForm organizationReportSelectionForm = (OrganizationReportSelectionForm) form;
        for (BudgetConstructionObjectPick budgetConstructionObjectPick : organizationReportSelectionForm.getObjectCodePickList()) {
            budgetConstructionObjectPick.setSelectFlag(new Integer(1));
        }

        return mapping.findForward(KFSConstants.MAPPING_BASIC);
    }

    /**
     * Selects all reason codes.
     */
    public ActionForward selectAllReasonCodes(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        OrganizationReportSelectionForm organizationReportSelectionForm = (OrganizationReportSelectionForm) form;
        for (BudgetConstructionReasonCodePick budgetConstructionReasonCodePick : organizationReportSelectionForm.getReasonCodePickList()) {
            budgetConstructionReasonCodePick.setSelectFlag(new Integer(1));
        }

        return mapping.findForward(KFSConstants.MAPPING_BASIC);
    }

    /**
     * Unselects all sub-fund codes.
     */
    public ActionForward unselectAllSubFunds(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        OrganizationReportSelectionForm organizationReportSelectionForm = (OrganizationReportSelectionForm) form;
        for (BudgetConstructionSubFundPick budgetConstructionSubFundPick : organizationReportSelectionForm.getSubFundPickList()) {
            budgetConstructionSubFundPick.setReportFlag(new Integer(0));
        }

        return mapping.findForward(KFSConstants.MAPPING_BASIC);
    }

    /**
     * unselects all object codes.
     */
    public ActionForward unselectAllObjectCodes(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        OrganizationReportSelectionForm organizationReportSelectionForm = (OrganizationReportSelectionForm) form;
        for (BudgetConstructionObjectPick budgetConstructionObjectPick : organizationReportSelectionForm.getObjectCodePickList()) {
            budgetConstructionObjectPick.setSelectFlag(new Integer(0));
        }

        return mapping.findForward(KFSConstants.MAPPING_BASIC);
    }

    /**
     * Unselects all reason codes.
     */
    public ActionForward unselectAllReasonCodes(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        OrganizationReportSelectionForm organizationReportSelectionForm = (OrganizationReportSelectionForm) form;
        for (BudgetConstructionReasonCodePick budgetConstructionReasonCodePick : organizationReportSelectionForm.getReasonCodePickList()) {
            budgetConstructionReasonCodePick.setSelectFlag(new Integer(0));
        }

        return mapping.findForward(KFSConstants.MAPPING_BASIC);
    }

    /**
     * Checks that at least one sub fund is selected and stores the selection settings. If no sub fund is selected, an error message
     * is displayed to the user.
     * 
     * @param subFundPickList - List of BudgetConstructionSubFundPick objects to check
     * @return boolean - true if there was a selection and the list was saved, otherwise false
     */
    protected boolean storedSelectedSubFunds(List<BudgetConstructionSubFundPick> subFundPickList) {
        boolean foundSelected = false;

        // check to see if at least one pullflag is set and store the reportflag settings for currently displayed set of sub-funds
        for (BudgetConstructionSubFundPick budgetConstructionSubFundPick : subFundPickList) {
            if (budgetConstructionSubFundPick.getReportFlag() > 0) {
                foundSelected = true;
            }
        }

        // if selection was found, save the sub-fund selections, otherwise build error message
        if (foundSelected) {
            SpringContext.getBean(BudgetReportsControlListService.class).updateSubFundSelectFlags(subFundPickList);
        }
        else {
            GlobalVariables.getErrorMap().putError(KFSConstants.GLOBAL_ERRORS, BCKeyConstants.ERROR_BUDGET_SUBFUND_NOT_SELECTED);
        }

        return foundSelected;
    }

    /**
     * Checks that at least one object code is selected and stores the selection settings. If no object code is selected, an error
     * message is displayed to the user.
     * 
     * @param objectCodePickList - List of BudgetConstructionObjectPick objects to check
     * @return boolean - true if there was a selection and the list was saved, otherwise false
     */
    protected boolean storedSelectedObjectCodes(List<BudgetConstructionObjectPick> objectCodePickList) {
        boolean foundSelected = false;

        // check to see if at least one pullflag is set and store the selectFlag settings for currently displayed set of object
        // codes
        for (BudgetConstructionObjectPick budgetConstructionObjectPick : objectCodePickList) {
            if (budgetConstructionObjectPick.getSelectFlag() > 0) {
                foundSelected = true;
            }
        }

        // if selection was found, save the object code selections, otherwise build error message
        if (foundSelected) {
            SpringContext.getBean(BudgetReportsControlListService.class).updateObjectCodeSelectFlags(objectCodePickList);
        }
        else {
            GlobalVariables.getErrorMap().putError(KFSConstants.GLOBAL_ERRORS, BCKeyConstants.ERROR_BUDGET_OBJECT_CODE_NOT_SELECTED);
        }

        return foundSelected;
    }

    /**
     * Checks that at least one reason code is selected and stores the selection settings. If no reason code is selected, an error
     * message is displayed to the user.
     * 
     * @param reasonCodePickList - List of BudgetConstructionReasonCodePick objects to check
     * @return boolean - true if there was a selection and the list was saved, otherwise false
     */
    protected boolean storedSelectedReasonCodes(List<BudgetConstructionReasonCodePick> reasonCodePickList) {
        boolean foundSelected = false;

        // check to see if at least one pullflag is set and store the selectFlag settings for currently displayed set of reason
        // codes
        for (BudgetConstructionReasonCodePick budgetConstructionReasonCodePick : reasonCodePickList) {
            if (budgetConstructionReasonCodePick.getSelectFlag() > 0) {
                foundSelected = true;
            }
        }

        // if selection was found, save the reason code selections, otherwise build error message
        if (foundSelected) {
            SpringContext.getBean(BudgetReportsControlListService.class).updateReasonCodeSelectFlags(reasonCodePickList);
        }
        else {
            GlobalVariables.getErrorMap().putError(KFSConstants.GLOBAL_ERRORS, BCKeyConstants.ERROR_BUDGET_REASON_CODE_NOT_SELECTED);
        }

        return foundSelected;
    }
}
