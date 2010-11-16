package com.cordys.coe.ac.genericldap.soap.impl;

import com.cordys.coe.ac.genericldap.EDynamicAction;
import com.cordys.coe.ac.genericldap.GenLDAPConstants;
import com.cordys.coe.ac.genericldap.exception.GenericLDAPConnectorException;
import com.cordys.coe.ac.genericldap.localization.GenLDAPExceptionMessages;
import com.cordys.coe.util.xml.nom.XPathHelper;

import com.eibus.soap.MethodDefinition;

import com.eibus.util.logger.CordysLogger;

import com.eibus.xml.xpath.XPathMetaInfo;

import java.lang.reflect.Constructor;

/**
 * This class creates the proper implementation objects based on the given Method Definition.
 *
 * @author  pgussow
 */
public class ImplementationFactory
{
    /**
     * Holds the logger to use.
     */
    private static final CordysLogger LOG = CordysLogger.getCordysLogger(ImplementationFactory.class);

    /**
     * This method creates the proper implementation based on the method definition.
     *
     * @param   md  The definition of the method.
     *
     * @return  The created implementation object.
     *
     * @throws  GenericLDAPConnectorException  In case of any exceptions.
     */
    public static BaseImplementation createImplementation(MethodDefinition md)
                                                   throws GenericLDAPConnectorException
    {
        BaseImplementation returnValue = null;

        // It's the new mode.
        String action = getActionFromImplementation(md);
        EDynamicAction daAction = EDynamicAction.valueOf(action.toUpperCase());

        Class<? extends BaseImplementation> cClass = daAction.getImplementationClass();

        if (LOG.isDebugEnabled())
        {
            LOG.debug("Instantiating class " + daAction.getImplementationClass());
        }

        try
        {
            Constructor<? extends BaseImplementation> cConstructor = cClass.getConstructor(int.class);

            returnValue = cConstructor.newInstance(md.getImplementation());
        }
        catch (Exception e)
        {
            throw new GenericLDAPConnectorException(e,
                                                    GenLDAPExceptionMessages.GLE_ERROR_CREATING_IMPLEMENTATION_CLASS);
        }

        return returnValue;
    }

    /**
     * This method gets the desired action from the method's implementation.
     *
     * @param   md  The definition of the request.
     *
     * @return  The action in the implementation.
     *
     * @throws  GenericLDAPConnectorException  In case the action was not found.
     */
    public static String getActionFromImplementation(MethodDefinition md)
                                              throws GenericLDAPConnectorException
    {
        String sAction;

        int iImplNode = md.getImplementation();

        XPathMetaInfo xmi = new XPathMetaInfo();
        xmi.addNamespaceBinding("ns", GenLDAPConstants.NS_METHODS_1_2_IMPL);

        sAction = XPathHelper.getStringValue(iImplNode, "./ns:*/@action", xmi, "");

        return sAction;
    }
}
