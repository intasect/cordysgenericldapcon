package com.cordys.coe.ac.genericldap.soap;

import com.cordys.coe.ac.genericldap.GenLDAPConstants;
import com.cordys.coe.ac.genericldap.config.IGenLDAPConfiguration;
import com.cordys.coe.ac.genericldap.exception.GenericLDAPConnectorException;

import com.eibus.soap.BodyBlock;

import com.eibus.xml.xpath.XPathMetaInfo;

/**
 * This class is the base for all SOAP methods.
 *
 * @author  pgussow
 */
public abstract class BaseMethod
{
    /**
     * Holds the configuration for the connector.
     */
    private IGenLDAPConfiguration m_configuration;
    /**
     * Holds the incoming request.
     */
    private BodyBlock m_request;
    /**
     * Holds the outgoing response.
     */
    private BodyBlock m_response;
    /**
     * Holds the XPathMetaInfo object to use.
     */
    private XPathMetaInfo m_xmi;

    /**
     * Constructor.
     *
     * @param  request        The incoming request.
     * @param  response       The outgoing response.
     * @param  configuration  The configuration.
     */
    public BaseMethod(BodyBlock request, BodyBlock response, IGenLDAPConfiguration configuration)
    {
        m_request = request;
        m_response = response;
        m_configuration = configuration;

        m_xmi = new XPathMetaInfo();
        m_xmi.addNamespaceBinding("impl", GenLDAPConstants.NS_METHODS_1_2_IMPL);
        m_xmi.addNamespaceBinding("ns", GenLDAPConstants.NS_METHODS_1_2);
    }
    
    /**
     * This method returns the implementation XML for the method.
     *  
     * @return The method being executed.
     */
    protected int getImplementationXML()
	{
		return getRequest().getMethodDefinition().getImplementation();
	}

    /**
     * This method is called to actually execute the method.
     *
     * @throws  GenericLDAPConnectorException  In case of any exceptions.
     */
    public abstract void execute()
                          throws GenericLDAPConnectorException;

    /**
     * This method gets the configuration.
     *
     * @return  The configuration.
     */
    public IGenLDAPConfiguration getConfiguration()
    {
        return m_configuration;
    }

    /**
     * This method gets the incoming request.
     *
     * @return  The incoming request.
     */
    public BodyBlock getRequest()
    {
        return m_request;
    }

    /**
     * This method gets the incoming request XML.
     *
     * @return  The incoming request XML.
     */
    public int getRequestXML()
    {
        return m_request.getXMLNode();
    }

    /**
     * This method gets the outgoing response.
     *
     * @return  The outgoing response.
     */
    public BodyBlock getResponse()
    {
        return m_response;
    }

    /**
     * This method gets the outgoing response XML.
     *
     * @return  The outgoing response XML.
     */
    public int getResponseXML()
    {
        return m_response.getXMLNode();
    }

    /**
     * This method gets the XPathMetaInfo object.
     *
     * @return  The XPathMetaInfo object.
     */
    public XPathMetaInfo getXPathMetaInfo()
    {
        return m_xmi;
    }
}
