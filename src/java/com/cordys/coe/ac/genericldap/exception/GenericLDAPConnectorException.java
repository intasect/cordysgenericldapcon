/**
 * © 2004 Cordys R&D B.V. All rights reserved. The computer program(s) is the
 * proprietary information of Cordys R&D B.V. and provided under the relevant
 * License Agreement containing restrictions on use and disclosure. Use is
 * subject to the License Agreement.
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
