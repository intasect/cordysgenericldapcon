package com.cordys.coe.ac.genericldap.config;

import com.cordys.coe.ac.genericldap.exception.GenericLDAPConnectorException;

/**
 * This factory creates the configuration object that is to be used.
 *
 * @author  pgussow
 */
public class ConfigurationFactory
{
    /**
     * This method creates the configuration object based on the source XML.
     *
     * @param   configuration  The configuration XML.
     *
     * @return  The created configuration object.
     *
     * @throws  GenericLDAPConnectorException  In case of any exceptions.
     */
    public static IGenLDAPConfiguration createConfiguration(int configuration)
                                                     throws GenericLDAPConnectorException
    {
        return new GenericLDAPConfiguration(configuration);
    }
}
