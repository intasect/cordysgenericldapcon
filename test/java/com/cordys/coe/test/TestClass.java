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
