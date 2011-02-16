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
package com.cordys.coe.ac.genericldap.exception;

import com.cordys.coe.exception.ServerLocalizableException;

import com.eibus.localization.IStringResource;

/**
 * General Exception class for the GenericLDAPConnector.
 */
public class GenericLDAPConnectorException extends ServerLocalizableException
{
    /**
     * Creates a new GenericLDAPConnectorException object.
     *
     * @param  srMessage     The localizable message.
     * @param  aoParameters  The list of parameters for the localizeable message.
     */
    public GenericLDAPConnectorException(IStringResource srMessage, Object... aoParameters)
    {
        super(srMessage, aoParameters);
    }

    /**
     * Creates a new GenericLDAPConnectorException object.
     *
     * @param  sFaultActor   The actor for the current fault.
     * @param  srMessage     The localizable message.
     * @param  aoParameters  The list of parameters for the localizeable message.
     */
    public GenericLDAPConnectorException(String sFaultActor, IStringResource srMessage,
                                         Object... aoParameters)
    {
        super(sFaultActor, srMessage, aoParameters);
    }

    /**
     * Creates a new GenericLDAPConnectorException object.
     *
     * @param  tCause        The exception that caused this exception.
     * @param  srMessage     The localizable message.
     * @param  aoParameters  The list of parameters for the localizeable message.
     */
    public GenericLDAPConnectorException(Throwable tCause, IStringResource srMessage,
                                         Object... aoParameters)
    {
        super(tCause, srMessage, aoParameters);
    }

    /**
     * Creates a new GenericLDAPConnectorException object.
     *
     * @param  tCause        The exception that caused this exception.
     * @param  sFaultActor   The actor for the current fault.
     * @param  srMessage     The localizable message.
     * @param  aoParameters  The list of parameters for the localizeable message.
     */
    public GenericLDAPConnectorException(Throwable tCause, String sFaultActor,
                                         IStringResource srMessage, Object... aoParameters)
    {
        super(tCause, sFaultActor, srMessage, aoParameters);
    }

    /**
     * Creates a new GenericLDAPConnectorException object.
     *
     * @param  tCause             The exception that caused this exception.
     * @param  plPreferredLocale  The preferred locale for this exception. It defaults to the SOAP
     *                            locale.
     * @param  sFaultActor        The actor for the current fault.
     * @param  srMessage          The localizable message.
     * @param  aoParameters       The list of parameters for the localizeable message.
     */
    public GenericLDAPConnectorException(Throwable tCause, PreferredLocale plPreferredLocale,
                                         String sFaultActor, IStringResource srMessage,
                                         Object... aoParameters)
    {
        super(tCause, plPreferredLocale, sFaultActor, srMessage, aoParameters);
    }
}
