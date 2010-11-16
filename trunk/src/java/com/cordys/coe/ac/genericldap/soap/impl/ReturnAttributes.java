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

import com.eibus.util.logger.CordysLogger;

import com.eibus.xml.nom.Node;
import com.eibus.xml.xpath.XPathMetaInfo;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This class contains the definition of the returning structure. Some attributes might be defined
 * here, others might come from the request.
 *
 * @author  pgussow
 */
public class ReturnAttributes
{
    /**
     * Holds the logger to use.
     */
    private static final CordysLogger LOG = CordysLogger.getCordysLogger(ReturnAttributes.class);
    /**
     * Holds whether or not all attributes are default included in the response.
     */
    private boolean m_defaultInclude;
    /**
     * Holds all the defined attributes for the exclusion part.
     */
    private Map<String, IAttributeDefinition> m_excludeAttributes = new LinkedHashMap<String, IAttributeDefinition>();
    /**
     * Holds all the defined attributes for the inclusion part.
     */
    private Map<String, IAttributeDefinition> m_includeAttributes = new LinkedHashMap<String, IAttributeDefinition>();
    /**
     * Holds the XPath for the exclude attributes.
     */
    private String m_xpathExclude = null;
    /**
     * Holds the XPath for the include attributes.
     */
    private String m_xpathInclude = null;

    /**
     * Creates a new ReturnAttributes object.
     *
     * @param   returnXML  The XML containing the definition.
     * @param   xmi        The XPathMetaInfo object containing the impl-mapping.
     *
     * @throws  GenericLDAPConnectorException  In case of any exceptions.
     */
    public ReturnAttributes(int returnXML, XPathMetaInfo xmi)
                     throws GenericLDAPConnectorException
    {
        // Get whether or not all attributes are either default included or default excluded.
        String defaultInclude = Node.getAttribute(returnXML, "default", "include");

        if (!"exclude".equalsIgnoreCase(defaultInclude))
        {
            m_defaultInclude = true;
        }

        // Get the include attributes.
        int includeXML = XPathHelper.selectSingleNode(returnXML, "impl:include", xmi);

        if (includeXML != 0)
        {
            m_xpathInclude = Node.getAttribute(includeXML, "xpath", null);

            if ((m_xpathInclude != null) && (m_xpathInclude.length() == 0))
            {
                throw new GenericLDAPConnectorException(GenLDAPExceptionMessages.GLE_THE_INCLUDE_TAG_CANNOT_HAVE_AN_EMPTY_XPATH_ATTRIBUTE);
            }

            // Now see if there are fixed attribute definitions.
            int[] attributes = XPathHelper.selectNodes(includeXML, "impl:attribute", xmi);

            if ((attributes != null) && (attributes.length > 0))
            {
                // Parse the predefined attributes.
                for (int attrDef : attributes)
                {
                    AttributeDefinition ad = new AttributeDefinition(attrDef);
                    m_includeAttributes.put(ad.getName(), ad);
                }
            }
        }

        // Now parse the exclude list.
        int excludeXML = XPathHelper.selectSingleNode(returnXML, "impl:exclude", xmi);

        if (excludeXML != 0)
        {
            m_xpathExclude = Node.getAttribute(excludeXML, "xpath", null);

            if ((m_xpathExclude != null) && (m_xpathExclude.length() == 0))
            {
                throw new GenericLDAPConnectorException(GenLDAPExceptionMessages.GLE_THE_EXCLUDE_TAG_CANNOT_HAVE_AN_EMPTY_XPATH_ATTRIBUTE);
            }

            // Now see if there are fixed attribute definitions.
            int[] attributes = XPathHelper.selectNodes(excludeXML, "impl:attribute", xmi);

            if ((attributes != null) && (attributes.length > 0))
            {
                // Parse the predefined attributes.
                for (int attrDef : attributes)
                {
                    AttributeDefinition ad = new AttributeDefinition(attrDef);
                    m_excludeAttributes.put(ad.getName(), ad);
                }
            }
        }
    }

    /**
     * This method gets whether or not all attributes are default included in the response.
     *
     * @return  Whether or not all attributes are default included in the response.
     */
    public boolean getDefaultInclude()
    {
        return m_defaultInclude;
    }

    /**
     * This method returns the list of attributes that should be excluded. If no attributes are
     * defined it will return an empty map. When an empty map is returned it means that ALL
     * attributes must be INCLUDED in the response unless the default is 'exclude'.
     *
     * @param   request  The request as received.
     * @param   xmi      The XPathMetaInfo object.
     *
     * @return  The list of attributes that should be included.
     *
     * @throws  GenericLDAPConnectorException  In case of any exceptions.
     */
    public Map<String, IAttributeDefinition> getExcludeAttributes(int request,
                                                                  XPathMetaInfo xmi)
                                                           throws GenericLDAPConnectorException
    {
        return getAttributes(request, xmi, m_excludeAttributes, m_xpathExclude);
    }

    /**
     * This method returns the list of attributes that should be included. If no attributes are
     * defined it will return an empty map. When an empty map is returned it means that ALL
     * attributes must be included in the response.
     *
     * @param   request  The request as received.
     * @param   xmi      The XPathMetaInfo object.
     *
     * @return  The list of attributes that should be included.
     *
     * @throws  GenericLDAPConnectorException  In case of any exceptions.
     */
    public Map<String, IAttributeDefinition> getIncludeAttributes(int request,
                                                                  XPathMetaInfo xmi)
                                                           throws GenericLDAPConnectorException
    {
        return getAttributes(request, xmi, m_includeAttributes, m_xpathInclude);
    }

    /**
     * This method sets wether or not all attributes are default included in the response.
     *
     * @param  defaultInclude  Whether or not all attributes are default included in the response.
     */
    public void setDefaultInclude(boolean defaultInclude)
    {
        m_defaultInclude = defaultInclude;
    }

    /**
     * This method returns the list of attributes that should be excluded. If no attributes are
     * defined it will return an empty map. When an empty map is returned it means that ALL
     * attributes must be INCLUDED in the response unless the default is 'exclude'.
     *
     * @param   request           The request as received.
     * @param   xmi               The XPathMetaInfo object.
     * @param   sourceAttributes  The source attributes.
     * @param   xpath             The Xpath to evaluate on the request.
     *
     * @return  The list of attributes that should be included.
     *
     * @throws  GenericLDAPConnectorException  In case of any exceptions.
     */
    private Map<String, IAttributeDefinition> getAttributes(int request, XPathMetaInfo xmi,
                                                            Map<String, IAttributeDefinition> sourceAttributes,
                                                            String xpath)
                                                     throws GenericLDAPConnectorException
    {
        Map<String, IAttributeDefinition> returnMap = new LinkedHashMap<String, IAttributeDefinition>();

        for (IAttributeDefinition ad : sourceAttributes.values())
        {
            returnMap.put(ad.getName(), ad);
        }

        if (xpath != null)
        {
            // Search the request.
            int[] attributes = XPathHelper.selectNodes(request, xpath, xmi);

            if ((attributes != null) && (attributes.length > 0))
            {
                // Parse the predefined attributes.
                for (int attrDef : attributes)
                {
                    AttributeDefinition ad = new AttributeDefinition(attrDef);
                    returnMap.put(ad.getName(), ad);
                }
            }
        }

        return returnMap;
    }

    /**
     * This class wraps the attribute definitions.
     *
     * @author  pgussow
     */
    public class AttributeDefinition
        implements IAttributeDefinition
    {
        /**
         * Holds the name of the attribute.
         */
        private String m_name;

        /**
         * Holds the type for the parameter.
         */
        private EAttributeType m_type;

        /**
         * Constructor.
         *
         * @param   attrDef  The XML definition of the attribute.
         *
         * @throws  GenericLDAPConnectorException  In case of any exceptions.
         */
        public AttributeDefinition(int attrDef)
                            throws GenericLDAPConnectorException
        {
            m_name = Node.getDataWithDefault(attrDef, "");

            if (m_name.length() == 0)
            {
                throw new GenericLDAPConnectorException(GenLDAPExceptionMessages.GLE_MISSING_ATTRIBUTE_NAME);
            }

            String type = Node.getAttribute(attrDef, "type", EAttributeType.STRING.name());

            try
            {
                m_type = EAttributeType.valueOf(type);
            }
            catch (Exception e)
            {
                if (LOG.isDebugEnabled())
                {
                    LOG.debug("Invalid attribute type: " + type);
                }
                m_type = EAttributeType.STRING;
            }
        }

        /**
         * @see  com.cordys.coe.ac.genericldap.soap.impl.IAttributeDefinition#getName()
         */
        public String getName()
        {
            return m_name;
        }

        /**
         * @see  com.cordys.coe.ac.genericldap.soap.impl.IAttributeDefinition#getType()
         */
        public EAttributeType getType()
        {
            return m_type;
        }

        /**
         * @see  com.cordys.coe.ac.genericldap.soap.impl.IAttributeDefinition#setName(java.lang.String)
         */
        public void setName(String name)
        {
            m_name = name;
        }

        /**
         * @see  com.cordys.coe.ac.genericldap.soap.impl.IAttributeDefinition#setType(com.cordys.coe.ac.genericldap.soap.impl.EAttributeType)
         */
        public void setType(EAttributeType type)
        {
            m_type = type;
        }
    }
}
