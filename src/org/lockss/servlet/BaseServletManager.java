/*
 * $Id: BaseServletManager.java,v 1.4 2004-09-27 22:39:06 smorabito Exp $
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.servlet;

import java.io.*;
import java.net.*;
import java.util.*;
import org.lockss.app.*;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.jetty.*;
import org.mortbay.http.*;
import org.mortbay.http.handler.*;
import org.mortbay.jetty.servlet.*;

/**
 * Base class for servlet managers.
 * Note: this class may be used in an environment where the LOCKSS app is
 * not running (<i>e.g.</i>, for {@link org.lockss.servlet.TinyUi}), so it
 * must not rely on any non-static app services, nor any other managers.
 */
public abstract class BaseServletManager extends JettyManager {

  public static final String UI_REALM = "LOCKSS Admin";
  public static final String PASSWORD_PROPERTY_FILE = "admin.props";

  public static final String PREFIX = Configuration.PREFIX + "ui.";
  public static final String PARAM_START = PREFIX + "start";
  public static final String PARAM_PORT = PREFIX + "port";

  public static final String IP_ACCESS_PREFIX = PREFIX + "access.ip.";
  public static final String PARAM_IP_INCLUDE = IP_ACCESS_PREFIX + "include";
  public static final String PARAM_IP_EXCLUDE = IP_ACCESS_PREFIX + "exclude";
  public static final String PARAM_LOG_FORBIDDEN =
    IP_ACCESS_PREFIX + "logForbidden";
  public static final boolean DEFAULT_LOG_FORBIDDEN = true;

  public static final String PARAM_USER_AUTH = PREFIX + "access.auth";
  public static final boolean DEFAULT_USER_AUTH = true;
  public static final String PARAM_LOGDIR =
    Configuration.PREFIX +  "platform.logdirectory";

  public static final boolean DEFAULT_START = true;
  public static final int DEFAULT_PORT = 8081;

  public static final String PARAM_PLATFORM_USERNAME =
    Configuration.PLATFORM + "ui.username";
  public static final String PARAM_PLATFORM_PASSWORD =
    Configuration.PLATFORM + "ui.password";


  private static Logger log = Logger.getLogger("ServletMgr");

  private static String textMimes[] = {
    "out", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
  };

  protected String serverName;		// for identification in messages
  protected int port;
  private boolean start;
  private String includeIps;
  private String excludeIps;
  private boolean logForbidden;
  protected boolean doAuth;
  protected String logdir;
  private String platUser;
  private String platPass;

  List accessHandlers = new ArrayList();

  public BaseServletManager(String serverName) {
    this.serverName = serverName;
  }

  /* ------- LockssManager implementation ------------------ */

  /** Start servlets  */
  public void startService() {
    if (start) {
      super.startService();
      startServlets();
    }
  }

  /** Stop servlets  */
  public void stopService() {
    stopServer();
    super.stopService();
  }

  public void setConfig(Configuration config, Configuration prevConfig,
			Configuration.Differences changedKeys) {
    super.setConfig(config, prevConfig, changedKeys);
    port = config.getInt(PARAM_PORT, DEFAULT_PORT);
    start = config.getBoolean(PARAM_START, DEFAULT_START);
    logdir = config.get(PARAM_LOGDIR);
    doAuth = config.getBoolean(PARAM_USER_AUTH, DEFAULT_USER_AUTH);
    platUser = config.get(PARAM_PLATFORM_USERNAME);
    platPass = config.get(PARAM_PLATFORM_PASSWORD);

    if (changedKeys.contains(PARAM_IP_INCLUDE) ||
	changedKeys.contains(PARAM_IP_EXCLUDE) ||
	changedKeys.contains(PARAM_LOG_FORBIDDEN)) {
      includeIps = config.get(PARAM_IP_INCLUDE, "");
      excludeIps = config.get(PARAM_IP_EXCLUDE, "");
      logForbidden = config.getBoolean(PARAM_LOG_FORBIDDEN,
				       DEFAULT_LOG_FORBIDDEN);
      log.debug("Installing new ip filter: incl: " + includeIps +
		", excl: " + excludeIps);
      setIpFilters();
    }
  }

  void setIpFilters() {
    for (Iterator iter = accessHandlers.iterator(); iter.hasNext(); ) {
      setIpFilter((IpAccessHandler)iter.next());
    }
  }

  void setIpFilter(IpAccessHandler ah) {
    try {
      IpFilter filter = new IpFilter();
      filter.setFilters(includeIps, excludeIps, ';');
      ah.setFilter(filter);
    } catch (IpFilter.MalformedException e) {
      log.warning("Malformed IP filter, filters not changed", e);
    }
    ah.setLogForbidden(logForbidden);
    ah.setAllowLocal(true);
  }

  // Manually install password set by platform config.
  // XXX Doesn't handle roles, will need to be integrated with daemon
  // password setting mechanism
  protected void setConfiguredPasswords(HashUserRealm realm) {
    if (!StringUtil.isNullString(platUser) &&
	!StringUtil.isNullString(platPass)) {
      realm.put(platUser, platPass);
    }
  }

  protected abstract void startServlets();

  protected void addAccessHandler(HttpContext context) {
    IpAccessHandler ah = new IpAccessHandler(serverName);
    setIpFilter(ah);
    context.addHandler(ah);
    accessHandlers.add(ah);
  }

  protected void setContextAuthHandler(HttpContext context, UserRealm realm) {
    if (realm != null) {
      context.setRealm(realm);
      context.setAuthenticator(new BasicAuthenticator());
      context.addHandler(new SecurityHandler());
      context.addSecurityConstraint("/",
				    new SecurityConstraint("Admin", "*"));
    }
  }


  protected void setupLogContext(HttpServer server, UserRealm realm,
				 String contextPath, String logdir)
      throws MalformedURLException {
    HttpContext context = server.getContext(contextPath);
    // Don't consume memory with cached log files
    context.setMaxCachedFileSize(0);

    // IpAccessHandler is always first handler
    addAccessHandler(context);

    // user authentication handler
    setContextAuthHandler(context, realm);

    // log dir resource
    String logdirname = (logdir != null) ? logdir : "";
    URL logResourceUrl=new URL("file", null,
			       new File(logdirname).getAbsolutePath());
    log.debug("Log Resource URL: " + logResourceUrl);
    context.setResourceBase(logResourceUrl.toString());
    LockssResourceHandler logRHandler = new LockssResourceHandler();
    logRHandler.setDirAllowed(true);
    //    logRHandler.setPutAllowed(false);
    //       rHandler.setDelAllowed(false);
    //       rHandler.setAcceptRanges(true);
    context.addHandler(logRHandler);
    for (int ix = 0; ix < textMimes.length; ix++) {
      context.setMimeMapping(textMimes[ix], "text/plain");
    }
    //	context.setMimeMapping("gz", "text/gzip");
    //	context.setTypeEncoding("text/gzip", "x-gzip");

    // NotFoundHandler
    context.addHandler(new NotFoundHandler());
  }

  // Add a servlet if its class can be loaded.
  protected void addServletIfAvailable(ServletHandler handler, String name,
				       String pathSpec, String servletClass) {
    try {
      Class.forName(servletClass);
      handler.addServlet(name, pathSpec, servletClass);
    } catch (ClassNotFoundException e) {
      log.warning("Not starting servlet \"" + name +
		  "\", class not found: " + servletClass);
    }
  }

}
