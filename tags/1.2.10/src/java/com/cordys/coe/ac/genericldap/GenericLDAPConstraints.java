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

import com.novell.ldap.LDAPControl;
import com.novell.ldap.LDAPSearchConstraints;

/**
 * Information about Activedirectory constraints can be found at:
 *
 * @author  gvdkolk
 * @see     http://msdn2.microsoft.com/en-us/library/ms684291.aspx
 */
public class GenericLDAPConstraints
{
    /**
     * Holds the Active Directory object id for showing deleted objects.
     */
    public static final String LDAP_SERVER_SHOW_DELETED_OID = "1.2.840.113556.1.4.417";

    /**
     * This method creates additional LDAP constraints.
     *
     * @param   lc          The LDAP constraints to enrich.
     * @param   constraint  The name of the constratint to add.
     *
     * @return  The passed on constraint, but enriched if needed.
     */
    public static LDAPSearchConstraints addConstraints(LDAPSearchConstraints lc, String constraint)
    {
        if (constraint.equals("LDAP_SERVER_SHOW_DELETED_OID"))
        {
            lc = addDeletedObjectsControl(lc);
        }

        return lc;
    }

    /**
     * DOCUMENTME.
     *
     * @param   lc  DOCUMENTME
     *
     * @return  DOCUMENTME
     */
    private static LDAPSearchConstraints addDeletedObjectsControl(LDAPSearchConstraints lc)
    {
        LDAPControl[] newControls = null;

        // create a longer array
        if (lc.getControls() == null)
        {
            newControls = new LDAPControl[1];
        }
        else
        {
            newControls = new LDAPControl[lc.getControls().length + 1];

            // copy existing elements in new array
            for (int i = 0; i < lc.getControls().length; i++)
            {
                newControls[i] = lc.getControls()[i];
            }
        }

        // add new element
        newControls[newControls.length - 1] = getShowDeletedObjectsControl();

        // set the new controls in LDAPConstraints and return
        lc.setControls(newControls);

        return lc;
    }

    /**
     * This method returns the LDAPControl to also show the deleted objects.
     *
     * @return  The LDAP Control to show the deleted objects as well.
     */
    private static LDAPControl getShowDeletedObjectsControl()
    {
        return new LDAPControl(LDAP_SERVER_SHOW_DELETED_OID, false, null);
    }
}
