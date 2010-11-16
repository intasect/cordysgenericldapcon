package com.cordys.coe.ac.genericldap.connection;

import com.cordys.coe.ac.genericldap.config.IGenLDAPConfiguration;
import com.cordys.coe.ac.genericldap.exception.GenericLDAPConnectorException;

import com.eibus.management.IManagedComponent;

/**
 * This factory creates the LDAP connection manager object.
 *
 * @author  pgussow
 */
public class ConnectionManagerFactory
{
    /**
     * This method creates a new connection manager object.
     *
     * @param   configuration  The configuration for the connection.
     * @param   mcParent       The managed component parent.
     *
     * @return  The connection manager to use.
     *
     * @throws  GenericLDAPConnectorException  In case of any exceptions.
     */
    public static IConnectionManager createConnection(IGenLDAPConfiguration configuration,
                                                      IManagedComponent mcParent)
                                               throws GenericLDAPConnectorException
    {
        return new ConnectionManager(configuration);
    }
}
