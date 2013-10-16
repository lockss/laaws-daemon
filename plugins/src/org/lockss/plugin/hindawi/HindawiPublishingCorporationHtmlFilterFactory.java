/*
 * $Id: HindawiPublishingCorporationHtmlFilterFactory.java,v 1.14 2013-10-16 01:42:34 thib_gc Exp $
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.hindawi;

import java.io.*;
import java.util.*;

import org.htmlparser.*;
import org.htmlparser.filters.*;
import org.lockss.daemon.PluginException;
import org.lockss.filter.*;
import org.lockss.filter.HtmlTagFilter.TagPair;
import org.lockss.filter.StringFilter;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.*;

public class HindawiPublishingCorporationHtmlFilterFactory implements FilterFactory {
  
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)                                     
      throws PluginException {
    NodeFilter[] filters = new NodeFilter[] {
        // Contains changing <meta> tags, <link> tags for CSS, etc.
        new TagNameFilter("head"),
        // There used to be <link> tags in mid-<body>
        new TagNameFilter("link"),
        // Filter out <script> tags that are edited often
        new TagNameFilter("script"),
        // Filter out all non-essential areas of pages
        HtmlNodeFilters.tagWithAttribute("div", "id", "site_head"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "dvLinks"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "ctl00_dvLinks"), // old name
        HtmlNodeFilters.tagWithAttribute("div", "id", "banner"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "journal_navigation"),
        HtmlNodeFilters.tagWithAttribute("div", "class", "footer_space"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "left_column"),
        HtmlNodeFilters.tagWithAttribute("div", "class", "right_column_actions"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "footer"),
        // widget that used to appear
        HtmlNodeFilters.tagWithAttribute("div", "id", "dropmenudiv"),
        // There used to be ASP-style forms, with or without "__", now gone altogether
        HtmlNodeFilters.tagWithAttribute("input", "id", "VIEWSTATE"),
        HtmlNodeFilters.tagWithAttribute("input", "id", "__VIEWSTATE"),
        HtmlNodeFilters.tagWithAttribute("input", "id", "EVENTVALIDATION"),
        HtmlNodeFilters.tagWithAttribute("input", "id", "__EVENTVALIDATION"),
        // <svg> may be embedded in a <span>
        HtmlNodeFilters.tagWithAttributeRegex("span", "style", "^width"),
        };
    InputStream htmlFilter = new HtmlFilterInputStream(in,
                                                       encoding,
                                                       HtmlNodeFilterTransform.exclude(new OrFilter(filters)));

    Reader reader = FilterUtil.getReader(htmlFilter, encoding);
    // consolidate white space before doing tagfilter stuff     
    Reader WSReader = new WhiteSpaceFilter(reader);
    Reader filtReader = makeFilteredReader(WSReader);
    return new ReaderInputStream(filtReader);
  }
  
  // Noisy whitespace has already been removed
  static Reader makeFilteredReader(Reader reader) {
    List tagList = ListUtil.list(
        // Remove DOCTYPE declaration which seems to vary but is not a node in the DOM
        new TagPair("<!DOCTYPE", ">", false, false),
        new TagPair("<html", ">", false, false),
        new TagPair("<br /><a href=\"http://dx.doi.org/10.1155/", "</pre>", false, false),
        new TagPair("<br />doi:10.1155/", "</pre>", false, false),
        new TagPair("<svg", "</svg>", true, false)
    );
    Reader tagFilter = HtmlTagFilter.makeNestedFilter(reader, tagList);
    String[][] findAndReplace = new String[][] { 
        // out of sync - some versions have extraneous single spaces, so remove between tags
        {"> <", "><"},
        // remove leading space after tags (extra spaces will already have been consolidated down to one 
        {"> ", ">"},
    };
    Reader stringFilter = StringFilter.makeNestedFilter(tagFilter,
        findAndReplace, false);
    return stringFilter;
   }

}
