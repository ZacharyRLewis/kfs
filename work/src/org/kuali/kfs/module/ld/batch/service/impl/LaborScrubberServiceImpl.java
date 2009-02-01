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
package org.kuali.kfs.module.ld.batch.service.impl;

import org.kuali.kfs.coa.service.ObjectCodeService;
import org.kuali.kfs.coa.service.OffsetDefinitionService;
import org.kuali.kfs.gl.businessobject.OriginEntryGroup;
import org.kuali.kfs.gl.service.OriginEntryGroupService;
import org.kuali.kfs.gl.service.ScrubberValidator;
import org.kuali.kfs.module.ld.batch.service.LaborReportService;
import org.kuali.kfs.module.ld.batch.service.LaborScrubberService;
import org.kuali.kfs.module.ld.service.LaborOriginEntryLookupService;
import org.kuali.kfs.module.ld.service.LaborOriginEntryService;
import org.kuali.kfs.sys.context.SpringContext;
import org.kuali.kfs.sys.dataaccess.UniversityDateDao;
import org.kuali.kfs.sys.service.FlexibleOffsetAccountService;
import org.kuali.kfs.sys.service.FinancialSystemDocumentTypeCodeService;
import org.kuali.rice.kew.doctype.service.DocumentTypeService;
import org.kuali.rice.kns.service.DateTimeService;
import org.kuali.rice.kns.service.KualiConfigurationService;
import org.kuali.rice.kns.service.PersistenceService;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation of LaborScrubberService.
 */
@Transactional
public class LaborScrubberServiceImpl implements LaborScrubberService {
    private static org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(LaborScrubberServiceImpl.class);

    private FlexibleOffsetAccountService flexibleOffsetAccountService;
    private FinancialSystemDocumentTypeCodeService financialSystemDocumentTypeCodeService;
    private LaborOriginEntryService laborOriginEntryService;
    private OriginEntryGroupService originEntryGroupService;
    private DateTimeService dateTimeService;
    private OffsetDefinitionService offsetDefinitionService;
    private ObjectCodeService objectCodeService;
    private KualiConfigurationService kualiConfigurationService;
    private UniversityDateDao universityDateDao;
    private PersistenceService persistenceService;
    private LaborReportService laborReportService;
    private ScrubberValidator scrubberValidator;
    
    private String batchFileDirectoryName;

    /**
     * @see org.kuali.module.labor.service.ScrubberService#scrubGroupReportOnly(org.kuali.kfs.gl.businessobject.OriginEntryGroup)
     */
    public void scrubGroupReportOnly(OriginEntryGroup group, String documentNumber) {
        LOG.debug("scrubGroupReportOnly() started");

        // The logic for this was moved into another object because the process was written using
        // many instance variables which shouldn't be used for Spring services

        LaborScrubberProcess sp = new LaborScrubberProcess(flexibleOffsetAccountService, financialSystemDocumentTypeCodeService, laborOriginEntryService, originEntryGroupService, dateTimeService, offsetDefinitionService, objectCodeService, kualiConfigurationService, universityDateDao, persistenceService, laborReportService, scrubberValidator, batchFileDirectoryName);
        sp.setReferenceLookup(SpringContext.getBean(LaborOriginEntryLookupService.class));
        sp.scrubGroupReportOnly(group, documentNumber);
        sp.setReferenceLookup(null);
    }

    /**
     * @see org.kuali.module.labor.service.ScrubberService#scrubEntries()
     */
    public void scrubEntries() {
        LOG.debug("scrubEntries() started");

        // The logic for this was moved into another object because the process was written using
        // many instance variables which shouldn't be used for Spring services

        LaborScrubberProcess sp = new LaborScrubberProcess(flexibleOffsetAccountService, financialSystemDocumentTypeCodeService, laborOriginEntryService, originEntryGroupService, dateTimeService, offsetDefinitionService, objectCodeService, kualiConfigurationService, universityDateDao, persistenceService, laborReportService, scrubberValidator, batchFileDirectoryName);
        sp.setReferenceLookup(SpringContext.getBean(LaborOriginEntryLookupService.class));
        sp.scrubEntries();
        sp.setReferenceLookup(null);
    }
    
    public void performDemerger() {
        LOG.debug("performDemerger() started");
        LaborScrubberProcess sp = new LaborScrubberProcess(flexibleOffsetAccountService, financialSystemDocumentTypeCodeService, laborOriginEntryService, originEntryGroupService, dateTimeService, offsetDefinitionService, objectCodeService, kualiConfigurationService, universityDateDao, persistenceService, laborReportService, scrubberValidator, batchFileDirectoryName);
        sp.performDemerger();
                
    }
    
    

    /**
     * Sets the flexibleOffsetAccountService attribute value.
     * 
     * @param flexibleOffsetAccountService The flexibleOffsetAccountService to set.
     */
    public void setFlexibleOffsetAccountService(FlexibleOffsetAccountService flexibleOffsetAccountService) {
        this.flexibleOffsetAccountService = flexibleOffsetAccountService;
    }

    /**
     * Sets the financialSystemDocumentTypeCodeService attribute value.
     * 
     * @param financialSystemDocumentTypeCodeService The documentTypeService to set.
     */
    public void setFinancialSystemDocumentTypeCodeService(FinancialSystemDocumentTypeCodeService financialSystemDocumentTypeCodeService) {
        this.financialSystemDocumentTypeCodeService = financialSystemDocumentTypeCodeService;
    }

    /**
     * Sets the setScrubberValidator attribute value.
     * 
     * @param sv The setScrubberValidator to set.
     */
    public void setScrubberValidator(ScrubberValidator sv) {
        scrubberValidator = sv;
    }

    /**
     * Sets the laborOriginEntryService attribute value.
     * 
     * @param loes The laborOriginEntryService to set.
     */
    public void setLaborOriginEntryService(LaborOriginEntryService loes) {
        this.laborOriginEntryService = loes;
    }

    /**
     * Sets the originEntryGroupService attribute value.
     * 
     * @param groupService The originEntryGroupService to set.
     */
    public void setOriginEntryGroupService(OriginEntryGroupService groupService) {
        this.originEntryGroupService = groupService;
    }

    /**
     * Sets the dateTimeService attribute value.
     * 
     * @param dts The dateTimeService to set.
     */
    public void setDateTimeService(DateTimeService dts) {
        this.dateTimeService = dts;
    }

    /**
     * Sets the universityDateDao attribute value.
     * 
     * @param universityDateDao The universityDateDao to set.
     */
    public void setUniversityDateDao(UniversityDateDao universityDateDao) {
        this.universityDateDao = universityDateDao;
    }

    /**
     * Sets the persistenceService attribute value.
     * 
     * @param ps The persistenceService to set.
     */
    public void setPersistenceService(PersistenceService ps) {
        persistenceService = ps;
    }

    /**
     * Sets the offsetDefinitionService attribute value.
     * 
     * @param offsetDefinitionService The offsetDefinitionService to set.
     */
    public void setOffsetDefinitionService(OffsetDefinitionService offsetDefinitionService) {
        this.offsetDefinitionService = offsetDefinitionService;
    }

    /**
     * Sets the objectCodeService attribute value.
     * 
     * @param objectCodeService The objectCodeService to set.
     */
    public void setObjectCodeService(ObjectCodeService objectCodeService) {
        this.objectCodeService = objectCodeService;
    }

    /**
     * Sets the kualiConfigurationService attribute value.
     * 
     * @param kualiConfigurationService The kualiConfigurationService to set.
     */
    public void setKualiConfigurationService(KualiConfigurationService kualiConfigurationService) {
        this.kualiConfigurationService = kualiConfigurationService;
    }

    /**
     * Sets the laborReportService attribute value.
     * 
     * @param laborReportService The laborReportService to set.
     */
    public void setLaborReportService(LaborReportService laborReportService) {
        this.laborReportService = laborReportService;
    }

    public void setBatchFileDirectoryName(String batchFileDirectoryName) {
        this.batchFileDirectoryName = batchFileDirectoryName;
    }
}
