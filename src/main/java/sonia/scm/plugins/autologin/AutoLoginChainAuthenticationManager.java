package sonia.scm.plugins.autologin;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sonia.scm.SCMContextProvider;
import sonia.scm.cache.Cache;
import sonia.scm.cache.CacheManager;
import sonia.scm.security.EncryptionHandler;
import sonia.scm.user.User;
import sonia.scm.user.UserManager;
import sonia.scm.util.AssertUtil;
import sonia.scm.util.IOUtil;
import sonia.scm.util.Util;
import sonia.scm.web.security.AbstractAuthenticationManager;
import sonia.scm.web.security.AuthenticationHandler;
import sonia.scm.web.security.AuthenticationListener;
import sonia.scm.web.security.AuthenticationResult;
import sonia.scm.web.security.AuthenticationState;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Override of the ChainAuthenticationManager class to prefer the
 * AutoLoginAuthenticationHandler over the DefaultAuthenticationHandler.
 * 
 * @author Clemens Rabe
 */
@Singleton
public class AutoLoginChainAuthenticationManager extends
    AbstractAuthenticationManager
{

  /** Field description */
  public static final String CACHE_NAME = "sonia.cache.auth";

  /** the logger for ChainAuthenticatonManager */
  private static final Logger logger = LoggerFactory
      .getLogger(AutoLoginChainAuthenticationManager.class);

  // ~--- constructors ---------------------------------------------------------

  /**
   * Constructs ...
   * 
   * 
   * 
   * @param userManager
   * @param authenticationHandlerSet
   * @param encryptionHandler
   * @param cacheManager
   * @param authenticationListenerProvider
   * @param authenticationListeners
   */
  @Inject
  public AutoLoginChainAuthenticationManager(UserManager userManager,
      Set<AuthenticationHandler> authenticationHandlerSet,
      EncryptionHandler encryptionHandler, CacheManager cacheManager,
      Set<AuthenticationListener> authenticationListeners)
  {
    AssertUtil.assertIsNotEmpty(authenticationHandlerSet);
    AssertUtil.assertIsNotNull(cacheManager);
    this.authenticationHandlers = sort(userManager, authenticationHandlerSet);
    this.encryptionHandler = encryptionHandler;
    this.cache = cacheManager.getCache(String.class,
        AuthenticationCacheValue.class, CACHE_NAME);

    if (Util.isNotEmpty(authenticationListeners))
    {
      addListeners(authenticationListeners);
    }
  }

  // ~--- methods --------------------------------------------------------------

  /**
   * Method description
   * 
   * 
   * @param request
   * @param response
   * @param username
   * @param password
   * 
   * @return
   */
  @Override
  public AuthenticationResult authenticate(HttpServletRequest request,
      HttpServletResponse response, String username, String password)
  {
    AssertUtil.assertIsNotEmpty(username);
    AssertUtil.assertIsNotEmpty(password);

    String encryptedPassword = encryptionHandler.encrypt(password);
    AuthenticationResult ar = getCached(username, encryptedPassword);

    if (ar == null)
    {
      if (logger.isTraceEnabled())
      {
        logger.trace("no authentication result for user {} found in cache",
            username);
      }

      ar = doAuthentication(request, response, username, password);

      if ((ar != null) && ar.isCacheable())
      {
        cache
            .put(username, new AuthenticationCacheValue(ar, encryptedPassword));
      }
    } else if (logger.isDebugEnabled())
    {
      logger.debug("authenticate {} via cache", username);
    }

    return ar;
  }

  /**
   * Method description
   * 
   * 
   * @throws IOException
   */
  @Override
  public void close() throws IOException
  {
    for (AuthenticationHandler authenticator : authenticationHandlers)
    {
      if (logger.isTraceEnabled())
      {
        logger.trace("close authenticator {}", authenticator.getClass());
      }

      IOUtil.close(authenticator);
    }
  }

  /**
   * Method description
   * 
   * 
   * @param context
   */
  @Override
  public void init(SCMContextProvider context)
  {
    for (AuthenticationHandler authenticator : authenticationHandlers)
    {
      if (logger.isTraceEnabled())
      {
        logger.trace("initialize authenticator {}", authenticator.getClass());
      }

      authenticator.init(context);
    }
  }

  /**
   * Method description
   * 
   * 
   * @param request
   * @param response
   * @param username
   * @param password
   * 
   * @return
   */
  private AuthenticationResult doAuthentication(HttpServletRequest request,
      HttpServletResponse response, String username, String password)
  {
    AuthenticationResult ar = null;

    if (logger.isTraceEnabled())
    {
      logger.trace("start authentication chain for user {}", username);
    }

    for (AuthenticationHandler authenticator : authenticationHandlers)
    {
      if (logger.isTraceEnabled())
      {
        logger.trace("check authenticator {} for user {}",
            authenticator.getClass(), username);
      }

      try
      {
        AuthenticationResult result = authenticator.authenticate(request,
            response, username, password);

        if (logger.isDebugEnabled())
        {
          logger.debug("authenticator {} ends with result, {}", authenticator
              .getClass().getName(), result);
        }

        if ((result != null)
            && (result.getState() != null)
            && (result.getState().isSuccessfully() || (result.getState() == AuthenticationState.FAILED)))
        {
          if (result.getState().isSuccessfully() && (result.getUser() != null))
          {
            User user = result.getUser();

            user.setType(authenticator.getType());
            ar = result;

            // notify authentication listeners
            fireAuthenticationEvent(request, response, user);
          }

          break;
        }
      } catch (Exception ex)
      {
        logger.error("error durring authentication process of "
            .concat(authenticator.getClass().getName()), ex);
      }
    }

    return ar;
  }

  /**
   * Method description
   * 
   * 
   * @param userManager
   * @param authenticationHandlerSet
   * 
   * @return
   */
  @VisibleForTesting
  private List<AuthenticationHandler> sort(UserManager userManager,
      Set<AuthenticationHandler> authenticationHandlerSet)
  {
    List<AuthenticationHandler> handlers = Lists
        .newArrayListWithCapacity(authenticationHandlerSet.size());
    AuthenticationHandler specialHandler = null;

    String first = Strings.nullToEmpty(userManager.getDefaultType());

    for (AuthenticationHandler handler : authenticationHandlerSet)
    {
      // AutoLoginAuthenticationHandler is added in next loop
      if (handler instanceof AutoLoginAuthenticationHandler)
      {
        specialHandler = handler;
        continue;
      }

      if (first.equals(handler.getType()))
      {
        handlers.add(0, handler);
      } else
      {
        handlers.add(handler);
      }
    }

    // Add the AutoLoginAuthenticationHandler to the first position
    if (specialHandler != null)
    {
      if (logger.isTraceEnabled())
      {
        logger
            .trace("Insert AutoLoginAuthenticationHandler at the beginning of handler list");
      }

      handlers.add(0, specialHandler);
    } else
    {
      logger
          .error("The AutoLoginAuthenticationHandler was not found in the provided list of AuthenticationHandlers!");
    }

    return handlers;
  }

  // ~--- get methods ----------------------------------------------------------

  /**
   * Method description
   * 
   * 
   * @param username
   * @param encryptedPassword
   * 
   * @return
   */
  private AuthenticationResult getCached(String username,
      String encryptedPassword)
  {
    AuthenticationResult result = null;
    AuthenticationCacheValue value = cache.get(username);

    if (value != null)
    {
      String cachedPassword = value.password;

      if (cachedPassword.equals(encryptedPassword))
      {
        result = value.authenticationResult;
      }
    }

    return result;
  }

  // ~--- inner classes --------------------------------------------------------

  /**
   * Class description
   * 
   * 
   * @version Enter version here..., 2011-01-15
   * @author Sebastian Sdorra
   */
  private static class AuthenticationCacheValue implements Serializable
  {

    /** Field description */
    private static final long serialVersionUID = 2201116145941277549L;

    // ~--- constructors -------------------------------------------------------

    /**
     * Constructs ...
     * 
     * 
     * 
     * @param ar
     * @param password
     */
    public AuthenticationCacheValue(AuthenticationResult ar, String password)
    {
      this.authenticationResult = new AuthenticationResult(
          ar.getUser().clone(), ar.getGroups(), ar.getState());
      this.password = password;
    }

    // ~--- fields -------------------------------------------------------------

    /** Field description */
    private AuthenticationResult authenticationResult;

    /** Field description */
    private String password;
  }

  // ~--- fields ---------------------------------------------------------------

  /** Field description */
  private List<AuthenticationHandler> authenticationHandlers;

  /** Field description */
  private Cache<String, AuthenticationCacheValue> cache;

  /** Field description */
  private EncryptionHandler encryptionHandler;
}