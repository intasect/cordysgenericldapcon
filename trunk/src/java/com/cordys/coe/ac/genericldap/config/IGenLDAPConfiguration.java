package com.cordys.coe.ac.genericldap.config;

import com.cordys.coe.ac.genericldap.connection.IConnectionManager;

import com.eibus.connector.nom.Connector;

import java.security.Provider;

/**
 * This method defines the configuration options for the generic LDAP connector.
 *
 * @author  pgussow
 */
public interface IGenLDAPConfiguration
{
    /**
     * This method gets the connection manager to use.
     *
     * @return  The connection manager to use.
     */
    IConnectionManager getConnectionManager();

    /**
     * This method gets the connection type (secure/plain).
     *
     * @return  The connection type (secure/plain).
     */
    EConnectionType getConnectionType();

    /**
     * This method gets the connector to use for sending out client requests.
     *
     * @return  The connector to use for sending out client requests.
     */
    Connector getConnector();

    /**
     * This method gets the keystore that contains the certificate of the LDAP server.
     *
     * @return  The keystore that contains the certificate of the LDAP server.
     */
    String getKeyStore();

    /**
     * This method gets the maximum number of search results.
     *
     * @return  The maximum number of search results.
     */
    int getMaximumNumberOfSearchResults();

    /**
     * This method gets the number of LDAP connections to create.
     *
     * @return  The number of LDAP connections to create.
     */
    int getNrOfConnections();

    /**
     * This method gets the password.
     *
     * @return  The password.
     */
    String getPassword();

    /**
     * This method gets the prot number.
     *
     * @return  The prot number.
     */
    int getPort();

    /**
     * This method gets the searchroot.
     *
     * @return  The searchroot.
     */
    String getSearchRoot();

    /**
     * This method gets the security provider to use for connecting to LDAP.
     *
     * @return  The security provider to use for connecting to LDAP.
     */
    Provider getSecurityProvider();

    /**
     * This method gets the server.
     *
     * @return  The server.
     */
    String getServer();

    /**
     * This method gets the user.
     *
     * @return  The user.
     */
    String getUser();

    /**
     * Returns the authenticate configuration value.
     *
     * @return  authenticate value.
     */
    boolean isAnonymousBind();

    /**
     * This method sets the connection manager to use.
     *
     * @param  connectionManager  The connection manager to use.
     */
    void setConnectionManager(IConnectionManager connectionManager);

    /**
     * This method sets the connector to use for sending out client requests.
     *
     * @param  connector  The connector to use for sending out client requests.
     */
    void setConnector(Connector connector);
}
