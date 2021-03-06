/*
 * $Id$
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

package org.lockss.util;
import java.util.*;
import java.sql.SQLException;
import java.text.Format;

import org.apache.commons.collections.map.ReferenceMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;

import org.lockss.config.*;

/**
 * <code>Logger</code> provides message logging functions.
 * All logging is done via a <code>Logger</code> instance obtained from
 * <code>Logger.getLogger()</code>.  A message is output if its severity
 * level is at least as great as the minimum severity level set in the
 * log instance.  A log's minimum severity can be set with
 * <code>setLevel()</code>, or with the configuration parameter
 * <tt>org.lockss.log.level.<i>log-name</i></tt>.
 * Output is sent to possibly many targets - see <code>addTarget()</code>
 */
public class Logger {

  static final String PREFIX = Configuration.PREFIX + "log.";
  static final String PARAM_DEFAULT_LEVEL = PREFIX + "default.level";
  static final String DEFAULT_DEFAULT_LEVEL = "info";

  /** Dateformat used for timestamps on log messages.  See {@link
   * org.apache.commons.lang3.time.SimpleDateFormat}.
   * <ul><li><code>HH:mm:ss.SSS</code>: 24-hour time with
   * millisecs. <li><code>MM/dd/yyyy HH:mm:ss.SSS</code>: date and 24-hour
   * time</ul>Doesn't apply to targets such as SyslogTarget that provide
   * their own timestamp mechanism. */
  static final String PARAM_TIMESTAMP_DATEFORMAT = PREFIX + "timeStampFormat";
  private static final String DEFAULT_TIMESTAMP_DATEFORMAT = "HH:mm:ss.SSS";

  static final String PARAM_LOG_LEVEL = PREFIX + "<logname>.level";
  static final String PARAM_LOG_TARGETS = PREFIX + "targets";

  /** prefix for target-specific params */
  public static final String TARGET_PREFIX = PREFIX + "target.";

  /** System property for name of default LogTarget */
  public static final String SYSPROP_DEFAULT_LOG_TARGET =
    "org.lockss.defaultLogTarget";
  /** System property for name of default log level */
  public static final String SYSPROP_DEFAULT_LOG_LEVEL =
    "org.lockss.defaultLogLevel";

  /** Critical errors require immediate attention from a human. */
  public static final int LEVEL_CRITICAL = 1;
  /** Errors indicate that the system may not operate correctly, but won't
   * damage anything. */
  public static final int LEVEL_ERROR = 2;
  /** Errors caused by misbehavior of some server out of our control. */
  public static final int LEVEL_SITE_ERROR = 3;
  /** Warnings are conditions that should not normally arise but don't
      prevent the system from continuing to run correctly. */
  public static final int LEVEL_WARNING = 4;
  /** Warnings about misbehavior of some server out of our control. */
  public static final int LEVEL_SITE_WARNING = 5;
  /** Informative messages that should normally be logged. */
  public static final int LEVEL_INFO = 6;
  /** Debugging messages. */
  public static final int LEVEL_DEBUG = 7;
  /** Debugging messages. */
  public static final int LEVEL_DEBUG1 = 7;
  /** Detailed debugging that would not produce a ridiculous amount of
   * output if it were enabled system-wide. */
  public static final int LEVEL_DEBUG2 = 8;
  /** Debugging messages that produce more output than would be reasonable
   * if this level were enabled system-wide.  (<i>Eg</i>, messages in inner
   * loops, or per-file, per-hash step, etc.) */
  public static final int LEVEL_DEBUG3 = 9;

  /** Log level (numeric) at which stack traces will be included */
  static final String PARAM_STACKTRACE_LEVEL = PREFIX + "stackTraceLevel";
  static final int DEFAULT_STACKTRACE_LEVEL = LEVEL_DEBUG;

  /** Log severity (numeric) for which stack traces will be included no
   * matter what the current log level */
  static final String PARAM_STACKTRACE_SEVERITY =
    PREFIX + "stackTraceSeverity";
  static final int DEFAULT_STACKTRACE_SEVERITY = LEVEL_ERROR;

  // Mapping between numeric level and string
  static LevelDescr levelDescrs[] = {
    new LevelDescr(LEVEL_CRITICAL, "Critical"),
    new LevelDescr(LEVEL_ERROR, "Error"),
    new LevelDescr(LEVEL_SITE_ERROR, "SiteError"),
    new LevelDescr(LEVEL_WARNING, "Warning"),
    new LevelDescr(LEVEL_SITE_WARNING, "SiteWarning"),
    new LevelDescr(LEVEL_INFO, "Info"),
    // There must be entries for both "Debug" and "Debug1" in table.
    // Whichever string is last will be used in messages
    new LevelDescr(LEVEL_DEBUG1, "Debug1"),
    new LevelDescr(LEVEL_DEBUG, "Debug"),
    new LevelDescr(LEVEL_DEBUG2, "Debug2"),
    new LevelDescr(LEVEL_DEBUG3, "Debug3"),
  };

  // Default default log level if config parameter not set.
  private static final int DEFAULT_LEVEL = LEVEL_INFO;

  private static int paramStackTraceLevel = DEFAULT_STACKTRACE_LEVEL;
  private static int paramStackTraceSeverity = DEFAULT_STACKTRACE_SEVERITY;
  private static Format timestampDf =
    FastDateFormat.getInstance(DEFAULT_TIMESTAMP_DATEFORMAT);

  private static/* final*/ Map<String, Logger> logs = new HashMap<String, Logger>();
  private static List<LogTarget> targets = new ArrayList<LogTarget>();

  /** Experimental for use in unit tests */
  public static void resetLogs() {
    logs = new HashMap<String, Logger>();
  }

  // allow default level to be specified on command line
  private static int globalDefaultLevel;

  private static boolean deferredInitDone = false;

  private static ThreadLocal targetStack = new ThreadLocal() {
      protected Object initialValue() {
	return new Vector();
      }
    };

  private static Logger myLog;

  // Use weak map so dead threads can get GCed
  private static int threadCtr = 0;
  private static Map threadIds = new ReferenceMap(ReferenceMap.WEAK,
						  ReferenceMap.HARD);


  int level;			// this log's level
  private String name;			// this log's name
  private int defaultLevel = globalDefaultLevel;
  private String defaultLevelParam = PARAM_DEFAULT_LEVEL;
  private boolean idThread = true;

  static {
    // until we get configured, output to default target
    setDefaultTarget();
    setInitialDefaultLevel();
  }

  // tk - make this private and use PrivilegedAccessor (which needs
  // to be enhanced to handle constructors)
  /** Constructor not intended for outside use - not private only so
   * test classes can use it
   */
  Logger(int level, String name) {
    this.level = level;
    this.name = name;
  }

  /** Private constructor for use by public factory methods
   */
  private Logger(int level, String name, String defaultLevelParam) {
    this.level = level;
    this.name = name;
    this.defaultLevel = level;
    this.defaultLevelParam = defaultLevelParam;
  }

  /**
   * Logger factory.  Return the unique instance
   * of <code>Logger</code> with the given name, creating it if necessary.
   * @param name identifies the log instance, appears in output
   */
  public static Logger getLogger(String name) {
    deferredInit();
    return getLoggerWithInitialLevel(name, getConfiguredLevel(name));
  }
  
  /**
   * <p>Convenience method to name a logger after a class.
   * Simply calls {@link #getLogger(String)} with the result of
   * {@link Class#getSimpleName()}.</p>
   * @param clazz The class after which to name the returned logger.
   * @return A logger named after the given class.
   * @since 1.56
   */
  public static Logger getLogger(Class<?> clazz) {
    return getLogger(clazz.getSimpleName());
  }

  private static void deferredInit() {
    // Can't call this until Configuration class is fully loaded.
    if (!deferredInitDone) {
      // set this true FIRST, as this method will be called recursively by
      // deferredInit()
      deferredInitDone = true;
      myLog = Logger.getLogger("Logger");
    }
  }

  /**
   * Special purpose Logger factory.  Return the unique instance
   * of <code>Logger</code> with the given name, creating it if necessary.
   * This is here primarily so <code>Configuration</code> can create a
   * log without being invoked recursively, which causes its class
   * initialization to not complete correctly.
   * @param name identifies the log instance, appears in output
   * @param initialLevel the initial log level (<code>Logger.LEVEL_XXX</code>).
   */
  public static Logger getLoggerWithInitialLevel(String name,
						 int initialLevel) {
    // This method MUST NOT make any reference to Configuration !!
    if (name == null) {
      name = genName();
    }
    Logger l = (Logger)logs.get(name);
    if (l == null) {
      l = new Logger(initialLevel, name);
      if (myLog != null) myLog.debug2("Creating logger: " + name);
      logs.put(name, l);
    }
    return l;
  }

  /**
   * Special purpose Logger factory.  Return the <code>Logger</code> with
   * the given name, establishing its initial default level, and the
   * configuration parameter name of its default level.  This is primarily
   * intended to be used by classes that implement a foreign logging
   * interface in terms of this logger.  Because such interfaces
   * potentially support logging from a large number of classes not
   * directly related to LOCKSS, it is useful to be able to establish
   * defaults that are distinct from the normal system-wide defaults.
   * @param name identifies the log instance, appears in output
   * @param defaultLevelName the default log level if no config param is
   * present specifying the level or the default level
   * @param defaultLevelParam the name of the config param specifying the
   * default level.
   */
  public static Logger getLoggerWithDefaultLevel(String name,
						 String defaultLevelName,
						 String defaultLevelParam) {
    deferredInit();
    if (name == null) {
      name = genName();
    }
    Logger l = (Logger)logs.get(name);
    if (l == null) {
      int defaultLevel = globalDefaultLevel;
      try {
	defaultLevel = levelOf(defaultLevelName);
      } catch (Exception e) {
      }
      l = new Logger(defaultLevel, name, defaultLevelParam);
      if (myLog != null) myLog.debug2("Creating logger: " + name);
      l.setLevel(l.getConfiguredLevel());
      logs.put(name, l);
    }
    return l;
  }

  public static Format getTimeStampFormat() {
    return timestampDf;
  }

  /** Get the log level for the given log name from the configuration.
   */
  int getConfiguredLevel() {
    return getConfiguredLevel(name, defaultLevelParam, defaultLevel);
  }

  /** Get the log level for the given log name from the configuration.
   */
  static int getConfiguredLevel(String name) {
    return getConfiguredLevel(name, PARAM_DEFAULT_LEVEL, globalDefaultLevel);
  }

  /** Get the log level for the given log name from the configuration.
   */
  static int getConfiguredLevel(String name, String defaultParamName,
				int defaultLevel) {
    String levelName =
      CurrentConfig.getParam(StringUtil.replaceString(PARAM_LOG_LEVEL,
                                                      "<logname>", name),
                             CurrentConfig.getParam(defaultParamName));
    int level = defaultLevel;
    if (levelName != null) {
      try {
	level = levelOf(levelName);
      } catch (IllegalArgumentException e) {
	level = defaultLevel;
      }
    }
    return level;
  }

  static int uncnt = 0;
  static String genName() {
    return "Unnamed" + ++uncnt;
  }

  static Vector<String> levelNames = null;

  // Make vector for fast level -> name lookup
  private static void initLevelNames() {
    levelNames = new Vector<String>();
    for (int ix = 0; ix < levelDescrs.length; ix++) {
      LevelDescr l = levelDescrs[ix];
      if (levelNames.size() < l.level + 1) {
	levelNames.setSize(l.level + 1);
      }
      levelNames.set(l.level, l.name);
    }
  }

  /** Get the initial default log level, specified by the
   * org.lockss.defaultLogLevel system property if present, or DEFAULT_LEVEL
   */
  public static int getInitialDefaultLevel() {
    String s = System.getProperty(SYSPROP_DEFAULT_LOG_LEVEL);
    int l = DEFAULT_LEVEL;
    if (s != null && !"".equals(s)) {
      try {
	l = levelOf(s);
      } catch (IllegalArgumentException e) {
	// no action
      }
    }
    return l;
  }

  /** Return numeric log level (<code>Logger.LEVEL_XXX</code>) for given name.
   */
  public static int levelOf(String name) throws IllegalArgumentException {
    for (int ix = 0; ix < levelDescrs.length; ix++) {
      LevelDescr l = levelDescrs[ix];
      if (l.name.equalsIgnoreCase(name)) {
	return l.level;
      }
    }
    throw new IllegalArgumentException("Log level not found: " + name);
  }

  /** Return name of given log level (<code>Logger.LEVEL_XXX</code>).
   */
  public static String nameOf(int level) {
    if (levelNames == null) {
      initLevelNames();
    }
    String name = null;
    if (level >= 0 && level < levelNames.size()) {
      name = (String)levelNames.get(level);
    }
    if (name == null) {
      name = "Unknown";
    }
    return name;
  }

  /**
   * Set a single output target for all loggers.
   * @param target object that implements <code>LogTarget</code> interface.
   */
  static void setTarget(LogTarget target) {
    targets.clear();
    addTarget(target);
  }

  /**
   * Set the default output target for all loggers.
   */
  static void setDefaultTarget() {
    setTarget(getDefaultTarget());
  }

  /**
   * Get the default output target for all loggers.
   */
  static LogTarget getDefaultTarget() {
    Class tgtClass = StdErrTarget.class;
    String tgtName = System.getProperty(SYSPROP_DEFAULT_LOG_TARGET);
    if (!StringUtil.isNullString(tgtName)) {
      try {
	tgtClass = Class.forName(tgtName);
      } catch (ClassNotFoundException e) {
	System.err.println("Couldn't load log target " + tgtName +
			   ": " + e.toString());
      }
    }
    LogTarget tgt;
    try {
      tgt = (LogTarget)tgtClass.newInstance();
    } catch (Exception e) {
      System.err.println("Couldn't instantiate log target " + tgtName +
			 ": " + e.toString());
      tgt = new StdErrTarget();
    }
    return tgt;
  }

  /**
   * Add an output target to all loggers.
   * @param target instance of <code>LogTarget</code> implementation.
   */
  public static void addTarget(LogTarget target) {
    addTargetTo(target, targets);
  }

  /**
   * Add an output target to a target list.  All target adding should
   * eventually call this
   * @param target instance of <code>LogTarget</code> implementation.
   * @param tgts list of targets to which target will be added after it is
   * initialize.
   */
  static void addTargetTo(LogTarget target, List<LogTarget> tgts) {
    if (!tgts.contains(target)) {
      try {
	target.init();
	tgts.add(target);		// add only if init succeeded
      } catch (Exception e) {
	myLog.error("Log target " + target + " threw", e);
      }
    }
  }

  /**
   * Install the targets, replacing any current targets
   * @param newTargets list of targets to be installed
   */
  public static void setTargets(List<LogTarget> newTargets) {
    List<LogTarget> tgts = new ArrayList<LogTarget>();
    // make list of initialized targets
    for (LogTarget lt : newTargets) {
      addTargetTo(lt, tgts);
    }
    if (!tgts.isEmpty()) {		// ensure targets is never empty
      targets = tgts;
    }
  }

  //private

  /** Set the initial default log level to that specified by the
   * org.lockss.defaultLogLevel system property if present, or DEFAULT_LEVEL
   */
  static void setInitialDefaultLevel() {
    globalDefaultLevel = getInitialDefaultLevel();
  }

  /** Return a callback to reset log levels when config changes.  XXX This
   * shouldn't be public.  Fix when there's a proper way to stop and
   * restart Logger
   */
  public static Configuration.Callback getConfigCallback() {
    return
      new Configuration.Callback() {
	public void configurationChanged(Configuration newConfig,
					 Configuration oldConfig,
					 Configuration.Differences diffs) {
	  if (diffs.contains(PREFIX)) {
	    setAllLogLevels();
	    if (diffs.contains(PARAM_LOG_TARGETS)) {
	      setLogTargets();
	    }
	    paramStackTraceLevel = newConfig.getInt(PARAM_STACKTRACE_LEVEL,
						    DEFAULT_STACKTRACE_LEVEL);
	    paramStackTraceSeverity =
	      newConfig.getInt(PARAM_STACKTRACE_SEVERITY,
			       DEFAULT_STACKTRACE_SEVERITY);

	    String df = newConfig.get(PARAM_TIMESTAMP_DATEFORMAT,
				      DEFAULT_TIMESTAMP_DATEFORMAT);

	    try {
	      timestampDf = FastDateFormat.getInstance(df);
	    } catch (IllegalArgumentException e) {
	      timestampDf =
		FastDateFormat.getInstance(DEFAULT_TIMESTAMP_DATEFORMAT);
	      myLog.warning("Invalid DataFormat: " + df + ", using default");
	    }
	  }
	}
      };
  }

  /** Set log level of all logs to the currently configured value
   */
  private static void setAllLogLevels() {
    for (Iterator iter = logs.values().iterator();
	 iter.hasNext(); ) {
      Logger l = (Logger)iter.next();
      l.setLevel(l.getConfiguredLevel());
    }
  }

  /** Change list of targets to that specified by config param
   */
  private static void setLogTargets() {
    List<LogTarget> tgts =
      targetListFromString(CurrentConfig.getParam(PARAM_LOG_TARGETS));
    if (tgts != null && !tgts.isEmpty()) {
      setTargets(tgts);
    } else {
      myLog.debug("Leaving log targets unchanged");
    }
  }

  /** Convert semicolon-separated string of log target class names to a
   * list of log target instances
   * @return List of instances, or null if any errors occurred
   */
  static List<LogTarget> targetListFromString(String s) {
    boolean err = false;
    List<LogTarget> tgts = new ArrayList<LogTarget>();
    Vector names = StringUtil.breakAt(s, ';');
    for (Iterator iter = names.iterator(); iter.hasNext(); ) {
      String targetName = (String)iter.next();
      try {
	Class targetClass = Class.forName(targetName);
	LogTarget target = (LogTarget)targetClass.newInstance();
	tgts.add(target);
      } catch (Exception e) {
	myLog.error("Can't create log target \"" + targetName + "\": " +
		    e.toString());
	err = true;
      }
    }
    return err ? null : tgts;
  }

  static List<LogTarget> getTargets() {
    return targets;
  }

  /**
   * Set minimum severity level logged by this log
   * @param level <code>Logger.LEVEL_XXX</code>
   */
  public void setLevel(int level) {
    if (this.level != level) {
//        info("Changing log level to " + nameOf(level));
      this.level = level;
    }
  }

  /**
   * Set minimum severity level logged by this log
   * @param levelName level string
   */
  public void setLevel(String levelName) {
    setLevel(levelOf(levelName));
  }

  /**
   * Return true if this log is logging at or above specified level
   * Use this in cases where generating the log message is expensive,
   * to avoid the overhead when the message will not be output.
   * @param level (<code>Logger.LEVEL_XXX</code>)
   */
  boolean isLevel(int level) {
    return this.level >= level;
  }

  /** Common case of </code>isLevel()</code> */
  public boolean isDebug() {
    return isLevel(LEVEL_DEBUG);
  }

  /** Common case of </code>isLevel()</code> */
  public boolean isDebug1() {
    return isLevel(LEVEL_DEBUG1);
  }

  /** Common case of </code>isLevel()</code> */
  public boolean isDebug2() {
    return isLevel(LEVEL_DEBUG2);
  }

  /** Common case of </code>isLevel()</code> */
  public boolean isDebug3() {
    return isLevel(LEVEL_DEBUG3);
  }

  /**
   * Return true if this log is logging at or above specified level
   * @param level name
   */
  public boolean isLevel(String level) {
    return this.level >= levelOf(level);
  }

  /**
   * Log a message with the specified log level
   * @param level log level (<code>Logger.LEVEL_XXX</code>)
   * @param msg log message
   * @param e <code>Throwable</code>
   */
  public void log(int level, String msg, Throwable e) {
    if (isLevel(level)) {
      StringBuilder sb = new StringBuilder();
      msg = StringUtils.stripEnd(msg, null);
      if (idThread) {
	sb.append(getThreadId(Thread.currentThread()));
	sb.append("-");
      }
      sb.append(name);
      sb.append(": ");
      sb.append(msg);
      if (e != null) {
	// toString() sometimes contains more info than getMessage()
	String emsg = e.toString();
	sb.append(": ");
	sb.append(emsg);
	if (level <= paramStackTraceSeverity ||
	    isLevel(paramStackTraceLevel)) {
	  sb.append("\n    ");
	  sb.append(StringUtil.trimStackTrace(emsg,
					      StringUtil.stackTraceString(e)));
	}

	if (e instanceof SQLException) {
	  SQLException sqle = ((SQLException)e).getNextException();

	  while (sqle != null) {
	    sb.append("\n    ");
	    sb.append("Next SQLException: " + sqle.toString());
	    sqle = sqle.getNextException();
	  }
	}
      }
      writeMsg(level, sb.toString());
    }
  }

  /**
   * Log a message with the specified log level
   * @param level log level (<code>Logger.LEVEL_XXX</code>)
   * @param msg log message
   */
  public void log(int level, String msg) {
    log(level, msg, null);
  }

  public void setIdThread(boolean ena) {
    idThread = ena;
  }

  private volatile String unannouncedName = null;
  private volatile String announcedName = null;

  public void threadNameChanged() {
    Thread thread = Thread.currentThread();
    String name = thread.getName();
    if (!name.equals(announcedName)) {
      unannouncedName = name;
    }
  }

  String getThreadId(Thread thread) {
    String id;
    synchronized (threadIds) {
      id = (String)threadIds.get(thread);
      if (id == null) {
	id = Integer.toString(++threadCtr);
	threadIds.put(thread, id);
	unannouncedName = thread.getName();
      }
    }
    // This recursive call MUST be AFTER the threadIds map is updated, and
    // AFTER setting unannouncedName to null
    if (unannouncedName != null) {
      String name = unannouncedName;
      unannouncedName = null;
      debug("ThreadId " + id + " is " + name);
      announcedName = name;
    }
    return id;
  }

  int getThreadMapSize() {
    return threadIds.size();
  }

  public String toString() {
    return "[Logger " + name + "]";
  }

  // Invoke all the targets to write a message.
  // Maintain a thread-local stack of targets, to avoid invoking any
  // target recursively.
  private void writeMsg(int level, String msg) {
    Iterator iter = targets.iterator();
    while (iter.hasNext()) {
      LogTarget target = (LogTarget)iter.next();
      Vector ts = (Vector)targetStack.get();
      if ( ! ts.contains(target)) {
	try {
	  ts.add(target);
	  target.handleMessage(this, level, msg);
	} catch (Exception e) {
	  // should log this?
	} finally {
	  ts.remove(target);
	}
      }
    }
  }

  // log instance methods
  /** Log a critical message */
  public void critical(String msg) {
    log(LEVEL_CRITICAL, msg, null);
  }

  /** Log a critical message with an exception backtrace */
  public void critical(String msg, Throwable e) {
    log(LEVEL_CRITICAL, msg, e);
  }

  /** Log an error message */
  public void error(String msg) {
    log(LEVEL_ERROR, msg, null);
  }

  /** Log an error message with an exception backtrace */
  public void error(String msg, Throwable e) {
    log(LEVEL_ERROR, msg, e);
  }

  /** Log a site error message */
  public void siteError(String msg) {
    log(LEVEL_SITE_ERROR, msg, null);
  }

  /** Log a site error message with an exception backtrace */
  public void siteError(String msg, Throwable e) {
    log(LEVEL_SITE_ERROR, msg, e);
  }

  /** Log a warning message */
  public void warning(String msg) {
    log(LEVEL_WARNING, msg, null);
  }

  /** Log a warning message with an exception backtrace */
  public void warning(String msg, Throwable e) {
    log(LEVEL_WARNING, msg, e);
  }

  /** Log a site warning message */
  public void siteWarning(String msg) {
    log(LEVEL_SITE_WARNING, msg, null);
  }

  /** Log a site warning message with an exception backtrace */
  public void siteWarning(String msg, Throwable e) {
    log(LEVEL_SITE_WARNING, msg, e);
  }

  /** Log an information message */
  public void info(String msg) {
    log(LEVEL_INFO, msg, null);
  }

  /** Log an information message with an exception backtrace */
  public void info(String msg, Throwable e) {
    log(LEVEL_INFO, msg, e);
  }

  /** Log a level 1 debug message */
  public void debug(String msg) {
    log(LEVEL_DEBUG, msg, null);
  }

  /** Log a level 1 debug message with an exception backtrace */
  public void debug(String msg, Throwable e) {
    log(LEVEL_DEBUG, msg, e);
  }

  /** Log a level 1 debug message */
  public void debug1(String msg) {
    log(LEVEL_DEBUG1, msg, null);
  }

  /** Log a level 1 debug message with an exception backtrace */
  public void debug1(String msg, Throwable e) {
    log(LEVEL_DEBUG1, msg, e);
  }

  /** Log a level 2 debug message */
  public void debug2(String msg) {
    log(LEVEL_DEBUG2, msg, null);
  }

  /** Log a level 2 debug message with an exception backtrace */
  public void debug2(String msg, Throwable e) {
    log(LEVEL_DEBUG2, msg, e);
  }

  /** Log a level 3 debug message */
  public void debug3(String msg) {
    log(LEVEL_DEBUG3, msg, null);
  }

  /** Log a level 3 debug message with an exception backtrace */
  public void debug3(String msg, Throwable e) {
    log(LEVEL_DEBUG3, msg, e);
  }

  // log level descriptor class
  private static class LevelDescr {
    int level;
    String name;

    LevelDescr(int level, String name) {
    this.level = level;
    this.name = name;
    }
  }

}
