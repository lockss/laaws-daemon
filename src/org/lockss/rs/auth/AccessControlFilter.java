/*

 Copyright (c) 2016-2017 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.rs.auth;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import org.lockss.account.UserAccount;
import org.lockss.app.LockssDaemon;
import org.lockss.config.Configuration;
import org.lockss.config.CurrentConfig;
import org.lockss.util.Logger;

/**
 * Abstract Access Control filter to be extended in each web service.
 */
public abstract class AccessControlFilter implements ContainerRequestFilter {
  public static final String BASIC_AUTH_TYPE = "basic";
  public static final String NONE_AUTH_TYPE = "none";

  /** Prefix for configuration properties. */
  public static final String PREFIX = Configuration.PREFIX + "restAuth.";
  public static final String PARAM_AUTH_TYPE = PREFIX + "authenticationType";
  public static final String DEFAULT_AUTH_TYPE = NONE_AUTH_TYPE;

  public static final String invalidAutheticationType =
      "Invalid Authentication Type (must be BASIC or NONE).";
  public static final String forbiddenAccess = "Access blocked for all users.";
  public static final String noAuthorizationHeader = "No authorization header.";
  public static final String noCredentials = "No userid/password credentials.";
  public static final String badCredentials =
      "Bad userid/password credentials.";
  public static final String noUser = "User not found.";
  public static final String noRequiredRole =
      "User does not have the required role.";

  private static final Logger log = Logger.getLogger(AccessControlFilter.class);

  @Context
  protected ResourceInfo resourceInfo;

  /**
   * Provides the names of the roles permissible for the user to be able to
   * execute operations of this web service when no javax.annotation.security
   * annotations are specified for web service operations.
   *
   * Overriden in a subclass.
   * 
   * @param resourceInfo
   *          A ResourceInfo with information about the resource involved.
   * @param requestContext
   *          A ContainerRequestContext with the request context.
   * 
   * @return a Set<String> with the permissible roles.
   */
  protected abstract Set<String> getPermissibleRoles(ResourceInfo resourceInfo,
      ContainerRequestContext requestContext) ;

  /**
   * Filter method called after a resource has been matched to a request, but
   * before the request has been dispatched to the resource.
   *
   * @param requestContext
   *          A ContainerRequestContext with the request context.
   */
  @Override
  public void filter(ContainerRequestContext requestContext) {
    final String DEBUG_HEADER = "filter(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Invoked.");

    // Get the configured authentication type.
    String authenticationType =
	CurrentConfig.getParam(PARAM_AUTH_TYPE, DEFAULT_AUTH_TYPE);
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "authenticationType = " + authenticationType);

    // Check whether access is allowed to anybody.
    if (NONE_AUTH_TYPE.equalsIgnoreCase(authenticationType)) {
      // Yes: Continue normally.
      if (log.isDebug2())
	log.debug2(DEBUG_HEADER + "Authorized (like everybody else).");
      return;
      // No: Check whether the authentication type is not "basic".
    } else if (!BASIC_AUTH_TYPE.equalsIgnoreCase(authenticationType)) {
      // Yes: Report the problem.
      log.error(invalidAutheticationType);
      log.error("authenticationType = " + authenticationType);

      requestContext.abortWith(Response.status(Response.Status.FORBIDDEN)
	  .entity(toJsonMessage(authenticationType + ": "
	      + invalidAutheticationType)).build());
      return;
    }

    Method method = resourceInfo.getResourceMethod();
    if (log.isDebug3()) {
      log.debug3(DEBUG_HEADER + "method = " + method);
      log.debug3(DEBUG_HEADER + "method.getDeclaredAnnotations() = "
	  + Arrays.toString(method.getDeclaredAnnotations()));
    }

    // Check whether access to this method is allowed to anybody.
    if (method.isAnnotationPresent(PermitAll.class)) {
      // Yes: Continue normally.
      if (log.isDebug2())
	log.debug2(DEBUG_HEADER + "Authorized (like everybody else).");
      return;
    }

    // Get the HTTP request method name.
    String httpMethodName = requestContext.getMethod().toUpperCase();
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "httpMethodName = " + httpMethodName);

    // Get the request path segments.
    List<PathSegment> pathSegments =
	requestContext.getUriInfo().getPathSegments();
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "pathSegments = " + pathSegments);

    // Check whether it is the base swagger request.
    if ("GET".equals(httpMethodName) &&
	method.getName().equals("getListing") && pathSegments.size() == 1 &&
	"swagger.json".equals(pathSegments.get(0).getPath().toLowerCase())) {
      // Yes: Continue normally.
      if (log.isDebug2())
	log.debug2(DEBUG_HEADER + "Authorized (like everybody else).");
      return;
    }

    // Check whether access to this method is denied to everybody.
    if (method.isAnnotationPresent(DenyAll.class)) {
      // Yes: Report the problem.
      log.info(DEBUG_HEADER + forbiddenAccess);
      log.info(DEBUG_HEADER + "method = " + method);
      log.info(DEBUG_HEADER + "method.getDeclaredAnnotations() = "
	  + Arrays.toString(method.getDeclaredAnnotations()));

      requestContext.abortWith(Response.status(Response.Status.FORBIDDEN)
	  .entity(toJsonMessage(forbiddenAccess)).build());
      return;
    }

    Set<String> permissibleRoles = null;

    // Check whether a role security annotation is present.
    if (method.isAnnotationPresent(RolesAllowed.class))	{
      // Yes: Get the permissible roles from the annotation.
      RolesAllowed rolesAnnotation = method.getAnnotation(RolesAllowed.class);
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "rolesAnnotation = " + rolesAnnotation);

      permissibleRoles =
	  new HashSet<String>(Arrays.asList(rolesAnnotation.value()));
    } else {
      // No: Get the custom permissible roles.
      permissibleRoles = getPermissibleRoles(resourceInfo, requestContext);
    }

    // Get the authorization header.
    String authorizationHeader =
	requestContext.getHeaderString("authorization");
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "authorizationHeader = " + authorizationHeader);

    // Check whether no authorization header was found.
    if (authorizationHeader == null) {
      // Yes: Report the problem.
      log.info(DEBUG_HEADER + noAuthorizationHeader);

      requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
	  .entity(toJsonMessage(noAuthorizationHeader)).build());
      return;
    }

    // Get the user credentials in the authorization header.
    String[] credentials = decodeBasicAuthorizationHeader(authorizationHeader);

    // Check whether no credentials were found.
    if (credentials == null) {
      // Yes: Report the problem.
      log.info(DEBUG_HEADER + noCredentials);

      requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
	  .entity(toJsonMessage(noCredentials)).build());
      return;
    }

    // Check whether the found credentials are not what was expected.
    if (credentials.length != 2) {
      // Yes: Report the problem.
      log.info(DEBUG_HEADER + badCredentials);
      log.info(DEBUG_HEADER
	  + "bad credentials = " + Arrays.toString(credentials));

      requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
	  .entity(toJsonMessage(badCredentials)).build());
      return;
    }

    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "credentials[0] = " + credentials[0]);

    // Get the user account.
    UserAccount userAccount = null;

    try {
      userAccount = LockssDaemon.getLockssDaemon().getAccountManager()
	  .getUser(credentials[0]);
    } catch (Exception e) {
      log.error("credentials[0] = " + credentials[0]);
      log.error("credentials[1] = " + credentials[1]);
      log.error("LockssDaemon.getLockssDaemon().getAccountManager().getUser(credentials[0])", e);
    }

    // Check whether no user was found.
    if (userAccount == null) {
      // Yes: Report the problem.
      log.info(DEBUG_HEADER + noUser);

      requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
	  .entity(toJsonMessage(badCredentials)).build());
      return;
    }

    if (log.isDebug3()) log.debug3(DEBUG_HEADER
	+ "userAccount.getName() = " + userAccount.getName());

    // Verify the user credentials.
    boolean goodCredentials = userAccount.check(credentials[1]);
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "goodCredentials = " + goodCredentials);

    // Check whether the user credentials are not good.
    if (!goodCredentials) {
      // Yes: Report the problem.
      log.info(DEBUG_HEADER + badCredentials);
      log.info(DEBUG_HEADER
	  + "userAccount.getName() = " + userAccount.getName());
      log.info(DEBUG_HEADER
	  + "bad credentials = " + Arrays.toString(credentials));

      requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
	  .entity(toJsonMessage(badCredentials)).build());
      // No: Check whether the user has the role required to execute this
      // operation.
    } else if (isAuthorized(userAccount, permissibleRoles)) {
      // Yes: Continue normally.
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Authorized.");
    } else {
      // No: Report the problem.
      log.info(DEBUG_HEADER + noRequiredRole);
      log.info(DEBUG_HEADER + "userName = " + userAccount.getName());

      requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
	  .entity(toJsonMessage(noRequiredRole)).build());
    }
  }

  /**
   * Decodes the basic authorization header
   * 
   * @param header
   *          A String with the Authorization header.
   * @return a String[] with the user name and the password.
   */
  private String[] decodeBasicAuthorizationHeader(String header) {
    final String DEBUG_HEADER = "decodeBasicAuthorizationHeader(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "header = " + header);

    // Get the header meaningful bytes.
    byte[] decodedBytes =
	Base64.getDecoder().decode(header.replaceFirst("[B|b]asic ", ""));

    // Check whether nothing was decoded.
    if (decodedBytes == null || decodedBytes.length == 0) {
      // Yes: Done.
      return null;
    }

    // No: Extract the individual credential items, the user name and the
    // password.
    String[] result = new String(decodedBytes).split(":", 2);
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "result = " + Arrays.toString(result));

    return result;
  }

  /**
   * Provides an indication of whether the user has the role required to execute
   * operations of this web service.
   * 
   * @param userAccount
   *          A UserAccount with the user account data.
   * @param permissibleRoles
   *          A String[] with the roles permissible for the user to be able to
   *          execute operations of this web service.
   * @return a boolean with <code>TRUE</code> if the user has the role required
   *         to execute operations of this web service, <code>FALSE</code>
   *         otherwise.
   */
  private boolean isAuthorized(UserAccount userAccount,
      Set<String> permissibleRoles) {
    final String DEBUG_HEADER = "isAuthorized(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER
	  + "userAccount.getName() = " + userAccount.getName());
      log.debug2(DEBUG_HEADER
	  + "userAccount.getRoles() = " + userAccount.getRoles());
      log.debug2(DEBUG_HEADER
	  + "userAccount.getRoleSet() = " + userAccount.getRoleSet());
    }

    // An administrator is always authorized.
    if (userAccount.isUserInRole(Roles.ROLE_USER_ADMIN)) {
      return true;
    }

    // Check whether there are no permissible roles.
    if (permissibleRoles == null || permissibleRoles.size() == 0) {
      // Yes: Normal users are not authorized.
      return false;
    }

    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "permissibleRoles = " + permissibleRoles);

    // Loop though all the permissible roles.
    for (String permissibleRole : permissibleRoles) {
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "permissibleRole = " + permissibleRole);

      // If any role is permitted, this user is authorized.
      if (Roles.ROLE_ANY.equals(permissibleRole)) {
	return true;
      }

      // The user is authorized if it has this permissible role.
      if (userAccount.isUserInRole(permissibleRole)) {
	return true;
      }
    }

    // The user is not authorized because it does not have any of the
    // permissible roles.
    return false;
  }

  /**
   * Formats to JSON any message to be returned.
   * 
   * @param message
   *          A String with the message to be formatted.
   * @return a String with the JSON-formatted message.
   */
  private static String toJsonMessage(String message) {
    return "{\"message\":\"" + message + "\"}"; 
  }
}
