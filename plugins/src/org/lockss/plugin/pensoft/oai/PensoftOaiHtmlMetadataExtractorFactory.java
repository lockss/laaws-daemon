/*
 * $Id:$
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

package org.lockss.plugin.pensoft.oai;

import java.io.IOException;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.FileMetadataExtractorFactory;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.extractor.SimpleHtmlMetaTagMetadataExtractor;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;

public class PensoftOaiHtmlMetadataExtractorFactory
  implements FileMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(PensoftOaiHtmlMetadataExtractorFactory.class);

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                               String contentType)
      throws PluginException {
    return new PensoftOaiHtmlMetadataExtractor();
  }

  public static class PensoftOaiHtmlMetadataExtractor
    extends SimpleHtmlMetaTagMetadataExtractor {
    private static MultiMap tagMap = new MultiValueMap();
    static {
      tagMap.put("citation_author", MetadataField.FIELD_AUTHOR);
      tagMap.put("citation_date", MetadataField.FIELD_DATE);
      tagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
      tagMap.put("citation_journal_title", MetadataField. FIELD_PUBLICATION_TITLE);
      tagMap.put("citation_volume", MetadataField.FIELD_VOLUME);
      tagMap.put("citation_issue", MetadataField.FIELD_ISSUE);
      tagMap.put("citation_firstpage", MetadataField.FIELD_START_PAGE);
      tagMap.put("citation_doi", MetadataField.FIELD_DOI);
      tagMap.put("citation_issn", MetadataField.FIELD_ISSN);
      tagMap.put("citation_isbn", MetadataField.FIELD_ISBN);
      tagMap.put("citation_abtract_html_url", MetadataField.FIELD_ACCESS_URL);
    }

    @Override
    public ArticleMetadata extract(MetadataTarget target, CachedUrl cu)
    throws IOException {
      ArticleMetadata am = super.extract(target, cu);
      am.cook(tagMap);
      return am;
    }
  }
}
