/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.silverchair.oup;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;

public class OupScHtmlCrawlFilterFactory implements FilterFactory {

  /*
   * AMA = American Medical Association (http://jamanetwork.com/)
   * Tabs 20151025
   * 1=extract/abstract/article
   * 2=discussion (w/i framework of article contents)
   * 3=figures
   * 4=tables
   * 5=video
   * 6=references
   * 7=letters
   * 8=cme
   * 9=citing
   * 10=comments
   * 12=supplemental
   * SPIE = SPIE (http://spiedigitallibrary.org/)
   * Tabs 20151025
   * 1= extract/abstract/article
   * 2=figures
   * 3=tables
   * 5=multimedia
   * 6=references
   *
   */

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    return new HtmlFilterInputStream(
    	//	<div id="resourceTypeList-OUP_Issue" class="article-list-resources">
      in,
      encoding,
    	  HtmlNodeFilterTransform.exclude(new OrFilter(new NodeFilter[] {
    		  HtmlNodeFilters.tagWithAttributeRegex("a", "class", "prev"),
    		  HtmlNodeFilters.tagWithAttributeRegex("a", "class", "next"),
    	  })
      )
    );
  }

}