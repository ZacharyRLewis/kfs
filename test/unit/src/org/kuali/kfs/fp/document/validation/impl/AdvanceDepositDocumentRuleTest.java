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
package org.kuali.module.financial.rules;

import static org.kuali.module.financial.rules.IsDebitTestUtils.Amount.NEGATIVE;
import static org.kuali.module.financial.rules.IsDebitTestUtils.Amount.POSITIVE;
import static org.kuali.test.fixtures.UserNameFixture.KHUNTLEY;

import org.kuali.core.service.DataDictionaryService;
import org.kuali.core.service.DocumentService;
import org.kuali.core.service.DocumentTypeService;
import org.kuali.core.util.KualiDecimal;
import org.kuali.kfs.bo.AccountingLine;
import org.kuali.kfs.bo.SourceAccountingLine;
import org.kuali.kfs.context.KualiTestBase;
import org.kuali.kfs.context.SpringContext;
import org.kuali.kfs.document.AccountingDocument;
import org.kuali.module.financial.document.AdvanceDepositDocument;
import org.kuali.test.ConfigureContext;


/**
 * This class tests the <code>AdvanceDepositDocumentRule</code>s
 * 
 * 
 */
@ConfigureContext(session = KHUNTLEY)
public class AdvanceDepositDocumentRuleTest extends KualiTestBase {

    /**
     * test that an <code>IllegalStateException</code> gets thrown for an error correction document
     * 
     * @throws Exception
     */
    public void testIsDebit_errorCorrection() throws Exception {
        AccountingDocument accountingDocument = IsDebitTestUtils.getErrorCorrectionDocument(SpringContext.getBean(DocumentService.class), AdvanceDepositDocument.class);
        AccountingLine accountingLine = IsDebitTestUtils.getIncomeLine(accountingDocument, SourceAccountingLine.class, POSITIVE);

        assertTrue(IsDebitTestUtils.isErrorCorrectionIllegalStateException(SpringContext.getBean(DocumentTypeService.class), SpringContext.getBean(DataDictionaryService.class), accountingDocument, accountingLine));
    }

    /**
     * tests false is returned for a positive income
     * 
     * @throws Exception
     */
    public void testIsDebit_income_positveAmount() throws Exception {
        AccountingDocument accountingDocument = IsDebitTestUtils.getDocument(SpringContext.getBean(DocumentService.class), AdvanceDepositDocument.class);
        AccountingLine accountingLine = IsDebitTestUtils.getIncomeLine(accountingDocument, SourceAccountingLine.class, POSITIVE);

        assertFalse(IsDebitTestUtils.isDebit(SpringContext.getBean(DocumentTypeService.class), SpringContext.getBean(DataDictionaryService.class), accountingDocument, accountingLine));
    }

    /**
     * tests true is returned for a negative income
     * 
     * @throws Exception
     */
    public void testIsDebit_income_negativeAmount() throws Exception {
        AccountingDocument accountingDocument = IsDebitTestUtils.getDocument(SpringContext.getBean(DocumentService.class), AdvanceDepositDocument.class);
        AccountingLine accountingLine = IsDebitTestUtils.getIncomeLine(accountingDocument, SourceAccountingLine.class, NEGATIVE);

        assertTrue(IsDebitTestUtils.isDebit(SpringContext.getBean(DocumentTypeService.class), SpringContext.getBean(DataDictionaryService.class), accountingDocument, accountingLine));

    }

    /**
     * tests an <code>IllegalStateException</code> is thrown for a zero income
     * 
     * @throws Exception
     */
    public void testIsDebit_income_zeroAmount() throws Exception {
        AccountingDocument accountingDocument = IsDebitTestUtils.getDocument(SpringContext.getBean(DocumentService.class), AdvanceDepositDocument.class);
        AccountingLine accountingLine = IsDebitTestUtils.getIncomeLine(accountingDocument, SourceAccountingLine.class, KualiDecimal.ZERO);

        assertTrue(IsDebitTestUtils.isDebitIllegalStateException(SpringContext.getBean(DocumentTypeService.class), SpringContext.getBean(DataDictionaryService.class), accountingDocument, accountingLine));
    }

    /**
     * tests true is returned for a positive expense
     * 
     * @throws Exception
     */
    public void testIsDebit_expense_positveAmount() throws Exception {
        AccountingDocument accountingDocument = IsDebitTestUtils.getDocument(SpringContext.getBean(DocumentService.class), AdvanceDepositDocument.class);
        AccountingLine accountingLine = IsDebitTestUtils.getExpenseLine(accountingDocument, SourceAccountingLine.class, POSITIVE);

        assertTrue(IsDebitTestUtils.isDebit(SpringContext.getBean(DocumentTypeService.class), SpringContext.getBean(DataDictionaryService.class), accountingDocument, accountingLine));
    }

    /**
     * tests false is returned for a negative expense
     * 
     * @throws Exception
     */
    public void testIsDebit_expense_negativeAmount() throws Exception {
        AccountingDocument accountingDocument = IsDebitTestUtils.getDocument(SpringContext.getBean(DocumentService.class), AdvanceDepositDocument.class);
        AccountingLine accountingLine = IsDebitTestUtils.getExpenseLine(accountingDocument, SourceAccountingLine.class, NEGATIVE);

        assertFalse(IsDebitTestUtils.isDebit(SpringContext.getBean(DocumentTypeService.class), SpringContext.getBean(DataDictionaryService.class), accountingDocument, accountingLine));

    }

    /**
     * tests an <code>IllegalStateException</code> is thrown for a zero expense
     * 
     * @throws Exception
     */
    public void testIsDebit_expense_zeroAmount() throws Exception {
        AccountingDocument accountingDocument = IsDebitTestUtils.getDocument(SpringContext.getBean(DocumentService.class), AdvanceDepositDocument.class);
        AccountingLine accountingLine = IsDebitTestUtils.getExpenseLine(accountingDocument, SourceAccountingLine.class, KualiDecimal.ZERO);

        assertTrue(IsDebitTestUtils.isDebitIllegalStateException(SpringContext.getBean(DocumentTypeService.class), SpringContext.getBean(DataDictionaryService.class), accountingDocument, accountingLine));
    }

    /**
     * tests true is returned for a positive asset
     * 
     * @throws Exception
     */
    public void testIsDebit_asset_positveAmount() throws Exception {
        AccountingDocument accountingDocument = IsDebitTestUtils.getDocument(SpringContext.getBean(DocumentService.class), AdvanceDepositDocument.class);
        AccountingLine accountingLine = IsDebitTestUtils.getAssetLine(accountingDocument, SourceAccountingLine.class, POSITIVE);

        assertTrue(IsDebitTestUtils.isDebit(SpringContext.getBean(DocumentTypeService.class), SpringContext.getBean(DataDictionaryService.class), accountingDocument, accountingLine));
    }

    /**
     * tests false is returned for a negative asset
     * 
     * @throws Exception
     */
    public void testIsDebit_asset_negativeAmount() throws Exception {
        AccountingDocument accountingDocument = IsDebitTestUtils.getDocument(SpringContext.getBean(DocumentService.class), AdvanceDepositDocument.class);
        AccountingLine accountingLine = IsDebitTestUtils.getAssetLine(accountingDocument, SourceAccountingLine.class, NEGATIVE);

        assertFalse(IsDebitTestUtils.isDebit(SpringContext.getBean(DocumentTypeService.class), SpringContext.getBean(DataDictionaryService.class), accountingDocument, accountingLine));

    }

    /**
     * tests an <code>IllegalStateException</code> is thrown for a zero asset
     * 
     * @throws Exception
     */
    public void testIsDebit_asset_zeroAmount() throws Exception {
        AccountingDocument accountingDocument = IsDebitTestUtils.getDocument(SpringContext.getBean(DocumentService.class), AdvanceDepositDocument.class);
        AccountingLine accountingLine = IsDebitTestUtils.getAssetLine(accountingDocument, SourceAccountingLine.class, KualiDecimal.ZERO);

        assertTrue(IsDebitTestUtils.isDebitIllegalStateException(SpringContext.getBean(DocumentTypeService.class), SpringContext.getBean(DataDictionaryService.class), accountingDocument, accountingLine));
    }

    /**
     * tests false is returned for a positive liability
     * 
     * @throws Exception
     */
    public void testIsDebit_liability_positveAmount() throws Exception {
        AccountingDocument accountingDocument = IsDebitTestUtils.getDocument(SpringContext.getBean(DocumentService.class), AdvanceDepositDocument.class);
        AccountingLine accountingLine = IsDebitTestUtils.getLiabilityLine(accountingDocument, SourceAccountingLine.class, POSITIVE);

        assertFalse(IsDebitTestUtils.isDebit(SpringContext.getBean(DocumentTypeService.class), SpringContext.getBean(DataDictionaryService.class), accountingDocument, accountingLine));
    }

    /**
     * tests true is returned for a negative liability
     * 
     * @throws Exception
     */
    public void testIsDebit_liability_negativeAmount() throws Exception {
        AccountingDocument accountingDocument = IsDebitTestUtils.getDocument(SpringContext.getBean(DocumentService.class), AdvanceDepositDocument.class);
        AccountingLine accountingLine = IsDebitTestUtils.getLiabilityLine(accountingDocument, SourceAccountingLine.class, NEGATIVE);

        assertTrue(IsDebitTestUtils.isDebit(SpringContext.getBean(DocumentTypeService.class), SpringContext.getBean(DataDictionaryService.class), accountingDocument, accountingLine));

    }

    /**
     * tests an <code>IllegalStateException</code> is thrown for a zero liability
     * 
     * @throws Exception
     */
    public void testIsDebit_liability_zeroAmount() throws Exception {
        AccountingDocument accountingDocument = IsDebitTestUtils.getDocument(SpringContext.getBean(DocumentService.class), AdvanceDepositDocument.class);
        AccountingLine accountingLine = IsDebitTestUtils.getLiabilityLine(accountingDocument, SourceAccountingLine.class, KualiDecimal.ZERO);

        assertTrue(IsDebitTestUtils.isDebitIllegalStateException(SpringContext.getBean(DocumentTypeService.class), SpringContext.getBean(DataDictionaryService.class), accountingDocument, accountingLine));
    }

}
