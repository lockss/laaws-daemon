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

package org.lockss.plugin.bmc;

import java.io.*;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.filters.*;
import org.htmlparser.nodes.TextNode;
import org.htmlparser.util.NodeList;
import org.lockss.daemon.PluginException;
import org.lockss.filter.*;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.ListUtil;
import org.lockss.util.ReaderInputStream;

public class BMCPluginHtmlFilterFactory implements FilterFactory {
  
  protected static final NodeFilter[] filters = new NodeFilter[] {
      // head tag - Extreme Hash filtering! the 3 following filters are now redundant
      HtmlNodeFilters.tag("head"),
      // Contains variable code
      HtmlNodeFilters.tag("script"),
      // Contains variable alternatives to the code
      HtmlNodeFilters.tag("noscript"),
      // remove all style tags!
      HtmlNodeFilters.tag("style"),
      // Contains ads
      HtmlNodeFilters.tag("iframe"),
      // Contains ads
      HtmlNodeFilters.tag("object"),
      // CSS and RSS links varied over time
      HtmlNodeFilters.tag("link"),
      //filter out comments
      HtmlNodeFilters.comment(),
      // malformed html causing low agreement <div id="oas-
      HtmlNodeFilters.tagWithAttributeRegex("div", "id", "^oas-"),
      // citation format changes and access changes - Extreme Hash filtering!
      HtmlNodeFilters.tagWithAttribute("div", "id", "topmatter"),
      HtmlNodeFilters.tagWithAttribute("section", "class", "cit"),
      // upper area above the article - Extreme Hash filtering!
      HtmlNodeFilters.tagWithAttribute("div", "id", "branding"),
      // left-hand area next to the article - Extreme Hash filtering!
      HtmlNodeFilters.tagWithAttribute("div", "class", "left-article-box"),
      HtmlNodeFilters.tagWithAttribute("div", "id", "left-article-box"),
      // right-hand area next to the article - Extreme Hash filtering!
      HtmlNodeFilters.tagWithAttribute("div", "id", "article-navigation-bar"),
      // alert signup - Extreme Hash filtering!
      // see http://www.biomedcentral.com/1471-2369/14/79/abstract in ingest4
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "-signup"),
      HtmlNodeFilters.tagWithAttribute("div", "class", "primary-content hide-on-print"),
      // found on issue pages http://www.biomedcentral.com/bmcanesthesiol/content/8/December/2008
      HtmlNodeFilters.tagWithAttribute("div", "class", "wrap-nav"),
      HtmlNodeFilters.tagWithAttribute("div", "class", "issuecover"),
      HtmlNodeFilters.tagWithAttribute("p", "class", "header"),
      // Extra links appeared on issue toc for citations
      HtmlNodeFilters.allExceptSubtree(
          HtmlNodeFilters.tagWithAttribute("p", "class", "nav"),
          HtmlNodeFilters.tagWithAttributeRegex("a", "class",
              "(abstract|fulltext|pdf.*)-link")),
      HtmlNodeFilters.tagWithAttribute("a", "id", "comments"),
      // Extreme filtering
      HtmlNodeFilters.tagWithAttribute("div", "id", "article-note"),
      
      // Contains one-time names inside the page
      HtmlNodeFilters.tagWithAttribute("a", "name"),
      // Links to one-time names inside the page
      HtmlNodeFilters.tagWithAttributeRegex("a", "href", "^#"),
      // Institution-dependent
      HtmlNodeFilters.tagWithAttribute("li", "class", "greeting"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "id", "message"), 
      
      // Contains the menu  <ul class="primary-nav">
      HtmlNodeFilters.tagWithAttribute("ul", "class", "primary-nav"),
      // remove footer
      //Contains the terms and conditions,copyright year & links to springer
      HtmlNodeFilters.tagWithAttribute("div", "id", "footer"),
      // Contains university name: <ul id="login"
      HtmlNodeFilters.tagWithAttribute("ul", "id", "login"),
      // Contains advertising
      HtmlNodeFilters.tagWithAttributeRegex("dl", "class", "google-ad"),
      // Social networking links (have counters)
      HtmlNodeFilters.tagWithAttribute("ul", "id", "social-networking-links"),
      // A usage counter/glif that gets updated over time
      HtmlNodeFilters.tagWithAttribute("div", "id", "impact-factor"),
      // Contains adverstising <a class="banner-ad"
      HtmlNodeFilters.tagWithAttribute("a", "class", "banner-ad"),
      // Contains adverstising <a class="skyscraper-ad" 
      HtmlNodeFilters.tagWithAttribute("a", "class", "skyscraper-ad"),
      // An open access link/glyph that may get added
      HtmlNodeFilters.tagWithAttributeRegex("a", "href", "/about/access"),
      // A highly accessed link/glyph that may get added
      HtmlNodeFilters.tagWithAttributeRegex("a", "href", "/about/mostviewed"),
      HtmlNodeFilters.tagWithAttributeRegex("a", "class", "(hidden|access)"),
      // remove the mobile sidebar
      HtmlNodeFilters.tagWithAttribute("div", "id", "mobile-sidebar"),
      // Institution-dependent image
      HtmlNodeFilters.tagWithAttributeRegex("img", "src", "^/sfx_links\\?"),
      // Institution-dependent link resolvers  v2 - added
      HtmlNodeFilters.tagWithAttributeRegex("a", "href", "^/sfx_links\\?"),
      // Institution-dependent link resolvers   v1
      HtmlNodeFilters.tagWithAttributeRegex("a", "href", "^/sfx_links\\.asp"),
      // Springer branding below the footer
      HtmlNodeFilters.tagWithAttribute("div", "class", "springer"),
      
      // The text of this link changed from "About this article" to "Article metrics"
      HtmlNodeFilters.tagWithAttributeRegex("a", "href", "/about$"),
      // removes mathml inline weirdnesses
      HtmlNodeFilters.tagWithAttribute("p", "class", "inlinenumber"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "style", "display:inline$"),
      HtmlNodeFilters.tagWithAttribute("span", "class", "mathjax"),
      HtmlNodeFilters.tagWithAttribute("span", "class", "inline-math"),
      HtmlNodeFilters.tagWithAttribute("span", "class", "inlinenumber"),
      
      // floating bottom banner announcing access to beta version of new site
      HtmlNodeFilters.tagWithAttributeRegex("div", "class",  "^banner-footer"),
      
      // grey item on volume issue page, missing on some pages
      HtmlNodeFilters.tagWithAttribute("li", "class", "tooltip"),
      
  };
  
  // HTML transform to convert all remaining nodes to plaintext nodes
  // cannot keep up with all the frequent changes to tags
  
  protected static HtmlTransform xformAllTags = new HtmlTransform() {
    @Override
    public NodeList transform(NodeList nodeList) throws IOException {
      NodeList nl = new NodeList();
      for (int sx = 0; sx < nodeList.size(); sx++) {
        Node snode = nodeList.elementAt(sx);
        TextNode tn = new TextNode(snode.toPlainTextString());
        nl.add(tn);
      }
      return nl;
    }
  };
  
  // HtmlNodeFilters.tag("head"),
  // HtmlNodeFilters.tag("script"),
  // HtmlNodeFilters.tag("noscript"),
  // Contains variable alternatives to the code which confuse the html parser??
  // in any case when tags are stripped in this way filtering is better
  private static final HtmlTagFilter.TagPair[] pairs = {
      new HtmlTagFilter.TagPair("<head", "</head>"),
      new HtmlTagFilter.TagPair("document.write", ");"),
      new HtmlTagFilter.TagPair("<script", "</script>"),
      new HtmlTagFilter.TagPair("<noscript", "</noscript>"),
  };
  
  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    
    HtmlTagFilter tagfilter = HtmlTagFilter.makeNestedFilter(
        FilterUtil.getReader(in, encoding), ListUtil.fromArray(pairs));
    InputStream inb = new BufferedInputStream(new ReaderInputStream(tagfilter, encoding));
    InputStream filtered =  new HtmlFilterInputStream(inb, encoding, 
        new HtmlCompoundTransform(
            HtmlNodeFilterTransform.exclude(new OrFilter(filters)), xformAllTags));
    Reader filteredReader = FilterUtil.getReader(filtered, encoding);
    // added whitespace filter
    return new ReaderInputStream(new WhiteSpaceFilter(filteredReader));
  }
  
}
