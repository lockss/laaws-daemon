/*
 * $Id: EdinburghUniversityPressHtmlFilterFactory.java,v 1.2 2010-04-12 22:58:10 edwardsb1 Exp $
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
package org.lockss.plugin.edinburgh;

import java.io.InputStream;

import org.htmlparser.filters.TagNameFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;

/**
 * @author edwardsb
 *
 */
public class EdinburghUniversityPressHtmlFilterFactory implements FilterFactory {

  /* (non-Javadoc)
   * @see org.lockss.plugin.FilterFactory#createFilteredInputStream(org.lockss.plugin.ArchivalUnit, java.io.InputStream, java.lang.String)
   */
  public InputStream createFilteredInputStream(ArchivalUnit au, InputStream in,
      String encoding) throws PluginException {
    HtmlTransform[] transforms = new HtmlTransform[] {
        HtmlNodeFilterTransform.exclude(new TagNameFilter("script")),

        HtmlNodeFilterTransform.exclude(HtmlNodeFilters.tagWithAttributeRegex("div",
            "class",
            "sidebar_panel")),
            
        HtmlNodeFilterTransform.exclude(HtmlNodeFilters.tagWithAttribute("div",
            "id",
            "footer")),
            
        HtmlNodeFilterTransform.exclude(HtmlNodeFilters.tagWithAttributeRegex("div",
            "class",
            "rightColumn_panels")),
    };
    return new HtmlFilterInputStream(in,
                                     encoding,
                                     new HtmlCompoundTransform(transforms));

  }

}
