/*
 * $Id: TestDbManager.java,v 1.12 2014-09-23 22:42:36 fergaloy-sf Exp $
 */

/*

 Copyright (c) 2013-2014 Board of Trustees of Leland Stanford Jr. University,
 all rights reserved.

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
 STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 Except as contained in this notice, the name of Stanford University shall not
 be used in advertising or otherwise to promote the sale, use or other dealings
 in this Software without prior written authorization from Stanford University.

 */
package org.lockss.db;

import static org.lockss.db.SqlConstants.*;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;
import org.lockss.config.ConfigManager;
import org.lockss.repository.LockssRepositoryImpl;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockLockssDaemon;
import org.lockss.util.Logger;

/**
 * Test class for org.lockss.db.DbManager.
 * 
 * @author Fernando Garcia-Loygorri
 * @version 1.0
 */
public class TestDbManager extends LockssTestCase {
  private static String TABLE_CREATE_SQL =
      "create table testtable (id bigint NOT NULL, name varchar(512))";

  private MockLockssDaemon theDaemon;
  private String tempDirPath;
  private DbManager dbManager;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    // Get the temporary directory used during the test.
    tempDirPath = getTempDir().getAbsolutePath();

    // Set the database log.
    System.setProperty("derby.stream.error.file",
		       new File(tempDirPath, "derby.log").getAbsolutePath());

    theDaemon = getMockLockssDaemon();
    theDaemon.setDaemonInited(true);
  }

  /**
   * Tests table creation with the minimal configuration.
   * 
   * @throws Exception
   */
  public void testCreateTable1() throws Exception {
    Properties props = new Properties();
    props.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    props
	.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, tempDirPath);
    ConfigurationUtil.setCurrentConfigFromProps(props);

    createTable();
  }

  /**
   * Tests table creation with an absolute database name path.
   * 
   * @throws Exception
   */
  public void testCreateTable2() throws Exception {
    Properties props = new Properties();
    props.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    props
	.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, tempDirPath);
    props.setProperty(DbManager.PARAM_DATASOURCE_DATABASENAME,
		      new File(tempDirPath, "db/TestDbManager")
			  .getCanonicalPath());
    ConfigurationUtil.setCurrentConfigFromProps(props);

    createTable();
  }

  /**
   * Tests table creation with the client datasource.
   * 
   * @throws Exception
   */
  public void testCreateTable3() throws Exception {
    Properties props = new Properties();
    props.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    props
	.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, tempDirPath);
    props.setProperty(DbManager.PARAM_DATASOURCE_CLASSNAME,
		      "org.apache.derby.jdbc.ClientDataSource");
    props.setProperty(DbManager.PARAM_DATASOURCE_PASSWORD, "somePassword");
    ConfigurationUtil.setCurrentConfigFromProps(props);

    createTable();
  }

  /**
   * Tests that setting enabled param to false works.
   * 
   * @throws Exception
   */
  public void testDisabled() throws Exception {
    ConfigurationUtil.addFromArgs(DbManager.PARAM_DBMANAGER_ENABLED, "false");

    startService();
    assertFalse(dbManager.isReady());

    try {
      dbManager.getConnection();
      fail("getConnection() should throw");
    } catch (DbException sqle) {
    }
  }

  /**
   * Tests a misconfigured datasource.
   * 
   * @throws Exception
   */
  public void testNotReady() throws Exception {
    Properties props = new Properties();
    props.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    props
	.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, tempDirPath);
    props.setProperty(DbManager.PARAM_DATASOURCE_CLASSNAME, "java.lang.String");
    ConfigurationUtil.setCurrentConfigFromProps(props);

    startService();
    assertEquals(false, dbManager.isReady());

    try {
      dbManager.getConnection();
      fail("getConnection() should throw");
    } catch (DbException sqle) {
    }
  }

  /**
   * Tests the safe roll back.
   * 
   * @throws Exception
   */
  public void testRollback() throws Exception {
    Properties props = new Properties();
    props.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    props
	.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, tempDirPath);
    ConfigurationUtil.setCurrentConfigFromProps(props);

    startService();
    assertTrue(dbManager.isReady());
    DbManagerSql dbManagerSql = dbManager.getDbManagerSql();

    Connection conn = dbManager.getConnection();
    assertNotNull(conn);
    assertFalse(dbManagerSql.tableExists(conn, "testtable"));
    assertTrue(dbManagerSql.createTableIfMissing(conn, "testtable",
					      TABLE_CREATE_SQL));
    assertTrue(dbManagerSql.tableExists(conn, "testtable"));

    DbManager.safeRollbackAndClose(conn);
    conn = dbManager.getConnection();
    assertNotNull(conn);
    assertFalse(dbManagerSql.tableExists(conn, "testtable"));
  }

  /**
   * Tests the commit or rollback method.
   * 
   * @throws Exception
   */
  public void testCommitOrRollback() throws Exception {
    Properties props = new Properties();
    props.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    props
	.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, tempDirPath);
    ConfigurationUtil.setCurrentConfigFromProps(props);

    startService();
    Connection conn = dbManager.getConnection();
    Logger logger = Logger.getLogger("testCommitOrRollback");
    DbManager.commitOrRollback(conn, logger);
    DbManager.safeCloseConnection(conn);

    conn = null;
    try {
      DbManager.commitOrRollback(conn, logger);
    } catch (DbException dbe) {
    }
  }

  @Override
  public void tearDown() throws Exception {
    dbManager.waitForThreadsToFinish(500);
    dbManager.stopService();
    theDaemon.stopDaemon();
    super.tearDown();
  }

  /**
   * Creates a table and verifies that it exists.
   * 
   * @throws Exception
   */
  protected void createTable() throws Exception {
    startService();
    assertTrue(dbManager.isReady());
    DbManagerSql dbManagerSql = dbManager.getDbManagerSql();

    Connection conn = dbManager.getConnection();
    assertNotNull(conn);
    assertFalse(dbManagerSql.tableExists(conn, "testtable"));
    assertTrue(dbManagerSql.createTableIfMissing(conn, "testtable",
					      TABLE_CREATE_SQL));
    assertTrue(dbManagerSql.tableExists(conn, "testtable"));
    dbManagerSql.logTableSchema(conn, "testtable");
    assertFalse(dbManagerSql.createTableIfMissing(conn, "testtable",
					       TABLE_CREATE_SQL));
  }

  /**
   * Creates and starts the DbManager.
   * 
   * @throws Exception
   */
  protected void startService() throws Exception {
    dbManager = new DbManager();
    theDaemon.setDbManager(dbManager);
    dbManager.initService(theDaemon);
    dbManager.startService();
  }

  /**
   * Tests an empty database before updating.
   * 
   * @throws Exception
   */
  public void testEmptyDbSetup() throws Exception {
    Properties props = new Properties();
    props.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    props
	.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, tempDirPath);
    ConfigurationUtil.setCurrentConfigFromProps(props);

    dbManager = new DbManager();
    theDaemon.setDbManager(dbManager);
    dbManager.initService(theDaemon);
    assertTrue(dbManager.setUpDatabase(0));
    dbManager.setTargetDatabaseVersion(0);
    dbManager.startService();
    assertTrue(dbManager.isReady());
    DbManagerSql dbManagerSql = dbManager.getDbManagerSql();

    Connection conn = dbManager.getConnection();
    assertNotNull(conn);
    assertFalse(dbManagerSql.tableExists(conn,
	SqlConstants.OBSOLETE_METADATA_TABLE));
  }

  /**
   * Tests version 1 set up.
   * 
   * @throws Exception
   */
  public void testV1Setup() throws Exception {
    Properties props = new Properties();
    props.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    props
	.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, tempDirPath);
    ConfigurationUtil.setCurrentConfigFromProps(props);

    dbManager = new DbManager();
    theDaemon.setDbManager(dbManager);
    dbManager.initService(theDaemon);
    assertTrue(dbManager.setUpDatabase(1));
    dbManager.setTargetDatabaseVersion(1);
    dbManager.startService();
    assertTrue(dbManager.isReady());
    DbManagerSql dbManagerSql = dbManager.getDbManagerSql();

    Connection conn = dbManager.getConnection();
    assertNotNull(conn);
    assertTrue(dbManagerSql.tableExists(conn,
	SqlConstants.OBSOLETE_METADATA_TABLE));

    PreparedStatement ps = dbManager.prepareStatement(conn,
	"select count(*) from " + SqlConstants.OBSOLETE_METADATA_TABLE);

    assertEquals(DbManager.DEFAULT_FETCH_SIZE, ps.getFetchSize());
  }

  /**
   * Tests an attempt to update the database to a lower version.
   * 
   * @throws Exception
   */
  public void testV1ToV0Update() throws Exception {
    Properties props = new Properties();
    props.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    props
	.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, tempDirPath);
    ConfigurationUtil.setCurrentConfigFromProps(props);

    dbManager = new DbManager();
    theDaemon.setDbManager(dbManager);
    dbManager.initService(theDaemon);
    assertTrue(dbManager.setUpDatabase(1));
    dbManager.setTargetDatabaseVersion(0);
    dbManager.startService();
    assertFalse(dbManager.isReady());
  }

  /**
   * Tests the update of the database from version 0 to version 1.
   * 
   * @throws Exception
   */
  public void testV0ToV1Update() throws Exception {
    Properties props = new Properties();
    props.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    props
	.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, tempDirPath);
    props.setProperty(DbManager.PARAM_FETCH_SIZE, "12345");
    ConfigurationUtil.setCurrentConfigFromProps(props);

    dbManager = new DbManager();
    theDaemon.setDbManager(dbManager);
    dbManager.initService(theDaemon);
    assertTrue(dbManager.setUpDatabase(0));
    dbManager.setTargetDatabaseVersion(1);
    dbManager.startService();
    assertTrue(dbManager.isReady());
    DbManagerSql dbManagerSql = dbManager.getDbManagerSql();

    Connection conn = dbManager.getConnection();
    assertNotNull(conn);
    assertTrue(dbManagerSql.tableExists(conn,
	SqlConstants.OBSOLETE_METADATA_TABLE));

    PreparedStatement ps = dbManager.prepareStatement(conn,
	"select count(*) from " + SqlConstants.OBSOLETE_METADATA_TABLE);

    assertEquals(12345, ps.getFetchSize());
  }

  /**
   * Tests the update of the database from version 0 to version 2.
   * 
   * @throws Exception
   */
  public void testV0ToV2Update() throws Exception {
    Properties props = new Properties();
    props.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    props
	.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, tempDirPath);
    ConfigurationUtil.setCurrentConfigFromProps(props);

    dbManager = new DbManager();
    theDaemon.setDbManager(dbManager);
    dbManager.initService(theDaemon);
    assertTrue(dbManager.setUpDatabase(0));
    dbManager.setTargetDatabaseVersion(2);
    dbManager.startService();
    assertTrue(dbManager.isReady());
    DbManagerSql dbManagerSql = dbManager.getDbManagerSql();

    Connection conn = dbManager.getConnection();
    assertNotNull(conn);
    assertFalse(dbManagerSql.tableExists(conn,
	SqlConstants.OBSOLETE_METADATA_TABLE));
    assertTrue(dbManagerSql.tableExists(conn, SqlConstants.VERSION_TABLE));
  }

  /**
   * Tests the update of the database from version 1 to version 2.
   * 
   * @throws Exception
   */
  public void testV1ToV2Update() throws Exception {
    Properties props = new Properties();
    props.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    props
	.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, tempDirPath);
    ConfigurationUtil.setCurrentConfigFromProps(props);

    dbManager = new DbManager();
    theDaemon.setDbManager(dbManager);
    dbManager.initService(theDaemon);
    assertTrue(dbManager.setUpDatabase(1));
    dbManager.setTargetDatabaseVersion(2);
    dbManager.startService();
    assertTrue(dbManager.isReady());
    DbManagerSql dbManagerSql = dbManager.getDbManagerSql();

    Connection conn = dbManager.getConnection();
    assertNotNull(conn);
    assertFalse(dbManagerSql.tableExists(conn,
	SqlConstants.OBSOLETE_METADATA_TABLE));
    assertTrue(dbManagerSql.tableExists(conn, SqlConstants.VERSION_TABLE));
  }

  /**
   * Tests text truncation.
   * 
   * @throws Exception
   */
  public void testTruncation() throws Exception {
    Properties props = new Properties();
    props.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    props
	.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, tempDirPath);
    ConfigurationUtil.setCurrentConfigFromProps(props);

    startService();

    String original = "Total characters = 21";

    String truncated = DbManagerSql.truncateVarchar(original, 30);
    assertTrue(original.equals(truncated));
    assertFalse(DbManagerSql.isTruncatedVarchar(truncated));

    truncated = DbManagerSql.truncateVarchar(original, original.length());
    assertTrue(original.equals(truncated));
    assertFalse(DbManagerSql.isTruncatedVarchar(truncated));

    truncated = DbManagerSql.truncateVarchar(original, original.length() - 3);
    assertFalse(original.equals(truncated));
    assertTrue(DbManagerSql.isTruncatedVarchar(truncated));
    assertTrue(truncated.length() == original.length() - 3);
  }

  /**
   * Tests authentication with the default data source.
   * 
   * @throws Exception
   */
  public void testAuthenticationDefault() throws Exception {
    Properties props = new Properties();
    props.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    props
	.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, tempDirPath);
    ConfigurationUtil.setCurrentConfigFromProps(props);

    startService();

    String dbUrlRoot = "jdbc:derby://localhost:1527/" + tempDirPath
	+ "/db/DbManager";

    try {
      Class.forName("org.apache.derby.jdbc.ClientDriver").newInstance();
      DriverManager.getConnection(dbUrlRoot);
      fail("getConnection() should throw");
    } catch (Exception e) {
    }

    String dbUrl = dbUrlRoot + ";user=LOCKSS";

    try {
      Class.forName("org.apache.derby.jdbc.ClientDriver").newInstance();
      DriverManager.getConnection(dbUrl);
      fail("getConnection() should throw");
    } catch (Exception e) {
    }

    dbUrl = dbUrlRoot + ";user=LOCKSS;password=somePassword";

    try {
      Class.forName("org.apache.derby.jdbc.ClientDriver").newInstance();
      DriverManager.getConnection(dbUrl);
      fail("getConnection() should throw");
    } catch (Exception e) {
    }
  }

  /**
   * Tests authentication with the embedded data source.
   * 
   * @throws Exception
   */
  public void testAuthenticationEmbedded() throws Exception {
    Properties props = new Properties();
    props.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    props
	.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, tempDirPath);
    props.setProperty(DbManager.PARAM_DATASOURCE_CLASSNAME,
	"org.apache.derby.jdbc.EmbeddedDataSource");
    ConfigurationUtil.setCurrentConfigFromProps(props);

    startService();

    String dbUrlRoot = "jdbc:derby://localhost:1527/" + tempDirPath
	+ "/db/DbManager";

    try {
      Class.forName("org.apache.derby.jdbc.ClientDriver").newInstance();
      DriverManager.getConnection(dbUrlRoot);
      fail("getConnection() should throw");
    } catch (Exception e) {
    }

    String dbUrl = dbUrlRoot + ";user=LOCKSS";

    try {
      Class.forName("org.apache.derby.jdbc.ClientDriver").newInstance();
      DriverManager.getConnection(dbUrl);
      fail("getConnection() should throw");
    } catch (Exception e) {
    }

    dbUrl = dbUrlRoot + ";user=LOCKSS;password=somePassword";

    try {
      Class.forName("org.apache.derby.jdbc.ClientDriver").newInstance();
      DriverManager.getConnection(dbUrl);
      fail("getConnection() should throw");
    } catch (Exception e) {
    }
  }

  /**
   * Tests authentication with the client data source.
   * 
   * @throws Exception
   */
  public void testAuthenticationClient() throws Exception {
    Properties props = new Properties();
    props.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    props
	.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, tempDirPath);
    props.setProperty(DbManager.PARAM_DATASOURCE_CLASSNAME,
	"org.apache.derby.jdbc.ClientDataSource");
    props.setProperty(DbManager.PARAM_DATASOURCE_PASSWORD, "somePassword");
    ConfigurationUtil.setCurrentConfigFromProps(props);

    startService();

    Connection conn = null;

    String dbUrlRoot = "jdbc:derby://localhost:1527/" + tempDirPath
	+ "/db/DbManager";

    try {
      Class.forName("org.apache.derby.jdbc.ClientDriver").newInstance();
      conn = DriverManager.getConnection(dbUrlRoot);
      fail("getConnection() should throw");
    } catch (Exception e) {
    }

    String dbUrl = dbUrlRoot + ";user=LOCKSS";

    try {
      Class.forName("org.apache.derby.jdbc.ClientDriver").newInstance();
      conn = DriverManager.getConnection(dbUrl);
      fail("getConnection() should throw");
    } catch (Exception e) {
    }

    dbUrl = dbUrlRoot + ";user=LOCKSS;password=wrongPassword";

    try {
      Class.forName("org.apache.derby.jdbc.ClientDriver").newInstance();
      conn = DriverManager.getConnection(dbUrl);
      fail("getConnection() should throw");
    } catch (Exception e) {
    }

    dbUrl = dbUrlRoot + ";password=somePassword";

    try {
      Class.forName("org.apache.derby.jdbc.ClientDriver").newInstance();
      conn = DriverManager.getConnection(dbUrl);
      fail("getConnection() should throw");
    } catch (Exception e) {
    }

    dbUrl = dbUrlRoot + ";user=wrongUser;password=somePassword";

    try {
      Class.forName("org.apache.derby.jdbc.ClientDriver").newInstance();
      conn = DriverManager.getConnection(dbUrl);
      fail("getConnection() should throw");
    } catch (Exception e) {
    }

    dbUrl = dbUrlRoot + ";user=LOCKSS;password=somePassword";

    try {
      Class.forName("org.apache.derby.jdbc.ClientDriver").newInstance();
      conn = DriverManager.getConnection(dbUrl);
    } catch (Exception e) {
      throw new Exception("Cannot get connection", e);
    }

    assertNotNull(conn);
  }

  /**
   * Tests set up with missing credentials.
   * 
   * @throws Exception
   */
  public void testMissingCredentialsSetUp() throws Exception {
    Properties props = new Properties();
    props.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    props
	.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, tempDirPath);
    props.setProperty(DbManager.PARAM_DATASOURCE_CLASSNAME,
	      "org.apache.derby.jdbc.ClientDataSource");
    ConfigurationUtil.setCurrentConfigFromProps(props);

    startService();
    assertTrue(dbManager.isReady());
  }

  /**
   * Tests set up with missing user.
   * 
   * @throws Exception
   */
  public void testMissingUserSetUp() throws Exception {
    Properties props = new Properties();
    props.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    props
	.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, tempDirPath);
    props.setProperty(DbManager.PARAM_DATASOURCE_CLASSNAME,
	      "org.apache.derby.jdbc.ClientDataSource");
    props.setProperty(DbManager.PARAM_DATASOURCE_USER, "");
    props.setProperty(DbManager.PARAM_DATASOURCE_PASSWORD, "somePassword");
    ConfigurationUtil.setCurrentConfigFromProps(props);

    startService();
    assertFalse(dbManager.isReady());
  }

  /**
   * Tests set up with missing password.
   * 
   * @throws Exception
   */
  public void testMissingPasswordSetUp() throws Exception {
    Properties props = new Properties();
    props.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    props
	.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, tempDirPath);
    props.setProperty(DbManager.PARAM_DATASOURCE_CLASSNAME,
	      "org.apache.derby.jdbc.ClientDataSource");
    props.setProperty(DbManager.PARAM_DATASOURCE_USER, "LOCKSS");
    props.setProperty(DbManager.PARAM_DATASOURCE_PASSWORD, "");
    ConfigurationUtil.setCurrentConfigFromProps(props);

    startService();
    assertFalse(dbManager.isReady());
  }

  /**
   * Tests the provider functionality.
   * 
   * @throws Exception
   */
  public void testProvider() throws Exception {
    Properties props = new Properties();
    props.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    props
	.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, tempDirPath);
    ConfigurationUtil.setCurrentConfigFromProps(props);

    startService();
    assertTrue(dbManager.isReady());

    Connection conn = dbManager.getConnection();

    // Add a provider with no LOCKSS identifier.
    Long providerSeq1 =
	dbManager.findOrCreateProvider(conn, null, "providerName1");
    checkProvider(conn, providerSeq1, null, "providerName1");

    // Add the new LOCKSS identifier to the same provider.
    Long providerSeq2 =
	dbManager.findOrCreateProvider(conn, "providerLid1", "providerName1");
    assertEquals(providerSeq1, providerSeq2);
    checkProvider(conn, providerSeq2, "providerLid1", "providerName1");

    // Add a new provider with a LOCKSS identifier.
    Long providerSeq3 =
	dbManager.findOrCreateProvider(conn, "providerLid2", "providerName2");
    assertNotEquals(providerSeq1, providerSeq3);
    checkProvider(conn, providerSeq3, "providerLid2", "providerName2");

    DbManager.commitOrRollback(conn, log);
    DbManager.safeCloseConnection(conn);
  }

  private void checkProvider(Connection conn, Long providerSeq,
      String expectedLid, String expectedName) throws Exception {
    String query = "select " + PROVIDER_NAME_COLUMN
	+ "," + PROVIDER_LID_COLUMN
	+ " from " + PROVIDER_TABLE
	+ " where " + PROVIDER_SEQ_COLUMN + " = ?";
    PreparedStatement stmt = dbManager.prepareStatement(conn, query);
    stmt.setLong(1, providerSeq);
    ResultSet resultSet = dbManager.executeQuery(stmt);

    assertTrue(resultSet.next());
    assertEquals(expectedLid, resultSet.getString(PROVIDER_LID_COLUMN));
    assertEquals(expectedName, resultSet.getString(PROVIDER_NAME_COLUMN));
    assertFalse(resultSet.next());
  }
}
