/*
 * $Id$
 */

/*

 Copyright (c) 2015 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.ws.control.client;

import java.util.ArrayList;
import java.util.List;
import org.lockss.ws.control.AuControlService;
import org.lockss.ws.entities.RequestCrawlResult;

/**
 * A client for the AuControlService.requestCrawlByIdList() web service
 * operation.
 */
public class RequestCrawlByIdListClient extends AuControlServiceBaseClient {
  /**
   * The main method.
   * 
   * @param args
   *          A String[] with the command line arguments.
   * @throws Exception
   */
  public static void main(String args[]) throws Exception {
    AuControlService proxy = new RequestCrawlByIdListClient().getProxy();

    String nextToLastArg = args[args.length - 2];
    Integer priority = null;

    // Assume args = auIds priority force.
    int auCount = args.length - 2;

    if (!"null".equals(nextToLastArg.toLowerCase())) {
      try {
	priority = Integer.parseInt(nextToLastArg);
	// It really is args = auIds priority force.
      } catch (NumberFormatException nfe) {
	// It really is args = auIds force.
	auCount = args.length - 1;
      }
    } else {
      // It really is args = auIds null force.
    }

    System.out.println("auCount = " + auCount);

    List<String> auIds = new ArrayList<String>(auCount);

    for (int i = 0; i < auCount; i++) {
      auIds.add(args[i]);
    }

    System.out.println("auIds = " + auIds);
    System.out.println("priority = " + priority);

    boolean force = Boolean.parseBoolean(args[args.length - 1]);
    System.out.println("force = " + force);

    List<RequestCrawlResult> result =
	proxy.requestCrawlByIdList(auIds, priority, force);
    System.out.println("result = " + result);
  }
}
