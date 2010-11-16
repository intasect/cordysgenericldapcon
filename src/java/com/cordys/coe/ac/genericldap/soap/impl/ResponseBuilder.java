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

import com.eibus.util.logger.CordysLogger;

import com.eibus.xml.nom.Node;

import com.novell.ldap.LDAPAttribute;
import com.novell.ldap.LDAPAttributeSchema;
import com.novell.ldap.LDAPEntry;
import com.novell.ldap.LDAPSchema;
import com.novell.ldap.util.Base64;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class builds up the response XML for the current request.
 *
 * @author  pgussow
 */
public class ResponseBuilder
{
    /**
     * Holds all know binary data types.
     */
    private static ArrayList<Integer> s_binaryTypes = new ArrayList<Integer>();

    static
    {
        // See http://www.alvestrand.no/objectid/1.3.6.1.4.1.1466.115.121.1.html for more
        // information.
        s_binaryTypes.add(4); // Audio
        s_binaryTypes.add(5); // Binary
        s_binaryTypes.add(8); // Certificate
        s_binaryTypes.add(9); // Certificate List
        s_binaryTypes.add(10); // Certificate key pair
        s_binaryTypes.add(28); // JPeg image
        s_binaryTypes.add(40); // Octect String
    }

    /**
     * Holds the logger to use.
     */
    private static final CordysLogger LOG = CordysLogger.getCordysLogger(ResponseBuilder.class);
    /**
     * Holds whether or not all non-mentioned attributes should be included.
     */
    private boolean m_defaultInclude;
    /**
     * Holds the entries to return.
     */
    private LDAPEntry[] m_entries;
    /**
     * Holds the list of attributes that should be excluded.
     */
    private Map<String, IAttributeDefinition> m_excludeAttributes;
    /**
     * Holds the list of attributes that should be included.
     */
    private Map<String, IAttributeDefinition> m_includeAttributes;
    /**
     * Holds the response XML.
     */
    private int m_responseXML;
    /**
     * Holds teh LDAP schema.
     */
    private LDAPSchema m_schema;
    /**
     * Holds the pattern for getting the last digits.
     */
    private final Pattern m_typePattern = Pattern.compile("^1\\.3\\.6\\.1\\.4\\.1\\.1466\\.115\\.121\\.1\\.([\\d]+)$");

    /**
     * Creates a new ResponseBuilder object.
     *
     * @param  responseXML        Holds the response XML.
     * @param  entries            Holds the entries to return.
     * @param  schema             Holds the schema to return.
     * @param  includeAttributes  Holds the list of attributes that should be included.
     * @param  excludeAttributes  Holds the list of attributes that should be excluded.
     * @param  defaultInclude     Holds whether or not all non-mentioned attributes should be
     *                            included.
     */
    public ResponseBuilder(int responseXML, LDAPEntry[] entries, LDAPSchema schema,
                           Map<String, IAttributeDefinition> includeAttributes,
                           Map<String, IAttributeDefinition> excludeAttributes,
                           boolean defaultInclude)
    {
        m_responseXML = responseXML;
        m_entries = entries;
        m_schema = schema;
        m_includeAttributes = includeAttributes;
        m_excludeAttributes = excludeAttributes;
        m_defaultInclude = defaultInclude;
    }

    /**
     * This method builds up the actual response.
     */
    public void buildResponse()
    {
        if (LOG.isDebugEnabled())
        {
            LOG.debug("Building response for " + m_entries.length + " objects.");
        }

        for (LDAPEntry entry : m_entries)
        {
            int tupleXML = Node.createElementWithParentNS("tuple", null, m_responseXML);
            int oldXML = Node.createElementWithParentNS("old", null, tupleXML);

            int entryXML = Node.createElementWithParentNS("entry", null, oldXML);
            Node.setAttribute(entryXML, "dn", entry.getDN());

            Iterator<?> attributes = entry.getAttributeSet().iterator();

            while (attributes.hasNext())
            {
                LDAPAttribute attribute = (LDAPAttribute) attributes.next();

                String attrName = attribute.getBaseName();

                if (shouldInclude(attrName))
                {
                    // Determine whether it's a binary or string attribute.
                    boolean isBinary = isBinary(attribute, m_schema);

                    // Create the root tag for the attribute.
                    int xmlAttribute = Node.createElementWithParentNS(attrName, null, entryXML);

                    if (isBinary)
                    {
                        Enumeration<?> buffers = attribute.getByteValues();

                        while (buffers.hasMoreElements())
                        {
                            Node.createCDataElementWithParentNS("binary",
                                                                Base64.encode((byte[])
                                                                              buffers
                                                                              .nextElement()),
                                                                xmlAttribute);
                        }
                    }
                    else
                    {
                        Enumeration<?> strings = attribute.getStringValues();

                        while (strings.hasMoreElements())
                        {
                            String value = strings.nextElement().toString();
                            Node.createElementWithParentNS("string", value, xmlAttribute);
                        }
                    }
                }
            }
        }
    }

    /**
     * This method returns whether or not the attribute should be included in the response.
     *
     * @param   attrName  The name of the attribute.
     *
     * @return  true if the attribute should be included. Otherwise false.
     */
    protected boolean shouldInclude(String attrName)
    {
        boolean returnValue = m_defaultInclude;

        if (m_defaultInclude)
        {
            if (m_excludeAttributes.containsKey(attrName))
            {
                returnValue = false;
            }
            else
            {
                returnValue = true;
            }
        }
        else
        {
            if (m_includeAttributes.containsKey(attrName))
            {
                returnValue = true;
            }
            else
            {
                returnValue = false;
            }
        }

        return returnValue;
    }

    /**
     * This method returns true if the data for this attribute is binary. For now this check is done
     * NOT based on the actual schema, but based on if the subtypes indicate it's binary.
     *
     * @param   attribute  The LDAP attribute.
     * @param   schema     The actual LDAP schema.
     *
     * @return  true if the data is binary. Otherwise false.
     */
    private boolean isBinary(LDAPAttribute attribute, LDAPSchema schema)
    {
        if (attribute == null)
        {
            return false;
        }

        boolean returnValue = false;
        List<String> subtypes = Arrays.asList(attribute.getSubtypes());

        if (subtypes.contains("binary"))
        {
            returnValue = true;
        }

        if (!returnValue)
        {
            // Last resort: check the defined attributes.
            if ((m_excludeAttributes != null) &&
                    m_excludeAttributes.containsKey(attribute.getBaseName()) &&
                    (m_excludeAttributes.get(attribute.getBaseName()).getType() ==
                         EAttributeType.BINARY))
            {
                returnValue = true;
            }

            if ((returnValue == false) && (m_includeAttributes != null) &&
                    m_includeAttributes.containsKey(attribute.getBaseName()) &&
                    (m_includeAttributes.get(attribute.getBaseName()).getType() ==
                         EAttributeType.BINARY))
            {
                returnValue = true;
            }
        }

        // Last resort: look at the schema and figure it out. Note: For the sake of performance this
        // schould really be moved to when the connection is made. There we should read the
        // attribute's schemas and decide the type for each attribute.
        if ((schema != null) && (returnValue == false))
        {
            LDAPAttributeSchema attrSchema = schema.getAttributeSchema(attribute.getBaseName());

            if (attrSchema != null)
            {
                String syntax = attrSchema.getSyntaxString();

                if (syntax != null)
                {
                    if (LOG.isDebugEnabled())
                    {
                        LOG.debug("Syntax for attribute " + attribute.getBaseName() + " is " +
                                  syntax);
                    }

                    Matcher m = m_typePattern.matcher(syntax);

                    if (m.matches())
                    {
                        int dataType = Integer.parseInt(m.group(1));

                        returnValue = s_binaryTypes.contains(dataType);
                    }
                }
                else
                {
                    if (LOG.isDebugEnabled())
                    {
                        LOG.debug("Attribute type could not be determined. Defaulting to binary.");
                    }
                }
            }
        }

        if (LOG.isDebugEnabled())
        {
            LOG.debug("Attribute " + attribute.getBaseName() + " is " +
                      (returnValue ? "" : "NOT ") + "binary.");
        }

        return returnValue;
    }
}
