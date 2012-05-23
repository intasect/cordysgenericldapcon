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
package com.cordys.coe.ac.genericldap.soap.compatibility;

import com.cordys.coe.ac.genericldap.GenericLDAPConstraints;
import com.cordys.coe.ac.genericldap.config.IGenLDAPConfiguration;
import com.cordys.coe.ac.genericldap.exception.GenericLDAPConnectorException;
import com.cordys.coe.ac.genericldap.localization.GenLDAPExceptionMessages;
import com.cordys.coe.ac.genericldap.soap.BaseMethod;

import com.eibus.directory.soap.DN;

import com.eibus.soap.BodyBlock;

import com.eibus.util.logger.CordysLogger;

import com.eibus.xml.nom.Document;
import com.eibus.xml.nom.Find;
import com.eibus.xml.nom.Node;

import com.novell.ldap.LDAPAttribute;
import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPEntry;
import com.novell.ldap.LDAPSearchConstraints;

import java.util.Enumeration;
import java.util.Iterator;

/**
 * Handle a read request, use the LDAP API and serach for entries that match. Return each entry as a
 * tupple old/new structure - 2 options : implementation name is SearchLDAP: all search vriteria are
 * provided in the request otherwise : search criteria is defined in implementation, but variables
 * must be substituted with valuse from the request parameters.
 *
 * @author  pgussow
 */
public class ReadLDAP extends BaseMethod
{
    /**
     * Holds the logger to use.
     */
    private static final CordysLogger LOG = CordysLogger.getCordysLogger(ReadLDAP.class);
    /**
     * DOCUMENTME.
     */
    private String[] mAttributeNames = new String[] {};
    /**
     * DOCUMENTME.
     */
    private LDAPSearchConstraints mConstraints;
    /**
     * DOCUMENTME.
     */
    private String[] mDn = new String[] { "" };
    /**
     * DOCUMENTME.
     */
    private boolean mExcludeValues = false;
    /**
     * DOCUMENTME.
     */
    private String mFilter;
    /**
     * DOCUMENTME.
     */
    private int mScope = 0;
    /**
     * DOCUMENTME.
     */
    private String mSort;

    /**
     * Constructor.
     *
     * @param  request        The incoming request.
     * @param  response       The outgoing response.
     * @param  configuration  The configuration.
     */
    public ReadLDAP(BodyBlock request, BodyBlock response, IGenLDAPConfiguration configuration)
    {
        super(request, response, configuration);

        mConstraints = new LDAPSearchConstraints();
    }

    /**
     * Handle a read request, use the LDAP API and serach for entries that match. Return each entry
     * as a tupple old/new structure - 2 options : implementation name is SearchLDAP: all search
     * vriteria are provided in the request otherwise : search criteria is defined in
     * implementation, but variables must be substituted with valuse from the request parameters.
     *
     * @throws  GenericLDAPConnectorException  DOCUMENTME
     *
     * @see     com.cordys.coe.ac.genericldap.soap.BaseMethod#execute()
     */
    @Override public void execute()
                           throws GenericLDAPConnectorException
    {
        Document document = Node.getDocument(getResponseXML());

        // most of the following code comes from com.eibus.applicationconnector.ldap.SearchLDAP.java
        // first check : if request is SearchLDAP:
        int requestImplementation = Node.getFirstElement(getRequest().getMethodDefinition()
                                                         .getImplementation());

        int requestInterface = getRequestXML();

        if (Node.getLocalName(requestInterface).equals("SearchLDAP"))
        {
            // take filter,dn,sort, scope format from request
            getSearchCriteria(requestInterface);
        }
        else
        {
            getSearchCriteria(requestImplementation);

            substituteParameters(requestInterface);
        }

        substituteSearchRoot();

        // add Searchroot to each DN
        if (getConfiguration().getSearchRoot() != null)
        {
            for (int i = 0; i < mDn.length; i++)
            {
                if (mDn[i].length() > 0)
                {
                    mDn[i] = mDn[i] + "," + getConfiguration().getSearchRoot();
                }
                else
                {
                    mDn[i] = getConfiguration().getSearchRoot();
                }

                // do basic format checks : com.eibus.dircetory.soap.DN.isDN(dn);
                if (!DN.isDN(mDn[i]))
                {
                    // invalid search node
                    throw new GenericLDAPConnectorException(GenLDAPExceptionMessages.GLE_METHOD_0_DOES_NOT_HAVE_A_PROPER_SEARCHROOT_FORMAT_1,
                                                            Node.getLocalName(requestInterface),
                                                            mDn[i]);
                }
            }
        }

        // now loop through all dn's
        for (int i = 0; i < mDn.length; i++)
        {
            if (LOG.isDebugEnabled())
            {
                StringBuilder sbTemp = new StringBuilder(2048);
                sbTemp.append("Search dn[").append(i).append("]: ").append(mDn[i]).append("\n");
                sbTemp.append("Search filter: ").append(mFilter).append("\n");
                sbTemp.append("Search sort: ").append(mSort).append("\n");

                String att = "";

                for (int j = 0; j < mAttributeNames.length; j++)
                {
                    att += (mAttributeNames[j] + ",");
                }

                sbTemp.append("Search attributes: ").append(att).append("\n");
                sbTemp.append("Search scope: ").append(mScope).append("\n");
                sbTemp.append("Search excludeValues: ").append(mExcludeValues);

                LOG.debug(sbTemp.toString());
            }

            // search directory
            // mConstraints = new LDAPSearchConstraints();
            mConstraints.setMaxResults(20000); // todo : make dynamic

            com.novell.ldap.LDAPSearchResults results = getConfiguration().getConnectionManager()
                                                                          .search(mDn[i], mScope,
                                                                                  mFilter,
                                                                                  mAttributeNames,
                                                                                  mExcludeValues,
                                                                                  mConstraints);
            // create output nodes in the tuple old format
            int tupleParent = getResponseXML();

            if (LOG.isDebugEnabled())
            {
                LOG.debug("Debug: results.getCount() = " + results.getCount());
            }

            while (results.hasMore())
            {
                try
                {
                    LDAPEntry entry = results.next();

                    if (entry.getDN() != null)
                    {
                        int tuple = document.createElement("tuple", tupleParent);
                        int old = document.createElement("old", tuple);
                        int entryNode = Node.createElement("entry", old);
                        Node.setAttribute(entryNode, "dn", entry.getDN());

                        Iterator<?> iAttributes = entry.getAttributeSet().iterator();

                        while (iAttributes.hasNext())
                        {
                            LDAPAttribute attribute = (LDAPAttribute) iAttributes.next();
                            Enumeration<?> strings = attribute.getStringValues();

                            while (strings.hasMoreElements())
                            {
                                Node.createTextElement(attribute.getName(),
                                                       strings.nextElement().toString(), entryNode);
                            }
                        }

                        if (LOG.isDebugEnabled())
                        {
                            LOG.debug("Read entry: " + Node.writeToString(entryNode, true));
                        }
                    }
                }
                catch (Exception ignore)
                {
                    // we catch the exception while we read the information, no way to recover
                    // as we read more info : ignore exception and continue with next nodes TODO
                    // : decide how often this happens and if we get away with ignoring the
                    // exception
                    if (LOG.isDebugEnabled())
                    {
                        LOG.debug("Ignoring exception while reading entries.", ignore);
                    }
                }
            }
        }
    }

    /**
     * Get the ldap serach criteria from the request or implementation look for :
     *
     * <pre>
       <dn>
       <sort>
       <filter>
       <scope>
       <returnvalues>
       <attributes><attribute> list
     * </pre>
     *
     * <p>All values are stored in private variables with simular names</p>
     *
     * @param  DefinitionNode  Node that contains the definition
     */
    private void getSearchCriteria(int DefinitionNode)
    {
        // pickup definitions for dn, sort, filter, exclusdevalues and attributes
        int[] dns = Find.match(DefinitionNode, "?<dn>");

        if (dns.length > 0)
        {
            mDn = new String[dns.length];

            for (int j = 0; j < dns.length; j++)
            {
                mDn[j] = Node.getData(dns[j]);
            }
        }

        mDn[0] = Node.getDataElement(DefinitionNode, "dn", "");

        mSort = Node.getDataElement(DefinitionNode, "sort", "");
        mFilter = Node.getDataElement(DefinitionNode, "filter", "");

        try
        {
            mScope = Integer.parseInt(Node.getDataElement(DefinitionNode, "scope", "0"));
        }
        catch (Exception ignore)
        {
            mScope = LDAPConnection.SCOPE_ONE;
        }

        mExcludeValues = Node.getDataElement(DefinitionNode, "returnvalues", "true")
                             .equalsIgnoreCase("false");

        int[] attributes = Find.match(DefinitionNode, "?<attribute>");

        if (attributes.length > 0)
        {
            mAttributeNames = new String[attributes.length];

            for (int j = 0; j < attributes.length; j++)
            {
                mAttributeNames[j] = Node.getData(attributes[j]);
            }
        }

        int[] constraints = Find.match(DefinitionNode, "?<constraint>");

        if (constraints.length > 0)
        {
            for (int j = 0; j < constraints.length; j++)
            {
                mConstraints = GenericLDAPConstraints.addConstraints(mConstraints,
                                                                     Node.getData(constraints[j]));
            }
        }
        return;
    }

    /**
     * Substitute the parameters from the request in the template See if there are places in the
     * SearchCriteria that need substitution (marked as :variableName) with a value from the
     * parameters (this makes it dynamic) replace with actual values from the parameters.
     *
     * @param  requestInterface  the xml with parametrs
     */
    private void substituteParameters(int requestInterface)
    {
        // and substitute filter,dn,sort, scope with parameters
        int parameter = Node.getFirstChild(requestInterface);

        while (parameter > 0)
        {
            String name = Node.getLocalName(parameter);

            // see if we can substitute the parameter in the dn
            int positionDN = mDn[0].indexOf(":" + name);

            if (positionDN >= 0)
            {
                // substitute in dn
                mDn[0] = mDn[0].substring(0, positionDN) + Node.getData(parameter) +
                         mDn[0].substring((positionDN + name.length()) + 1);
            }

            // see if we can substitute the parameter in the filter
            int positionFilter = mFilter.indexOf(":" + name);

            if (positionFilter >= 0)
            {
                // substitute in filter
                mFilter = mFilter.substring(0, positionFilter) + Node.getData(parameter) +
                          mFilter.substring((positionFilter + name.length()) + 1);
            }

            // see if we can substitute the parameter in the sort
            int positionSort = mSort.indexOf(":" + name);

            if (positionSort >= 0)
            {
                // substitute in sort
                mSort = mSort.substring(0, positionSort) + Node.getData(parameter) +
                        mSort.substring((positionSort + name.length()) + 1);
            }

            parameter = Node.getNextSibling(parameter); // next parameter
        }
    }

    /**
     * Replace the :Searchroot entries in filter and dn with the searchroot.
     */
    private void substituteSearchRoot()
    {
        String searchRoot = getConfiguration().getSearchRoot();

        if ((searchRoot != null) && (searchRoot.length() > 0))
        {
            // see if we can substitute the searchroot in the dn's
            for (int i = 0; i < mDn.length; i++)
            {
                int positionSRinDN = mDn[i].indexOf(":SearchRoot");

                if (positionSRinDN >= 0)
                {
                    // substitute in filter
                    mDn[i] = mDn[i].substring(0, positionSRinDN) + searchRoot +
                             mDn[i].substring(positionSRinDN + 11);
                }
            }

            // see if we can substitute the searchroot in the filter
            int positionSRinFilter = mFilter.indexOf(":SearchRoot");

            if (positionSRinFilter >= 0)
            {
                // substitute in filter
                mFilter = mFilter.substring(0, positionSRinFilter) + searchRoot +
                          mFilter.substring(positionSRinFilter + 11);
            }
        }
    }
}
