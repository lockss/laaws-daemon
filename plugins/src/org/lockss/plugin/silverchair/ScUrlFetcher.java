/*

Copyright (c) 2000-2017 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.plugin.silverchair;

import org.apache.http.client.utils.URIBuilder;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.base.BaseUrlFetcher;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.regex.Pattern;

public class ScUrlFetcher extends BaseUrlFetcher {

  protected static final Pattern PATTERN_POST =
    Pattern.compile("&post=json$",
                    Pattern.CASE_INSENSITIVE);

  public ScUrlFetcher(final CrawlerFacade crawlFacade,
                                 final String url) {
    super(crawlFacade, url);
  }

  /**
   * convert the query portion of a GET url into  json  for POST
   * @param url the url with the json arguments as GET query arguments
   * @return the json string to be sent in the POST
   * @throws IOException thrown if the url is foobar
   */
  protected String queryToJsonString(String url) throws IOException {
    try {
      URI uri = new URIBuilder(url).build();
      String query = uri.getQuery();
      String[] pairs = query.split("&");

      // there should be a minimum of two arguments the first is real, the second is virtual
      int idx;
      String key;
      String value;
      for (int i = 0; i < pairs.length-1; i++) {
	idx = pairs[i].indexOf("=");
	key = URLDecoder.decode(pairs[i].substring(0, idx), "UTF-8");
	value = URLDecoder.decode(pairs[i].substring(idx + 1), "UTF-8");
	if("json".equals(key)) {
	  return value;
	}
      }
    } catch (URISyntaxException e) {
    }
    return "";
  }
}
