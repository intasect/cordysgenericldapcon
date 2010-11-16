/**
 * Copyright © 2001 Cordys Systems, B.V. All rights reserved.
 * The copyright of the computer program(s) herein is the
 * property of Cordys Systems, B.V. The program(s)
 * may be used/copied only with the written permission of the
 * owner or in accordance with the terms and conditions stipulated
 * in the agreement/contract under which the program(s) have
 * been supplied.
 *
 */
package com.cordys.coe.ac.genericldap.soap.compatibility;

import java.util.Enumeration;
import java.util.Vector;

import com.cordys.coe.ac.genericldap.config.IGenLDAPConfiguration;
import com.cordys.coe.ac.genericldap.connection.IConnectionManager;
import com.cordys.coe.ac.genericldap.exception.GenericLDAPConnectorException;
import com.cordys.coe.ac.genericldap.localization.GenLDAPExceptionMessages;
import com.cordys.coe.ac.genericldap.localization.GeneralMessages;
import com.cordys.coe.ac.genericldap.soap.BaseMethod;
import com.eibus.directory.soap.DN;
import com.eibus.directory.soap.EntryToXML;
import com.eibus.directory.soap.LDAPUtil;
import com.eibus.directory.soap.RDN;
import com.eibus.directory.soap.XMLToEntry;
import com.eibus.soap.BodyBlock;
import com.eibus.util.StringSorter;
import com.eibus.util.logger.CordysLogger;
import com.eibus.xml.nom.Find;
import com.eibus.xml.nom.Node;
import com.novell.ldap.LDAPAttribute;
import com.novell.ldap.LDAPAttributeSet;
import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPConstraints;
import com.novell.ldap.LDAPEntry;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPModification;
import com.novell.ldap.LDAPSearchResults;

/**
 * This method takes care of the updates to LDAP (insert, update and deletes).
 *
 * @author pgussow
 */
public class Update extends BaseMethod
{
    /**
     * Contains the logger.
     */
    public static CordysLogger LOG = CordysLogger.getCordysLogger(Update.class);
    /**
     * Holds the connection that should be used.
     */
    private LDAPConnection connection;
    /**
     * DOCUMENTME.
     */
    private int requestInterface;
    /**
     * DOCUMENTME.
     */
    private LDAPUpdate[] updates;

    /**
     * Constructor.
     *
     * @param  request        The incoming request.
     * @param  response       The outgoing response.
     * @param  configuration  The configuration.
     */
    public Update(BodyBlock request, BodyBlock response, IGenLDAPConfiguration configuration)
    {
        super(request, response, configuration);
    }

    /**
     * @see  com.cordys.coe.ac.genericldap.soap.BaseMethod#execute()
     */
    @Override public void execute()
                           throws GenericLDAPConnectorException
    {
        IConnectionManager cm = getConfiguration().getConnectionManager();

        try
        {
            connection = cm.getConnection();

            if (prepare())
            {
                try
                {
                    commit();
                }
                catch (LDAPException e)
                {
                    throw new GenericLDAPConnectorException(e,
                                                            GenLDAPExceptionMessages.GLE_ERROR_COMMITTING_THE_TRANSACTION);
                }
            }
        }
        finally
        {
            if (connection != null)
            {
                cm.releaseConnection(connection);
            }
        }
    }

    /**
     * DOCUMENTME.
     *
     * @throws  LDAPException                  DOCUMENTME
     * @throws  GenericLDAPConnectorException  DOCUMENTME
     */
    private void commit()
                 throws LDAPException, GenericLDAPConnectorException
    {
        int i = 0;

        try
        {
            for (i = 0; i < updates.length; i++)
            {
                updates[i].commit();
            }

            if (LOG.isDebugEnabled())
            {
                LOG.debug("Response from LDAP Service is ...\n" +
                          Node.writeToString(Node.getRoot(getResponseXML()), true));
            }
        }
        catch (LDAPException e)
        {
            boolean rollbackSuccessfull = true;
            StringBuffer messages = new StringBuffer();

            for (int j = i - 1; j >= 0; j--)
            {
                try
                {
                    updates[j].rollback();
                }
                catch (LDAPException err)
                {
                    if (LOG.isWarningEnabled())
                    {
                        LOG.warn(e, GeneralMessages.UPDATE_ROLLBACK_FAILED_FOR_TUPLE,
                                 Node.writeToString(updates[j].tuple, true));
                    }

                    // log it, but continue
                    rollbackSuccessfull = false;
                    messages.append("Tuple rollback failed: ");
                    messages.append(err.getMessage());
                    messages.append(" ");
                    messages.append(err.getLDAPErrorMessage());
                }
            }

            if (rollbackSuccessfull)
            {
                throw e;
            }
            throw new GenericLDAPConnectorException(e,
                                                    GenLDAPExceptionMessages.GLE_ERROR_DURING_UPDATE_BUT_ROLLBACK_FAILED_REASON_,
                                                    messages.toString());
        }
    }

    /**
     * DOCUMENTME.
     *
     * @return  DOCUMENTME
     *
     * @throws  GenericLDAPConnectorException  In case of any exceptions.
     */
    private boolean prepare()
                     throws GenericLDAPConnectorException
    {
        int[] tuples = Find.match(requestInterface, "fChild<tuple>");
        updates = new LDAPUpdate[tuples.length];

        if (tuples.length > 0)
        {
            if (connection != null)
            {
                for (int i = 0; i < tuples.length; i++)
                {
                    updates[i] = new LDAPUpdate(tuples[i]);

                    int detail = updates[i].prepare(); // returns 0 if OK

                    if (detail != 0)
                    {
                        // something went wrong, return..
                        Node.appendToChildren(tuples[i], tuples[tuples.length - 1], detail);
                        return false;
                    }
                }
                // Append the all tuples if success.
                Node.appendToChildren(tuples[0], tuples[tuples.length - 1], getResponseXML());
                return true;
            }

            throw new GenericLDAPConnectorException(GenLDAPExceptionMessages.GLE_NO_ACTIVE_CONNECTION_TO_THE_LDAP);
        }
        return true;
    }

    /**
     * DOCUMENTME.
     *
     * @author  $author$
     */
    class LDAPUpdate
    {
        /**
         * DOCUMENTME.
         */
        LDAPConstraints constraints = new LDAPConstraints();

        /**
         * DOCUMENTME.
         */
        String dn;

        /**
         * DOCUMENTME.
         */
        Vector<LDAPModification> modifications = new Vector<LDAPModification>();
        /**
         * DOCUMENTME.
         */
        String proposedDn;
        /**
         * DOCUMENTME.
         */
        Vector<LDAPModification> rollBackModifications = new Vector<LDAPModification>();

        /**
         * DOCUMENTME.
         */
        private Vector<LDAPEntry> deletedEntriesList = new Vector<LDAPEntry>();
        /**
         * DOCUMENTME.
         */
        private boolean deleteRecursive = false;

        /**
         * DOCUMENTME.
         */
        private LDAPEntry ldapEntry;

        /**
         * DOCUMENTME.
         */
        private int original;

        /**
         * DOCUMENTME.
         */
        private int proposal;

        /**
         * DOCUMENTME.
         */
        private boolean rename = false;
        /**
         * DOCUMENTME.
         */
        private int tuple;

        /**
         * Creates a new LDAPUpdate object.
         *
         * @param  tuple  DOCUMENTME
         */
        LDAPUpdate(int tuple)
        {
            this.tuple = tuple;
            constraints.setTimeLimit(0);
        }

        /**
         * DOCUMENTME.
         *
         * @throws  LDAPException  DOCUMENTME
         */
        void commit()
             throws LDAPException
        {
            try
            {
                if (original == 0) // insert of entry
                {
                    if (LOG.isDebugEnabled())
                    {
                        LOG.debug("'" + dn + "' is being added to the LDAP server.");
                    }

                    connection.add(ldapEntry, constraints);
                }
                else if (proposal == 0) // delete of entry
                {
                    deleteEntry(dn);

                    deletedEntriesList.addElement(ldapEntry);
                }
                else
                // modification of entry
                {
                    if (rename)
                    {
                        DN newDN = DN.getDN(proposedDn);

                        connection.rename(dn, newDN.getRDN().toRFCString(),
                                          newDN.getParent().toRFCString(), true, constraints);

                        if (LOG.isDebugEnabled())
                        {
                            LOG.debug("Entry '" + dn + "' renamed to '" + proposedDn + "'.");
                        }
                    }
                    else
                    {
                        if (LOG.isDebugEnabled())
                        {
                            LOG.debug("Changes to '" + dn + "' are being saved.");
                        }

                        LDAPModification[] modification = new LDAPModification[modifications.size()];
                        modifications.copyInto(modification);
                        connection.modify(proposedDn, modification, constraints);
                    }
                }
            }
            catch (LDAPException e)
            {
                // No rollback needed here... Because the changing actions are 'atomic' for our
                // point of view. If something went wrong, it will not be done on the backend (as
                // long as the backend implemented a proper rollback itself)
                LOG.error(e, GeneralMessages.UPDATE_COMMIT_LEVEL_FAILED, dn);
                throw e;
            }
        }

        /**
         * DOCUMENTME.
         *
         * @return  DOCUMENTME
         *
         * @throws  GenericLDAPConnectorException  In case of any exceptions.
         */
        int prepare()
             throws GenericLDAPConnectorException
        {
            original = Find.firstMatch(tuple, "<tuple><old><entry>");
            proposal = Find.firstMatch(tuple, "<tuple><new><entry>");

            if ((original == 0) && (proposal == 0)) // invalid request
            {
                throw new GenericLDAPConnectorException(GenLDAPExceptionMessages.GLE_TUPLE_DID_NOT_CONTAIN_A_OLD_OR_A_NEW_TAG);
            }

            if (original == 0) // insert
            {
                dn = Node.getAttribute(proposal, "dn");

                if (!isValidDN(dn))
                {
                    throw new GenericLDAPConnectorException(GenLDAPExceptionMessages.THE_DN_IS_INVALID);
                }

                try
                {
                    LDAPEntry ldapEntry = connection.read(dn);

                    if (ldapEntry.getDN() != null)
                    {
                        String msg = "Object already exists";
                        Node.getDocument(tuple).createTextElement("error", msg, tuple);

                        int newNode = Node.getParent(proposal);
                        Node.delete(proposal, 0);
                        EntryToXML.appendToChildren(ldapEntry, newNode);

                        throw new GenericLDAPConnectorException(GenLDAPExceptionMessages.GLE_ALREADY_EXISTS_IN_LDAP,
                                                                dn);
                    }
                }
                catch (LDAPException le)
                {
                    // ignore
                }
                ldapEntry = XMLToEntry.getEntry(proposal);

                DN distinguishName = DN.getDN(dn);
                RDN rfcrdn = distinguishName.getRDN();
                String rfc = rfcrdn.toRFCString();
                int index = rfc.indexOf("=");

                if (index > -1)
                {
                    String type = rfc.substring(0, index); // extract here cn or o or ou
                    String ldapAttribute = LDAPUtil.getStringValue(ldapEntry, type, ""); // get the Values from LDAP
                    ldapAttribute = type + "=" + ldapAttribute;

                    if (!new RDN(ldapAttribute).equals(rfcrdn))
                    {
                        throw new GenericLDAPConnectorException(GenLDAPExceptionMessages.GLE_0_AND_1_DO_NOT_MATCH,
                                                                ldapAttribute, rfc);
                    }
                }
                else
                {
                    throw new GenericLDAPConnectorException(GenLDAPExceptionMessages.GLE_DN_AND_CN_DO_NOT_MATCH);
                }
            }
            else if (proposal == 0) // is is a delete
            {
                dn = Node.getAttribute(original, "dn");

                if (!isValidDN(dn))
                {
                    throw new GenericLDAPConnectorException(GenLDAPExceptionMessages.THE_DN_IS_INVALID);
                }

                try
                {
                    ldapEntry = connection.read(dn);
                }
                catch (LDAPException ldap)
                {
                    throw new GenericLDAPConnectorException(GenLDAPExceptionMessages.GLE_0_HAS_BEEN_DELETED,
                                                            dn);
                }

                LDAPEntry oldEntry = XMLToEntry.getEntry(original);

                DN distinguishName = DN.getDN(dn);
                RDN rfcrdn = distinguishName.getRDN();
                String rfc = rfcrdn.toRFCString();
                int index = rfc.indexOf("=");

                if (index > -1)
                {
                    String type = rfc.substring(0, index); // extract here cn or o or ou
                    String ldapAttribute = LDAPUtil.getStringValue(oldEntry, type, ""); // get the Values from LDAP
                    ldapAttribute = type + "=" + ldapAttribute;

                    if (!new RDN(ldapAttribute).equals(rfcrdn))
                    {
                        throw new GenericLDAPConnectorException(GenLDAPExceptionMessages.GLE_0_AND_1_DO_NOT_MATCH,
                                                                ldapAttribute, rfc);
                    }
                }
                else
                {
                    throw new GenericLDAPConnectorException(GenLDAPExceptionMessages.GLE_0_AND_1_DO_NOT_MATCH,
                                                            dn, rfc);
                }

                // Check current version in LDAP
                if (!optimisticLockCheck())
                {
                    // Delete the content of the <old> and insert a new version of this entry (if
                    // there is one)
                    if (ldapEntry != null)
                    {
                        int old = Node.getParent(original);
                        Node.delete(original, 0);
                        EntryToXML.appendToChildren(ldapEntry, old);
                    }

                    String msg = "Delete failed";
                    Node.getDocument(tuple).createTextElement("error", msg, tuple);

                    throw new GenericLDAPConnectorException(GenLDAPExceptionMessages.GLE_0_HAS_BEEN_MODIFIED_ON_THE_LDAP_SERVER,
                                                            dn);
                }

                deleteRecursive = Node.getAttribute(tuple, "recursive", "false").equals("true");
            }
            else
            // it is a modification
            {
                dn = Node.getAttribute(original, "dn");
                proposedDn = Node.getAttribute(proposal, "dn");

                if ((!isValidDN(dn)) || (!isValidDN(proposedDn)))
                {
                    throw new GenericLDAPConnectorException(GenLDAPExceptionMessages.THE_DN_IS_INVALID);
                }

                try
                {
                    ldapEntry = connection.read(dn); // the original one!
                }
                catch (LDAPException ldap)
                {
                    throw new GenericLDAPConnectorException(GenLDAPExceptionMessages.GLE_0_HAS_BEEN_DELETED,
                                                            dn);
                }

                if (!dn.equals(proposedDn))
                {
                    rename = true;

                    try
                    {
                        connection.read(proposedDn);
                        throw new GenericLDAPConnectorException(GenLDAPExceptionMessages.GLE_ALREADY_EXISTS_IN_LDAP,
                                                                proposedDn);
                    }
                    catch (LDAPException ldap)
                    {
                        // ignore, it should give this exception, otherwise the entry will exist
                    }
                }

                LDAPEntry newEntry = XMLToEntry.getEntry(proposal);
                DN distinguishName = DN.getDN(proposedDn);
                RDN rfcrdn = distinguishName.getRDN();
                String rfc = rfcrdn.toRFCString();
                int index = rfc.indexOf("=");
                String rdnName = "";

                if (index > -1)
                {
                    rdnName = rfc.substring(0, index); // extract here cn or o or ou

                    String ldapAttribute = LDAPUtil.getStringValue(newEntry, rdnName, ""); // get the Values from LDAP
                    ldapAttribute = rdnName + "=" + ldapAttribute;

                    if (!new RDN(ldapAttribute).equals(rfcrdn))
                    {
                        throw new GenericLDAPConnectorException(GenLDAPExceptionMessages.GLE_0_AND_1_DO_NOT_MATCH,
                                                                ldapAttribute, rfc);
                    }
                }
                else
                {
                    throw new GenericLDAPConnectorException(GenLDAPExceptionMessages.GLE_0_AND_1_DO_NOT_MATCH,
                                                            dn, rfc);
                }

                // Check current version in LDAP
                if (!optimisticLockCheck())
                {
                    // Delete the old <old> and insert a new version of this entry (if there is one)
                    if (ldapEntry != null)
                    {
                        int old = Node.getParent(original);
                        Node.delete(original, 0);

                        LDAPEntry proposedEntry = XMLToEntry.getEntry(proposal);
                        int newNode = Node.getParent(proposal);
                        Node.delete(proposal, 0);
                        EntryToXML.appendToChildren(ldapEntry, old);
                        EntryToXML.appendToChildren(proposedEntry, newNode);
                    }

                    // String msg = "'"+dn+"' has been modified on the LDAP server by another
                    // user.";

                    throw new GenericLDAPConnectorException(GenLDAPExceptionMessages.GLE_0_HAS_BEEN_MODIFIED_ON_THE_LDAP_SERVER,
                                                            dn);
                }

                if (!rename)
                {
                    // now create LDAP modification set
                    // Three versions of attribute-modification
                    // 1. delete of an attribute value
                    // 2. addition of an attribute value
                    // 3. change of some attribute values

                    LDAPAttribute[] newAttributeValues = XMLToEntry.getAttributes(proposal);

                    LDAPAttribute[] oldAttributeValues = XMLToEntry.getAttributes(original);

                    for (int i = 0; i < newAttributeValues.length; i++)
                    {
                        String attributeName = newAttributeValues[i].getName();

                        if (rename &&
                                (attributeName.equals(rdnName) ||
                                     attributeName.equals("distinguishedName")))
                        {
                            continue;
                        }

                        boolean foundInOld = false;

                        for (int j = 0; j < oldAttributeValues.length; j++)
                        {
                            if (oldAttributeValues[j].getName().equalsIgnoreCase(attributeName))
                            {
                                // check whether they are the same
                                if (!equalAttributes(newAttributeValues[i], oldAttributeValues[j]))
                                {
                                    if (LOG.isDebugEnabled())
                                    {
                                        LOG.debug("The '" + attributeName +
                                                  "' attribute has new values.");
                                    }

                                    // create a replace-modification
                                    modifications.addElement(new LDAPModification(LDAPModification.REPLACE,
                                                                                  newAttributeValues[i]));
                                    rollBackModifications.addElement(new LDAPModification(LDAPModification.REPLACE,
                                                                                          oldAttributeValues[j]));
                                }
                                else
                                {
                                    if (LOG.isDebugEnabled())
                                    {
                                        LOG.debug("The '" + attributeName +
                                                  "' attribute does not have new values.");
                                    }

                                    // if (Spy.active) Spy.send("LDAP", "The '"+attributeName+"'
                                    // attribute does not have new values.");
                                }
                                foundInOld = true;
                            }
                        }

                        if (!foundInOld)
                        {
                            // create a insert-modification

                            if (LOG.isDebugEnabled())
                            {
                                LOG.debug("The '" + attributeName + "' attribute will be added.");
                            }

                            // modificationSet.add(LDAPModification.ADD, newAttributeValues[i]);
                            modifications.addElement(new LDAPModification(LDAPModification.ADD,
                                                                          newAttributeValues[i]));
                            rollBackModifications.addElement(new LDAPModification(LDAPModification.DELETE,
                                                                                  newAttributeValues[i]));
                        }
                    }

                    for (int i = 0; i < oldAttributeValues.length; i++)
                    {
                        String attributeName = oldAttributeValues[i].getName();
                        boolean foundInNew = false;

                        for (int j = 0; j < newAttributeValues.length; j++)
                        {
                            if (newAttributeValues[j].getName().equalsIgnoreCase(attributeName))
                            {
                                // Just switch flag.
                                foundInNew = true;
                            }
                        }

                        if (!foundInNew)
                        {
                            if (LOG.isDebugEnabled())
                            {
                                LOG.debug("The '" + attributeName + "' attribute will be deleted.");
                            }

                            // create a delete-modification
                            modifications.addElement(new LDAPModification(LDAPModification.DELETE,
                                                                          oldAttributeValues[i]));
                            rollBackModifications.addElement(new LDAPModification(LDAPModification.ADD,
                                                                                  oldAttributeValues[i]));
                        }
                    }
                }
            }
            return 0;
        }

        /**
         * DOCUMENTME.
         *
         * @throws  LDAPException  DOCUMENTME
         */
        void rollback()
               throws LDAPException
        {
            try
            {
                if (original == 0) // insert of entry
                {
                    if (LOG.isDebugEnabled())
                    {
                        LOG.debug("Rolling back '" + dn + "' by removing it.");
                    }

                    connection.delete(ldapEntry.getDN(), constraints);
                }
                else if (proposal == 0) // delete of entry
                {
                    for (int i = deletedEntriesList.size() - 1; i >= 0; i--)
                    {
                        // try to restore as much as possible:
                        LDAPException err = null;
                        LDAPEntry entry = (LDAPEntry) deletedEntriesList.get(i);

                        if (LOG.isDebugEnabled())
                        {
                            LOG.debug("Rolling back '" + entry.getDN() + "' by adding it back.");
                        }

                        try
                        {
                            // This is known to fail in AD situations... :(
                            connection.add(entry, constraints);
                        }
                        catch (LDAPException e)
                        {
                            if (e.getResultCode() == 53) // WILL_NOT_PERFORM
                            {
                                // TODO: document this behaviour!?

                                // AD workaround remove the objectGUID and retry
                                LDAPAttributeSet attrSet = entry.getAttributeSet();

                                if (attrSet.getAttribute("objectGUID") != null)
                                {
                                    attrSet.remove(attrSet.getAttribute("objectGUID"));
                                }

                                try
                                {
                                    connection.add(entry, constraints);
                                }
                                catch (LDAPException e2)
                                {
                                    if (err == null)
                                    {
                                        err = e2;
                                    }
                                }
                            }

                            // set this to e, but try to recover the other ones too... continuation
                            // has a meaning because not all previous elements should be child
                            // elements of this element..
                            if (err == null)
                            {
                                err = e;
                            }
                        }

                        if (err != null)
                        {
                            throw err;
                        }
                    }
                }
                else
                // modification of entry
                {
                    if (rename)
                    {
                        DN oldDN = DN.getDN(dn);

                        connection.rename(proposedDn, oldDN.getRDN().toRFCString(),
                                          oldDN.getParent().toRFCString(), true, constraints);

                        if (LOG.isDebugEnabled())
                        {
                            LOG.debug("Entry '" + proposedDn + "' renamed to '" + dn + "'.");
                        }
                    }
                    else
                    {
                        if (LOG.isDebugEnabled())
                        {
                            LOG.debug("Changes to '" + dn + "' are being rolled back.");
                        }

                        LDAPModification[] modification = new LDAPModification[rollBackModifications
                                                              .size()];
                        rollBackModifications.copyInto(modification);
                        connection.modify(proposedDn, modification, constraints);
                    }
                }
            }
            catch (LDAPException e)
            {
                // No rollback needed here...
                LOG.error(e, GeneralMessages.UPDATE_ROLLBACK_FAILED_FOR_DN, dn);

                throw e;
            }
        }

        /**
         * Deletes all the entries recursive... If something goes wrong... Stop deleting... Calling
         * function is responsible for rollback!
         *
         * @param   dn
         *
         * @throws  LDAPException
         */
        private void deleteEntry(String dn)
                          throws LDAPException
        {
            if (deleteRecursive)
            {
                LDAPSearchResults results = connection.search(dn, LDAPConnection.SCOPE_ONE,
                                                              "objectclass=*", null, false);

                while (results.hasMore())
                {
                    LDAPEntry entry = results.next();

                    deleteEntry(entry.getDN());

                    deletedEntriesList.addElement(entry);
                }
            }

            if (LOG.isDebugEnabled())
            {
                LOG.debug("'" + dn + "' is being deleted from the LDAP server.");
            }

            connection.delete(dn, constraints);
        }

        /**
         * DOCUMENTME.
         *
         * @param   newAttribute  DOCUMENTME
         * @param   oldAttribute  DOCUMENTME
         *
         * @return  DOCUMENTME
         */
        private boolean equalAttributes(LDAPAttribute newAttribute, LDAPAttribute oldAttribute)
        {
            if (LOG.isDebugEnabled())
            {
                LOG.debug("Comparing '" + newAttribute + "' to '" + oldAttribute + "'");
            }

            if (newAttribute.getStringValueArray().length !=
                    oldAttribute.getStringValueArray().length)
            {
                return false;
            }

            Enumeration<?> newValues = StringSorter.sort(newAttribute.getStringValues());
            Enumeration<?> oldValues = StringSorter.sort(oldAttribute.getStringValues());

            while (newValues.hasMoreElements()) // also old values will have more elements because
                                                // they have same size
            {
                String newValue = (String) newValues.nextElement();
                String oldValue = (String) oldValues.nextElement();

                if (!newValue.equals(oldValue))
                {
                    return false;
                }
            }

            return true;
        }

        /**
         * Checks if the DN is valid. This is a more relaxed version of the BCP method, because e.g.
         * &amp; characters are allowed in Dn's. TODO Describe the method.
         *
         * @param   sDn
         *
         * @return
         */
        private boolean isValidDN(String sDn)
        {
            if (sDn == null)
            {
                return false;
            }

            for (int i = 0; i < sDn.length(); i++)
            {
                char currentChar = sDn.charAt(i);

                switch (currentChar)
                {
                    case '\\':
                        return false;

                    case '/':
                        return false;

                    case ';':
                        return false;

                    case '"':
                        return false;

                    case '\'':
                        return false;

                    case '<':
                        return false;

                    case '>':
                        return false;

                    case '#':
                        return false;

                    case '+':
                        return false;
                }
            }

            return DN.isDN(dn);
        }

        /**
         * Executed to determine whether the user has a valid copy.
         *
         * @return  DOCUMENTME
         *
         * @throws  GenericLDAPConnectorException  In case of any exceptions.
         */
        private boolean optimisticLockCheck()
                                     throws GenericLDAPConnectorException
        {
            if (LOG.isDebugEnabled())
            {
                LOG.debug("Starting the optimistic-lock check.");
            }

            try
            {
                ldapEntry = connection.read(dn);

                LDAPAttribute[] clientAttributeValues = XMLToEntry.getAttributes(original);
                LDAPAttributeSet serverAttributeValues = ldapEntry.getAttributeSet();

                for (int i = 0; i < clientAttributeValues.length; i++)
                {
                    String attrLDAPName = clientAttributeValues[i].getName();

                    if (LOG.isDebugEnabled())
                    {
                        LOG.debug("Comparing the '" + attrLDAPName + "' attribute.");
                    }

                    LDAPAttribute serverAttribute = serverAttributeValues.getAttribute(attrLDAPName);

                    if (serverAttribute == null)
                    {
                        if (LOG.isDebugEnabled())
                        {
                            LOG.debug("Optimistic locking failed for '" + dn + "', because the '" +
                                      attrLDAPName + "' attribute has been modifed.");
                        }

                        throw new GenericLDAPConnectorException(GenLDAPExceptionMessages.GLE_0_OF_1_HAS_BEEN_DELETED_ON_THE_LDAP_SERVER,
                                                                attrLDAPName, dn);
                    }

                    if (!equalAttributes(clientAttributeValues[i], serverAttribute))
                    {
                        if (LOG.isDebugEnabled())
                        {
                            LOG.debug("Optimistic locking failed for '" + dn + "', because the '" +
                                      attrLDAPName + "' attribute has been modifed.");
                        }

                        throw new GenericLDAPConnectorException(GenLDAPExceptionMessages.GLE_0_OF_1_HAS_BEEN_MODIFIED_ON_THE_LDAP_SERVER,
                                                                attrLDAPName, dn);
                    }
                }

                if (LOG.isDebugEnabled())
                {
                    LOG.debug("'" + dn + "' has been modifed on the LDAP server.");
                }

                return true;
            }
            catch (LDAPException le) // Could not read the entry
            {
                if (le.getResultCode() == LDAPException.NO_SUCH_OBJECT)
                {
                    if (LOG.isDebugEnabled())
                    {
                        LOG.debug("Optimistic locking failed for '" + dn +
                                  "', because it has been deleted.", le);
                    }

                    throw new GenericLDAPConnectorException(GenLDAPExceptionMessages.GLE_0_HAS_BEEN_DELETED,
                                                            dn);
                }

                if (LOG.isDebugEnabled())
                {
                    LOG.debug("Optimistic locking failed for '" + dn +
                              "', because an LDAP exception occurred.", le);
                }

                throw new GenericLDAPConnectorException(le,
                                                        GenLDAPExceptionMessages.GLE_OBJECT_IS_CHANGED_BY_OTHER_USER_IN_LDAP);
            }
        }
    }
}
