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

package org.lockss.plugin.highwire.bmj;

import java.util.Properties;

import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.plugin.UrlNormalizer;
import org.lockss.plugin.definable.DefinablePlugin;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
/*
 * Parent UrlNormalizer removes  suffixes, etc.
 * http://www.bmj.com/content/303/1/C1/F1.large.jpg?width=800&height=600
 * & http://www.bmj.com/content/303/1/C1/F1.large.jpg?download=true
 * to http://www.bmj.com/content/303/1/C1/F1.large.jpg
 * 
 * http://static.www.bmj.com/content/304/2/H253.full-text.pdf
 * & http://static.beta.www.bmj.com/content/304/2/H253.full-text.pdf
 * to http://www.bmj.com/content/ajpheart/304/2/H253.full.pdf
 */
import org.lockss.test.MockArchivalUnit;

public class TestBMJDrupalUrlNormalizer extends LockssTestCase {
  
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String VOL_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  private DefinablePlugin plugin;
  private MockArchivalUnit m_mau;
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    plugin = new DefinablePlugin();
    plugin.initPlugin(getMockLockssDaemon(),
        "org.lockss.plugin.highwire.bmj.BMJDrupalPlugin");
    Properties props = new Properties();
    props.setProperty(VOL_KEY, "303");
    props.setProperty(BASE_URL_KEY, "http://www.bmj.com/");
    
    Configuration config = ConfigurationUtil.fromProps(props);
    m_mau = new MockArchivalUnit();
    m_mau.setConfiguration(config);
    plugin.configureAu(config, null);
    }
  
  public void testUrlNormalizer() throws Exception {
    UrlNormalizer normalizer = new BMJDrupalUrlNormalizer();
    // http://static.www.bmj.com/sites/default/files/attachments/bmj-article/pre-pub-history
    
    assertEquals("http://www.bmj.com/content/303/1/C1/F1.large.jpg",
        normalizer.normalizeUrl("http://www.bmj.com/content/303/1/C1/F1.large.jpg?width=800&height=600", m_mau));
    
    assertEquals("http://www.bmj.com/content/bmj.h3779.full.pdf",
        normalizer.normalizeUrl("https://static.www.bmj.com/content/bmj.h3779.full.pdf", m_mau));
    
    assertEquals("http://www.bmj.com/content/bmj.h3779.full.pdf",
        normalizer.normalizeUrl("http://static.beta.www.bmj.com/content/bmj.h3779.full.pdf", m_mau));
    
    assertEquals("http://www.bmj.com/content/304/2/H253",
        normalizer.normalizeUrl("https://static.beta.ww.bmj.com/content/304/2/H253", m_mau));
    
    assertEquals("https://staticbeta.www.bmj.com/content/304/2/H253",
        normalizer.normalizeUrl("https://staticbeta.www.bmj.com/content/304/2/H253", m_mau));
  }
  
}
