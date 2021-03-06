/*
 * $Id$
 */

/*

 Copyright (c) 2014 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.app.LockssDaemon;
import org.lockss.daemon.LockssRunnable;
import org.lockss.util.Logger;

/**
 * Migrates in a separate thread the database from version 16 to version 17.
 */
public class DbVersion16To17Migrator extends LockssRunnable {
  private static Logger log = Logger.getLogger(DbVersion16To17Migrator.class);

  /**
   * Constructor.
   */
  public DbVersion16To17Migrator() {
    super("DbVersion16To17Migrator");
  }

  /**
   * Entry point to start the process of migrating the database from version 16
   * to version 17.
   */
  public void lockssRun() {
    final String DEBUG_HEADER = "lockssRun(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    try {
      DbManager dbManager = LockssDaemon.getLockssDaemon().getDbManager();
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "Obtained DbManager.");

      DbManagerSql dbManagerSql = dbManager.getDbManagerSqlBeforeReady();
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "Obtained DbManagerSql.");

      // Perform the actual work.
      dbManagerSql.migrateDatabaseFrom16To17();

      dbManager.cleanUpThread("DbVersion16To17Migrator");
    } catch (Exception e) {
      log.error("Cannot migrate the database from version 16 to 17", e);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }
}
