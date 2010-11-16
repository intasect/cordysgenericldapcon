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
package com.cordys.coe.ac.genericldap;

import com.cordys.coe.ac.genericldap.soap.impl.BaseImplementation;
import com.cordys.coe.ac.genericldap.soap.impl.GetLDAPObjectImpl;
import com.cordys.coe.ac.genericldap.soap.impl.SearchLDAPImpl;
import com.cordys.coe.ac.genericldap.soap.impl.UpdateImpl;

/**
 * This enum identifies the different methods that can be executed.
 *
 * @author  pgussow
 */
public enum EDynamicAction
{
    SEARCH_LDAP(SearchLDAPImpl.class),
    GET_LDAP_OBJECT(GetLDAPObjectImpl.class),
    UPDATE(UpdateImpl.class);

    /**
     * Holds the implementation class for the method.
     */
    private Class<? extends BaseImplementation> m_cImplClass;

    /**
     * Constructor. Creates the action definition.
     *
     * @param  cImplClass  The implementation class for this method.
     */
    EDynamicAction(Class<? extends BaseImplementation> cImplClass)
    {
        m_cImplClass = cImplClass;
    }

    /**
     * This method gets the implementation class to use.
     *
     * @return  The implementation class to use.
     */
    public Class<? extends BaseImplementation> getImplementationClass()
    {
        return m_cImplClass;
    }
}
