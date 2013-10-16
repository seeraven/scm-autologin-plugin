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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.inject.Singleton;

/**
 * Configuration container for the AutoLogin plugin.
 * 
 * @author Clemens Rabe
 */
@Singleton
@XmlRootElement(name = "config")
@XmlAccessorType(XmlAccessType.FIELD)
public class AutoLoginConfig
{

  @XmlElement(name = "variable-name")
  private String variableName = "X_REMOTE_USER";

  @XmlElement(name = "password")
  private String password = "autoLogin";

  @XmlElement(name = "allow-unknown")
  private boolean allowUnknown = true;

  @XmlElement(name = "email-domain")
  private String emailDomain = "example.com";

  /**
   * Get the name of the HTTP header variable containing the user name.
   * 
   * @return The name of the HTTP header variable containing the user name.
   */
  public String getVariableName()
  {
    return variableName;
  }

  /**
   * Set the name of the HTTP header variable containing the user name.
   * 
   * @param variableName
   *          - The name of the HTTP header variable.
   */
  public void setVariableName(String variableName)
  {
    this.variableName = variableName;
  }

  /**
   * Get the password used for all found users. This password must be set for
   * existing users, otherwise they can't use the auto-login feature.
   * 
   * @return password - The password.
   */
  public String getPassword()
  {
    return password;
  }

  /**
   * Set the password used for all users. This password must be set for existing
   * users, otherwise they can't use the auto-login feature.
   * 
   * @param password
   *          - The new password.
   */
  public void setPassword(String password)
  {
    this.password = password;
  }

  /**
   * If the flag is set to true, users unknown to SCM-Manager are allowed to log
   * in.
   * 
   * @return The flag whether unknown users are allowed to log in.
   */
  public boolean getAllowUnknown()
  {
    return allowUnknown;
  }

  /**
   * Set the flag whether users unknown to SCM-Manager are allowed to log in.
   * 
   * @param allowUnknown
   *          - If the flag is set to true, users unknown to SCM-Manager are
   *          allowed to log in.
   */
  public void setAllowUnknown(boolean allowUnknown)
  {
    this.allowUnknown = allowUnknown;
  }

  /**
   * Get the email domain of the user.
   * 
   * @return The email domain of the user.
   */
  public String getEmailDomain()
  {
    return emailDomain;
  }

  /**
   * Set the email domain of the user.
   * 
   * @param emailDomain
   *          - The email domain.
   */
  public void setEmailDomain(String emailDomain)
  {
    this.emailDomain = emailDomain;
  }

}
