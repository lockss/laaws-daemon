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

package org.lockss.plugin.atypon.emeraldgroup;

import java.io.InputStream;
import java.util.Vector;

import org.htmlparser.*;
import org.htmlparser.tags.*;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.atypon.BaseAtyponHtmlHashFilterFactory;

// Keeps contents only (includeNodes), then hashes out unwanted nodes 
// within the content (excludeNodes).
public class EmeraldGroupHtmlHashFilterFactory 
  extends BaseAtyponHtmlHashFilterFactory  {
     
  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in, 
                                               String encoding) {
    NodeFilter[] includeNodes = new NodeFilter[] {
        // manifest pages
        // <ul> and <li> without attributes (unlike TOC/full/abs/ref breadcrumbs)
        new NodeFilter() {
          @Override
          public boolean accept(Node node) {
            if (HtmlNodeFilters.tagWithAttributeRegex("a", "href", 
                                                      "/toc/").accept(node)) {
              Node liParent = node.getParent();
              if (liParent instanceof Bullet) {
                Bullet li = (Bullet)liParent;
                Vector liAttr = li.getAttributesEx();
                if (liAttr != null && liAttr.size() == 1) {
                  Node ulParent = li.getParent();
                  if (ulParent instanceof BulletList) {
                    BulletList ul = (BulletList)ulParent;
                    Vector ulAttr = ul.getAttributesEx();
                    return ulAttr != null && ulAttr.size() == 1;
                  }
                }
              }
            } else if (HtmlNodeFilters.tagWithAttributeRegex("a", "href", 
                                                      "/doi/book/").accept(node)) {
            // book manifest page has single doi/book ref whose parent is just the <body> element
            // http://emeraldinsight.com/clockss/eisbn/9780080549910
              Node liParent = node.getParent();
              if (liParent instanceof BodyTag) {
                return true;
              }  
            }
            return false;
          }
        },
        // book - landing page main contents chapter list and synopsis)
        // http://emeraldinsight.com/doi/book/10.1108/9780080549910
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "literatumBookDetailsWidget"),
        // toc - contents only
        // http://www.emeraldinsight.com/toc/aaaj/26/8
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", 
                                              "literatumTocWidget"),
        // abs, full, ref - contents only
        // http://www.emeraldinsight.com/doi/full/10.1108/AAAJ-05-2013-1360
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", 
                                          "literatumPublicationContentWidget"),
        // showCitFormats
        // http://www.emeraldinsight.com/action/
        //                      showCitFormats?doi=10.1108%2F09513571311285621                                      
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", 
                                              "downloadCitationsWidget"),
        // showPopup - generated by BaseAtyponHtmlLinkExtractorFactory
        // http://www.emeraldinsight.com/action/showPopup?citid=citart1
        //                          &id=FN_fn1&doi=10.1108%2FAAAJ-02-2012-00947                                      
        HtmlNodeFilters.tagWithAttributeRegex("body", "class", "popupBody")                     
    };
    
    // handled by parent: script, sfxlink, stylesheet, pdfplus file sise
    // <head> tag, <li> item has the text "Cited by", accessIcon, 
    NodeFilter[] excludeNodes = new NodeFilter[] {
        // toc, abs, full, ref - Reprints and Permissions        
        HtmlNodeFilters.tagWithAttributeRegex("a", "class", "rightsLink"),
        // toc - above the first toc entry with Track Citations
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "toc-actions"),
        // abs, full, ref - downloads count
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "downloadsCount"),
        // full - section choose pulldown appeared in multiple sections
        // http://www.emeraldinsight.com/doi/full/10.1108/AAAJ-02-2013-1228
        HtmlNodeFilters.tagWithAttribute("div",  "class", "sectionJumpTo"),
        // abs, full, ref - Article Options and Tools 
        HtmlNodeFilters.allExceptSubtree(
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "options"),
                HtmlNodeFilters.tagWithAttributeRegex(
                    "a", "href", "/action/showCitFormats\\?")),                    
        // abs, full, ref - random html - potential problem
        HtmlNodeFilters.tagWithAttribute("span",  "class", "Z3988"),
        // full, ref - references section - Crossref/ISI/Abstract/Infotrieve
        // separated by a comma. Not easy to remove the comma, so hash out
        // class citation
        HtmlNodeFilters.tagWithAttribute("div", "class", "citation"),     
        //TOC - in case icon options change
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "icon-key"),
        // on the full/abs/ref pages there are little style definitions that 
        HtmlNodeFilters.tagWithAttributeRegex("style", "type", "text/css"),
    };
    return super.createFilteredInputStream(au, in, encoding, 
                                           includeNodes, excludeNodes);
  }

  @Override
  public boolean doTagIDFiltering() {
    return true;
  }
  
  @Override
  public boolean doWSFiltering() {
    return true;
  }
  
}
