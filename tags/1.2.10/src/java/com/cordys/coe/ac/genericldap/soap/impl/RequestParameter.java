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

import com.cordys.coe.ac.genericldap.exception.GenericLDAPConnectorException;
import com.cordys.coe.ac.genericldap.localization.GenLDAPExceptionMessages;
import com.cordys.coe.util.xml.nom.XPathHelper;

import com.eibus.xml.nom.Node;
import com.eibus.xml.xpath.XPathMetaInfo;

/**
 * This call holds the parameter defintion details.
 *
 * @author  pgussow
 */
class RequestParameter
{
    /**
     * Holds the default value to use if the XPath returns no value.
     */
    private String m_defaultValue = null;
    /**
     * Holds the fixed value that should be used.
     */
    private String m_fixedValue = null;

    /**
     * Holds whether or not the parameter is mandatory.
     */
    private boolean m_mandatory;
    /**
     * Holds whether or not the XPath returns multiple values.
     */
    private boolean m_multiOcc = false;
    /**
     * Holds the name for the parameter.
     */
    private String m_name;
    /**
     * Holds the XPath for this parameter.
     */
    private String m_xpath;

    /**
     * Creates a new RequestParameter object.
     *
     * @param  name      The name of the parameter.
     * @param  xpath     The XPath to execute.
     * @param  multiOcc  Whether or not this XPath returns multiple values.
     */
    public RequestParameter(String name, String xpath, boolean multiOcc)
    {
        this(name, xpath, multiOcc, null);
    }

    /**
     * Creates a new RequestParameter object.
     *
     * @param  name          The name of the parameter.
     * @param  xpath         The XPath to execute.
     * @param  multiOcc      Whether or not this XPath returns multiple values.
     * @param  defaultValue  The default value to use if the XPath returns no value.
     */
    public RequestParameter(String name, String xpath, boolean multiOcc, String defaultValue)
    {
        m_name = name;
        m_xpath = xpath;
        m_multiOcc = multiOcc;
        m_defaultValue = defaultValue;
    }

    /**
     * Internal constructor. Used in the getInstance method.
     */
    private RequestParameter()
    {
    }

    /**
     * This method parses the information from the parameter definition.
     *
     * @param   parameterXML  The parameter XML.
     *
     * @return  The wrapper for the definition.
     *
     * @throws  GenericLDAPConnectorException  In case of any exceptions.
     */
    public static RequestParameter getInstance(int parameterXML)
                                        throws GenericLDAPConnectorException
    {
        if (parameterXML == 0)
        {
            throw new GenericLDAPConnectorException(GenLDAPExceptionMessages.GLE_THE_PARAMETER_XML_IS_NOT_DEFINED);
        }

        RequestParameter returnValue = new RequestParameter();

        // Get the name based on the local name.
        returnValue.setName(Node.getLocalName(parameterXML));

        // Get the XPath if defined. If no XPath is defined a fixed value is assumed.
        String xpath = Node.getAttribute(parameterXML, "xpath", null);

        if (xpath != null)
        {
            returnValue.setXPath(xpath);
        }
        else
        {
            // It's a fixed value.
            returnValue.setFixedValue(Node.getDataWithDefault(parameterXML, ""));
        }

        // Check if a default value is set.
        String defaultValue = Node.getAttribute(parameterXML, "default", null);

        if (defaultValue != null)
        {
            returnValue.setDefaultValue(defaultValue);
        }

        // Check the multi occ
        String multiOcc = Node.getAttribute(parameterXML, "multiOcc", "");

        if ("true".equalsIgnoreCase(multiOcc))
        {
            returnValue.setMultiOcc(true);
        }
        else
        {
            returnValue.setMultiOcc(false);
        }

        // Check  the mandatory
        String mandatory = Node.getAttribute(parameterXML, "mandatory", "");

        if ("true".equalsIgnoreCase(mandatory))
        {
            returnValue.setMandatory(true);
        }
        else
        {
            returnValue.setMandatory(false);
        }

        // Now we need to validate the definition (i.e. a non fixedValue field MUST have an XPath)
        if (!returnValue.hasFixedValue() &&
                ((returnValue.getXPath() == null) || (returnValue.getXPath().length() == 0)))
        {
            throw new GenericLDAPConnectorException(GenLDAPExceptionMessages.GLE_PARAMETER_0_MUST_HAVE_AN_XPATH_DEFINED,
                                                    returnValue.getName());
        }

        if ((returnValue.getXPath() != null) && returnValue.hasFixedValue())
        {
            throw new GenericLDAPConnectorException(GenLDAPExceptionMessages.GLE_PARAMETER_0_MUST_HAVE_EITHER_A_FIXEDVALUE_OR_AN_XPATH_BUT_NOT_BOTH,
                                                    returnValue.getName());
        }

        return returnValue;
    }

    /**
     * This method returns the boolean value for the current parameter. If the fixed value is
     * defined it will always return that value. If an XPath is defined ity will be executed on the
     * request XML. If not found and the parameter is defined as mandatory it will throw an
     * exception. If the parameter is optional it will return the default value.
     *
     * @param   requestXML  The XML to execute the XPath on.
     * @param   xmi         The namespace mapping object. The prefix ns MUST be mapped to the XML's
     *                      namespace.
     *
     * @return  The value for this parameter.
     *
     * @throws  GenericLDAPConnectorException  In case of any exceptions.
     */
    public boolean getBooleanValue(int requestXML, XPathMetaInfo xmi)
                            throws GenericLDAPConnectorException
    {
        boolean returnValue = false;

        String temp = getStringValue(requestXML, xmi);

        if ("true".equalsIgnoreCase(temp))
        {
            returnValue = true;
        }

        return returnValue;
    }

    /**
     * This method gets the default value to use if the XPath returns no value.
     *
     * @return  The default value to use if the XPath returns no value.
     */
    public String getDefaultValue()
    {
        return m_defaultValue;
    }

    /**
     * This method gets the fixed value that should be used.
     *
     * @return  The fixed value that should be used.
     */
    public String getFixedValue()
    {
        return m_fixedValue;
    }

    /**
     * This method returns the int value for the current parameter. If the fixed value is defined it
     * will always return that value. If an XPath is defined ity will be executed on the request
     * XML. If not found and the parameter is defined as mandatory it will throw an exception. If
     * the parameter is optional it will return the default value.
     *
     * @param   requestXML  The XML to execute the XPath on.
     * @param   xmi         The namespace mapping object. The prefix ns MUST be mapped to the XML's
     *                      namespace.
     *
     * @return  The value for this parameter.
     *
     * @throws  GenericLDAPConnectorException  In case of any exceptions.
     */
    public int getIntValue(int requestXML, XPathMetaInfo xmi)
                    throws GenericLDAPConnectorException
    {
        int returnValue = 0;

        String value = getStringValue(requestXML, xmi);

        if ((value != null) && (value.length() > 0))
        {
            returnValue = Integer.parseInt(value);
        }

        return returnValue;
    }

    /**
     * This method returns the int value for the parameter, but it allows to override the default as
     * defined in the method implementation XML.
     *
     * @param   requestXML            The XML to execute the XPath on.
     * @param   xmi                   The namespace mapping object. The prefix ns MUST be mapped to
     *                                the XML's namespace.
     * @param   defaultValueOverride  The new default value.
     *
     * @return  The value for this parameter.
     *
     * @throws  GenericLDAPConnectorException  In case of any exceptions.
     */
    public int getIntValue(int requestXML, XPathMetaInfo xmi, int defaultValueOverride)
                    throws GenericLDAPConnectorException
    {
        int returnValue = getIntValue(requestXML, xmi);

        if (returnValue == 0)
        {
            returnValue = defaultValueOverride;
        }

        return returnValue;
    }

    /**
     * This method gets whether or not the parameter is mandatory.
     *
     * @return  Whether or not the parameter is mandatory.
     */
    public boolean getMandatory()
    {
        return m_mandatory;
    }

    /**
     * This method gets whether or not the XPath returns multiple values.
     *
     * @return  Whether or not the XPath returns multiple values.
     */
    public boolean getMultiOcc()
    {
        return m_multiOcc;
    }

    /**
     * This method gets the name for the parameter.
     *
     * @return  The name for the parameter.
     */
    public String getName()
    {
        return m_name;
    }

    /**
     * This method returns the string value for the current parameter. If the fixed value is defined
     * it will always return that value. If an XPath is defined ity will be executed on the request
     * XML. If not found and the parameter is defined as mandatory it will throw an exception. If
     * the parameter is optional it will return the default value.
     *
     * @param   requestXML  The XML to execute the XPath on.
     * @param   xmi         The namespace mapping object. The prefix ns MUST be mapped to the XML's
     *                      namespace.
     *
     * @return  The value for this parameter.
     *
     * @throws  GenericLDAPConnectorException  In case of any exceptions.
     */
    public String getStringValue(int requestXML, XPathMetaInfo xmi)
                          throws GenericLDAPConnectorException
    {
        String returnValue = null;

        if (m_fixedValue != null)
        {
            returnValue = m_fixedValue;
        }
        else
        {
            returnValue = XPathHelper.getStringValue(requestXML, m_xpath, xmi, null);

            if (((returnValue == null) || (returnValue.trim().length() == 0)))
            {
                if (m_mandatory)
                {
                    throw new GenericLDAPConnectorException(GenLDAPExceptionMessages.GLE_MISSING_MANDATORY_PARAMETER,
                                                            getName(), getXPath());
                }

                returnValue = m_defaultValue;
            }
        }

        return returnValue;
    }

    /**
     * This method gets the XPath for this parameter.
     *
     * @return  The XPath for this parameter.
     */
    public String getXPath()
    {
        return m_xpath;
    }

    /**
     * This method returns whether or not this parameter has a fixed value defined.
     *
     * @return  Whether or not this parameter has a fixed value defined.
     */
    public boolean hasFixedValue()
    {
        return m_fixedValue != null;
    }

    /**
     * This method sets the default value to use if the XPath returns no value.
     *
     * @param  defaultValue  The default value to use if the XPath returns no value.
     */
    public void setDefaultValue(String defaultValue)
    {
        m_defaultValue = defaultValue;
    }

    /**
     * This method sets the fixed value that should be used.
     *
     * @param  fixedValue  The fixed value that should be used.
     */
    public void setFixedValue(String fixedValue)
    {
        m_fixedValue = fixedValue;
    }

    /**
     * This method sets wether or not the parameter is mandatory.
     *
     * @param  mandatory  Whether or not the parameter is mandatory.
     */
    public void setMandatory(boolean mandatory)
    {
        m_mandatory = mandatory;
    }

    /**
     * This method sets wether or not the XPath returns multiple values.
     *
     * @param  multiOcc  Whether or not the XPath returns multiple values.
     */
    public void setMultiOcc(boolean multiOcc)
    {
        m_multiOcc = multiOcc;
    }

    /**
     * This method sets the name for the parameter.
     *
     * @param  name  The name for the parameter.
     */
    public void setName(String name)
    {
        m_name = name;
    }

    /**
     * This method sets the XPath for this parameter.
     *
     * @param  xpath  The XPath for this parameter.
     */
    public void setXPath(String xpath)
    {
        m_xpath = xpath;
    }
}
