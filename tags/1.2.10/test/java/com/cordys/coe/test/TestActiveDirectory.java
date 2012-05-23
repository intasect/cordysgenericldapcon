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
package com.cordys.coe.test;

import com.cordys.coe.ac.genericldap.config.EConnectionType;
import com.cordys.coe.ac.genericldap.config.IGenLDAPConfiguration;
import com.cordys.coe.ac.genericldap.connection.ConnectionManagerFactory;
import com.cordys.coe.ac.genericldap.connection.IConnectionManager;

import com.eibus.connector.nom.Connector;
import com.novell.ldap.LDAPAttribute;
import com.novell.ldap.LDAPAttributeSchema;
import com.novell.ldap.LDAPEntry;
import com.novell.ldap.LDAPSchema;

import java.security.Provider;
import java.util.Enumeration;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * DOCUMENTME.
 *
 * @author  $author$
 */
public class TestActiveDirectory
{
    /**
     * Main method.
     *
     * @param  saArguments  Commandline arguments.
     */
    public static void main(String[] saArguments)
    {
        try
        {
        	BasicConfigurator.configure();
        	Logger.getRootLogger().setLevel(Level.DEBUG);
        	
            LocalConfig configuration = new LocalConfig();
            IConnectionManager cm = ConnectionManagerFactory.createConnection(configuration, null);
            configuration.setConnectionManager(cm);
            
            LDAPEntry entry = cm.readLDAPEntry("CN=LostAndFound,DC=DomainDnsZones,DC=vanenburg,DC=com");
            System.out.println(entry.getAttributeSet().size());
            
            LDAPSchema schema = cm.getSchema();
            
            LDAPAttribute attr = entry.getAttribute("objectGUID");
            String name = attr.getName();
            LDAPAttribute aschema = schema.getAttribute("AttributeDefinition/" + name);
            
            LDAPAttributeSchema attrSchema = schema.getAttributeSchema("objectGUID");
            System.out.println("ObjectGUID: " + attrSchema.getSyntaxString());
            
            attrSchema = schema.getAttributeSchema("name");
            System.out.println("Name: " + attrSchema.getSyntaxString());
            
            attrSchema = schema.getAttributeSchema("whenCreated");
            System.out.println("whenCreated: " + attrSchema.getSyntaxString());
            System.out.println(attrSchema.isSingleValued());
            
            Enumeration<?> e = schema.getAttributeSchemas();
            while(e.hasMoreElements())
            {
            	Object s = e.nextElement();
            	System.out.println(s.toString());
            }
            System.out.println(aschema);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * DOCUMENTME.
     *
     * @author  $author$
     */
    public static class LocalConfig
        implements IGenLDAPConfiguration
    {
        /**
         * DOCUMENTME.
         */
        private IConnectionManager m_cm;

        /**
         * @see  com.cordys.coe.ac.genericldap.config.IGenLDAPConfiguration#getConnectionManager()
         */
        @Override public IConnectionManager getConnectionManager()
        {
            // TODO Auto-generated method stub
            return m_cm;
        }

        /**
         * @see  com.cordys.coe.ac.genericldap.config.IGenLDAPConfiguration#getConnectionType()
         */
        @Override public EConnectionType getConnectionType()
        {
            // TODO Auto-generated method stub
            return EConnectionType.PLAIN;
        }

        /**
         * @see  com.cordys.coe.ac.genericldap.config.IGenLDAPConfiguration#getConnector()
         */
        @Override public Connector getConnector()
        {
            // TODO Auto-generated method stub
            return null;
        }

        /**
         * @see  com.cordys.coe.ac.genericldap.config.IGenLDAPConfiguration#getKeyStore()
         */
        @Override public String getKeyStore()
        {
            // TODO Auto-generated method stub
            return null;
        }

        /**
         * @see  com.cordys.coe.ac.genericldap.config.IGenLDAPConfiguration#getMaximumNumberOfSearchResults()
         */
        @Override public int getMaximumNumberOfSearchResults()
        {
            // TODO Auto-generated method stub
            return 50;
        }

        /**
         * @see  com.cordys.coe.ac.genericldap.config.IGenLDAPConfiguration#getNrOfConnections()
         */
        @Override public int getNrOfConnections()
        {
            // TODO Auto-generated method stub
            return 5;
        }

        /**
         * @see  com.cordys.coe.ac.genericldap.config.IGenLDAPConfiguration#getPassword()
         */
        @Override public String getPassword()
        {
            // TODO Auto-generated method stub
            return "phillip31";
        }

        /**
         * @see  com.cordys.coe.ac.genericldap.config.IGenLDAPConfiguration#getPort()
         */
        @Override public int getPort()
        {
            // TODO Auto-generated method stub
            return 389;
        }

        /**
         * @see  com.cordys.coe.ac.genericldap.config.IGenLDAPConfiguration#getSearchRoot()
         */
        @Override public String getSearchRoot()
        {
            // TODO Auto-generated method stub
            return null;
        }

        /**
         * @see  com.cordys.coe.ac.genericldap.config.IGenLDAPConfiguration#getSecurityProvider()
         */
        @Override public Provider getSecurityProvider()
        {
            // TODO Auto-generated method stub
            return null;
        }

        /**
         * @see  com.cordys.coe.ac.genericldap.config.IGenLDAPConfiguration#getServer()
         */
        @Override public String getServer()
        {
            // TODO Auto-generated method stub
            return "10.1.36.15";
        }

        /**
         * @see  com.cordys.coe.ac.genericldap.config.IGenLDAPConfiguration#getUser()
         */
        @Override public String getUser()
        {
            // TODO Auto-generated method stub
            return "NTDOM\\pgussow";
        }

        /**
         * @see  com.cordys.coe.ac.genericldap.config.IGenLDAPConfiguration#isAnonymousBind()
         */
        @Override public boolean isAnonymousBind()
        {
            // TODO Auto-generated method stub
            return false;
        }

        /**
         * @see  com.cordys.coe.ac.genericldap.config.IGenLDAPConfiguration#setConnectionManager(com.cordys.coe.ac.genericldap.connection.IConnectionManager)
         */
        @Override public void setConnectionManager(IConnectionManager connectionManager)
        {
            m_cm = connectionManager;
        }

        /**
         * @see  com.cordys.coe.ac.genericldap.config.IGenLDAPConfiguration#setConnector(com.eibus.connector.nom.Connector)
         */
        @Override public void setConnector(Connector connector)
        {
            // TODO Auto-generated method stub
        }
    }
}
