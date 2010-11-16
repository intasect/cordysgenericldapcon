/**
 * Indicates whether or not the page has fully loaded. Only when the page has fully loaded the
 * rowSelects are allowed to do anything.
 */
var g_bInitialized = false;
/**
 * Global XML document to be able to feed dummy data to the models.
 */
var g_xmlDoc = new ActiveXObject("Microsoft.XMLDOM");
/**
 * Holds the lib to do the password encoding.
 */
var libPassword = null;
/**
 * Holds the lib to do XML manipulations.
 */
var libXMLUtil = null;
/**
 * Holds the configuration namespace.
 */
var NS_CONFIGURATION = "http://genldap.coe.cordys.com/1.2/configuration";
/**
 * Holds the default namespace prefix mapping for the XML.
 */
var NAMESPACE_PREFIXES = "xmlns:ns='" + NS_CONFIGURATION + "'";

/**
 * This method is called when the form is loaded. It will initialize 
 * the screen and insert a dummy XML in the current model to make sure 
 * all tags are created properly.
 *  
 * @param eventObject The event that occurred.
 */
function handleInitDone(eventObject)
{
	if (DEBUG_USERS && DEBUG_USERS.indexOf(system.getUser().name) > -1)
	{
		debugger;
	}
	else
	{
		btnTestSave.hide();
	}
	
    //Create the libPassword object
    libPassword = window.document.createElement("SPAN");
    libPassword.id = "libPassword";
    
    //Attach the base64 library.
    application.addLibrary("/cordys/wcp/admin/library/base64encoder.htm", libPassword);
    
	//Mandatory initialization of the applicationconnector.js
    if (parent.initialize)
    {
        parent.initialize();
        
        //In case of a new connector the XML is not filled. In that case we'll
        //fill it with a dummy XML. If no data has been put in the model getData
        //returns a document.
        if (mdlConfiguration.getData().documentElement)
        {
        	fillInPropertyScreen(xmlBaseObject.documentElement);
        }
    }
    else
    {
    	//Standalone (thus preview) mode
    	fillInPropertyScreen(xmlBaseObject.documentElement);
    }
    
    g_bInitialized = true;
}

/**
 * This method is called when the form is closed.
 */
function closeForm()
{
	application.removeLibrary("/cordys/wcp/admin/library/base64encoder.htm", libPassword);
	application.addGarbage(libPassword);
}

/**
 * This method fills the models based on the configuration XML which is currently available.
 *
 * @param nConfigNode The current configuration node.
 */
function fillInPropertyScreen(nConfigNode)
{
	if (DEBUG_USERS.indexOf(system.getUser().name) > -1)
	{
		debugger;
	}
	
	//Create a XPath version of the document.
	var xmlDoc = createXMLDocument(nConfigNode.xml, NAMESPACE_PREFIXES);
	var nRoot = xmlDoc.documentElement;
	
    var nNode = nRoot.selectSingleNode("//ns:configuration[ns:server]");
    
    if (nNode == null)
    {
    	//No configuration found, use the empty template.
    	nNode = xmlBaseObject.documentElement;
    }
    
    nNode = createSimpleXMLDocument(nNode.xml).documentElement;
    
	//Decode all user passwords to avoid double encoding
	var anNodes = nNode.selectNodes(".//password");
	for (var iCount = 0; iCount < anNodes.length; iCount++)
	{
		anNodes[iCount].text = libPassword.decode(anNodes[iCount].text);
	}
    
    mdlConfiguration.putData(nNode);
    mdlConfiguration.refreshAllViews();
}

/**
 * This method stores the configuration.
 *
 * @param nConfigNode The current configuration node.
 */
function createConnectorConfiguration(nConfigNode)
{
	if (DEBUG_USERS.indexOf(system.getUser().name) > -1)
	{
		debugger;
	}
	
    var nData = mdlConfiguration.getData();
    if (nData != null)
    {
        var nClonedData = nData.cloneNode(true);
        
        //Encode all the passwords.
        var anNodes = nClonedData.selectNodes("//password");
    	for (var iCount = 0; iCount < anNodes.length; iCount++)
    	{
    		anNodes[iCount].text = libPassword.encode(anNodes[iCount].text);
    	}
        
        //Remove the inserted attribute
        var alNodes = nClonedData.selectNodes("//*[@inserted]");
        for (var iCount = 0; alNodes != null && iCount < alNodes.length; iCount++)
        {
            alNodes[iCount].removeAttribute("inserted");
        }
        
        //Remove the sync_id attribute
        var alNodes = nClonedData.selectNodes("//*[@sync_id]");
        for (var iCount = 0; alNodes != null && iCount < alNodes.length; iCount++)
        {
            alNodes[iCount].removeAttribute("sync_id");
        }
        
        var alNodes = nClonedData.selectNodes("//*[@clientattr:sync_id]");
        for (var iCount = 0; alNodes != null && iCount < alNodes.length; iCount++)
        {
            alNodes[iCount].removeAttribute("clientattr:sync_id");
        }
        
        nConfigNode.appendChild(nClonedData);
    }
    
    return true;
}
 
/**
 * This method is used to test the save functionality.
 * 
 * @param eventObject The event object.
 * 
 * @return Nothing.
 */
function handleTestSave(eventObject)
{
	xmlDoc = createSimpleXMLDocument("<config/>");
	createConnectorConfiguration(xmlDoc.documentElement);
	alert(xmlDoc.xml);
}