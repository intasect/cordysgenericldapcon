/*
 * Copyright 2007 Cordys R&D B.V.
 *
 * This file is part of the Cordys Generic LDAP Connector.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");  you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software  distributed under the License is distributed on
 * an "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and  limitations under the License.
 */
package com.cordys.coe.ac.genericldap.soap.impl;

import com.cordys.coe.ac.genericldap.EDynamicAction;
import com.cordys.coe.ac.genericldap.connection.IConnectionManager;
import com.cordys.coe.ac.genericldap.exception.GenericLDAPConnectorException;
import com.cordys.coe.ac.genericldap.localization.GenLDAPExceptionMessages;
import com.cordys.coe.ac.genericldap.soap.BaseMethod;
import com.cordys.coe.util.xml.nom.XPathHelper;

import com.eibus.util.logger.CordysLogger;

import com.eibus.xml.nom.Node;
import com.eibus.xml.xpath.XPathMetaInfo;

import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPEntry;
import com.novell.ldap.LDAPSearchConstraints;
import com.novell.ldap.LDAPSearchResults;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This calls wraps the implementation of the SearchLDAP action.
 *
 * @author  pgussow
 */
public class SearchLDAPImpl extends GetLDAPObjectImpl
{
    /**
     * Holds the logger to use.
     */
    private static final CordysLogger LOG = CordysLogger.getCordysLogger(SearchLDAPImpl.class);
    /**
     * Holds the name of the parameter 'scope'.
     */
    private static final String PARAM_SCOPE = "scope";
    /**
     * Holds the name of the parameter 'filter'.
     */
    private static final String PARAM_FILTER = "filter";
    /**
     * Holds the name of the parameter 'sort'.
     */
    private static final String PARAM_SORT = "sort";
    /**
     * Holds the name of the parameter 'maxsearchresults'.
     */
    private static final String PARAM_MAX_SEARCH_RESULTS = "maxsearchresults";
    /**
     * Holds the name of the parameter 'referralfollowing'.
     */
    private static final String PARAM_REFERRAL_FOLLOWING = "referralfollowing";

    /**
     * Creates a new SearchLDAPImpl object.
     *
     * @param   implementation  The implementation XML.
     *
     * @throws  GenericLDAPConnectorException  In case of any exceptions.
     */
    public SearchLDAPImpl(int implementation)
                   throws GenericLDAPConnectorException
    {
        super(EDynamicAction.SEARCH_LDAP, implementation);

        int actionXML = Node.getFirstElement(implementation);

        // Parse the implementation for parameter scope.
        int scope = XPathHelper.selectSingleNode(actionXML, "impl:scope", m_xmi);

        if (scope == 0)
        {
            throw new GenericLDAPConnectorException(GenLDAPExceptionMessages.GLE_MISSING_REQUEST_INFORMATION_0,
                                                    "scope");
        }

        RequestParameter param = RequestParameter.getInstance(scope);
        addRequestParameter(param);

        // Parse the implementation for parameter filter.
        int filter = XPathHelper.selectSingleNode(actionXML, "impl:filter", m_xmi);

        if (filter == 0)
        {
            throw new GenericLDAPConnectorException(GenLDAPExceptionMessages.GLE_MISSING_REQUEST_INFORMATION_0,
                                                    "filter");
        }

        param = RequestParameter.getInstance(filter);
        addRequestParameter(param);

        // Parse the implementation for parameter sort.
        int sort = XPathHelper.selectSingleNode(actionXML, "impl:sort", m_xmi);

        if (sort == 0)
        {
            throw new GenericLDAPConnectorException(GenLDAPExceptionMessages.GLE_MISSING_REQUEST_INFORMATION_0, "sort");
        }

        param = RequestParameter.getInstance(sort);
        addRequestParameter(param);

        // Parse the implementation for parameter maxsearchresults.
        int maxsearchresults = XPathHelper.selectSingleNode(actionXML, "impl:maxsearchresults", m_xmi);

        if (maxsearchresults == 0)
        {
            throw new GenericLDAPConnectorException(GenLDAPExceptionMessages.GLE_MISSING_REQUEST_INFORMATION_0,
                                                    "maxsearchresults");
        }

        param = RequestParameter.getInstance(maxsearchresults);
        addRequestParameter(param);

        // Parse the implementation for parameter referralfollowing.
        int referralfollowing = XPathHelper.selectSingleNode(actionXML, "impl:referralfollowing", m_xmi);

        if (referralfollowing == 0)
        {
            throw new GenericLDAPConnectorException(GenLDAPExceptionMessages.GLE_MISSING_REQUEST_INFORMATION_0,
                                                    "referralfollowing");
        }

        param = RequestParameter.getInstance(referralfollowing);
        addRequestParameter(param);
    }

    /**
     * @see  com.cordys.coe.ac.genericldap.soap.impl.GetLDAPObjectImpl#handleRequest(com.cordys.coe.ac.genericldap.soap.BaseMethod)
     */
    @Override public void handleRequest(BaseMethod method)
                                 throws GenericLDAPConnectorException
    {
        // Now we have the DN to read, so let's try it.
        IConnectionManager connectionManager = method.getConfiguration().getConnectionManager();

        // Now we need to get the value for the parameter 'dn'
        RequestParameter paramDN = getParameter(PARAM_DN);
        RequestParameter paramScope = getParameter(PARAM_SCOPE);
        RequestParameter paramFilter = getParameter(PARAM_FILTER);
        RequestParameter paramSort = getParameter(PARAM_SORT);
        RequestParameter paramMaxSearchResults = getParameter(PARAM_MAX_SEARCH_RESULTS);
        RequestParameter paramReferralFollowing = getParameter(PARAM_REFERRAL_FOLLOWING);

        // Prepare the XPathMetaInfo object
        XPathMetaInfo xmi = new XPathMetaInfo();
        xmi.addNamespaceBinding("ns", Node.getNamespaceURI(method.getRequestXML()));

        // Get the parameter values.
        String dn = paramDN.getStringValue(method.getRequestXML(), xmi);
        int scope = LDAPConnection.SCOPE_SUB;
        scope = paramScope.getIntValue(method.getRequestXML(), xmi);

        String filter = paramFilter.getStringValue(method.getRequestXML(), xmi);
        String sort = paramSort.getStringValue(method.getRequestXML(), xmi);
        int maxSearchResults = paramMaxSearchResults.getIntValue(method.getRequestXML(), xmi,
                                                                 method.getConfiguration()
                                                                 .getMaximumNumberOfSearchResults());
        boolean referralFollowing = paramReferralFollowing.getBooleanValue(method.getRequestXML(), xmi);

        String[] attributeNames = determineAttributesToIncludeInSearch(method, xmi);

        try
        {
            if (LOG.isDebugEnabled())
            {
                LOG.debug("Executing search. Parameters:\nDN: '" + dn + "'\nScope: '" + scope + "'\nFilter: '" +
                          filter + "'\nMax search results: '" + maxSearchResults + "'\nSort: '" + sort +
                          "'\nFollow referrals: '" + referralFollowing + "'");
            }

            // Add contraints.
            LDAPSearchConstraints constraints = new LDAPSearchConstraints();
            constraints.setMaxResults(maxSearchResults);
            constraints.setReferralFollowing(referralFollowing);

            // Execute the actual LDAP search.
            LDAPSearchResults results = connectionManager.search(dn, scope, filter, (String[])
                                                                 attributeNames, false, constraints);

            ArrayList<LDAPEntry> entriesList = new ArrayList<LDAPEntry>();

            while (results.hasMore())
            {
                LDAPEntry entry = results.next();

                entriesList.add(entry);
            }

            // Build up the actual response.
            buildResponse(method, connectionManager, xmi, entriesList.toArray(new LDAPEntry[entriesList.size()]));
        }
        catch (Exception e)
        {
            throw new GenericLDAPConnectorException(e, GenLDAPExceptionMessages.ERROR_EXECUTING_SEARCH_REQUEST);
        }
    }

    /**
     * Determine the attributes to include in the search.
     *
     * @param   method  The basemethod.
     * @param   xmi     The namespace definitions.
     *
     * @return  A list of attribute names
     *
     * @throws  GenericLDAPConnectorException  In case of any exceptions
     */
    private String[] determineAttributesToIncludeInSearch(BaseMethod method, XPathMetaInfo xmi)
                                                       throws GenericLDAPConnectorException
    {
        Map<String, IAttributeDefinition> includeAttr = getReturnAttributes().getIncludeAttributes(method
                                                                                                   .getRequestXML(),
                                                                                                   xmi);
        Map<String, IAttributeDefinition> excludeAttr = getReturnAttributes().getExcludeAttributes(method
                                                                                                   .getRequestXML(),
                                                                                                   xmi);
        List<String> attributeNames = new ArrayList<String>();

        for (String attName : includeAttr.keySet())
        {
        	// Only return attributes that are not present in exclude list.
            if (!excludeAttr.containsKey(attName))
            {
                attributeNames.add(attName);
            }
        }
        
        if ((includeAttr.size() > 0) && (attributeNames.size() == 0))   
        {
        	// All included attributes are also excluded; throw error
        	throw new GenericLDAPConnectorException(GenLDAPExceptionMessages.NO_ATTRIBUTES_TO_INCLUDE_IN_SEARCH);
        }

        return attributeNames.toArray(new String[0]);
    }
}
