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

import com.cordys.coe.ac.genericldap.exception.GenericLDAPConnectorException;

import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPEntry;
import com.novell.ldap.LDAPSchema;
import com.novell.ldap.LDAPSearchConstraints;
import com.novell.ldap.LDAPSearchResults;

/**
 * This interface describes the LDAP connection manager.
 *
 * @author  pgussow
 */
public interface IConnectionManager
{
    /**
     * This method should be called to gracefully close all connections.
     */
    void disconnect();

    /**
     * This method will return an active connection. If none is available it will wait.
     *
     * @return  The connection or null if not connected.
     *
     * @throws  GenericLDAPConnectorException  In case no connection could be obtained.
     */
    LDAPConnection getConnection()
                          throws GenericLDAPConnectorException;

    /**
     * This method returns the schema for the current LDAP server.
     *
     * @return  The schema for the current LDAP server.
     */
    LDAPSchema getSchema();

    /**
     * This method will read a specific DN from the LDAP server.
     *
     * @param   dn  The DN to read.
     *
     * @return  The LDAP entry that was read.
     *
     * @throws  GenericLDAPConnectorException  In case of any exceptions.
     */
    LDAPEntry readLDAPEntry(String dn)
                     throws GenericLDAPConnectorException;
    
    /**
     * This method will read a specific DN from the LDAP server.
     *
     * @param   dn  The DN to read.
     *
     * @return  The LDAP entry that was read.
     *
     * @throws  GenericLDAPConnectorException  In case of any exceptions.
     */
    LDAPEntry readLDAPEntry(String dn, String[] attributes)
                     throws GenericLDAPConnectorException;

    /**
     * This method releases the connection to the pool.
     *
     * @param  connection  The connection to releases the connection to the pool.
     */
    void releaseConnection(LDAPConnection connection);

    /**
     * This method searches LDAP for entries.
     *
     * @param   rootDN          The DN to start searching from.
     * @param   scope           The scope for searching.
     * @param   filter          The filter to use.
     * @param   attributeNames  The names of the attributes to retrieve.
     * @param   excludeValues   Whether or not to exclude the values when returning the results.
     * @param   constraints     The constraints to apply.
     *
     * @return  The result from the search query.
     *
     * @throws  GenericLDAPConnectorException  In case of any exceptions.
     */
    LDAPSearchResults search(String rootDN, int scope, String filter, String[] attributeNames,
                             boolean excludeValues, LDAPSearchConstraints constraints)
                      throws GenericLDAPConnectorException;
}
