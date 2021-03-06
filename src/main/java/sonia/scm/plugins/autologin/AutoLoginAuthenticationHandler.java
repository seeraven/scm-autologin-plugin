/**
 * Copyright (c) 2013, Clemens Rabe
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 3. Neither the name of SCM-Manager; nor the names of its
 *    contributors may be used to endorse or promote products derived from this
 *    software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package sonia.scm.plugins.autologin;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.io.IOException;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import sonia.scm.SCMContextProvider;
import sonia.scm.plugin.ext.Extension;
import sonia.scm.user.User;
import sonia.scm.user.UserManager;
import sonia.scm.web.security.AuthenticationHandler;
import sonia.scm.web.security.AuthenticationResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sonia.scm.store.Store;
import sonia.scm.store.StoreFactory;

/**
 * The authentication handler to support the auto login.
 * 
 * @author Clemens Rabe
 * 
 */
@Singleton
@Extension
public class AutoLoginAuthenticationHandler implements AuthenticationHandler
{

  /** The authentication type. */
  public static final String TYPE = "autoLogin";

  /** The type used for the store. */  
  public static final String STORETYPE = "autoLogin";

  /** the logger for AutoLoginAuthenticationHandler */
  private static final Logger logger = LoggerFactory
      .getLogger(AutoLoginAuthenticationHandler.class);

  /** The configuration of the plugin. */
  private AutoLoginConfig config;

  /** The store of the configuration. */
  private Store<AutoLoginConfig> store;

  /** The user manager. */
  private UserManager userManager;

  /**
   * Constructor.
   * 
   * @param userManager
   *          - The user manager.
   * @param storeFactory
   *          - The factory to get the store.
   */
  @Inject
  public AutoLoginAuthenticationHandler(UserManager userManager,
      StoreFactory storeFactory)
  {
    this.userManager = userManager;
    store = storeFactory.getStore(AutoLoginConfig.class, STORETYPE);
  }

  /**
   * Initialize the AutoLoginAuthenticationHandler.
   */
  @Override
  public void init(SCMContextProvider context)
  {
    config = store.get();

    if (config == null)
    {
      config = new AutoLoginConfig();
    }
  }

  /**
   * Close the AutoLoginAuthenticationHandler.
   */
  @Override
  public void close() throws IOException
  {
  }

  /**
   * Get the type of the AutoLoginAuthenticationHandler.
   */
  @Override
  public String getType()
  {
    return TYPE;
    // return userManager.getDefaultType();
  }

  @Override
  public AuthenticationResult authenticate(HttpServletRequest request,
      HttpServletResponse response, String username, String password)
  {
    // Extract REMOTE_USER and act only if it is the same as username
	// Since hooks can be sent without the REMOTE_USER header, the
	// header is only verified if available. Otherwise the user is
	// verified the usual way.
    String headerValue = request.getHeader(getConfig().getVariableName());

    if (headerValue != null)
    {
      String remoteUser = AutoLoginHelper.extractUsername(headerValue);
      
      // No header variable -> return NOT_FOUND
      if (remoteUser == null)
      {
        return AuthenticationResult.NOT_FOUND;
      }

      // Different user -> return NOT_FOUND
      if (!remoteUser.equals(username))
      {
        if (logger.isDebugEnabled())
        {
          logger.debug("remote user is {}, but user {} shall be authenticated",
              remoteUser, username);
        }

        return AuthenticationResult.NOT_FOUND;
      }
    }

    // The request originated from the AutoLoginAuthenticationFilter...
    AuthenticationResult result = null;

    // Search for the user in the user manager
    User user = userManager.get(username);

    if (user != null)
    {
      if (TYPE.equals(user.getType()))
      {
        // Login user without a password.
        if (logger.isDebugEnabled())
        {
          logger.debug(
              "user {} successfully authenticated by auto login plugin",
              username);
        }

        user.setPassword(null);

        // Set groups
        Set<String> groups = AutoLoginHelper.splitGroups(config.getGroups());

        // result = new AuthenticationResult(user, AuthenticationState.SUCCESS);
        result = new AuthenticationResult(user, groups);
      } else
      {
        if (logger.isDebugEnabled())
        {
          logger.debug("{} is not an {} user", username, TYPE);
        }

        result = AuthenticationResult.NOT_FOUND;
      }
    // Create user if requested and REMOTE_USER variable is set
    // (don't create users when called from a hook).
    } else if (config.getAllowUnknown() && (headerValue != null))
    {
      // Create user when enabled
      user = createAutoLoginUser(username);

      // Set groups
      Set<String> groups = AutoLoginHelper.splitGroups(config.getGroups());

      // result = new AuthenticationResult(user, AuthenticationState.SUCCESS);
      result = new AuthenticationResult(user, groups);

      if (logger.isDebugEnabled())
      {
        logger.debug("user {} successfully created by auto login plugin",
            username);
      }
    }

    if (result == null)
    {
      if (logger.isDebugEnabled())
      {
        logger.debug("user {} not authenticated by auto login plugin ",
            username);
      }

      result = AuthenticationResult.NOT_FOUND;
    }

    return result;
  }

  /**
   * Set the plugin configuration and store it in the store.
   * 
   * @param config
   *          - The plugin configuration.
   */
  public void storeConfig(AutoLoginConfig config)
  {
    this.config = config;
    store.set(config);
  }

  /**
   * Get the plugin configuration.
   * 
   * @return The plugin configuration.
   */
  public AutoLoginConfig getConfig()
  {
    return config;
  }

  /**
   * Set the plugin configuration.
   * 
   * @param config
   *          - The plugin configuration.
   */
  public void setConfig(AutoLoginConfig config)
  {
    this.config = config;
  }

  /**
   * Create a new user and save it in the user database.
   * 
   * @param username
   *          - The user name.
   * @return The user object.
   */
  private User createAutoLoginUser(String username)
  {
    User user = new User();

    user.setName(username);
    user.setDisplayName(username);
    user.setMail(username + "@" + config.getEmailDomain());

    // Do not encrypt password, because this would allow others to
    // login with the common password!
    user.setPassword("autoLogin");

    user.setType(TYPE);

    return user;
  }
}
