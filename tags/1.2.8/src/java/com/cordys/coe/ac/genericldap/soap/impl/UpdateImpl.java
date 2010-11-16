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
package com.cordys.coe.ac.genericldap.soap.impl;

import java.util.ArrayList;
import java.util.List;

import com.cordys.coe.ac.genericldap.EDynamicAction;
import com.cordys.coe.ac.genericldap.connection.IConnectionManager;
import com.cordys.coe.ac.genericldap.exception.GenericLDAPConnectorException;
import com.cordys.coe.ac.genericldap.localization.GenLDAPExceptionMessages;
import com.cordys.coe.ac.genericldap.localization.GeneralMessages;
import com.cordys.coe.ac.genericldap.soap.BaseMethod;
import com.cordys.coe.util.xml.nom.XPathHelper;
import com.eibus.util.logger.CordysLogger;
import com.eibus.xml.nom.Node;
import com.eibus.xml.xpath.XPathMetaInfo;
import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPException;

/**
 * This class holds the implementation for the Update method.
 * 
 * @author pgussow
 */
public class UpdateImpl extends BaseImplementation 
{
	/**
	 * Holds the logger to use.
	 */
	private static final CordysLogger LOG = CordysLogger.getCordysLogger(UpdateImpl.class);
	
	/**
	 * Constructor. Creates the method based on the current implementation.
	 *  
	 * @param implementation The actual implementation.
	 */
	public UpdateImpl(int implementation) 
	{
		super(EDynamicAction.UPDATE, implementation);
	}

	/**
	 * @see com.cordys.coe.ac.genericldap.soap.impl.BaseImplementation#handleRequest(com.cordys.coe.ac.genericldap.soap.BaseMethod)
	 */
	@Override
	public void handleRequest(BaseMethod method) throws GenericLDAPConnectorException 
	{
		IConnectionManager cm = method.getConfiguration().getConnectionManager();

        LDAPConnection connection = null;
		try
        {
            connection = cm.getConnection();

            //Prepare all individual update requests.
            List<LDAPUpdateWrapper> updateWrappers = prepare(method, connection);
            
            //If there are actual updates we do the commit.
            if (updateWrappers.size() > 0)
            {
                commit(updateWrappers, method, connection);
            }
        }
        finally
        {
            if (connection != null)
            {
                cm.releaseConnection(connection);
            }
        }
	}
	
	/**
     * This method prepares the tuples that need to be updated in LDAP.
     * 
	 * @param method      The method that is being executed. 
	 * @param connection  The actual LDAP connection that will be used.
     *
     * @return  The list of actual update requests that need to be executed.
     *
     * @throws  GenericLDAPConnectorException  In case of any exceptions.
     */
    private List<LDAPUpdateWrapper> prepare(BaseMethod method, LDAPConnection connection)
                     throws GenericLDAPConnectorException
    {
    	//Create the result object
    	List<LDAPUpdateWrapper> returnValues = new ArrayList<LDAPUpdateWrapper>();
    	
    	//Find all tuples
    	XPathMetaInfo xmi = method.getXPathMetaInfo();
        int[] tuples = XPathHelper.selectNodes(method.getRequestXML(), "ns:tuple", xmi);

        
        if (tuples.length > 0)
        {
            if (connection != null)
            {
            	boolean ok = true;
            	
                for (int i = 0; i < tuples.length; i++)
                {
                    LDAPUpdateWrapper luw = new LDAPUpdateWrapper(tuples[i]);

                    //If the prepare fails it throws an exception.
                    luw.prepare(connection, method);
                    
                    returnValues.add(luw);
                }
                
                // Append the all tuples to the response already.
                if (ok)
                {
                	Node.appendToChildren(tuples[0], tuples[tuples.length - 1], method.getResponseXML());
                }
            }
            else
            {
            	throw new GenericLDAPConnectorException(GenLDAPExceptionMessages.GLE_NO_ACTIVE_CONNECTION_TO_THE_LDAP);
            }
        }
        
        return returnValues;
    }
    
    /**
     * This method takes care of the actual commit of the planned actions to LDAP.
     * 
     * @param updateWrappers  The update wrappers that should be committed.
     * @param method          The SOAP method that is being executed.
     * @param connection      The actual LDAP connection to use.
     *
     * @throws  GenericLDAPConnectorException  In case of any exceptions.
     */
    private void commit(List<LDAPUpdateWrapper> updateWrappers, BaseMethod method, LDAPConnection connection)
                 throws GenericLDAPConnectorException
    {
        int count = 0;

        try
        {
        	//Commit each individual entry
            for (count = 0; count < updateWrappers.size(); count++)
            {
            	LDAPUpdateWrapper luw = updateWrappers.get(count);
                luw.commit(connection);
            }

            if (LOG.isDebugEnabled())
            {
                LOG.debug("Response from LDAP Service is:\n" +
                          Node.writeToString(Node.getRoot(method.getResponseXML()), true));
            }
        }
        catch (LDAPException e)
        {
            boolean rollbackSuccessfull = true;
            StringBuffer messages = new StringBuffer();

            //There was an exception, so we need to roll back all the changes done so far.
            for (int j = count - 1; j >= 0; j--)
            {
                try
                {
                    updateWrappers.get(j).rollback(connection);
                }
                catch (LDAPException err)
                {
                    if (LOG.isWarningEnabled())
                    {
                        LOG.warn(e, GeneralMessages.UPDATE_ROLLBACK_FAILED_FOR_TUPLE,
                                 Node.writeToString(updateWrappers.get(j).getTuple(), true));
                    }

                    // log it, but continue
                    rollbackSuccessfull = false;
                    messages.append("Tuple rollback failed: ");
                    messages.append(err.getMessage());
                    messages.append(" ");
                    messages.append(err.getLDAPErrorMessage());
                }
            }

            if (rollbackSuccessfull)
            {
                throw new GenericLDAPConnectorException(e, GenLDAPExceptionMessages.ERROR_UPDATING_LDAP);
            }
            
            //Throw exception that the rollback failed.
            throw new GenericLDAPConnectorException(e,
                                                    GenLDAPExceptionMessages.GLE_ERROR_DURING_UPDATE_BUT_ROLLBACK_FAILED_REASON_,
                                                    messages.toString());
        }
    }

}
