package org.kuali.kfs.module.cab.businessobject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.kuali.kfs.sys.businessobject.FinancialSystemDocumentHeader;
import org.kuali.kfs.sys.businessobject.FinancialSystemDocumentTypeCode;
import org.kuali.rice.kns.bo.PersistableBusinessObjectBase;
import org.kuali.rice.kns.util.TypedArrayList;

/**
 * @author Kuali Nervous System Team (kualidev@oncourse.iu.edu)
 */
public class PurchasingAccountsPayableDocument extends PersistableBusinessObjectBase {

    private String documentNumber;
    private Integer purapDocumentIdentifier;
    private Integer purchaseOrderIdentifier;
    private String documentTypeCode;
    private boolean active;

    // References
    private FinancialSystemDocumentTypeCode financialSystemDocumentTypeCode;
    private FinancialSystemDocumentHeader documentHeader;
    private List<PurchasingAccountsPayableItemAsset> purchasingAccountsPayableItemAssets;
    
    // non-persistent
    private String purApContactEmailAddress;
    private String purApContactPhoneNumber;
    
    public PurchasingAccountsPayableDocument() {
        this.purchasingAccountsPayableItemAssets = new TypedArrayList(PurchasingAccountsPayableItemAsset.class);
    }

    
    /**
     * Gets the documentNumber attribute. 
     * @return Returns the documentNumber.
     */
    public String getDocumentNumber() {
        return documentNumber;
    }


    /**
     * Sets the documentNumber attribute value.
     * @param documentNumber The documentNumber to set.
     */
    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }


    /**
     * Gets the purapDocumentIdentifier attribute. 
     * @return Returns the purapDocumentIdentifier.
     */
    public Integer getPurapDocumentIdentifier() {
        return purapDocumentIdentifier;
    }


    /**
     * Sets the purapDocumentIdentifier attribute value.
     * @param purapDocumentIdentifier The purapDocumentIdentifier to set.
     */
    public void setPurapDocumentIdentifier(Integer purapDocumentIdentifier) {
        this.purapDocumentIdentifier = purapDocumentIdentifier;
    }


    /**
     * Gets the purchaseOrderIdentifier attribute. 
     * @return Returns the purchaseOrderIdentifier.
     */
    public Integer getPurchaseOrderIdentifier() {
        return purchaseOrderIdentifier;
    }


    /**
     * Sets the purchaseOrderIdentifier attribute value.
     * @param purchaseOrderIdentifier The purchaseOrderIdentifier to set.
     */
    public void setPurchaseOrderIdentifier(Integer purchaseOrderIdentifier) {
        this.purchaseOrderIdentifier = purchaseOrderIdentifier;
    }


    /**
     * Gets the documentTypeCode attribute. 
     * @return Returns the documentTypeCode.
     */
    public String getDocumentTypeCode() {
        return documentTypeCode;
    }


    /**
     * Sets the documentTypeCode attribute value.
     * @param documentTypeCode The documentTypeCode to set.
     */
    public void setDocumentTypeCode(String documentTypeCode) {
        this.documentTypeCode = documentTypeCode;
    }


    /**
     * Gets the active attribute. 
     * @return Returns the active.
     */
    public boolean isActive() {
        return active;
    }


    /**
     * Sets the active attribute value.
     * @param active The active to set.
     */
    public void setActive(boolean active) {
        this.active = active;
    }


    /**
     * Gets the financialSystemDocumentTypeCode attribute. 
     * @return Returns the financialSystemDocumentTypeCode.
     */
    public FinancialSystemDocumentTypeCode getFinancialSystemDocumentTypeCode() {
        return financialSystemDocumentTypeCode;
    }


    /**
     * Sets the financialSystemDocumentTypeCode attribute value.
     * @param financialSystemDocumentTypeCode The financialSystemDocumentTypeCode to set.
     */
    public void setFinancialSystemDocumentTypeCode(FinancialSystemDocumentTypeCode financialSystemDocumentTypeCode) {
        this.financialSystemDocumentTypeCode = financialSystemDocumentTypeCode;
    }


    /**
     * Gets the documentHeader attribute. 
     * @return Returns the documentHeader.
     */
    public FinancialSystemDocumentHeader getDocumentHeader() {
        return documentHeader;
    }


    /**
     * Sets the documentHeader attribute value.
     * @param documentHeader The documentHeader to set.
     */
    public void setDocumentHeader(FinancialSystemDocumentHeader documentHeader) {
        this.documentHeader = documentHeader;
    }


    /**
     * Gets the purchasingAccountsPayableItemAssets attribute. 
     * @return Returns the purchasingAccountsPayableItemAssets.
     */
    public List<PurchasingAccountsPayableItemAsset> getPurchasingAccountsPayableItemAssets() {
        return purchasingAccountsPayableItemAssets;
    }


    /**
     * Sets the purchasingAccountsPayableItemAssets attribute value.
     * @param purchasingAccountsPayableItemAssets The purchasingAccountsPayableItemAssets to set.
     */
    public void setPurchasingAccountsPayableItemAssets(List<PurchasingAccountsPayableItemAsset> purchasingAccountsPayableItemAssets) {
        this.purchasingAccountsPayableItemAssets = purchasingAccountsPayableItemAssets;
    }


    /**
     * Gets the purApContactEmailAddress attribute. 
     * @return Returns the purApContactEmailAddress.
     */
    public String getPurApContactEmailAddress() {
        return purApContactEmailAddress;
    }


    /**
     * Sets the purApContactEmailAddress attribute value.
     * @param purApContactEmailAddress The purApContactEmailAddress to set.
     */
    public void setPurApContactEmailAddress(String purApContactEmailAddress) {
        this.purApContactEmailAddress = purApContactEmailAddress;
    }


    /**
     * Gets the purApContactPhoneNumber attribute. 
     * @return Returns the purApContactPhoneNumber.
     */
    public String getPurApContactPhoneNumber() {
        return purApContactPhoneNumber;
    }


    /**
     * Sets the purApContactPhoneNumber attribute value.
     * @param purApContactPhoneNumber The purApContactPhoneNumber to set.
     */
    public void setPurApContactPhoneNumber(String purApContactPhoneNumber) {
        this.purApContactPhoneNumber = purApContactPhoneNumber;
    }


    /**
     * Need to override this method, so we can save item assets, the framework can delete the allocated item assets.
     * 
     * @see org.kuali.rice.kns.bo.PersistableBusinessObjectBase#buildListOfDeletionAwareLists()
     */
    @Override
    public List buildListOfDeletionAwareLists() {
        List<List> managedLists = new ArrayList<List>();

        managedLists.add(getPurchasingAccountsPayableItemAssets());
        return managedLists;
    }

    /**
     * @see org.kuali.rice.kns.bo.BusinessObjectBase#toStringMapper()
     */
    protected LinkedHashMap toStringMapper() {
        LinkedHashMap m = new LinkedHashMap();
        m.put("documentNumber", this.documentNumber);
        return m;
    }

}
