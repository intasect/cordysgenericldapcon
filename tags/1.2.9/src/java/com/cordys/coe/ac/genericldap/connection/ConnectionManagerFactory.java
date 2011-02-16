/*
 * Copyright 2007 Cordys R&D B.V. 
 *
 *   This file is part of the Cordys Generic LDAP Connector. 
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
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
