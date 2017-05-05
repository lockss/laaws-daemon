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
package org.lockss.config;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.jboss.resteasy.client.jaxrs.BasicAuthentication;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.lockss.util.Logger;

/**
 * A client for the Configuration REST web service operation that provides the
 * configuration of an Archival Unit.
 */
public class GetAuConfigClient {
  private static Logger log = Logger.getLogger(GetAuConfigClient.class);

  private String serviceLocation = null;
  private String serviceUser = null;
  private String servicePassword = null;
  private Integer serviceTimeout = null;

  /**
   * Constructor.
   * 
   * @param location
   *          A String with the location of the REST web service.
   * @param userName
   *          A String with the name of the user that performs the operation.
   * @param password
   *          A String with the password of the user that performs the
   *          operation.
   * @param timeoutValue
   *          An Integer with the connection and socket timeout, in seconds.
   */
  public GetAuConfigClient(String location, String userName, String password,
      Integer timeoutValue) {
    serviceLocation = location;
    serviceUser = userName;
    servicePassword = password;
    serviceTimeout = timeoutValue;
  }

  /**
   * Retrieves the configuration of an Archival Unit from a Configuration REST
   * web service.
   * 
   * @param auId
   *          A String with the Archival Unit identifier.
   * @return a TdbAu with the Archival Unit title database.
   */
  public Configuration getAuConfig(String auId)
      throws UnsupportedEncodingException, Exception {
    final String DEBUG_HEADER = "getAuConfig(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "serviceLocation = " + serviceLocation);
      log.debug2(DEBUG_HEADER + "serviceUser = " + serviceUser);
      //log.debug2(DEBUG_HEADER + "servicePassword = " + servicePassword);
      log.debug2(DEBUG_HEADER + "serviceTimeout = " + serviceTimeout);
      log.debug2(DEBUG_HEADER + "auId = " + auId);
    }

    String encodedAuId = URLEncoder.encode(auId, "UTF-8");
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "encodedAuId = " + encodedAuId);

    // Build the basic JSON client.
    ResteasyClientBuilder builder = new ResteasyClientBuilder()
	.register(JacksonJsonProvider.class);

    // Specify the timeout values, if necessary.
    if (serviceTimeout != null) {
      builder.establishConnectionTimeout(serviceTimeout, TimeUnit.SECONDS)
	.socketTimeout(serviceTimeout, TimeUnit.SECONDS);
    }

    // Make the request to the REST service and get its response.
    Properties result = (Properties)builder.build().target(serviceLocation)
	.register(new BasicAuthentication(serviceUser, servicePassword))
	.path("aus").path(encodedAuId).request().get(Properties.class);
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);

    // Convert the response into a the expected result.
    Configuration config = ConfigManager.newConfiguration();

    for (Object key : result.keySet()) {
      config.put((String)key,  (String)result.get((String)key));
    }

    if (log.isDebug2()) log.debug2("config = " + config);
    return config;
  }
}
