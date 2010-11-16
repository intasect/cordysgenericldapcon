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
