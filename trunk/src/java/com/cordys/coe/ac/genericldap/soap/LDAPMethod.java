package com.cordys.coe.ac.genericldap.soap;

import com.cordys.coe.ac.genericldap.config.IGenLDAPConfiguration;
import com.cordys.coe.ac.genericldap.exception.GenericLDAPConnectorException;
import com.cordys.coe.ac.genericldap.soap.impl.BaseImplementation;
import com.cordys.coe.ac.genericldap.soap.impl.ImplementationCache;

import com.eibus.soap.BodyBlock;

/**
 * This method handles method calls new style.
 *
 * @author  pgussow
 */
public class LDAPMethod extends BaseMethod
{
    /**
     * Constructor.
     *
     * @param  request        The incoming request.
     * @param  response       The outgoing response.
     * @param  configuration  The configuration.
     */
    public LDAPMethod(BodyBlock request, BodyBlock response, IGenLDAPConfiguration configuration)
    {
        super(request, response, configuration);
    }

    /**
     * This method is called to actually execute the method.
     *
     * @throws  GenericLDAPConnectorException  In case of any exceptions.
     *
     * @see     com.cordys.coe.ac.genericldap.soap.BaseMethod#execute()
     */
    @Override public void execute()
                           throws GenericLDAPConnectorException
    {
        BaseImplementation impl = ImplementationCache.getImplementation(getRequest()
                                                                        .getMethodDefinition());
        impl.handleRequest(this);
    }
}
