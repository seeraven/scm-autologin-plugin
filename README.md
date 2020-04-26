scm-autologin-plugin
====================

This is an authentication plugin for [scm-manager]
that uses the `X_REMOTE_USER` http header to authenticate a user. It
is based on the [scm-auth-remoteuser-plugin] by Dominik Ruf.

This plugin is intended to be used in combination with a reverse proxy
(aka gateway) which performs the authentication.

It supports two different values for the `X_REMOTE_USER` variable. The first
one assumes a Basic authentification provided by the web server acting
as a gateway to the [scm-manager] server. In this case a matching user of
the [scm-manager] is automatically authenticated.

The second type is intended to be used in a SSL-secured environment together
with the FakeBasicAuth authentication method. In this case, the `X_REMOTE_USER`
variable contains the DN of the client certificate. Here, the CN component
is extracted and matching user names are authenticated automatically.

**IMPORTANT SECURITY INFORMATION
THIS PLUGIN IS ONLY MEANT TO BE USED BEHIND A REVERSE PROXY SERVER
THAT PREVENTS THE END USER OF SETTING THE HEADER VARIABLE!**


Requirements
============
 - [scm-manager] 1.35+ (currently you have to build the development version)
 - Apache2 or similar to provide the authentication

Take a look at the project [scm-environment] for ready-to-use installation
scripts.


Apache2 Configuration
=====================

Let us assume you are running the [scm-manager] on the same machine as the
web-server, so the [scm-manager] is available on the URI
`http://localhost:8080/scm`. The [scm-manager] should be available on your
web-server under the URI `https://your.domain.org/private/scm/private` using
either a basic authentication OR client certificates.

The configuration of the SSL setup is beyond these configuration notes,
but you should be able to find enough information about it using google.

First of all, make sure you load the following apache modules:

  - `mod_proxy` and `mod_proxy_http`
  - `mod_headers`
  - `mod_rewrite`

The first step is to configure the reverse proxy (gateway) part. This is
done using the following configuration:

    ProxyRequests     Off
    ProxyPreserveHost On

Now you have to configure the location `/private/scm/private` in more detail:

    <Location /private/scm/private>
        SSLRequireSSL
        SSLVerifyClient optional
        SSLVerifyDepth  2
        SSLOptions     +FakeBasicAuth
        SSLRequire      %{SSL_CIPHER_USEKEYSIZE} >= 128

        AuthName             "Restricted Access to Private SCM Manager"
        AuthType             Basic
        AuthBasicProvider    file
        AuthUserFile         /etc/apache2/private_scm.txt
        Require              valid-user

        ProxyPass                    http://localhost:8080/scm
        ProxyPassReverse             /scm
        ProxyPassReverseCookiePath   /scm /private/scm/private
        ProxyPassReverseCookieDomain localhost:8080 your.domain.org

        SetOutputFilter   INFLATE;proxy-html;DEFLATE
        ProxyHTMLURLMap   /scm/                /private/scm/private/
        ProxyHTMLURLMap   /private/scm/private /private/scm/private
        ProxyHTMLExtended On

        RewriteEngine On
        RewriteCond %{LA-U:REMOTE_USER} (.+)
        RewriteRule . - [E=RU:%1,NS]
        RequestHeader set X_REMOTE_USER "%{RU}e" env=RU
        RequestHeader unset Authorization
    </Location>

The first part sets up the SSL authentication part. Using the `FakeBasicAuth`
configuration, a basic authentication request is performed using the DN of
the client certificate as the username together with the encrypted password
"password". If there is no client-certificate or the authentication using the
DN fails, a basic authentication is performed as stated in the second part
of the configuration section. The last part is used to store the value of the
`REMOTE_USER` environment variable in the `X_REMOTE_USER` HTTP header. This header
is then used by the plugin to authenticate the user. Since a valid user is
required by the apache webserver, no invalid user can access the [scm-manager].
To avoid problems with the internal basic authentication method of [scm-manager],
the Authorization HTTP header is removed.


SCM-Manager Configuration
=========================

To ensure [scm-manager] is only available on the localhost, you should change
the file `scm-server/conf/server-config.xml` and replace the line

    <SystemProperty name="jetty.host" />

with

    <SystemProperty name="jetty.host" default="localhost" />

In addition, you have to uncomment the 'forwarded' option:

    <Set name="forwarded">true</Set>


Build and install the SCM-Manager
=================================

You need a recent JDK and Maven 3 to build [Scm-Manager] from scratch. On
Ubuntu 12.04+ you can install the Oracle JDK using the following commands:

    sudo add-apt-repository ppa:webupd8team/java
    sudo apt-get update
    sudo apt-get install oracle-java6-installer

If you had already installed an OpenJDK, you might want to switch to the
Oracle JDK using

    sudo update-java-alternatives -s java-6-oracle

Now install Maven3 and git:

    sudo apt-get install maven git

Clone the source code and build it:

    git clone https://github.com/scm-manager/scm-manager
    cd scm-manager
    mvn clean install

You find the standalone app archive under

    scm-server/target/scm-server-app.tar.gz


Build and install the Plugin
============================

You need Maven 3 to build this plugin. On Ubuntu 12.04+ you can install maven
by executing

    sudo apt-get install maven

Make sure the [scm-manager] source is available from the same base directory,
so that the directory layout looks like this:

    <base>/scm-manager
    <base>/scm-autologin-plugin

Now you have to create a target directory and execute the following commands
from the root directory of this plugin:

    cd <base>/scm-autologin-plugin
    mkdir ../scm-autologin-plugin-dist
    mvn scmp:install -DscmHome=../scm-autologin-plugin-dist

In the given target directory, you'll find the plugins subdirectory that you
have to merge with your [Scm-Manager] plugins directory (`~/.scm/plugins`). To do
this, you should copy the plugin source tree first by

    rsync -av ../scm-autologin-plugin-dist/plugins/sonia ~/.scm/plugins/

and then edit the file `~/.scm/plugins/classpath.xml` to include the installed
JAR file:

    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
    <classpath>
        ...
        <path>/sonia/scm/plugins/scm-autologin-plugin/1.0-SNAPSHOT/scm-autologin-plugin-1.0-SNAPSHOT.jar</path>
        ...
    </classpath>


[scm-manager]: http://www.scm-manager.org
[scm-auth-remoteuser-plugin]: https://bitbucket.org/domruf/scm-auth-remoteuser-plugin
[scm-environment]: https://github.com/seeraven/scm-environment