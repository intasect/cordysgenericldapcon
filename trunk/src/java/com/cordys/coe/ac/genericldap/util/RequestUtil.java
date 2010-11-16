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
