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

import com.cordys.coe.ac.genericldap.config.IGenLDAPConfiguration;
import com.cordys.coe.ac.genericldap.exception.GenericLDAPConnectorException;
import com.cordys.coe.ac.genericldap.localization.GenLDAPExceptionMessages;
import com.cordys.coe.ac.genericldap.soap.BaseMethod;
import com.cordys.coe.ac.genericldap.util.RequestUtil;

import com.eibus.connector.nom.Connector;
import com.eibus.connector.nom.SOAPMessage;

import com.eibus.soap.BodyBlock;

import com.eibus.xml.nom.Document;
import com.eibus.xml.nom.Find;
import com.eibus.xml.nom.Node;

/**
 * Helper method to create or update a cordys user cordys user is defined as user. This will make it
 * easier to use this in combination with mdm etc.
 *
 * <pre>
   <updateCordysUsers xmlns="http://schemas.cordys.com/1.0/adsldap">
       <tuple>
           <old>
               <cordysuser>
                   <user>PARAMETER</user>
                   <displayname>PARAMETER</displayname>
                   <companyname>PARAMETER</companyname>
                   <phone>PARAMETER</phone>
                   <email>PARAMETER</email>
                   <osname>PARAMETER</osname>
                   <roles>
                       <role>PARAMETER</role>
                   </roles>
               </cordysuser>
           </old>
           <new>
               <cordysuser>
                   etc
               </cordysuser>
           </new>
       </tuple>
   </updateCordysUsers>
 * </pre>
 *
 * @author  pgussow
 */
public class CreateOrUpdateCordysuser extends BaseMethod
{
    /**
     * Creates a new CreateOrUpdateCordysuser object.
     *
     * @param  request        The incoming request.
     * @param  response       The outgoing response.
     * @param  configuration  DOCUMENTME
     */
    public CreateOrUpdateCordysuser(BodyBlock request, BodyBlock response,
                                    IGenLDAPConfiguration configuration)
    {
        super(request, response, configuration);
    }

    /**
     * Helper method to create or update a cordys user cordys user is defined as user. This will
     * make it easier to use this in combination with mdm etc.
     *
     * <pre>
       <updateCordysUsers xmlns="http://schemas.cordys.com/1.0/adsldap">
           <tuple>
               <old>
                   <cordysuser>
                       <user>PARAMETER</user>
                       <displayname>PARAMETER</displayname>
                       <companyname>PARAMETER</companyname>
                       <phone>PARAMETER</phone>
                       <email>PARAMETER</email>
                       <osname>PARAMETER</osname>
                       <roles>
                           <role>PARAMETER</role>
                       </roles>
                   </cordysuser>
               </old>
               <new>
                   <cordysuser>
                       etc
                   </cordysuser>
               </new>
           </tuple>
       </updateCordysUsers>
     * </pre>
     *
     * @throws  GenericLDAPConnectorException  In case of any exceptions.
     *
     * @see     com.cordys.coe.ac.genericldap.soap.BaseMethod#execute()
     */
    @Override public void execute()
                           throws GenericLDAPConnectorException
    {
        Document document = Node.getDocument(getResponseXML());

        int ldapRequestNode = 0;
        int ldapResponseNode = 0;
        int updateResponseNode = 0;
        int updateRequestNode = 0;

        try
        {
            // get information
            int requestNode = getRequestXML();
            int requestUserNode = Find.firstMatch(requestNode, "?<user>");

            if (requestUserNode == 0)
            {
                throw new GenericLDAPConnectorException(GenLDAPExceptionMessages.GLE_THE_USER_COULD_NOT_BE_FOUND_IN_THE_REQUEST);
            }

            String requestUser = Node.getData(requestUserNode);

            // get information
            String organizationalUser = getRequest().getSOAPTransaction().getIdentity()
                                                    .getOrgUserDN();

            String organization = organizationalUser.substring(organizationalUser.indexOf("o="));
            String postfix = organization.substring(organization.indexOf(",") + 1);
            String user = organizationalUser.substring(0, organizationalUser.indexOf(","));

            Connector tmpConnector = getConfiguration().getConnector();

            // create request: get authenticated user from cordys
            int ldapmethodNode = tmpConnector.createSOAPMethodEx("http://schemas.cordys.com/1.0/ldap",
                                                                 "SearchLDAP", organization,
                                                                 organizationalUser, null, null);
            ldapRequestNode = SOAPMessage.getRootEnvelopeNode(ldapmethodNode);
            Node.createTextElement("dn", "cn=" + requestUser + ",cn=authenticated users," + postfix,
                                   ldapmethodNode);
            Node.createTextElement("filter", "(objectclass=busauthenticationuser)", ldapmethodNode);
            // send it
            ldapResponseNode = tmpConnector.sendAndWait(ldapRequestNode);

            // good response

            /*       <old>
             * <entry dn="cn=kvginkel,cn=authenticated users,cn=cordys,o=vanenburg.com">  <o>
             * <string>Cordys</string>  </o>  <mail>    <string>kvginkel@cordys.com</string> </mail>
             * <cn>    <string>kvginkel</string>  </cn>  <osidentity> <string>kvginkel</string>
             * </osidentity>  <defaultcontext> <string>o=iProject,cn=cordys,o=vanenburg.com</string>
             * </defaultcontext> <objectclass>    <string>top</string>
             * <string>busauthenticationuser</string> </objectclass>  <description>
             * <string>kvginkel</string>  </description> </entry> </old>
             */

            // check response
            int responseBody = SOAPMessage.getRootBodyNode(ldapResponseNode);

            // System.out.println(Node.writeToString(responseBody, true));
            int methodResult = Node.getFirstChild(responseBody);

            if (methodResult == 0)
            {
                throw new Exception("Backend failure: no response data");
            }

            if (Node.getLocalName(methodResult).equals("Fault")) // SOAP Fault (e.g. Server.Error)
            {
                throw new Exception("Backend Exception: " + Node.writeToString(methodResult, true));
            }

            // WARNING : the next request is modified so it looks like it runs from the SYSTEM
            // Organisation The user that runs the method must be a proper user with rigths in the
            // SYSTEM organisation update or create the user
            int updatemethodNode = tmpConnector.createSOAPMethodEx("http://schemas.cordys.com/1.0/ldap",
                                                                   "Update",
                                                                   /*organization, */ "o=SYSTEM," + postfix,
                                                                   /*organizationalUser,*/ user +
                                                                   ",cn=organizational users,o=SYSTEM," +
                                                                   postfix, null, null);
            updateRequestNode = SOAPMessage.getRootEnvelopeNode(updatemethodNode);

            // get values from request
            String displayname = RequestUtil.getValueFromRequest(requestNode, "displayname",
                                                                 requestUser);
            String companyname = RequestUtil.getValueFromRequest(requestNode, "companyname", "");
            String phone = RequestUtil.getValueFromRequest(requestNode, "phone", "");
            String email = RequestUtil.getValueFromRequest(requestNode, "email", "");
            String osname = RequestUtil.getValueFromRequest(requestNode, "osname", requestUser);

            // alwasy create tupple/new
            int tuple = document.createElement("tuple", updatemethodNode);
            int newnode = document.createElement("new", tuple);
            int entryNode = Node.createElement("entry", newnode);
            Node.setAttribute(entryNode, "dn",
                              "cn=" + requestUser + ",cn=authenticated users," + postfix);
            Node.createTextElement("string", displayname,
                                   Node.createElement("description", entryNode));
            Node.createTextElement("string", requestUser, Node.createElement("cn", entryNode));
            Node.createTextElement("string", osname, Node.createElement("osidentity", entryNode));
            Node.createTextElement("string", companyname, Node.createElement("o", entryNode));
            Node.createTextElement("string", email, Node.createElement("mail", entryNode));
            Node.createTextElement("string", phone,
                                   Node.createElement("telephoneNumber", entryNode));
            Node.createTextElement("string", organization,
                                   Node.createElement("defaultcontext", entryNode));

            int objectclassnode = document.createElement("objectclass", entryNode);
            Node.createTextElement("string", "top", objectclassnode);
            Node.createTextElement("string", "busauthenticationuser", objectclassnode);

            // now check if we have the ldap representation or just an empty node (user unknown)
            int tuppleNode = Node.getFirstChild(methodResult);

            if (tuppleNode != 0)
            {
                // user was known, create an update request
                int oldNode = Node.getFirstChild(tuppleNode);
                Node.duplicateAndAppend(oldNode, oldNode, newnode);
            }

            // send update or create
            // System.out.println(Node.writeToString(updateRequestNode, true));
            // send it
            updateResponseNode = tmpConnector.sendAndWait(updateRequestNode);

            // System.out.println(Node.writeToString(UpdateResponseBody, true));
            int tupleParent = getResponseXML();
            tuple = document.createElement("tuple", tupleParent);

            int old = document.createElement("old", tuple);
            int userNode = Node.createElement("cordysuser", old);
            Node.createTextElement("user", requestUser, userNode);
            Node.createTextElement("displayname", displayname, userNode);
            Node.createTextElement("companyname", companyname, userNode);
            Node.createTextElement("phone", phone, userNode);
            Node.createTextElement("email", email, userNode);
            Node.createTextElement("osname", osname, userNode);

            int rolesNode = Node.createElement("roles", userNode);
            Node.createTextElement("role", "Not yet implemented", rolesNode);
            Node.createTextElement("role", "Not yet implemented", rolesNode);
        }
        catch (Exception e)
        {
            throw new GenericLDAPConnectorException(e,
                                                    GenLDAPExceptionMessages.GLE_ERROR_CREATING_OR_UPDATING_CORDYS_USERS);
        }
        finally
        {
            // cleanup xml data nodes
            if (ldapRequestNode != 0)
            {
                Node.delete(ldapRequestNode);
            }

            if (ldapResponseNode != 0)
            {
                Node.delete(Node.getRoot(ldapResponseNode));
            }

            if (updateRequestNode != 0)
            {
                Node.delete(updateRequestNode);
            }

            if (updateResponseNode != 0)
            {
                Node.delete(Node.getRoot(updateResponseNode));
            }
        }
    }
}
