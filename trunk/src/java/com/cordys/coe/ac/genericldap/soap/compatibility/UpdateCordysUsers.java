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
 * This class handles the UpdateCordysUsers operation.
 *
 * <p>Helper method to update a cordys user with tuplpe/old/new structure cordys user is defined as
 * authenticated user. This will make it aesyer to use this in combination with mdm etc Our format:
 * </p>
 *
 * <pre>
   <tuple>
     <old>
       <cordysuser>
         <user>user</user>
         <displayname>displayname</displayname>
         <companyname>companyname</companyname>
         <phone>phone</phone>
         <email>email</email>
         <osname>osname</osname>
         <roles>                                   // todo : not yet implemented
           <role>Role1</role>                      // todo
           <role>Role2</role>                      // todo
         </roles>                                  // todo
       </cordysuser>
     </old>
   </tuple>
 * </pre>
 *
 * @author  pgussow
 */
public class UpdateCordysUsers extends BaseMethod
{
    /**
     * Creates a new UpdateCordysUsers object.
     *
     * @param  request        The incoming request.
     * @param  response       The outgoing response.
     * @param  configuration  The configuration.
     */
    public UpdateCordysUsers(BodyBlock request, BodyBlock response,
                             IGenLDAPConfiguration configuration)
    {
        super(request, response, configuration);
    }

    /**
     * Helper method to update a cordys user with tuplpe/old/new structure cordys user is defined as
     * authenticated user. This will make it aesyer to use this in combination with mdm etc Our
     * format:
     *
     * <pre>
       <tuple>
         <old>
           <cordysuser>
             <user>user</user>
             <displayname>displayname</displayname>
             <companyname>companyname</companyname>
             <phone>phone</phone>
             <email>email</email>
             <osname>osname</osname>
             <roles>                                   // todo : not yet implemented
               <role>Role1</role>                      // todo
               <role>Role2</role>                      // todo
             </roles>                                  // todo
           </cordysuser>
         </old>
       </tuple>
     * </pre>
     *
     * @throws  GenericLDAPConnectorException  DOCUMENTME
     *
     * @see     com.cordys.coe.ac.genericldap.soap.BaseMethod#execute()
     */
    @Override public void execute()
                           throws GenericLDAPConnectorException
    {
        Document document = Node.getDocument(getResponseXML());

        int ldapRequestNode = 0;
        int ldapresponseBody = 0;
        int updateRequestNode = 0;
        int UpdateResponseBody = 0;

        try
        {
            // get information
            int requestNode = getRequestXML();

            // System.out.println(Node.writeToString(requestNode, true));
            // get information
            String organizationalUser = getRequest().getSOAPTransaction().getIdentity()
                                                    .getOrgUserDN();
            String organization = organizationalUser.substring(organizationalUser.indexOf("o="));
            String postfix = organization.substring(organization.indexOf(",") + 1);
            String user = organizationalUser.substring(0, organizationalUser.indexOf(","));
            Connector tmpConnector = getConfiguration().getConnector();

            int currentrequestNode = Find.firstMatch(requestNode, "?<tuple>");

            while (currentrequestNode != 0)
            {
                // for each tupple :
                int oldNode = Find.firstMatch(currentrequestNode, "?<old>");
                int newNode = Find.firstMatch(currentrequestNode, "?<new>");

                boolean delete = ((oldNode != 0) && (newNode == 0));

                // boolean insert = (oldNode==0 && newNode!=0 );
                boolean update = ((oldNode != 0) && (newNode != 0));

                // System.out.println("Tuple delete="+delete+", insert="+insert+", update="+update);
                if (delete)
                {
                    String username = RequestUtil.getValueFromRequest(oldNode, "user", "");

                    deleteCordysUser(username, user, postfix);
                    return;
                }

                int currentTuppleNode = newNode;
                String username = RequestUtil.getValueFromRequest(currentTuppleNode, "user", "");

                try
                {
                    if (update)
                    {
                        // create request: get authenticated user from cordys
                        int ldapmethodNode = tmpConnector.createSOAPMethodEx("http://schemas.cordys.com/1.0/ldap",
                                                                             "SearchLDAP",
                                                                             organization,
                                                                             organizationalUser,
                                                                             null, null);
                        ldapRequestNode = SOAPMessage.getRootEnvelopeNode(ldapmethodNode);
                        Node.createTextElement("dn",
                                               "cn=" + username + ",cn=authenticated users," +
                                               postfix, ldapmethodNode);
                        Node.createTextElement("filter", "(objectclass=busauthenticationuser)",
                                               ldapmethodNode);

                        // System.out.println(Node.writeToString(ldapmethodNode, true));
                        // send it
                        int ldapResponseNode = tmpConnector.sendAndWait(ldapRequestNode);

                        // check response
                        ldapresponseBody = SOAPMessage.getRootBodyNode(ldapResponseNode);
                        // System.out.println(Node.writeToString(ldapresponseBody, true));
                        RequestUtil.checkSoapResponseforError(ldapresponseBody);
                    }

                    // WARNING : the next request is modified so it looks like it runs from the
                    // SYSTEM Organisation The user that runs the method must be a proper user with
                    // rigths in the SYSTEM organisation update or create the user
                    int updatemethodNode = tmpConnector.createSOAPMethodEx("http://schemas.cordys.com/1.0/ldap",
                                                                           "Update",
                                                                           /*organization, */ "o=SYSTEM," + postfix,
                                                                           /*organizationalUser,*/ user +
                                                                           ",cn=organizational users,o=SYSTEM," +
                                                                           postfix, null, null);
                    updateRequestNode = SOAPMessage.getRootEnvelopeNode(updatemethodNode);

                    // get values from request
                    String displayname = RequestUtil.getValueFromRequest(currentTuppleNode,
                                                                         "displayname", username);
                    String companyname = RequestUtil.getValueFromRequest(currentTuppleNode,
                                                                         "companyname", "");
                    String phone = RequestUtil.getValueFromRequest(currentTuppleNode, "phone", "");
                    String email = RequestUtil.getValueFromRequest(currentTuppleNode, "email", "");
                    String osname = RequestUtil.getValueFromRequest(currentTuppleNode, "osname",
                                                                    username);

                    // alwasy create tupple/new
                    int tuple = document.createElement("tuple", updatemethodNode);
                    int newnode = document.createElement("new", tuple);
                    int entryNode = Node.createElement("entry", newnode);
                    Node.setAttribute(entryNode, "dn",
                                      "cn=" + username + ",cn=authenticated users," + postfix);
                    Node.createTextElement("string", displayname,
                                           Node.createElement("description", entryNode));
                    Node.createTextElement("string", username, Node.createElement("cn", entryNode));
                    Node.createTextElement("string", osname,
                                           Node.createElement("osidentity", entryNode));
                    Node.createTextElement("string", companyname,
                                           Node.createElement("o", entryNode));
                    Node.createTextElement("string", email, Node.createElement("mail", entryNode));
                    Node.createTextElement("string", phone,
                                           Node.createElement("telephoneNumber", entryNode));
                    Node.createTextElement("string", organization,
                                           Node.createElement("defaultcontext", entryNode));

                    int objectclassnode = document.createElement("objectclass", entryNode);
                    Node.createTextElement("string", "top", objectclassnode);
                    Node.createTextElement("string", "busauthenticationuser", objectclassnode);

                    if (update)
                    {
                        // user was known, create an update request
                        int ldapoldnode = 0;

                        if (ldapresponseBody != 0)
                        {
                            ldapoldnode = Find.firstMatch(ldapresponseBody, "?<tuple><old>");
                            // user was known, create an update request
                        }

                        if (ldapoldnode != 0)
                        {
                            Node.duplicateAndAppend(ldapoldnode, ldapoldnode, newnode);
                        }
                    }

                    // send update or create
                    // System.out.println(Node.writeToString(updateRequestNode, true));
                    // send it
                    int updateResponseNode = tmpConnector.sendAndWait(updateRequestNode);

                    UpdateResponseBody = SOAPMessage.getRootBodyNode(updateResponseNode);
                    RequestUtil.checkSoapResponseforError(UpdateResponseBody);

                    // System.out.println(Node.writeToString(UpdateResponseBody, true));
                    int tuppleParent = getResponseXML();
                    tuple = document.createElement("tuple", tuppleParent);

                    int old = document.createElement("old", tuple);
                    int userNode = Node.createElement("cordysuser", old);
                    Node.createTextElement("user", username, userNode);
                    Node.createTextElement("displayname", displayname, userNode);
                    Node.createTextElement("companyname", companyname, userNode);
                    Node.createTextElement("phone", phone, userNode);
                    Node.createTextElement("email", email, userNode);
                    Node.createTextElement("osname", osname, userNode);

                    int rolesNode = Node.createElement("roles", userNode);
                    Node.createTextElement("role", "not implemented", rolesNode);
                    Node.createTextElement("role", "not implemented", rolesNode);

                    // find next node
                    currentrequestNode = Node.getNextSibling(currentrequestNode);
                }
                finally
                {
                    Node.delete(ldapRequestNode);
                    ldapRequestNode = 0;

                    Node.delete(Node.getRoot(ldapresponseBody));
                    ldapresponseBody = 0;

                    Node.delete(updateRequestNode);
                    updateRequestNode = 0;

                    Node.delete(Node.getRoot(UpdateResponseBody));
                    UpdateResponseBody = 0;
                }
            }
        }
        catch (Exception ex)
        {
            throw new GenericLDAPConnectorException(ex,
                                                    GenLDAPExceptionMessages.GLE_FAILED_TO_UPDATECORDYSUSERS);
        }
    }

    /**
     * Delete an authenticated user from cordys.
     *
     * <pre>
       <DeleteRecursive xmlns="http://schemas.cordys.com/1.0/ldap" sync_id="0">
         <tuple sync_id="0">
           <old>
             <entry dn="cn=user,cn=authenticated users,cn=cordys,o=vanenburg.com"/>
           </old>
         </tuple>
       </DeleteRecursive>
     * </pre>
     *
     * @param   username  Username to delete
     * @param   thisUser  Current user that does the request
     * @param   postfix   searchroot of th ecordys ldap server
     *
     * @throws  Exception  DOCUMENTME
     */
    private void deleteCordysUser(String username, String thisUser, String postfix)
                           throws Exception
    {
        int deleteResponseNode = 0;
        int deleteRequestNode = 0;

        try
        {
            Connector tmpConnector = getConfiguration().getConnector();
            int deleteMethodNode = tmpConnector.createSOAPMethodEx("http://schemas.cordys.com/1.0/ldap",
                                                                   "DeleteRecursive",
                                                                   /*organization, */ "o=SYSTEM," + postfix, // NOTE : fool system to think we are in the SYSTEM organisation
                                                                   /*organizationalUser,*/ thisUser +
                                                                   ",cn=organizational users,o=SYSTEM," +
                                                                   postfix, // NOTE : current user
                                                                            // must be a user of
                                                                            // SYSTEM
                                                                   null, null);
            deleteRequestNode = SOAPMessage.getRootEnvelopeNode(deleteMethodNode);

            // alwasy create tupple/old
            com.eibus.xml.nom.Document document = Node.getDocument(deleteRequestNode);
            int tuple = document.createElement("tuple", deleteMethodNode);
            int newnode = document.createElement("old", tuple);
            int entryNode = Node.createElement("entry", newnode);
            Node.setAttribute(entryNode, "dn",
                              "cn=" + username + ",cn=authenticated users," + postfix);
            // send it
            deleteResponseNode = tmpConnector.sendAndWait(deleteRequestNode);

            int deleteResponseBody = SOAPMessage.getRootBodyNode(deleteResponseNode);
            RequestUtil.checkSoapResponseforError(deleteResponseBody);
        }
        finally
        {
            Node.delete(deleteRequestNode);
            Node.delete(Node.getRoot(deleteResponseNode));
        }
    }
}
