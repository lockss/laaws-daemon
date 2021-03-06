/*
 * $Id$
 */

/*

Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.util.urlconn;

import org.lockss.daemon.*;
import org.lockss.plugin.*;


public interface CacheResultHandler extends PluginFetchEventResponse {

  /** Unused */
  @Deprecated
  public void init(CacheResultMap map) throws PluginException;

  /** Determine what action to take in response to HTTP result codes.
   *
   * The handler should return a CacheException with attributes specifying
   * the number and timing of retries, if any, and the action the crawler
   * should take (ignore, warning, error, abort).  See
   * HttpReaultHandler.initExceptionTable()} for the default actions.
   */
  public CacheException handleResult(ArchivalUnit au,
				     String url,
				     int code)
      throws PluginException;

  /** Determine what action to take when an exception is thrown while
   * fetching content from the network or writing to the repository.  The
   * exception may be:<ul>
   *
   * <li>An IOException thrown while opening a socket (e.g.,
   * UnknownHostException, SocketException)</li>
   *
   * <li>An IOException thrown while reading data from a socket (e.g.,
   * LockssUrlConnection.ConnectionTimeoutException,
   * SocketTimeoutException)</li>
   *
   * <li>a CacheException.RepositoryException wrapping an exception thrown
   * while storing data in the repository</li>
   *
   * <li>a ContentValidationException throws by DefaultUrlCacher or a
   * plugin FileValidator</li>
   *
   * </ul>
   *
   * The handler should return a CacheException with attributes specifying
   * the number and timing of retries, if any, and the action the crawler
   * should take (ignore, warning, error, abort).  See
   * HttpReaultHandler.initExceptionTable()} for the default actions.
   */
  public CacheException handleResult(ArchivalUnit au,
				     String url,
				     Exception ex)
      throws PluginException;
}
