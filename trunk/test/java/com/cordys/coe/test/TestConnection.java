package com.cordys.coe.test;

import com.cordys.coe.ac.genericldap.config.ConfigurationFactory;
import com.cordys.coe.ac.genericldap.config.IGenLDAPConfiguration;
import com.cordys.coe.ac.genericldap.connection.ConnectionManagerFactory;
import com.cordys.coe.ac.genericldap.connection.IConnectionManager;
import com.cordys.coe.util.FileUtils;
import com.cordys.coe.util.general.Util;

import com.eibus.xml.nom.Document;

import com.novell.ldap.LDAPEntry;

import java.io.File;

/**
 * This class will just test the connection to a certain LDAP server.
 *
 * <p>Note: you need to have the BouncyCastle jars on the classpath.</p>
 *
 * <p>To connect to a Cordys LDAP you need to do the following:</p>
 *
 * <ul>
 *   <li>Get the public key of the CARS server</li>
 *   <li>Create a new JKS keystore WITHOUT a password</li>
 *   <li>Add the public key to that keystore.</li>
 * </ul>
 *
 * <p>This class uses the testconfig.xml. Make sure you set set the password tag to a BASE64 encoded version of your
 * LDAP password</p>
 *
 * @author  pgussow
 */
public class TestConnection
{
    static
    {
        // Initializing the Cordys_home for the test
        try
        {
            System.setProperty("CORDYS_INSTALL_DIR", new File("./src/content").getCanonicalPath());
        }
        catch (Exception e)
        {
            System.err.println(Util.getStackTrace(e));
            System.exit(1);
        }
    }

    /**
     * Holds the temp NOM document.
     */
    private static Document m_doc = new Document();

    /**
     * Main method.
     *
     * @param  saArguments  Commandline arguments.
     */
    public static void main(String[] saArguments)
    {
        try
        {
            // Load the configuration XML
            String config = FileUtils.readTextResourceContents("testconfig.xml", TestConnection.class);
            int configuration = m_doc.parseString(config);
            IGenLDAPConfiguration genLDAPConfig = ConfigurationFactory.createConfiguration(configuration);

            // Create the connection
            IConnectionManager connectionManager = ConnectionManagerFactory.createConnection(genLDAPConfig, null);

            LDAPEntry entry = connectionManager.readLDAPEntry(genLDAPConfig.getSearchRoot());

            System.out.println(entry.getAttribute("cn").getStringValue());

            connectionManager.disconnect();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
