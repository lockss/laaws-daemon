/*
 * $Id: TestColumnDescriptor.java,v 1.1 2003-07-01 19:45:18 troberts Exp $
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.daemon.status;
import org.lockss.test.*;

public class TestColumnDescriptor extends LockssTestCase {
  public void testEqualsIfEqual() {
    ColumnDescriptor cd1 = new ColumnDescriptor("name", "title", 0);
    ColumnDescriptor cd2 = new ColumnDescriptor("name", "title", 0);
    assertTrue(cd1.equals(cd2));
    assertTrue(cd2.equals(cd1));
  }

  public void testNotEqualsIfDifferent() {
    ColumnDescriptor cd1 = new ColumnDescriptor("name", "title", 0);
    ColumnDescriptor cd2 = new ColumnDescriptor("name2", "title", 0);
    ColumnDescriptor cd3 = new ColumnDescriptor("name", "title2", 0);
    ColumnDescriptor cd4 = new ColumnDescriptor("name", "title", 2);
    assertFalse(cd1.equals(cd2));
    assertFalse(cd1.equals(cd3));
    assertFalse(cd1.equals(cd4));

    assertFalse(cd2.equals(cd1));
    assertFalse(cd2.equals(cd3));
    assertFalse(cd2.equals(cd4));

    assertFalse(cd3.equals(cd1));
    assertFalse(cd3.equals(cd2));
    assertFalse(cd3.equals(cd4));

    assertFalse(cd4.equals(cd1));
    assertFalse(cd4.equals(cd2));
    assertFalse(cd4.equals(cd3));
  }

  public void testNotEqualsIfDiffType() {
    ColumnDescriptor cd1 = new ColumnDescriptor("name", "title", 0);
    assertFalse(cd1.equals("String"));
  }

}
