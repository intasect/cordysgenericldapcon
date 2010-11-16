package com.cordys.coe.ac.genericldap.soap.impl;

import com.cordys.coe.ac.genericldap.exception.GenericLDAPConnectorException;

import com.eibus.soap.MethodDefinition;

import com.eibus.util.logger.CordysLogger;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This cache stores the parsed objects for the given method and namespace.
 *
 * @author  pgussow
 */
public class ImplementationCache
{
    /**
     * Holds the logger to use.
     */
    private static final CordysLogger LOG = CordysLogger.getCordysLogger(ImplementationCache.class);
    /**
     * Holds the singleton.
     */
    private static ImplementationCache s_singleton = new ImplementationCache();
    /**
     * Holds the currently cached implementations.
     */
    private Map<String, BaseImplementation> m_implementations = Collections.synchronizedMap(new LinkedHashMap<String, BaseImplementation>());

    /**
     * Creates a new ImplementationCache object.
     */
    private ImplementationCache()
    {
    }

    /**
     * This method gets the parsed implementation object for the current method definition.
     *
     * @param   md  The definition of the method.
     *
     * @return  The implementation to use for this method.
     *
     * @throws  GenericLDAPConnectorException  In case of any exceptions.
     */
    public static BaseImplementation getImplementation(MethodDefinition md)
                                                throws GenericLDAPConnectorException
    {
        ImplementationCache cache = getInstance();

        return cache._getImplementation(md);
    }

    /**
     * This method gets the instance of the cache to use.
     *
     * @return  The instance of the cache to use.
     */
    public static ImplementationCache getInstance()
    {
        return s_singleton;
    }

    /**
     * This method returns the parsed implementation for the method.
     *
     * @param   md  The definition of the current method.
     *
     * @return  The parsed definition of the method.
     *
     * @throws  GenericLDAPConnectorException  In case of any exceptions.
     */
    private BaseImplementation _getImplementation(MethodDefinition md)
                                           throws GenericLDAPConnectorException
    {
        BaseImplementation returnValue = null;

        String methodDN = md.getMethodDN().toString();

        // Synchronize the access to the map in order to make sure the cache remains intact
        synchronized (m_implementations)
        {
            boolean create = true;

            if (m_implementations.containsKey(methodDN))
            {
                returnValue = m_implementations.get(methodDN);

                // Now we need to make sure it's still the same object.
                if (returnValue.getImplementation() != md.getImplementation())
                {
                    // Appearantly the implementation has changed, so we need to parse it again.
                    if (LOG.isDebugEnabled())
                    {
                        LOG.debug("Implementation differs. Going to remove the old one: " +
                                  methodDN);
                    }
                    m_implementations.remove(methodDN);
                }
                else
                {
                    // Reuse existing object.
                    if (LOG.isDebugEnabled())
                    {
                        LOG.debug("Reusing existing object");
                    }
                    create = false;
                }
            }

            if (create)
            {
                if (LOG.isDebugEnabled())
                {
                    LOG.debug("Creating method wrapper for " + methodDN);
                }

                // There is no reference yet for this method. So we'll parse it and add it to the
                // cache.
                returnValue = ImplementationFactory.createImplementation(md);
                m_implementations.put(methodDN, returnValue);
            }
        }

        return returnValue;
    }
}
