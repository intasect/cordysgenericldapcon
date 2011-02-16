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

import com.cordys.coe.ac.genericldap.config.IGenLDAPConfiguration;
import com.cordys.coe.ac.genericldap.exception.GenericLDAPConnectorException;
import com.cordys.coe.ac.genericldap.localization.GenLDAPExceptionMessages;
import com.cordys.coe.ac.genericldap.localization.GeneralMessages;
import com.cordys.coe.ac.genericldap.soap.BaseMethod;
import com.cordys.coe.ac.genericldap.soap.LDAPMethod;
import com.cordys.coe.ac.genericldap.soap.compatibility.CreateOrUpdateCordysuser;
import com.cordys.coe.ac.genericldap.soap.compatibility.ReadLDAP;
import com.cordys.coe.ac.genericldap.soap.compatibility.Update;
import com.cordys.coe.ac.genericldap.soap.compatibility.UpdateCordysUsers;
import com.cordys.coe.ac.genericldap.soap.impl.ImplementationFactory;
import com.cordys.coe.exception.ServerLocalizableException;
import com.cordys.coe.util.general.ExceptionUtil;

import com.eibus.soap.ApplicationTransaction;
import com.eibus.soap.BodyBlock;

import com.eibus.util.logger.CordysLogger;

import com.eibus.xml.nom.Node;

/**
 * This class is the Implementation of ApplicationTransaction. This class will recieve the request
 * process it if it is a valid one. The GENLDAP Transaction handles 3 kind of transactions : - the
 * main one is to read from the LDAP server - the second and third are supporting functions to
 * create authenticated users in cordys.
 */
public class GenericLDAPTransaction
    implements ApplicationTransaction
{
    /**
     * Contains the logger.
     */
    public static CordysLogger LOG = CordysLogger.getCordysLogger(GenericLDAPTransaction.class);
    /**
     * The request type by which the request is to be redirected to different classes.
     */
    private static final String SERVICE_TYPE = "GENLDAP";
    /**
     * Holds the configuration for the current processor.
     */
    private IGenLDAPConfiguration m_configuration;

    /**
     * Creates the transactional object.
     *
     * @param  configuration  the default search root for each request
     */
    public GenericLDAPTransaction(IGenLDAPConfiguration configuration)
    {
        m_configuration = configuration;
    }

    /**
     * This will be called when a transaction is being aborted.
     */
    public void abort()
    {
        if (LOG.isWarningEnabled())
        {
            LOG.warn(null, GeneralMessages.TRANSACTION_ABORT);
        }
    }

    /**
     * This method returns returns if this transaction can process requests of the given type.
     *
     * @param   sType  The type of message that needs to be processed
     *
     * @return  true if the type can be processed. Otherwise false.
     */
    public boolean canProcess(String sType)
    {
        return SERVICE_TYPE.equals(sType);
    }

    /**
     * This method is called when the transaction is committed.
     */
    public void commit()
    {
        if (LOG.isInfoEnabled())
        {
            LOG.info(GeneralMessages.TRANSACTION_COMMIT);
        }
    }

    /**
     * This method processes the received request.
     *
     * @param   bbRequest   The request-bodyblock.
     * @param   bbResponse  The response-bodyblock.
     *
     * @return  true if the connector has to send the response. If someone else sends the response
     *          false is returned.
     */
    public boolean process(BodyBlock bbRequest, BodyBlock bbResponse)
    {
        boolean bReturn = true;

        try
        {
            if (LOG.isDebugEnabled())
            {
                LOG.debug("Incoming SOAP request: " +
                          Node.writeToString(bbRequest.getXMLNode(), true));
            }

            // Get the implementation of the method. We need to see if it's
            // one of the old methods or a new one.
            BaseMethod actualMethod = null;

            String action = ImplementationFactory.getActionFromImplementation(bbRequest
                                                                              .getMethodDefinition());

            if ((action != null) && (action.length() > 0))
            {
                // It's the new 1.2 way of calling methods.
                actualMethod = new LDAPMethod(bbRequest, bbResponse, m_configuration);
            }
            else
            {
                if (LOG.isDebugEnabled())
                {
                    LOG.debug("Running in compatibility mode");
                }

                // Run in compatibility mode.
                // check possible operation : read, createOrUpdateCordysUser or UpdateCordysUsers
                int implementation = bbRequest.getMethodDefinition().getImplementation();
                String operation = Node.getAttribute(implementation, "operation");

                // helper function to create a cordys user (not in tupple old/new format)
                if (operation.equalsIgnoreCase("createOrUpdateCordysUser"))
                {
                    actualMethod = new CreateOrUpdateCordysuser(bbRequest, bbResponse,
                                                                m_configuration);
                }
                else if (operation.equalsIgnoreCase("UpdateCordysUsers"))
                {
                    actualMethod = new UpdateCordysUsers(bbRequest, bbResponse, m_configuration);
                }
                else if (operation.equalsIgnoreCase("read"))
                {
                    actualMethod = new ReadLDAP(bbRequest, bbResponse, m_configuration);
                }
                else if (operation.equalsIgnoreCase("update"))
                {
                    actualMethod = new Update(bbRequest, bbResponse, m_configuration);
                }
                else
                {
                    throw new GenericLDAPConnectorException(GenLDAPExceptionMessages.GLE_OPERATION_0_IS_NOT_IMPLEMENTED,
                                                            operation);
                }
            }

            // Do the actual execution.
            if (LOG.isDebugEnabled())
            {
                LOG.debug("Executing method " + actualMethod.getClass().getSimpleName() + " (" +
                          actualMethod.getClass().getName() + ")");
            }
            actualMethod.execute();
        }
        catch (Throwable tException)
        {
            String sMessage = tException.getLocalizedMessage();
            LOG.error(tException, GeneralMessages.TRANSACTION_ERROR, sMessage);

            ServerLocalizableException sle = null;

            if (!(tException instanceof ServerLocalizableException))
            {
                sle = new GenericLDAPConnectorException(tException,
                                                        GenLDAPExceptionMessages.GLE_ERROR_EXECUTING_REQUEST_0,
                                                        ExceptionUtil.getSimpleErrorTrace(tException,
                                                                                          true));
            }
            else
            {
                sle = (ServerLocalizableException) tException;
            }

            // Create the proper SOAP fault.
            sle.setPreferredLocale(ServerLocalizableException.PreferredLocale.SOAP_LOCALE);
            sle.toSOAPFault(bbResponse);

            if (bbRequest.isAsync())
            {
                bbRequest.continueTransaction();
                bReturn = false;
            }
        }

        return bReturn;
    }
}
