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

package org.kuali.module.purap.bo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.kuali.core.util.KualiDecimal;
import org.kuali.core.util.ObjectUtils;
import org.kuali.module.purap.PurapPropertyConstants;
import org.kuali.module.purap.document.PurchaseOrderDocument;

/**
 * Purchase Order Item Business Object.
 */
public class PurchaseOrderItem extends PurchasingItemBase {
    private static org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(PurchaseOrderItem.class);

    private String documentNumber;
    private KualiDecimal itemInvoicedTotalQuantity;
    private KualiDecimal itemInvoicedTotalAmount;
    private KualiDecimal itemReceivedTotalQuantity;
    private KualiDecimal itemReturnedTotalQuantity;
    private KualiDecimal itemOutstandingEncumberedQuantity;
    private KualiDecimal itemOutstandingEncumberedAmount;
    private boolean itemActiveIndicator=true;
    private String purchaseOrderCommodityCd;

    private PurchaseOrderDocument purchaseOrder;

    // Not persisted to DB
    private boolean itemSelectedForRetransmitIndicator;

    /**
     * Default constructor.
     */
    public PurchaseOrderItem() {

    }

    /**
     * Constructor.
     * 
     * @param ri
     * @param po
     */
    public PurchaseOrderItem(RequisitionItem ri, PurchaseOrderDocument po) {
        super();
        
        this.setPurchaseOrder(po);

        this.setItemLineNumber(ri.getItemLineNumber());
        this.setItemUnitOfMeasureCode(ri.getItemUnitOfMeasureCode());
        this.setItemQuantity(ri.getItemQuantity());
        this.setItemCatalogNumber(ri.getItemCatalogNumber());
        this.setItemDescription(ri.getItemDescription());
        this.setItemCapitalAssetNoteText(ri.getItemCapitalAssetNoteText());
        this.setItemUnitPrice(ri.getItemUnitPrice());
        this.setItemAuxiliaryPartIdentifier(ri.getItemAuxiliaryPartIdentifier());
        this.setItemAssignedToTradeInIndicator(ri.getItemAssignedToTradeInIndicator());
        
        this.setExternalOrganizationB2bProductReferenceNumber(ri.getExternalOrganizationB2bProductReferenceNumber());
        this.setExternalOrganizationB2bProductTypeName(ri.getExternalOrganizationB2bProductTypeName());

        this.setCapitalAssetTransactionTypeCode(ri.getCapitalAssetTransactionTypeCode());
        this.setItemTypeCode(ri.getItemTypeCode());
        
        if (ri.getSourceAccountingLines() != null && ri.getSourceAccountingLines().size() > 0) {
            List accounts = new ArrayList();
            for (PurApAccountingLine account : ri.getSourceAccountingLines()) {
                PurchaseOrderAccount poAccount = new PurchaseOrderAccount(account);
                poAccount.setPurchaseOrderItem(this);
                accounts.add(poAccount);
            }
            this.setSourceAccountingLines(accounts);
        }
        //By default, set the item active indicator to true. 
        //In amendment, the user can set it to false when they click on 
        //the inactivate button.
        this.setItemActiveIndicator(true);
    }    

    public boolean isItemActiveIndicator() {
        return itemActiveIndicator;
    }

    public void setItemActiveIndicator(boolean itemActiveIndicator) {
        this.itemActiveIndicator = itemActiveIndicator;
    }

    public KualiDecimal getItemInvoicedTotalAmount() {
        return itemInvoicedTotalAmount;
    }

    public void setItemInvoicedTotalAmount(KualiDecimal itemInvoicedTotalAmount) {
        this.itemInvoicedTotalAmount = itemInvoicedTotalAmount;
    }

    public KualiDecimal getItemInvoicedTotalQuantity() {
        return itemInvoicedTotalQuantity;
    }

    public void setItemInvoicedTotalQuantity(KualiDecimal itemInvoicedTotalQuantity) {
        this.itemInvoicedTotalQuantity = itemInvoicedTotalQuantity;
    }

    public KualiDecimal getItemOutstandingEncumberedQuantity() {
        return itemOutstandingEncumberedQuantity;
    }

    public void setItemOutstandingEncumberedQuantity(KualiDecimal itemOutstandingEncumberedQuantity) {
        this.itemOutstandingEncumberedQuantity = itemOutstandingEncumberedQuantity;
    }

    public KualiDecimal getItemOutstandingEncumberedAmount() {
        return itemOutstandingEncumberedAmount;
    }

    public void setItemOutstandingEncumberedAmount(KualiDecimal itemOutstandingEncumbranceAmount) {
        this.itemOutstandingEncumberedAmount = itemOutstandingEncumbranceAmount;
    }

    public KualiDecimal getItemReceivedTotalQuantity() {
        return itemReceivedTotalQuantity;
    }

    public void setItemReceivedTotalQuantity(KualiDecimal itemReceivedTotalQuantity) {
        this.itemReceivedTotalQuantity = itemReceivedTotalQuantity;
    }

    public KualiDecimal getItemReturnedTotalQuantity() {
        return itemReturnedTotalQuantity;
    }

    public void setItemReturnedTotalQuantity(KualiDecimal itemReturnedTotalQuantity) {
        this.itemReturnedTotalQuantity = itemReturnedTotalQuantity;
    }

    public PurchaseOrderDocument getPurchaseOrder() {
        if (ObjectUtils.isNull(purchaseOrder)) {
            refreshReferenceObject(PurapPropertyConstants.PURCHASE_ORDER);
        }
        return purchaseOrder;
    }

    public void setPurchaseOrder(PurchaseOrderDocument purchaseOrder) {
        this.purchaseOrder = purchaseOrder;
    }

    public String getPurchaseOrderCommodityCd() {
        return purchaseOrderCommodityCd;
    }

    public void setPurchaseOrderCommodityCd(String purchaseOrderCommodityCd) {
        this.purchaseOrderCommodityCd = purchaseOrderCommodityCd;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public boolean isItemSelectedForRetransmitIndicator() {
        return itemSelectedForRetransmitIndicator;
    }

    public void setItemSelectedForRetransmitIndicator(boolean itemSelectedForRetransmitIndicator) {
        this.itemSelectedForRetransmitIndicator = itemSelectedForRetransmitIndicator;
    }

    /**
     * @see org.kuali.core.bo.BusinessObjectBase#toStringMapper()
     */
    @Override
    protected LinkedHashMap toStringMapper() {
        LinkedHashMap m = new LinkedHashMap();
        m.put("documentNumber", this.documentNumber);
        if (this.getItemIdentifier() != null) {
            m.put("itemIdentifier", this.getItemIdentifier().toString());
        }
        return m;
    }

    /**
     * @see org.kuali.module.purap.bo.PurApItem#getAccountingLineClass()
     */
    public Class getAccountingLineClass() {
        return PurchaseOrderAccount.class;
    }

    public boolean isCanInactivateItem() {
        if (versionNumber == null) {
            //don't allow newly added item to be inactivatable.
            return false;
        }
        else if (versionNumber!= null && itemActiveIndicator && !getPurchaseOrder().getContainsUnpaidPaymentRequestsOrCreditMemos()) {
            return true;
        }
        return false;
    }
}
