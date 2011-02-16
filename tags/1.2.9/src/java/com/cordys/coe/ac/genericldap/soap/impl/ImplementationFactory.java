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

import com.cordys.coe.ac.genericldap.EDynamicAction;
import com.cordys.coe.ac.genericldap.GenLDAPConstants;
import com.cordys.coe.ac.genericldap.exception.GenericLDAPConnectorException;
import com.cordys.coe.ac.genericldap.localization.GenLDAPExceptionMessages;
import com.cordys.coe.util.xml.nom.XPathHelper;

import com.eibus.soap.MethodDefinition;

import com.eibus.util.logger.CordysLogger;

import com.eibus.xml.xpath.XPathMetaInfo;

import java.lang.reflect.Constructor;

/**
 * This class creates the proper implementation objects based on the given Method Definition.
 *
 * @author  pgussow
 */
public class ImplementationFactory
{
    /**
     * Holds the logger to use.
     */
    private static final CordysLogger LOG = CordysLogger.getCordysLogger(ImplementationFactory.class);

    /**
     * This method creates the proper implementation based on the method definition.
     *
     * @param   md  The definition of the method.
     *
     * @return  The created implementation object.
     *
     * @throws  GenericLDAPConnectorException  In case of any exceptions.
     */
    public static BaseImplementation createImplementation(MethodDefinition md)
                                                   throws GenericLDAPConnectorException
    {
        BaseImplementation returnValue = null;

        // It's the new mode.
        String action = getActionFromImplementation(md);
        EDynamicAction daAction = EDynamicAction.valueOf(action.toUpperCase());

        Class<? extends BaseImplementation> cClass = daAction.getImplementationClass();

        if (LOG.isDebugEnabled())
        {
            LOG.debug("Instantiating class " + daAction.getImplementationClass());
        }

        try
        {
            Constructor<? extends BaseImplementation> cConstructor = cClass.getConstructor(int.class);

            returnValue = cConstructor.newInstance(md.getImplementation());
        }
        catch (Exception e)
        {
            throw new GenericLDAPConnectorException(e,
                                                    GenLDAPExceptionMessages.GLE_ERROR_CREATING_IMPLEMENTATION_CLASS);
        }

        return returnValue;
    }

    /**
     * This method gets the desired action from the method's implementation.
     *
     * @param   md  The definition of the request.
     *
     * @return  The action in the implementation.
     *
     * @throws  GenericLDAPConnectorException  In case the action was not found.
     */
    public static String getActionFromImplementation(MethodDefinition md)
                                              throws GenericLDAPConnectorException
    {
        String sAction;

        int iImplNode = md.getImplementation();

        XPathMetaInfo xmi = new XPathMetaInfo();
        xmi.addNamespaceBinding("ns", GenLDAPConstants.NS_METHODS_1_2_IMPL);

        sAction = XPathHelper.getStringValue(iImplNode, "./ns:*/@action", xmi, "");

        return sAction;
    }
}
