package com.cordys.coe.ac.genericldap.soap.impl;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

import com.cordys.coe.ac.genericldap.exception.GenericLDAPConnectorException;
import com.cordys.coe.ac.genericldap.localization.GenLDAPExceptionMessages;
import com.cordys.coe.ac.genericldap.localization.GeneralMessages;
import com.cordys.coe.ac.genericldap.soap.BaseMethod;
import com.cordys.coe.util.xml.nom.XPathHelper;
import com.eibus.directory.soap.DN;
import com.eibus.directory.soap.EntryToXML;
import com.eibus.directory.soap.LDAPUtil;
import com.eibus.directory.soap.RDN;
import com.eibus.directory.soap.XMLToEntry;
import com.eibus.localization.IStringResource;
import com.eibus.localization.StringFormatter;
import com.eibus.soap.SOAPTransaction;
import com.eibus.util.StringSorter;
import com.eibus.util.logger.CordysLogger;
import com.eibus.util.system.EIBProperties;
import com.eibus.xml.nom.Node;
import com.eibus.xml.xpath.XPathMetaInfo;
import com.novell.ldap.LDAPAttribute;
import com.novell.ldap.LDAPAttributeSet;
import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPConstraints;
import com.novell.ldap.LDAPEntry;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPModification;
import com.novell.ldap.LDAPSearchResults;

/**
 * This class wraps the actual LDAP modification. It takes care of the optimistic locking
 * @author Administrator
 *
 */
class LDAPUpdateWrapper 
{
	/**
	 * Holds the logger to use.
	 */
	private static final CordysLogger LOG = CordysLogger.getCordysLogger(LDAPUpdateWrapper.class);
	/**
     * Holds the LDAP constraints for this operation.
     */
    private LDAPConstraints constraints = new LDAPConstraints();
    /**
     * Holds the DN of the entry that needs to be modified.
     */
    private String dn;
    /**
     * Holds the actual modifications that need to be done.
     */
    private List<LDAPModification> modifications = new ArrayList<LDAPModification>();
    /**
     * Holds the new DN for the entry (in case of a rename).
     */
    private String proposedDn;
    /**
     * Holds the modifications that need to be done in order to roll back the changes.
     */
    private List<LDAPModification> rollBackModifications = new ArrayList<LDAPModification>();
    /**
     * Holds the list of entries that have been deleted in this operation.
     */
    private List<LDAPEntry> deletedEntriesList = new ArrayList<LDAPEntry>();
    /**
     * Holds whether or not the entries should be deleted recursively.
     */
    private boolean deleteRecursive = false;
    /**
     * Holds the entry as it should be stored in LDAP.
     */
    private LDAPEntry ldapEntry;
    /**
     * Holds the original version of the entry (thus the 'old' part).
     */
    private int original;
    /**
     * Holds the final version for the entry (thus the 'new' part).
     */
    private int proposal;
    /**
     * Holds whether or not the entry was renamed.
     */
    private boolean rename = false;
    /**
     * Holds the complete current tuple.
     */
    private int tuple;
    /**
     * Holds the locale that should be used for messages.
     */
	private Locale locale;

    /**
     * Creates a new LDAPUpdate object.
     *
     * @param  tuple  The tuple that needs to be updated.
     */
    LDAPUpdateWrapper(int tuple)
    {
        this.tuple = tuple;
        
        constraints.setTimeLimit(0);
        
        locale = EIBProperties.get_ManagementLocale();
    	try
    	{
    		locale = SOAPTransaction.getCurrentSOAPTransaction().getLocale();
    	}
    	catch(Exception ignored)
    	{
    		if (LOG.isDebugEnabled())
    		{
    			LOG.debug("Could not get the locale from the SOAP request, so using the management locale: " + locale.toString(), ignored);
    		}
    	}
    }

    /**
     * This method commits the actual changes to the LDAP server.
     * 
     * @param connection The LDAP connection that should be used.
     *
     * @throws  LDAPException  In case of any exceptions
     */
    void commit(LDAPConnection connection)
         throws LDAPException
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
            deleteEntry(dn, connection);

            deletedEntriesList.add(ldapEntry);
        }
        else
        {
            // modification of entry
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

                LDAPModification[] modification = modifications.toArray(new LDAPModification[0]);
                connection.modify(proposedDn, modification, constraints);
            }
        }
    }
    
    /**
     * This method returns the properly localized version of the given message.
     * 
     * @param sr The string resource to render.
     * @param parameters The parameters for the message.
     * 
     * @return The message in the locale of the current SOAP request.
     */
    private String getLocalizedMessage(IStringResource sr, Object... parameters)
    {
    	return StringFormatter.format(locale, sr, parameters);
    }

    /**
     * This method prepares the update object. First it will determine what sort of operation 
     * this is (insert, update or delete). Then it will validate in the LDAP backend that the 
     * entries exist and whether or not the target already exists.
     * 
     * @param connection  The LDAP connection to use.
     * @param method      The actual request.
     *
     * @throws  GenericLDAPConnectorException  In case of any exceptions.
     */
    void prepare(LDAPConnection connection, BaseMethod method)
         throws GenericLDAPConnectorException
    {
    	XPathMetaInfo xmi = method.getXPathMetaInfo();
        original = XPathHelper.selectSingleNode(tuple, "ns:old/ns:entry", xmi);
        proposal = XPathHelper.selectSingleNode(tuple, "ns:new/ns:entry", xmi);

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
                    Node.createElementWithParentNS("error", getLocalizedMessage(GeneralMessages.OBJECT_ALREADY_EXISTS), tuple);

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
            if (!optimisticLockCheck(connection))
            {
                // Delete the content of the <old> and insert a new version of this entry (if
                // there is one)
                if (ldapEntry != null)
                {
                    int old = Node.getParent(original);
                    Node.delete(original, 0);
                    EntryToXML.appendToChildren(ldapEntry, old);
                }

                Node.getDocument(tuple).createTextElement("error", getLocalizedMessage(GeneralMessages.DELETE_FAILED), tuple);

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
            if (!optimisticLockCheck(connection))
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
                                modifications.add(new LDAPModification(LDAPModification.REPLACE,
                                                                              newAttributeValues[i]));
                                rollBackModifications.add(new LDAPModification(LDAPModification.REPLACE,
                                                                                      oldAttributeValues[j]));
                            }
                            else
                            {
                                if (LOG.isDebugEnabled())
                                {
                                    LOG.debug("The '" + attributeName +
                                              "' attribute does not have new values.");
                                }
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

                        modifications.add(new LDAPModification(LDAPModification.ADD,
                                                                      newAttributeValues[i]));
                        rollBackModifications.add(new LDAPModification(LDAPModification.DELETE,
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
                        modifications.add(new LDAPModification(LDAPModification.DELETE,
                                                                      oldAttributeValues[i]));
                        rollBackModifications.add(new LDAPModification(LDAPModification.ADD,
                                                                              oldAttributeValues[i]));
                    }
                }
            }
        }
    }

    /**
     * This method tries to roll back all the current changes.
     * @param connection  The LDAP connection to use.
     *
     * @throws  LDAPException  In case of any exceptions.
     */
    void rollback(LDAPConnection connection)
           throws LDAPException
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

                    LDAPModification[] modification = rollBackModifications.toArray(new LDAPModification[0]);
                    connection.modify(proposedDn, modification, constraints);
                }
            }
        
    }

    /**
     * Deletes all the entries recursive... If something goes wrong... Stop deleting... Calling
     * function is responsible for rollback!
     *
     * @param dn  The DN of the entry that should be deleted.
     * @param connection  The LDAP connection that should be used.
     *
     * @throws  LDAPException In case of any exceptions.
     */
    private void deleteEntry(String dn, LDAPConnection connection)
                      throws LDAPException
    {
        if (deleteRecursive)
        {
            LDAPSearchResults results = connection.search(dn, LDAPConnection.SCOPE_ONE,
                                                          "objectclass=*", null, false);

            while (results.hasMore())
            {
                LDAPEntry entry = results.next();

                deleteEntry(entry.getDN(), connection);

                deletedEntriesList.add(entry);
            }
        }

        if (LOG.isDebugEnabled())
        {
            LOG.debug("'" + dn + "' is being deleted from the LDAP server.");
        }

        connection.delete(dn, constraints);
    }

    /**
     * This method compares the 2 attributes to check if their values are equal to eachother.
     *
     * @param   newAttribute  The new attribute.
     * @param   oldAttribute  The old attribute.
     *
     * @return  true if the attributes are the same. Otherwise false.
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
     * @param   dn The DN to check.
     *
     * @return true if the DN is valid. Otherwise false.
     */
    private boolean isValidDN(String dn)
    {
        if (dn == null)
        {
            return false;
        }

        for (int i = 0; i < dn.length(); i++)
        {
            char currentChar = dn.charAt(i);

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
     * @param connection The connection to use.
     *
     * @return  true if the old still matches the actual object from the LDAP server.
     *
     * @throws  GenericLDAPConnectorException  In case of any exceptions.
     */
    private boolean optimisticLockCheck(LDAPConnection connection)
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

    /**
     * This method returns the source tuple XML for this entry.
     * 
     * @return The source tuple XML for this entry.
     */
	public int getTuple() 
	{
		return tuple;
	}
}
