/*
 * Copyright (c) 2004, 2005 The National Association of College and University Business Officers,
 * Cornell University, Trustees of Indiana University, Michigan State University Board of Trustees,
 * Trustees of San Joaquin Delta College, University of Hawai'i, The Arizona Board of Regents on
 * behalf of the University of Arizona, and the r*smart group.
 * 
 * Licensed under the Educational Community License Version 1.0 (the "License"); By obtaining,
 * using and/or copying this Original Work, you agree that you have read, understand, and will
 * comply with the terms and conditions of the Educational Community License.
 * 
 * You may obtain a copy of the License at:
 * 
 * http://kualiproject.org/license.html
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES
 * OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package org.kuali.module.gl;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.kuali.core.service.PersistenceService;
import org.kuali.core.util.SpringServiceLocator;
import org.kuali.module.gl.bo.OriginEntry;
import org.kuali.module.gl.bo.OriginEntryGroup;
import org.kuali.module.gl.dao.OriginEntryDao;
import org.kuali.module.gl.dao.OriginEntryGroupDao;
import org.kuali.module.gl.dao.UnitTestSqlDao;
import org.kuali.test.KualiTestBaseWithSpringOnly;
import org.springframework.beans.factory.BeanFactory;

public class OriginEntryTestBase extends KualiTestBaseWithSpringOnly {
  private static org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(OriginEntryTestBase.class);

  protected BeanFactory beanFactory;
  protected TestDateTimeService dateTimeService;
  protected PersistenceService persistenceService;
  protected UnitTestSqlDao unitTestSqlDao = null;
  protected OriginEntryDao originEntryDao = null;
  protected OriginEntryGroupDao originEntryGroupDao = null;
  protected Date date = new Date();

  public OriginEntryTestBase() {
    super();
  }

  /* (non-Javadoc)
   * @see org.springframework.test.AbstractTransactionalSpringContextTests#onSetUpInTransaction()
   */
  protected void setUp() throws Exception {
    super.setUp();

    beanFactory = SpringServiceLocator.getBeanFactory();

    dateTimeService = (TestDateTimeService)beanFactory.getBean("testDateTimeService");

    // Get this for use in retrieving objects
    persistenceService = (PersistenceService)beanFactory.getBean("persistenceService");

    // get the sql DAO
    unitTestSqlDao = (UnitTestSqlDao) beanFactory.getBean("glUnitTestSqlDao");

    // Origin Entry DAO's
    originEntryDao = (OriginEntryDao) beanFactory.getBean("glOriginEntryDao");
    originEntryGroupDao = (OriginEntryGroupDao) beanFactory.getBean("glOriginEntryGroupDao");
  }


  protected class EntryHolder {
    public String groupCode;
    public String transactionLine;
    public EntryHolder(String groupCode,String transactionLine) {
      this.groupCode = groupCode;
      this.transactionLine = transactionLine;
    }
  }

  protected void loadInputTransactions(String groupCode,String[] transactions) {
    OriginEntryGroup group = createNewGroup(groupCode);
    loadTransactions(transactions,group);
  }

  protected void loadTransactions(String[] transactions, OriginEntryGroup group) {
    for (int i = 0; i < transactions.length; i++) {
        createEntry(transactions[i], group);
    }
  }

  protected void clearGlEntryTable() {
    unitTestSqlDao.sqlCommand("delete from gl_entry_t");
  }

  protected void clearReversalTable() {
    unitTestSqlDao.sqlCommand("delete from gl_reversal_t");
  }

  protected void clearGlBalanceTable() {
    unitTestSqlDao.sqlCommand("delete from gl_balance_t");
  }

  protected void clearEncumbranceTable() {
    unitTestSqlDao.sqlCommand("delete from gl_encumbrance_t");    
  }

  protected void clearGlAccountBalanceTable() {
    unitTestSqlDao.sqlCommand("delete from gl_acct_balances_t");
  }

  protected void clearOriginEntryTables() {
    unitTestSqlDao.sqlCommand("delete from gl_origin_entry_t");
    unitTestSqlDao.sqlCommand("delete from gl_origin_entry_grp_t");
  }

  protected OriginEntryGroup createNewGroup(String code) {
    OriginEntryGroup group = new OriginEntryGroup();
    group.setDate(new java.sql.Date(date.getTime()));
    group.setProcess(Boolean.TRUE);
    group.setScrub(Boolean.TRUE);
    group.setValid(Boolean.TRUE);
    group.setSourceCode(code);
    originEntryGroupDao.save(group);
    return group;
  }

  protected OriginEntry createEntry(String line, OriginEntryGroup group) {
    OriginEntry entry = new OriginEntry(line);
    entry.setGroup(group);
    
    // This is being done to fool the caching.  If it isn't done, when
    // we try to retrieve this entry later, none of the referenced tables will
    // be loaded.
    persistenceService.retrieveNonKeyFields(entry);

    originEntryDao.saveOriginEntry(entry);
    return entry;
  }

  /**
   * Check all the entries in gl_origin_entry_t against the data passed in EntryHolder[].  If any of them
   * are different, assert an error.
   * 
   * @param requiredEntries
   */
  protected void assertOriginEntries(int groupCount,EntryHolder[] requiredEntries) {
      List groups = unitTestSqlDao.sqlSelect("select * from gl_origin_entry_grp_t order by origin_entry_grp_src_cd");
      assertEquals("Number of groups is wrong", groupCount, groups.size());

      Collection c = originEntryDao.testingGetAllEntries();
      assertEquals("Wrong number of transactions in Origin Entry",requiredEntries.length,c.size());

      int count = 0;
      for (Iterator iter = c.iterator(); iter.hasNext();) {
        OriginEntry foundTransaction = (OriginEntry)iter.next();

        // Check group
        int group = getGroup(groups,requiredEntries[count].groupCode);
        assertEquals("Group for transaction " + foundTransaction.getEntryId() + " is wrong",group,foundTransaction.getEntryGroupId().intValue());

        // Check transaction - this is done this way so that Anthill prints the two transactions to make
        // resolving the issue easier.
        if ( ! foundTransaction.getLine().trim().equals(requiredEntries[count].transactionLine.trim()) ) {
          System.err.println("Expected transaction: " + requiredEntries[count].transactionLine);
          System.err.println("Found transaction:    " + foundTransaction.getLine());
          fail("Transaction " + foundTransaction.getEntryId() + " doesn't match expected output");
        }
        count++;
      }
  }

  protected int getGroup(List groups,String groupCode) {
    for (Iterator iter = groups.iterator(); iter.hasNext();) {
      Map element = (Map)iter.next();

      String sourceCode = (String)element.get("ORIGIN_ENTRY_GRP_SRC_CD");
      if ( groupCode.equals(sourceCode) ) {
        BigDecimal groupId = (BigDecimal)element.get("ORIGIN_ENTRY_GRP_ID");
        return groupId.intValue();
      }
    }
    return -1;
  }
}
