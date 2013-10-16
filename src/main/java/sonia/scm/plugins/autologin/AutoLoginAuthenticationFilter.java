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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import sonia.scm.plugin.ext.Extension;
import sonia.scm.user.User;
import sonia.scm.user.UserManager;
import sonia.scm.web.filter.AutoLoginModule;

/**
 * This class is used in the BasicAuthenticationFilter to provide the auto-login
 * feature.
 * 
 * @author Clemens Rabe
 */
@Singleton
@Extension
public class AutoLoginAuthenticationFilter implements AutoLoginModule
{

  /** the logger for AutoLoginAuthenticationFilter */
  private static final Logger logger = LoggerFactory
      .getLogger(AutoLoginAuthenticationFilter.class);

  /** the configuration of the plugin */
  private AutoLoginConfig config;

  /**
   * Constructor of the authentication filter.
   * 
   * @param configuration
   *          - The configuration of SCM-Manager.
   * @param pluginConfiguration
   *          - The configuration of the plugin.
   */
  @Inject
  public AutoLoginAuthenticationFilter(AutoLoginConfig pluginConfiguration)
  {
    this.config = pluginConfiguration;
  }

  /**
   * Authenticate a user using the given request object. If the user can not be
   * authenticated, e.g., because required headers are not set null must be
   * returned.
   * 
   * @param request
   *          The HTTP request.
   * @param response
   *          The HTTP response. Use only if absolutely necessary.
   * @param subject
   *          The subject object.
   * @return Return a User object or null.
   */
  public User authenticate(HttpServletRequest request,
      HttpServletResponse response, Subject subject)
  {
    String headerValue = request.getHeader(config.getVariableName());
    User user = null;

    if (headerValue != null)
    {
      String remoteUser = AutoLoginHelper.extractUsername(headerValue);
      logger.debug(config.getVariableName() + " => " + remoteUser);

      try
      {
        subject.login(new UsernamePasswordToken(remoteUser,
            config.getPassword(), request.getRemoteAddr()));
        user = subject.getPrincipals().oneByType(User.class);
      } catch (AuthenticationException ex)
      {
        logger.warn("Can't login user '" + remoteUser + "' with password '"
            + AutoLoginAuthenticationHandler.USERPASS + "'");
      }
    } else
    {
      logger.debug("Can't determine auto login using http header variable "
          + config.getVariableName());
    }

    if (user != null)
    {
      logger.debug("Auto-Login successfull.");
    } else
    {
      logger.debug("Auto-Login failed.");
    }

    return user;
  }
}
