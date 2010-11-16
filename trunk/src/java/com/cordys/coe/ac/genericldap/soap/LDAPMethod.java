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
