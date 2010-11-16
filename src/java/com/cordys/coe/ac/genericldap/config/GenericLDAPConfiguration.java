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
package com.cordys.coe.ac.genericldap.config;

import com.cordys.coe.ac.genericldap.GenLDAPConstants;
import com.cordys.coe.ac.genericldap.connection.IConnectionManager;
import com.cordys.coe.ac.genericldap.exception.GenericLDAPConnectorException;
import com.cordys.coe.ac.genericldap.localization.GenLDAPExceptionMessages;
import com.cordys.coe.ac.genericldap.localization.GeneralMessages;
import com.cordys.coe.util.xml.nom.XPathHelper;

import com.eibus.connector.nom.Connector;

import com.eibus.util.logger.CordysLogger;
import com.eibus.util.system.Native;

import com.eibus.xml.nom.Node;
import com.eibus.xml.xpath.XPathMetaInfo;

import java.security.Provider;

/**
 * This class holds the configuration details for the Generic LDAP Connector.
 */
class GenericLDAPConfiguration
    implements IGenLDAPConfiguration
{
    /**
     * Holds the name of the tag holding the anonymous bind information.
     */
    private static final String TAG_ANONYMOUS_BIND = "anonymousbind";
    /**
     * Holds the name of the tag holding the user information.
     */
    private static final String TAG_USER = "user";
    /**
     * Holds the name of the tag holding the server information.
     */
    private static final String TAG_SERVER = "server";
    /**
     * Holds the name of the tag holding the security provider information.
     */
    private static final String TAG_SECURITYPROVIDER = "securityprovider";
    /**
     * Holds the name of the tag holding the search root information.
     */
    private static final String TAG_SEARCHROOT = "searchroot";
    /**
     * Holds the name of the tag holding the port information.
     */
    private static final String TAG_PORT = "port";
    /**
     * Holds the name of the tag holding the password information.
     */
    private static final String TAG_PASSWORD = "password";
    /**
     * Holds the name of the tag holding the maximum number of search results information.
     */
    private static final String TAG_MAXIMUNNOOFSEACHRESULT = "maximunnoofseachresult";
    /**
     * Holds the name of the tag holding the keystore information.
     */
    private static final String TAG_KEYSTORE = "keystore";
    /**
     * Holds the name of the tag holding the connection type information.
     */
    private static final String TAG_CONNECTIONTYPE = "connectiontype";
    /**
     * Holds the name of the tag holding the configuration roottag.
     */
    private static final String TAG_CONFIGURATION = "configuration";
    /**
     * Holds the name of the tag holding the number of LDAP connections tag.
     */
    private static final String TAG_NR_OF_CONNECTIONS = "nrofconnections";
    /**
     * Contains the logger.
     */
    public static CordysLogger LOG = CordysLogger.getCordysLogger(GenericLDAPConfiguration.class);
    /**
     * Holds whether or not anononymous access is used.
     */
    private boolean m_anonymousBind;
    /**
     * Holds the connection manager to use.
     */
    private IConnectionManager m_connectionManager;
    /**
     * Holds the connection type.
     */
    private EConnectionType m_connectionType;
    /**
     * This method returns the connector to use.
     */
    private Connector m_connector;
    /**
     * Holds the location of the key store.
     */
    private String m_keyStore;
    /**
     * Holds the maximum number of search results.
     */
    private int m_maximumNumberOfSearchResults;
    /**
     * Holds the number of connections to start up.
     */
    private int m_nrOfConnections;
    /**
     * Holds the password to use.
     */
    private String m_password;
    /**
     * Holds the portnumber.
     */
    private int m_port;
    /**
     * Holds the search root.
     */
    private String m_searchroot;
    /**
     * Holds the security provider.
     */
    private String m_securityprovider;
    /**
     * Holds the server name.
     */
    private String m_server;
    /**
     * Holds the username.
     */
    private String m_user;

    /**
     * Creates the constructor.This loads the configuration object and pass it to XMLProperties for
     * processing.
     *
     * @param   configurationXML  The xml-node that contains the configuration.
     *
     * @throws  GenericLDAPConnectorException  In case of any exceptions.
     */
    public GenericLDAPConfiguration(int configurationXML)
                             throws GenericLDAPConnectorException
    {
        if (LOG.isDebugEnabled())
        {
            LOG.debug("Creating configuration object using this XML:\n" +
                      Node.writeToString(configurationXML, false));
        }

        if (configurationXML == 0)
        {
            throw new GenericLDAPConnectorException(GenLDAPExceptionMessages.GLE_CONFIGURATION_NOT_FOUND);
        }

        if (!Node.getName(configurationXML).equals(TAG_CONFIGURATION))
        {
            throw new GenericLDAPConnectorException(GenLDAPExceptionMessages.GLE_ROOTTAG_OF_THE_CONFIGURATION_SHOULD_BE_CONFIGURATION);
        }

        XPathMetaInfo xmi = new XPathMetaInfo();
        xmi.addNamespaceBinding("ns", GenLDAPConstants.NS_CONFIGURATION);
        
        if (XPathHelper.selectSingleNode(configurationXML, "ns:configuration", xmi) != 0)
        {
        	configurationXML = XPathHelper.selectSingleNode(configurationXML, "ns:configuration", xmi);
        }

        // Read all the data.
        m_connectionType = EConnectionType.valueOf(XPathHelper.getStringValue(configurationXML,
                                                                              "ns:" +
                                                                              TAG_CONNECTIONTYPE,
                                                                              xmi, ""));
        m_keyStore = XPathHelper.getStringValue(configurationXML, "ns:" + TAG_KEYSTORE, xmi, "");
        m_maximumNumberOfSearchResults = XPathHelper.getIntegerValue(configurationXML,
                                                                     "ns:" +
                                                                     TAG_MAXIMUNNOOFSEACHRESULT,
                                                                     xmi, 50);
        m_password = XPathHelper.getStringValue(configurationXML, "ns:" + TAG_PASSWORD, xmi, "");

        if ((m_password != null) && (m_password.trim().length() > 0))
        {
            m_password = new String(Native.decodeBinBase64(m_password.getBytes(),
                                                           m_password.length()));
        }
        m_port = XPathHelper.getIntegerValue(configurationXML, "ns:" + TAG_PORT, xmi, 389);
        m_searchroot = XPathHelper.getStringValue(configurationXML, "ns:" + TAG_SEARCHROOT, xmi,
                                                  "");
        m_securityprovider = XPathHelper.getStringValue(configurationXML,
                                                        "ns:" + TAG_SECURITYPROVIDER, xmi, "");
        m_anonymousBind = XPathHelper.getBooleanValue(configurationXML, "ns:" + TAG_ANONYMOUS_BIND,
                                                      xmi, true);
        m_user = XPathHelper.getStringValue(configurationXML, "ns:" + TAG_USER, xmi, "");
        m_server = XPathHelper.getStringValue(configurationXML, "ns:" + TAG_SERVER, xmi, "");
        m_nrOfConnections = XPathHelper.getIntegerValue(configurationXML,
                                                        "ns:" + TAG_NR_OF_CONNECTIONS, xmi, 5);
    }

    /**
     * @see  com.cordys.coe.ac.genericldap.config.IGenLDAPConfiguration#getConnectionManager()
     */
    @Override public IConnectionManager getConnectionManager()
    {
        return m_connectionManager;
    }

    /**
     * @see  com.cordys.coe.ac.genericldap.config.IGenLDAPConfiguration#getConnectionType()
     */
    public EConnectionType getConnectionType()
    {
        return m_connectionType;
    }

    /**
     * @see  com.cordys.coe.ac.genericldap.config.IGenLDAPConfiguration#getConnector()
     */
    @Override public Connector getConnector()
    {
        return m_connector;
    }

    /**
     * @see  com.cordys.coe.ac.genericldap.config.IGenLDAPConfiguration#getKeyStore()
     */
    public String getKeyStore()
    {
        return m_keyStore;
    }

    /**
     * @see  com.cordys.coe.ac.genericldap.config.IGenLDAPConfiguration#getMaximumNumberOfSearchResults()
     */
    public int getMaximumNumberOfSearchResults()
    {
        return m_maximumNumberOfSearchResults;
    }

    /**
     * @see  com.cordys.coe.ac.genericldap.config.IGenLDAPConfiguration#getNrOfConnections()
     */
    @Override public int getNrOfConnections()
    {
        return m_nrOfConnections;
    }

    /**
     * @see  com.cordys.coe.ac.genericldap.config.IGenLDAPConfiguration#getPassword()
     */
    public String getPassword()
    {
        return m_password;
    }

    /**
     * @see  com.cordys.coe.ac.genericldap.config.IGenLDAPConfiguration#getPort()
     */
    public int getPort()
    {
        return m_port;
    }

    /**
     * @see  com.cordys.coe.ac.genericldap.config.IGenLDAPConfiguration#getSearchRoot()
     */
    public String getSearchRoot()
    {
        return m_searchroot;
    }

    /**
     * @see  com.cordys.coe.ac.genericldap.config.IGenLDAPConfiguration#getSecurityProvider()
     */
    public Provider getSecurityProvider()
    {
        Provider pReturn = null;

        String sClassName = m_securityprovider;

        if (sClassName.length() > 0)
        {
            try
            {
                Class<?> cProvider = Class.forName(sClassName);

                if (cProvider != null)
                {
                    Object oProvider = cProvider.newInstance();

                    if (oProvider instanceof Provider)
                    {
                        pReturn = (Provider) oProvider;
                    }
                    else
                    {
                        throw new GenericLDAPConnectorException(GenLDAPExceptionMessages.GLE_THE_CLASS_0_DOES_NOT_EXTEND_THE_JAVASECURITYPROVIDER_CLASS,
                                                                sClassName);
                    }
                }
            }
            catch (Exception e)
            {
                LOG.error(e, GeneralMessages.CONFIG_SECURITY_PROVIDER_NOT_FOUND, sClassName);
            }
        }

        return pReturn;
    }

    /**
     * @see  com.cordys.coe.ac.genericldap.config.IGenLDAPConfiguration#getServer()
     */
    public String getServer()
    {
        return m_server;
    }

    /**
     * @see  com.cordys.coe.ac.genericldap.config.IGenLDAPConfiguration#getUser()
     */
    public String getUser()
    {
        return m_user;
    }

    /**
     * @see  com.cordys.coe.ac.genericldap.config.IGenLDAPConfiguration#isAnonymousBind()
     */
    public boolean isAnonymousBind()
    {
        return m_anonymousBind;
    }

    /**
     * @see  com.cordys.coe.ac.genericldap.config.IGenLDAPConfiguration#setConnectionManager(com.cordys.coe.ac.genericldap.connection.IConnectionManager)
     */
    @Override public void setConnectionManager(IConnectionManager connectionManager)
    {
        m_connectionManager = connectionManager;
    }

    /**
     * @see  com.cordys.coe.ac.genericldap.config.IGenLDAPConfiguration#setConnector(com.eibus.connector.nom.Connector)
     */
    @Override public void setConnector(Connector connector)
    {
        m_connector = connector;
    }
}
