/*
 * $Id$
 */

/*

Copyright (c) 2017 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.IOException;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.extractor.MetadataField.Cardinality;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;

public class OupScHtmlMetadataExtractorFactory implements FileMetadataExtractorFactory {

  protected static final String KEY_PUBLICATION_ABBREV = "publication.abbrev";
  
  protected static final MetadataField FIELD_PUBLICATION_ABBREV = new MetadataField(KEY_PUBLICATION_ABBREV, Cardinality.Single);
  
  protected static final MultiMap tagMap = new MultiValueMap(); // see static initializer
  static {
    // All below seen in ACCP, ACP, AMA, APA
    tagMap.put("citation_publisher", MetadataField.FIELD_PUBLISHER);
    tagMap.put("citation_journal_title", MetadataField.FIELD_PUBLICATION_TITLE);
    // replacement title for proceedings
    tagMap.put("citation_conference_title", MetadataField.FIELD_PUBLICATION_TITLE);
    tagMap.put("citation_journal_abbrev", FIELD_PUBLICATION_ABBREV);
    tagMap.put("citation_issn", MetadataField.FIELD_ISSN);
    tagMap.put("citation_volume", MetadataField.FIELD_VOLUME);
    tagMap.put("citation_issue", MetadataField.FIELD_ISSUE);
    tagMap.put("citation_firstpage", MetadataField.FIELD_START_PAGE);
    tagMap.put("citation_lastpage", MetadataField.FIELD_END_PAGE);
    tagMap.put("citation_doi", MetadataField.FIELD_DOI);
    tagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
    tagMap.put("citation_date", MetadataField.FIELD_DATE);
    // replacement date for proceedings
    tagMap.put("citation_publication_date", MetadataField.FIELD_DATE);
    tagMap.put("citation_author", MetadataField.FIELD_AUTHOR);
    tagMap.put("citation_keyword", MetadataField.FIELD_KEYWORDS);
    // addition for proceedings
    tagMap.put("citation_pdf_url", MetadataField.DC_FIELD_IDENTIFIER);
  }
  

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                           String contentType)
      throws PluginException {
    return new AMAHtmlMetadataExtractor();
  }
    
  public static class AMAHtmlMetadataExtractor
  extends SimpleHtmlMetaTagMetadataExtractor {
    
    @Override
    public ArticleMetadata extract(MetadataTarget target, CachedUrl cu)
        throws IOException {
      ArticleMetadata am = super.extract(target, cu);
      am.putRaw("extractor.type", "HTML");
      am.cook(tagMap);
      String url = am.get(MetadataField.FIELD_ACCESS_URL);
      ArchivalUnit au = cu.getArchivalUnit();
      if (url == null || url.isEmpty() || !au.makeCachedUrl(url).hasContent()) {
        url = cu.getUrl();
      }
      am.replace(MetadataField.FIELD_ACCESS_URL, url);
      return am;
    }
  }
  
}
