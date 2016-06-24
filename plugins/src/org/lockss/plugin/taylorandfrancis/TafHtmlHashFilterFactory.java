/*
 * $Id$
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.taylorandfrancis;

import java.io.*;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CountingInputStream;
import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Tag;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.tags.BodyTag;
import org.htmlparser.tags.Bullet;
import org.htmlparser.tags.BulletList;
import org.htmlparser.tags.CompositeTag;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.Span;
import org.htmlparser.util.NodeList;
import org.lockss.daemon.PluginException;
import org.lockss.filter.*;
import org.lockss.filter.HtmlTagFilter.TagPair;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.*;

/**
 * This filter will eventually replace
 * {@link TaylorAndFrancisHtmlHashFilterFactory}.
 */
public class TafHtmlHashFilterFactory implements FilterFactory {

  private static final Logger log = Logger.getLogger(TafHtmlHashFilterFactory.class);
  
  @Override
  public InputStream createFilteredInputStream(final ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {

    HtmlFilterInputStream filtered = new HtmlFilterInputStream(
      in,
      encoding,
      new HtmlCompoundTransform(
        /*
         * KEEP: throw out everything but main content areas
         */
        HtmlNodeFilterTransform.include(new OrFilter(new NodeFilter[] {
            // KEEP top part of main content area [TOC, abs, full, ref]
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "overview"),
            // KEEP each article block [TOC]
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "\\barticle\\b"), // avoid match on pageArticle
            // KEEP abstract [abs, full, ref]
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "abstract"),
            // KEEP active content area [abs, full, ref, suppl]
            HtmlNodeFilters.tagWithAttribute("div", "id", "informationPanel"), // article info [abs]
            HtmlNodeFilters.tagWithAttribute("div", "id", "fulltextPanel"), // full text [full]
            HtmlNodeFilters.tagWithAttribute("div", "id", "referencesPanel"), // references [ref]
            HtmlNodeFilters.tagWithAttribute("div", "id", "supplementaryPanel"), // supplementary materials [suppl]
            // KEEP citation format form [showCitFormats]
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "citationContainer"),
            // KEEP popup window content area [showPopup]
            HtmlNodeFilters.tagWithAttribute("body", "class", "popupBody"),
            
            new NodeFilter() {
              @Override
              public boolean accept(Node node) {
                if (node instanceof LinkTag) {
                  String link = ((LinkTag) node).getAttribute("href");
                  if(link != null && !link.isEmpty() && link.matches("^(https?://[^/]+)?/toc/[^/]+/[^/]+/[^/]+/?$")) {
                    Node parent = node.getParent().getParent();
                    if(parent instanceof BulletList) {
                      if(parent.getParent() instanceof BodyTag) {
                        return true;
                      }
                    }
                  }
                }
                return false;
              }
            },
        })),
        /*
         * DROP: filter remaining content areas
         */
        HtmlNodeFilterTransform.exclude(new OrFilter(new NodeFilter[] {
            // DROP scripts, styles, comments
            HtmlNodeFilters.tag("script"),
            HtmlNodeFilters.tag("noscript"),
            HtmlNodeFilters.tag("style"),
            HtmlNodeFilters.comment(),
            // DROP social media bar [overview]
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "social"),
            // DROP access box (changes e.g. when the article becomes free) [article block, abs/full/ref/suppl overview]
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "accessmodule"),
            HtmlNodeFilters.tagWithAttribute("div", "class", "access"), // formerly by itself
            // DROP number of article views [article block, abs/full/ref/suppl overview]
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "articleUsage"),
            // DROP "Related articles" variants [article block, abs/full/ref/suppl overview]
            HtmlNodeFilters.tagWithAttributeRegex("a", "class", "relatedLink"), // old?
            HtmlNodeFilters.tagWithAttributeRegex("li", "class", "relatedArticleLink"), // [article block]
            HtmlNodeFilters.tagWithText("h3", "Related articles"), // [abs/full/ref/suppl overview]
            HtmlNodeFilters.tagWithAttributeRegex("a", "class", "searchRelatedLink"), // [abs/full/ref/suppl overview]
            // DROP title options (e.g. 'Publication History', 'Sample this title') [TOC overview]
            HtmlNodeFilters.tagWithAttribute("div", "class", "options"),
            // DROP title icons (e.g. 'Routledge Open Select') [TOC overview]
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "journalOverviewAds"),
            // DROP book review subtitle (added later)
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "subtitle"), // [TOC] e.g. http://www.tandfonline.com/toc/wtsw20/33/1)
            // ...placeholder for [abs/full/ref/suppl overview] e.g. http://www.tandfonline.com/doi/full/10.1080/08841233.2013.751003
            // DROP Google Translate artifacts [abs/full/ref/suppl overview]
            HtmlNodeFilters.tagWithAttribute("div", "id", "google_translate_element"), // current
            HtmlNodeFilters.tagWithAttribute("div", "id", "goog-gt-tt"), // old
            HtmlNodeFilters.tagWithText("a", "Translator disclaimer"),
            HtmlNodeFilters.tagWithText("a", "Translator&nbsp;disclaimer"),
            // DROP "Alert me" variants [abs/full/ref overview]
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "alertDiv"), // current
            HtmlNodeFilters.tagWithAttributeRegex("div", "id", "alertDiv"), // old
            // DROP "Publishing models and article dates explained" link [abs/full/ref/suppl overview]
            HtmlNodeFilters.tagWithAttributeRegex("a", "href", "models-and-dates-explained"),
            // DROP article dates which sometimes get fixed later [abs/full/ref/suppl overview]
            HtmlNodeFilters.tagWithAttribute("div", "class", "articleDates"),
            // DROP subtitle for journal section/subject (added later) [abs/full/ref/suppl overview]
            HtmlNodeFilters.tagWithAttribute("span", "class", "subj-group"),
            // DROP non-access box article links (e.g. "View full text"->"Full text HTML") [abs/full/ref/suppl overview]
            HtmlNodeFilters.tagWithAttributeRegex("ul", "class", "top_article_links"),
            // DROP outgoing links and SFX links [article block, full, ref]
            HtmlNodeFilters.allExceptSubtree(HtmlNodeFilters.tagWithAttribute("span", "class", "referenceDiv"),
                                             HtmlNodeFilters.tagWithAttribute("a", "class", "dropDownLabel")), // popup at each inline citation [full]
            HtmlNodeFilters.tagWithAttributeRegex("a", "href", "^/servlet/linkout\\?"), // [article block, full/ref referencesPanel]
            HtmlNodeFilters.tagWithAttribute("a", "class", "sfxLink"), // [article block, full/ref referencesPanel]
            // DROP "Jump to section" popup menus [full]
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "summationNavigation"),
            HtmlNodeFilters.tagWithAttributeRegex("a", "title", "(Next|Previous) issue"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "id", "breadcrumb"),
            //descriptive text that often changes
            HtmlNodeFilters.tagWithAttribute("td", "class", "note"),
            new NodeFilter() {
              @Override
              public boolean accept(Node node) {
                if (node instanceof Span) {
                  Span span = ((Span) node);
                  for(Node child:span.getChildrenAsNodeArray()) {
                    if (child != null && child instanceof LinkTag) {
                      String title = ((LinkTag) child).getAttribute("title");
                      if (title != null && !title.isEmpty() && title.contains("Previous issue")) {
                        return true;
                      }
                    }
                  }
                }
                return false;
              }
            },
            
            new NodeFilter() {
              @Override
              public boolean accept(Node node) {
                if (node instanceof Div) {
                  Div div = ((Div) node);
                  String divClass = div.getAttribute("class");
                  if(divClass != null && !divClass.isEmpty() && divClass.contains("right")) {
                    Node parent = div.getParent();
                    if (parent != null && parent instanceof Div) {
                      String parentClass = ((Div) parent).getAttribute("class");
                        if (parentClass != null && !parentClass.isEmpty() && parentClass.contains("bodyFooterContent")) {
                          return true;
                        }
                      }
                  }
                }
                return false;
              }
            }
        }))
      )
    );
    
    Reader reader = FilterUtil.getReader(filtered, encoding);

    Reader stringFilter = StringFilter.makeNestedFilter(reader,
                                                        new String[][] {
        // Markup changes over time
        {"&nbsp;", " "},
        {"&amp;", "&"},
        {"\\", ""}, // e.g. \(, \-, present during encoding glitch (or similar)
    }, true);

    Reader tagFilter = HtmlTagFilter.makeNestedFilter(stringFilter,
                                                      Arrays.asList(
        // Alternate forms of citation links [article block]
        new TagPair("<li><div><strong>Citing Articles:", "</li>", true), // current
        new TagPair("<li><strong>Citations:", "</li>", true), // old?
        new TagPair("<li><strong><a href=\"/doi/citedby/", "</li>", true), // old?
        new TagPair("<li><strong>Citation information:", "</li>", true), // old?
        // Wording change over time, and publication dates get fixed much later [article block, abs/full/ref/suppl overview]
        // For older versions with plain text instead of <div class="articleDates">
        new TagPair("Published online:", ">", true), // current
        new TagPair("Available online:", ">", true), // old
        new TagPair("Version of record first published:", ">", true), // old
        // Leftover commas after outgoing/SFX links removed [full/ref referencesPanel]
        new TagPair("</pub-id>", "</li>", true)
    ));
    
    // Remove all inner tag content
    Reader noTagFilter = new HtmlTagFilter(new StringFilter(tagFilter, "<", " <"), new TagPair("<", ">"));
    
    // Remove white space
    Reader noWhiteSpace = new WhiteSpaceFilter(noTagFilter);
    
    InputStream ret = new ReaderInputStream(noWhiteSpace);

    // Instrumentation
    return new CountingInputStream(ret) {
      @Override
      public void close() throws IOException {
        long bytes = getByteCount();
        if (bytes <= 100L) {
          log.debug(String.format("%d byte%s in %s", bytes, bytes == 1L ? "" : "s", au.getName()));
        }
        if (log.isDebug2()) {
          log.debug2(String.format("%d byte%s in %s", bytes, bytes == 1L ? "" : "s", au.getName()));
        }
        super.close();
      }
    };
  }

}
