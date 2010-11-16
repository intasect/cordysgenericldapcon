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
package com.cordys.coe.ac.genericldap.util;

import com.eibus.xml.nom.Find;
import com.eibus.xml.nom.Node;

/**
 * Util class for request parameters.
 *
 * @author  pgussow
 */
public class RequestUtil
{
    /**
     * Generic function to check th eend result of a outgoing soap call.
     *
     * @param   responseBody  The xml containing the response
     *
     * @throws  Exception
     */
    public static void checkSoapResponseforError(int responseBody)
                                          throws Exception
    {
        int methodResult = Node.getFirstChild(responseBody);

        if (methodResult == 0)
        {
            throw new Exception("Backend failure: no response data");
        }

        if (Node.getLocalName(methodResult).equals("Fault")) // SOAP Fault (e.g. Server.Error)
        {
            throw new Exception("Backend Exception: " + Node.writeToString(methodResult, true));
        }
    }

    /**
     * Helper function to get values from the input xml structure.
     *
     * @param   requestNode   DOCUMENTME
     * @param   tagname       DOCUMENTME
     * @param   defaultValue  DOCUMENTME
     *
     * @return  DOCUMENTME
     */
    public static String getValueFromRequest(int requestNode, String tagname, String defaultValue)
    {
        int theNode = Find.firstMatch(requestNode, "?<" + tagname + ">");
        return (theNode == 0) ? defaultValue : Node.getData(theNode);
    }
}
