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
import com.cordys.coe.ac.genericldap.soap.BaseMethod;

import com.eibus.xml.xpath.XPathMetaInfo;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This class holds the base definition of an action.
 *
 * @author  pgussow
 */
public abstract class BaseImplementation
{
    /**
     * Holds the XPathMetaInfo to use.
     */
    protected XPathMetaInfo m_xmi = null;
    /**
     * Holds the action for this implementation.
     */
    private EDynamicAction m_action;
    /**
     * Holds the int pointing to the implementation XML. It is only used to be able to identityif
     * the method implementation has changed.
     */
    private int m_implementation;
    /**
     * Holds the request parameters defined for this implementation.
     */
    private Map<String, RequestParameter> m_parameters = new LinkedHashMap<String, RequestParameter>();

    /**
     * Creates a new BaseImplementation object.
     *
     * @param  action          Holds the action for this implementation
     * @param  implementation  Holds the implementation XML.
     */
    public BaseImplementation(EDynamicAction action, int implementation)
    {
        m_action = action;
        m_implementation = implementation;

        m_xmi = new XPathMetaInfo();
        m_xmi.addNamespaceBinding("impl", GenLDAPConstants.NS_METHODS_1_2_IMPL);
    }

    /**
     * This method will execute the implementation against the actual method.
     *
     * <p>Note: Be aware that this method can be called by multiple threads simultaniously, so NO
     * member variables should be used to store data.</p>
     *
     * @param   method  The actual method.
     *
     * @throws  GenericLDAPConnectorException  In case of any exceptions.
     */
    public abstract void handleRequest(BaseMethod method)
                                throws GenericLDAPConnectorException;

    /**
     * This method gets the action for this implementation.
     *
     * @return  The action for this implementation.
     */
    public EDynamicAction getAction()
    {
        return m_action;
    }

    /**
     * This method gets the implementation XML.
     *
     * @return  The implementation XML.
     */
    public int getImplementation()
    {
        return m_implementation;
    }

    /**
     * This method gets the parameter with the given name.
     *
     * @param   name  The name of the parameter.
     *
     * @return  The parameter with the given name.
     */
    public RequestParameter getParameter(String name)
    {
        return m_parameters.get(name);
    }

    /**
     * This method sets the action for this implementation.
     *
     * @param  action  The action for this implementation.
     */
    public void setAction(EDynamicAction action)
    {
        m_action = action;
    }

    /**
     * This method adds a new parameter defintion with the given definition.
     *
     * @param  requestParameter  The parameter to add.
     */
    protected void addRequestParameter(RequestParameter requestParameter)
    {
        if (requestParameter != null)
        {
            if (!m_parameters.containsKey(requestParameter.getName()))
            {
                m_parameters.put(requestParameter.getName(), requestParameter);
            }
        }
    }

    /**
     * This method returns all parameters that are defined.
     *
     * @return  The list of parameters.
     */
    protected Map<String, RequestParameter> getParameters()
    {
        return new LinkedHashMap<String, RequestParameter>(m_parameters);
    }
}
