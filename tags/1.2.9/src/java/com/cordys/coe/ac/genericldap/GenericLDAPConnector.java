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
package com.cordys.coe.ac.genericldap;

import com.cordys.coe.ac.genericldap.config.ConfigurationFactory;
import com.cordys.coe.ac.genericldap.config.IGenLDAPConfiguration;
import com.cordys.coe.ac.genericldap.connection.ConnectionManagerFactory;
import com.cordys.coe.ac.genericldap.connection.IConnectionManager;
import com.cordys.coe.ac.genericldap.exception.GenericLDAPConnectorException;
import com.cordys.coe.ac.genericldap.localization.GenLDAPExceptionMessages;
import com.cordys.coe.ac.genericldap.localization.GeneralMessages;
import com.cordys.coe.coelib.LibraryVersion;
import com.cordys.coe.exception.ServerLocalizableException;
import com.cordys.coe.util.system.SystemInfo;

import com.eibus.connector.nom.Connector;

import com.eibus.soap.ApplicationConnector;
import com.eibus.soap.ApplicationTransaction;
import com.eibus.soap.Processor;
import com.eibus.soap.SOAPTransaction;

import com.eibus.util.logger.CordysLogger;

/**
 * The Generic LDAP connector opens a directory server via LDAP. Mostly used to read information
 * about users and organizatons and synchronize this with Cordys.
 */
public class GenericLDAPConnector extends ApplicationConnector
{
    /**
     * Contains the logger.
     */
    public static CordysLogger LOG = CordysLogger.getCordysLogger(GenericLDAPConnector.class);
    /**
     * Holds the name of the connector.
     */
    private static final String CONNECTOR_NAME = "Generic LDAP Connector Client";
    /**
     * Holds the configuration object for this connector.
     */
    private IGenLDAPConfiguration m_configuration;

    /**
     * This method gets called when the processor is being stopped.
     *
     * @param  pProcessor  The processor that is being stopped.
     */
    @Override public void close(Processor pProcessor)
    {
        if ((m_configuration != null) && (m_configuration.getConnectionManager() != null))
        {
            // Close all LDAP connections gracefully.
            m_configuration.getConnectionManager().disconnect();
        }

        if (LOG.isInfoEnabled())
        {
            LOG.info(GeneralMessages.CONNECTOR_STOPPED);
        }
    }

    /**
     * This method creates the transaction that will handle the requests.
     *
     * @param   stTransaction  The SOAP-transaction containing the message.
     *
     * @return  The newly created transaction.
     */
    @Override public ApplicationTransaction createTransaction(SOAPTransaction stTransaction)
    {
        return new GenericLDAPTransaction(m_configuration);
    }

    /**
     * This method gets called when the processor is started. It reads the configuration of the
     * processor and creates the connector with the proper parameters. It will also create a client
     * connection to Cordys.
     *
     * @param  pProcessor  The processor that is started.
     */
    @Override public void open(Processor pProcessor)
    {
        // Check the CoELib version.
        try
        {
            LibraryVersion.loadAndCheckLibraryVersionFromResource(this.getClass(), true);
        }
        catch (Exception e)
        {
            LOG.fatal(e, GeneralMessages.COELIB_VERSION_MISMATCH);
            throw new IllegalStateException(e.toString());
        }

        try
        {
            if (LOG.isInfoEnabled())
            {
                LOG.info(GeneralMessages.CONNECTOR_STARTING, SystemInfo.getSystemInformation());
            }

            // Check if the coe.connector.startup.delay is set to an int bigger then 0.
            // If so we will sleep to allow attaching of the debugger.
            String sTemp = System.getProperty("coe.connector.startup.delay");

            if ((sTemp != null) && (sTemp.length() > 0))
            {
                try
                {
                    long lTime = Long.parseLong(sTemp);

                    if (lTime > 0)
                    {
                        if (LOG.isDebugEnabled())
                        {
                            LOG.debug("Going to pause for " + lTime +
                                      " ms to allow debugger attachment.");
                        }
                        Thread.sleep(lTime);
                    }
                }
                catch (Exception e)
                {
                    if (LOG.isDebugEnabled())
                    {
                        LOG.debug("Error checking for debugger delay", e);
                    }
                }
            }

            // Get the configuration
            m_configuration = ConfigurationFactory.createConfiguration(getConfiguration());

            // Create the client connection to Cordys.
            Connector connector = Connector.getInstance(CONNECTOR_NAME);

            if (!connector.isOpen())
            {
                connector.open();
            }

            m_configuration.setConnector(connector);

            // Create the LDAP connection manager
            IConnectionManager connectionManager = ConnectionManagerFactory.createConnection(m_configuration,
                                                                                             getManagedComponent());
            m_configuration.setConnectionManager(connectionManager);

            if (LOG.isInfoEnabled())
            {
                LOG.info(GeneralMessages.CONNECTOR_STARTED);
            }
        }
        catch (ServerLocalizableException sle)
        {
            LOG.fatal(sle, GeneralMessages.CONNECTOR_START_EXCEPTION);
            throw new IllegalStateException(sle);
        }
        catch (Exception e)
        {
            LOG.fatal(e, GeneralMessages.CONNECTOR_START_EXCEPTION);
            throw new IllegalStateException(new GenericLDAPConnectorException(e,
                                                                              GenLDAPExceptionMessages.GLE_ERROR_STARTING_GENERIC_LDAP_CONNECTOR));
        }
    }

    /**
     * This method gets called when the processor is ordered to rest.
     *
     * @param  processor  The processor that is to be in reset state
     */
    @Override public void reset(Processor processor)
    {
        if (LOG.isInfoEnabled())
        {
            LOG.info(GeneralMessages.CONNECTOR_RESET);
        }
    }
}
