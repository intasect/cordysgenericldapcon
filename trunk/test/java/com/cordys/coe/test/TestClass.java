package com.cordys.coe.test;

import com.cordys.coe.ac.genericldap.GenLDAPConstants;
import com.cordys.coe.util.xml.nom.XPathHelper;
import com.eibus.xml.nom.Document;
import com.eibus.xml.nom.Node;
import com.eibus.xml.xpath.XPathMetaInfo;

public class TestClass
{
	/**
	 * Main method.
	 *
	 * @param saArguments Commandline arguments.
	 */
	public static void main(String[] saArguments)
	{
		try
		{
			Document doc = new Document();
			int implNode = doc.parseString("<implementation type=\"GENLDAP\">" +
					"<SearchLDAP xmlns=\"http://genldap.coe.cordys.com/1.2/methods/implementation\" action=\"SeadddrchLDAP\">" +
					"<dn>:dn</dn></SearchLDAP></implementation>");
			XPathMetaInfo xmi = new XPathMetaInfo();
	        xmi.addNamespaceBinding("ns", GenLDAPConstants.NS_METHODS_1_2_IMPL);

	        String action = XPathHelper.getStringValue(implNode, "./ns:*/@action", xmi, "");
	        
	        System.out.println(action);
	        
	        Node.createTextElement("test", "(&(mail=peter.fuchs.ext@siemens.com)(|(status=A)(!(status=*)))(|(recordType=H)(!(recordType=*))))'<mailto:mail=peter.fuchs.ext@siemens.com)(|(status=A)(!(status=*)))(|(recordType=H)(!(recordType=*))))", implNode);
	       
	        System.out.println(Node.writeToString(implNode, true));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
