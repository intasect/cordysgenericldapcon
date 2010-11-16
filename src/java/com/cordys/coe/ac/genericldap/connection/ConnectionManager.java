package com.cordys.coe.ac.genericldap.connection;

import com.cordys.coe.ac.genericldap.config.EConnectionType;
import com.cordys.coe.ac.genericldap.config.IGenLDAPConfiguration;
import com.cordys.coe.ac.genericldap.exception.GenericLDAPConnectorException;
import com.cordys.coe.ac.genericldap.localization.GenLDAPExceptionMessages;
import com.cordys.coe.ac.genericldap.localization.GeneralMessages;

import com.eibus.util.Queue;
import com.eibus.util.logger.CordysLogger;

import com.novell.ldap.LDAPAttributeSchema;
import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPEntry;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPJSSESecureSocketFactory;
import com.novell.ldap.LDAPSchema;
import com.novell.ldap.LDAPSearchConstraints;
import com.novell.ldap.LDAPSearchResults;

import java.security.Provider;
import java.security.Security;

import java.util.Enumeration;

/**
 * This class maintains the connection to the LDAP server. It will automatically reconnect is for
 * some reason the connection died.
 *
 * <p>This class will be accessed by multiple threads.</p>
 *
 * @author  pgussow
 */
class ConnectionManager
    implements IConnectionManager
{
    /**
     * Holds the logger to use.
     */
    private static final CordysLogger LOG = CordysLogger.getCordysLogger(ConnectionManager.class);
    /**
     * Holds the configuration to use.
     */
    private IGenLDAPConfiguration m_configuration;
    /**
     * Holds the available connections.
     */
    private Queue m_connectionQueue;
    /**
     * Holds the schema for this connection.
     */
    private LDAPSchema m_schema;
    /**
     * Holds the optional SSL socket factory,
     */
    private LDAPJSSESecureSocketFactory m_ssf;

    /**
     * Construction. It will initialize the actual connections to LDAP.
     *
     * @param   configuration  The configuration to use.
     *
     * @throws  GenericLDAPConnectorException  In case of any exceptions.
     */
    public ConnectionManager(IGenLDAPConfiguration configuration)
                      throws GenericLDAPConnectorException
    {
        m_configuration = configuration;

        if (LOG.isDebugEnabled())
        {
            LOG.debug("Creating " + configuration.getNrOfConnections() + " connections of type " +
                      configuration.getConnectionType() + ". The connections will " +
                      (configuration.isAnonymousBind() ? "NOT " : "") + " use authentication.");
        }

        // Create the placeholder for the connections.
        m_connectionQueue = new Queue(configuration.getNrOfConnections());

        // Check if the connection is supposed to be secure. If so, set up the security.
        m_ssf = null;

        if (configuration.getConnectionType() == EConnectionType.SECURE)
        {
            Provider pTemp = configuration.getSecurityProvider();

            if (pTemp == null)
            {
                throw new GenericLDAPConnectorException(GenLDAPExceptionMessages.GLE_COULD_NOT_CREATE_THE_DESIDERED_SECURITY_PROVIDER);
            }
            Security.addProvider(pTemp);
            System.setProperty("javax.net.ssl.trustStore", configuration.getKeyStore());

            m_ssf = new LDAPJSSESecureSocketFactory();
        }

        LDAPConnection connection = null;

        try
        {
            connection = createConnection(m_ssf, configuration);

            // Now also read the schema
            String schemaDN = connection.getSchemaDN();

            if ((schemaDN != null) && (schemaDN.length() > 0))
            {
                if (LOG.isDebugEnabled())
                {
                    LOG.debug("The DN for the schema: " + schemaDN);
                }

                m_schema = connection.fetchSchema(schemaDN);

                if (LOG.isDebugEnabled())
                {
                    // Dump the schema
                    StringBuilder sbTemp = new StringBuilder(1024);

                    Enumeration<?> enumNames = m_schema.getAttributeNames();

                    while (enumNames.hasMoreElements())
                    {
                        String name = (String) enumNames.nextElement();
                        LDAPAttributeSchema attrSchema = m_schema.getAttributeSchema(name);
                        String syntax = attrSchema.getSyntaxString();

                        sbTemp.append(name).append(": ").append(attrSchema.getID())
                              .append("; Syntax: ").append(syntax).append("\n");
                    }
                    
                    LOG.debug(sbTemp.toString());
                }
            }
        }
        catch (Exception e)
        {
            throw new GenericLDAPConnectorException(e,
                                                    GenLDAPExceptionMessages.GLE_ERROR_CREATING_CONNECTION_TO_01,
                                                    configuration.getServer(),
                                                    configuration.getPort());
        }

        // Store the connection.
        m_connectionQueue.put(connection);

        // Create the rest of the connections.
        for (int iCount = 1; iCount < configuration.getNrOfConnections(); iCount++)
        {
            try
            {
                m_connectionQueue.put(createConnection(m_ssf, configuration));
            }
            catch (Exception e)
            {
                throw new GenericLDAPConnectorException(e,
                                                        GenLDAPExceptionMessages.GLE_ERROR_CREATING_CONNECTION_NUMBER_0_FOR_SERVER_12,
                                                        iCount, configuration.getServer(),
                                                        configuration.getPort());
            }
        }
    }

    /**
     * @see  com.cordys.coe.ac.genericldap.connection.IConnectionManager#disconnect()
     */
    @Override public void disconnect()
    {
        if (LOG.isDebugEnabled())
        {
            LOG.debug("Trying to disconnect " + m_connectionQueue.size() + " connections");
        }

        while (m_connectionQueue.size() > 0)
        {
            LDAPConnection con = (LDAPConnection) m_connectionQueue.nonBlockingGet();

            if (con == null)
            {
                // This connection is already gone.
            }
            else if (con.isConnected())
            {
                try
                {
                    con.disconnect();
                }
                catch (LDAPException e)
                {
                    LOG.warn(e, GeneralMessages.ERROR_DISCONNECTING_CONNECTION);
                }
            }
        }
    }

    /**
     * This method will return an active connection. If none is available it will wait.
     *
     * @return  The connection or null if not connected.
     *
     * @throws  GenericLDAPConnectorException  In case no connection could be obtained.
     */
    public LDAPConnection getConnection()
                                 throws GenericLDAPConnectorException
    {
        LDAPConnection returnConnection = null;

        if (LOG.isDebugEnabled())
        {
            LOG.debug("Waiting for connection");
        }

        LDAPConnection con = (LDAPConnection) m_connectionQueue.get();

        if (LOG.isDebugEnabled())
        {
            LOG.debug("Obtained connection");
        }

        // Put in a try-finally construct to make sure the connection is never lost.
        if (LOG.isDebugEnabled())
        {
            LOG.debug("Current connection connected: " + con.isConnected() + " and alive: " +
                      con.isConnectionAlive());
        }

        if (con.isConnected())
        {
            // It's a normal, active connection.
            returnConnection = con;
        }
        else
        {
            // Kill the connection and recreate it.
            try
            {
                con.disconnect();
            }
            catch (Exception e)
            {
                LOG.warn(e, GeneralMessages.ERROR_DISCONNECTING_CONNECTION);
            }

            try
            {
                con = createConnection(m_ssf, m_configuration);
            }
            catch (Exception e)
            {
                // This is serious, because it means that we will have 1 connection less then
                // before.
                LOG.fatal(e,
                          GeneralMessages.ERROR_LOST_A_CONNECTION_BECAUSE_IT_COULD_NOT_BE_RECREATED);
            }
            
            //Return the freshly created connection.
            returnConnection = con;
        }

        if (returnConnection == null)
        {
            throw new GenericLDAPConnectorException(GenLDAPExceptionMessages.GLE_COULD_NOT_OBTAIN_FREE_LDAP_CONNECTION);
        }

        return returnConnection;
    }

    /**
     * @see  com.cordys.coe.ac.genericldap.connection.IConnectionManager#getSchema()
     */
    @Override public LDAPSchema getSchema()
    {
        return m_schema;
    }

    /**
     * This method will read a specific DN from the LDAP server.
     *
     * @param   dn  The DN to read.
     *
     * @return  The LDAP entry that was read.
     *
     * @throws  GenericLDAPConnectorException  In case of any exceptions.
     */
    public LDAPEntry readLDAPEntry(String dn)
                            throws GenericLDAPConnectorException
    {
        LDAPEntry returnLDAPEntry = null;

        LDAPConnection con = getConnection();

        try
        {
            if (LOG.isDebugEnabled())
            {
                LOG.debug("Reading entry with DN: " + dn);
            }
            returnLDAPEntry = con.read(dn);
        }
        catch (Exception e)
        {
            throw new GenericLDAPConnectorException(e,
                                                    GenLDAPExceptionMessages.GLE_ERROR_READING_LDAP_ENTRY_WITH_DN_0,
                                                    dn);
        }
        finally
        {
            // Put the connection back in the queue.
            releaseConnection(con);
        }

        return returnLDAPEntry;
    }

    /**
     * This method releases the connection to the pool.
     *
     * @param  connection  The connection to releases the connection to the pool.
     */
    public void releaseConnection(LDAPConnection connection)
    {
        if (connection != null)
        {
            if (LOG.isDebugEnabled())
            {
                LOG.debug("Releasing connection. Size before release: " + m_connectionQueue.size());
            }

            m_connectionQueue.put(connection);
        }
    }

    /**
     * This method searches LDAP for entries.
     *
     * @param   rootDN          The DN to start searching from.
     * @param   scope           The scope for searching.
     * @param   filter          The filter to use.
     * @param   attributeNames  The names of the attributes to retrieve.
     * @param   excludeValues   Whether or not to exclude the values when returning the results.
     * @param   constraints     The constraints to apply.
     *
     * @return  The result from the search query.
     *
     * @throws  GenericLDAPConnectorException  In case of any exceptions.
     *
     * @see     com.cordys.coe.ac.genericldap.connection.IConnectionManager#search(java.lang.String,
     *          int, java.lang.String, java.lang.String[], boolean,
     *          com.novell.ldap.LDAPSearchConstraints)
     */
    @Override public LDAPSearchResults search(String rootDN, int scope, String filter,
                                              String[] attributeNames, boolean excludeValues,
                                              LDAPSearchConstraints constraints)
                                       throws GenericLDAPConnectorException
    {
        LDAPSearchResults returnLDAPSearchResults = null;

        LDAPConnection con = getConnection();

        try
        {
            if (LOG.isDebugEnabled())
            {
                LOG.debug(getDetails(rootDN, scope, filter, attributeNames, excludeValues,
                                     constraints));
            }

            // Do the actual search.
            returnLDAPSearchResults = con.search(rootDN, scope, filter, attributeNames,
                                                 excludeValues, constraints);
        }
        catch (Exception e)
        {
            throw new GenericLDAPConnectorException(e,
                                                    GenLDAPExceptionMessages.GLE_ERROR_EXECUTING_SEARCH_WITH_CRITERIAN0,
                                                    getDetails(rootDN, scope, filter,
                                                               attributeNames, excludeValues,
                                                               constraints));
        }
        finally
        {
            // Put the connection back in the queue.
            m_connectionQueue.put(con);
        }

        return returnLDAPSearchResults;
    }

    /**
     * This method creates a connection.
     *
     * @param   ssf            The secure socket factory to support SSL.
     * @param   configuration  The configuration to use.
     *
     * @return  The LDAP connection to use.
     *
     * @throws  LDAPException  In case of any exception.
     */
    private LDAPConnection createConnection(LDAPJSSESecureSocketFactory ssf,
                                            IGenLDAPConfiguration configuration)
                                     throws LDAPException
    {
        LDAPConnection returnConnection = null;

        // Try one connection to see if authentication and setup is OK
        if (ssf == null)
        {
            returnConnection = new LDAPConnection();
        }
        else
        {
            returnConnection = new LDAPConnection(ssf);
        }

        // Do the connect
        returnConnection.connect(configuration.getServer(), configuration.getPort());

        if (!configuration.isAnonymousBind())
        {
            returnConnection.bind(LDAPConnection.LDAP_V3, configuration.getUser(),
                                  configuration.getPassword().getBytes());
        }

        return returnConnection;
    }

    /**
     * DOCUMENTME.
     *
     * @param   rootDN          DOCUMENTME
     * @param   scope           DOCUMENTME
     * @param   filter          DOCUMENTME
     * @param   attributeNames  DOCUMENTME
     * @param   excludeValues   DOCUMENTME
     * @param   constraints     DOCUMENTME
     *
     * @return  DOCUMENTME
     */
    private String getDetails(String rootDN, int scope, String filter, String[] attributeNames,
                              boolean excludeValues, LDAPSearchConstraints constraints)
    {
        StringBuilder sbTemp = new StringBuilder(2048);
        sbTemp.append("Search dn: ").append(rootDN).append("\n");
        sbTemp.append("Search filter: ").append(filter).append("\n");

        String att = "";

        if (attributeNames != null)
        {
            for (int j = 0; j < attributeNames.length; j++)
            {
                att += (attributeNames[j] + ",");
            }

            sbTemp.append("Search attributes: ").append(att).append("\n");
        }
        sbTemp.append("Search scope: ").append(scope).append("\n");
        sbTemp.append("Search excludeValues: ").append(excludeValues);

        return sbTemp.toString();
    }
}
