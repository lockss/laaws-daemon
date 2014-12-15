/*
 * $Id: HighWireDrupalHtmlFilterFactory.java,v 1.10 2014-12-15 20:39:45 etenbrink Exp $
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.highwire;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Arrays;
import java.util.Vector;

import org.htmlparser.Attribute;
import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Tag;
import org.htmlparser.filters.*;
import org.htmlparser.nodes.TextNode;
import org.htmlparser.util.NodeList;
import org.lockss.daemon.PluginException;
import org.lockss.filter.FilterUtil;
import org.lockss.filter.WhiteSpaceFilter;
import org.lockss.filter.html.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;
import org.lockss.util.Logger;
import org.lockss.util.ReaderInputStream;

public class HighWireDrupalHtmlFilterFactory implements FilterFactory {
  
  Logger log = Logger.getLogger(HighWireDrupalHtmlFilterFactory.class);
  
  protected static NodeFilter[] baseHWDrupalFilters = new NodeFilter[] {
    // Publisher adding/updating meta tags
    new TagNameFilter("head"),
    // remove ALL comments
    HtmlNodeFilters.comment(),
    // No relevant content in header/footer (in crawl filter)
    new TagNameFilter("header"),
    new TagNameFilter("footer"),
    // no need to include aside (in crawl filter) 
    new TagNameFilter("aside"),
    // right sidebar, prev/next pager can change (in crawl filter) 
//    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "sidebar-right-wrapper"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "pane-highwire-node-pager"),
    // copyright statement may change
    HtmlNodeFilters.tagWithAttribute("ul", "class", "copyright-statement"),
    // messages can appear arbitrarily
    HtmlNodeFilters.tagWithAttributeRegex("div", "id", "messages"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "alert"),
    // citation reference extras
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "cit-extra"),
    
    // most scripts are in head, however, if any are in the body they are filtered
    new TagNameFilter("script"),
    new TagNameFilter("noscript"),
    // while we collect rapid-responses, they change, so do not include for hash
    // HtmlNodeFilters.tagWithAttributeRegex("div", "class", "rapid-responses"),
    // BMJ had tags on http://www.bmj.com/content/330/7506/0.8/rapid-responses
//    HtmlNodeFilters.tagWithAttribute("div", "id", "skip-link"),
//    HtmlNodeFilters.tagWithAttribute("div", "id", "cookie-notice"),
//    HtmlNodeFilters.tagWithAttribute("div", "class", "vote-widget"),
    // <div class="panel-panel panel-region-content-header">
    // <div class="panel-pane pane-panels-mini pane-oup-explore-citing-articles">
    // <div class="panel-pane pane-panels-mini pane-oup-explore-related-articles">
    // <div class="ui-dialog ui-widget
  };
  
  // HTML transform to convert all remaining nodes to plaintext nodes
  // http://ajpheart.physiology.org/content/306/10/H1385 (HW had replaced SPAN tags with DIV
  // cannot keep up with all the continual changes to 
  
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
  
  // Transform to remove attributes from select tags
  // some attributes changed over time, either arbitrarily or sequentially
  protected static HtmlTransform xformAttributes = new HtmlTransform() {
    @Override
    public NodeList transform(NodeList nodeList) throws IOException {
      NodeList nl = new NodeList();
      for (int sx = 0; sx < nodeList.size(); sx++) {
        Node snode = nodeList.elementAt(sx);
        if (snode instanceof Tag) {
          Tag tag = (Tag) snode;
          if (tag.isEmptyXmlTag()) {
            continue;
          }
          String tagName = tag.getTagName().toLowerCase();
          NodeList knl = tag.getChildren();
          if (knl != null) {
            tag.setChildren(transform(knl));
          }
          if ("div".equals(tagName)) {
            Attribute a = tag.getAttributeEx(tagName);
            Vector<Attribute> v = new Vector<Attribute>();
            v.add(a);
            tag.setAttributesEx(v);
          }
          nl.add(tag);
        }
        else {
          nl.add(snode);
        }
      }
      return nl;
    }
  };
  
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    
    InputStream filtered = doFiltering(in, encoding, null);
    return filtered;
  }
  
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding,
                                               NodeFilter[] moreNodes)
      throws PluginException {
    
    InputStream filtered = doFiltering(in, encoding, moreNodes);
    return filtered;
  }
  
  /* the shared portion of the filtering
   * pick up the extra nodes from the child if there are any
   * Use the xform to text, depending on the return value of the doXformToText().
   *  
   */
  private InputStream doFiltering(InputStream in, String encoding, NodeFilter[] moreNodes) {
    NodeFilter[] filters = baseHWDrupalFilters;
    if (moreNodes != null) {
      filters = addTo(moreNodes);
    }
    
    InputStream combinedFiltered;
    if (doXformToText()) {
      combinedFiltered = new HtmlFilterInputStream(in, encoding,
          new HtmlCompoundTransform(
              HtmlNodeFilterTransform.exclude(new OrFilter(filters)), xformAllTags));
    } else if (doTagAttributeFiltering()) {
      combinedFiltered = new HtmlFilterInputStream(in, encoding,
          new HtmlCompoundTransform(
              HtmlNodeFilterTransform.exclude(new OrFilter(filters)), xformAttributes));
    } else {
      combinedFiltered = new HtmlFilterInputStream(in, encoding,
          HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
    }
    
    if (doWSFiltering()) {
      Reader filteredReader = FilterUtil.getReader(combinedFiltered, encoding);
      return new ReaderInputStream(new WhiteSpaceFilter(filteredReader));
    } else {
      return combinedFiltered;
    }
  }
  
  /** Create an array of NodeFilters that combines the baseHWDrupalFilters with
   *  the given array
   *  @param nodes The array of NodeFilters to add
   */
  protected NodeFilter[] addTo(NodeFilter[] nodes) {
    NodeFilter[] result  = Arrays.copyOf(baseHWDrupalFilters, baseHWDrupalFilters.length + nodes.length);
    System.arraycopy(nodes, 0, result, baseHWDrupalFilters.length, nodes.length);
    return result;
  }
  
  /*
   * HighWire Drupal children can turn on/off extra levels of filtering
   * by overriding the getter methods.
   */
  
  /* doFiltering() will query this method and if it is true,
   * will xform all nodes to plaintext TextNodes
   * default is false;
   */
  public boolean doXformToText() {
    return false;
  }
  
  /*
   * doFiltering() will query this method and if it is true,
   *  will remove extra whitespace
   * default is true;
   */
  public boolean doWSFiltering() {
    return true;
  }
  
  /*
   * doFiltering() will query this method and if it is true, 
   * will remove extra attributes from tags
   * default is false;
   */
  public boolean doTagAttributeFiltering() {
    return false;
  }
  
}
