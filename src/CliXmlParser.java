


import java.io.*;

import javax.xml.parsers.*;
//import javax.xml.transform.*;
//import javax.xml.transform.dom.DOMSource;
//import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.*;
import org.xml.sax.*; 


public class CliXmlParser 
{
    ConfigTree configTree;
    
    
    public void setConfigTree(ConfigTree configTree) {
        this.configTree = configTree;
    }


    public boolean parseXMLFile(String fileName, CliCommandTree mainTree, CliCommandTree diagTree)
    {
        CliCommandTree curCmdTree;
        boolean  result = true;
        
        System.out.println("process file: " + fileName + "...");
        
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document xml_doc = db.parse(fileName);
            xml_doc.normalize();
            
            Element root = xml_doc.getDocumentElement(); 
            if (root == null) return false; 
 
            NodeList cliCommandList = root.getElementsByTagName("command"); 
            if (cliCommandList == null) return false;
            
            for(int i = 0; i < cliCommandList.getLength(); i++) 
            {  
                Element cliElement = (Element) cliCommandList.item(i);
                if (cliElement.getNodeType() != Node.ELEMENT_NODE)
                    continue;

                CliCommand cliCommand = parserCliCommand(cliElement);
                //System.out.println("command parse finished");
                if (cliCommand == null) {
                    //System.out.println("1");
                    result = false;
                    //continue;
                    //return false;
                }

                if (cliCommand.isSCMCommand() && (!isScmCommand)) {
                    //System.out.println("2");
                    continue;
                }
                
                if (!cliCommand.validate()) {
                    result = false;
                    //System.out.println("3");
                    continue;
                    //return false;
                }
                
                //System.out.println(cliCommand.getRuntimeHelpMsg(true));
                
                curCmdTree = null;
                if (cliCommand.getCliCmdMode() == CliCommand.CliMode.diag)
                    curCmdTree = diagTree;
                else
                    curCmdTree = mainTree;
                
                if (!curCmdTree.addCommand(cliCommand)) {
                    result = false;
                    //System.out.println("4");
                    continue;
                    //return false;
                }
                
                //System.out.println("add UI <" + cliCommand.getKeywords() + "> done!");
            }
            //result = true;
            
        } catch (ParserConfigurationException e) {  
            e.printStackTrace();  
            result = false;
        } catch (FileNotFoundException e) {  
            e.printStackTrace();
            result = false;
        } catch (SAXException e) {  
            e.printStackTrace();
            result = false;
        } catch (IOException e) {  
            e.printStackTrace();
            result = false;
        } 
        
        if (result)
            System.out.println("process file: " + fileName + " done!");
        else
            System.out.println("Error in process file: " + fileName + "!");
        
        return result;
    }
    

    private CliCommand parserCliCommand(Element cliElement)
    {
        NodeList commandNodes = cliElement.getChildNodes();  
        if (commandNodes == null) return null;
         
        CliCommand cliCommand = new CliCommand();
        
        cliCommand.setSource(CliCommand.Source.xml);
        
        for (int i = 0; i < commandNodes.getLength(); i++) 
        {
            Node node = commandNodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE)
                continue;
       
            // syntax MUST be firstly analysised to get some basic information
            if (node.getNodeName().equals("syntax"))
                parseCliSyntax(node, cliCommand);
            
             // this IS NEED, to be added
            if (node.getNodeName().equals("keyword"))
            {
                parseCliKeywords(node.getTextContent(), cliCommand);
                //System.out.println("keyword=<" + cliCommand.getKeywords()+">");
            }
            
            else if (node.getNodeName().equals("help"))
                cliCommand.addHelp(node.getTextContent());
            
            else if (node.getNodeName().equals("example"))
                cliCommand.addExample(node.getTextContent());
            
            else if (node.getNodeName().equals("param"))
                parseCliParameters(node, cliCommand);
        }
        
        // set the function node
        CliNodeFunction cliFun = new CliNodeFunction();
        cliFun.setSyntaxKeyword(cliCommand.getFunctionName());
        cliFun.setNodeShow(cliCommand.getDisplayMode());
        cliFun.setCliCommand(cliCommand);
        cliCommand.addNode(cliFun);
        
        return cliCommand;
    }

    /*
    <param required="mandatory", datatype="Ustr", unit="second">
        <keyword>client-name</keyword>
        <default>10</default>
        <range>1-GFI_LIB_MODULE_NAME_LEN</range>
        <help>the name of the gfi-log buffer which can be found via UI: \"diag view gfi log status\"</help>
    </param>    
    */
    private void parseCliParameters(Node node, CliCommand cliCommand)
    {
        String token;
        Element parameterElement = (Element)node;
        NodeList parameterNodes = parameterElement.getChildNodes();
        if (parameterNodes == null) return;
        
        CliNodeParameter cliNodeParameter = new CliNodeParameter();
        cliNodeParameter.cliCommand = cliCommand;
        
        String required = parameterElement.getAttribute("required").trim();
        if (!cliNodeParameter.setRequired(required)) {
            cliCommand.addErrorMsg("Error: unsupportted required defined: <" 
                                 + required + "> in command <" + cliCommand.getSyntaxString() + ">");
            return ;
        }
        
        
        /* first check the reference, if there is reference exist, 
         * then only read from reference and ignore the following definition
         */
        String paramName = parameterElement.getAttribute("reference").trim();
        if (paramName.length() != 0) {
            ConfigNode configNode = configTree.findConfig(paramName);
            if (configNode == null) {
                cliCommand.addErrorMsg("Error: can not find parameter: <" + paramName 
                                     + "> in command <" + cliCommand.getSyntaxString() + ">");
                return;
            }

            if (!configNode.isLeaf() && !configNode.isLeafList()) {
                cliCommand.addErrorMsg("Error: parameter reference: <" + paramName 
                        + "> is not valid, in command <" + cliCommand.getSyntaxString() + ">"
                        + ", it must be a leaf or a leaf-list");
                return;
            }
            cliNodeParameter.referName = paramName;
            cliNodeParameter.copyFromConfigNode(configNode);
            cliCommand.addNode(cliNodeParameter);
            
            System.out.println("CliXmlParser.parseCliParameters, configNode: " + configNode);
            System.out.println("CliXmlParser.parseCliParameters, CliNodeParameter: " + cliNodeParameter);
            
            return;
        }
        
        
        
        //if ()

        String keyword = parameterElement.getElementsByTagName("keyword").item(0).getTextContent().trim();
        //System.out.println("parameter keyword= " + keyword);
      
        
        
        String dataType = parameterElement.getAttribute("datatype").trim();
        if (!cliNodeParameter.setDataType(dataType))
            cliCommand.addErrorMsg("Error: unsupportted data type defined: <" + dataType + "> in parameter <" + keyword + ">");
        
        if (parameterElement.hasAttribute("unit")){
            String unit = parameterElement.getAttribute("unit").trim();
            cliNodeParameter.setUnit(unit);
        }
        
        for (int i = 0; i < parameterNodes.getLength(); i++) 
        {
            Node curNode = parameterNodes.item(i);
            if (curNode.getNodeType() != Node.ELEMENT_NODE)
                continue;
            //System.out.println("CliXmlParser parseCliParameters: " + curNode.getNodeName());
            
            token = curNode.getNodeName().trim();

            if (token.equals("keyword")) {
                boolean result = true;
                String key = curNode.getTextContent().trim();
                if (cliNodeParameter.getDataType().equals("Case")) {
                    if (key.startsWith("{"))
                        cliNodeParameter.setSyntaxKeyword(key);
                    else
                        cliNodeParameter.setSyntaxKeyword("{"+key+"}");
                }
                else if (cliNodeParameter.getRequired()) {
                    result = cliNodeParameter.setSyntaxKeyword("<"+key+">");
                }
                else {
                    result = cliNodeParameter.setSyntaxKeyword("["+key+"]");
                }
                
                if (!result)
                    cliCommand.addErrorMsg("Error: keyword too long <" + key + ">");
                //System.out.println("key="+cliNodeParameter.getKeyword());
            }
            else if (token.equals("range")) {
                if (!cliNodeParameter.setRange(curNode.getTextContent()))
                    cliCommand.addErrorMsg("Error: unsupportted range format in parameter <" + keyword + ">: " + curNode.getTextContent());
            }
            
            else if (token.equals("default")) {
                if (!cliNodeParameter.setDefValue(curNode.getTextContent()))
                    cliCommand.addErrorMsg("Error: unsupportted default format in parameter <" + keyword + ">:" + curNode.getTextContent());
            }
            
            else if (token.equals("help"))
                cliNodeParameter.addHelp(curNode.getTextContent());
            
        }
        
        cliCommand.addNode(cliNodeParameter);
    }
    
    // format of keyword as following: view:gfi:log:buffer
    private void parseCliKeywords(String keyword, CliCommand cliCommand)
    {
        String[] keywords = keyword.split(":");
        for (String token: keywords) {
            CliNodeKeyword cliKeyword = new CliNodeKeyword();
            token.trim();
            
            /*
            if (!cliKeyword.setSyntaxKeyword(token.trim()))
                cliCommand.addErrorMsg("Error: keyword is too longer: " + token);
            */
            
            cliKeyword.setNodeShow(cliCommand.getDisplayMode());

            cliCommand.addNode(cliKeyword);
        }
    }
    
    /* <syntax mode="diag" board="all" type= "sys-parameter" privilege="view" function="UIgfi_log_show_status"/> */
    // if attribute is not defined, then getAttribute("type") return empty string ""
    private void parseCliSyntax(Node node, CliCommand cliCommand)
    {
        Element syntax = (Element)node;

        String mode = syntax.getAttribute("mode");
        if (!mode.equals(CliCommand.CliMode.main.toString()) && !mode.equals(CliCommand.CliMode.diag.toString()))
            cliCommand.addErrorMsg("Error: unsupportted mode defined: " + mode);
        cliCommand.setCliCmdMode(CliCommand.CliMode.valueOf(mode));

        if (syntax.hasAttribute("display")){
            cliCommand.setDisplayMode(syntax.getAttribute("display"));
        }
        
        if (syntax.hasAttribute("type")){
            String type = syntax.getAttribute("type");
            if (!type.equals("sys-parameter"))
                cliCommand.addErrorMsg("Error: unsupportted type defiend: " + type);
            
            cliCommand.setType(type);
        }
        
        if (syntax.hasAttribute("source")){
            String source = syntax.getAttribute("source");
            if (source.equals("def"))
                cliCommand.setSource(CliCommand.Source.def);
            else if (source.equals("xml"))
                cliCommand.setSource(CliCommand.Source.xml);
            else
                cliCommand.addErrorMsg("Error: unsupportted source defined: " + source);
        }
        
        String board = syntax.getAttribute("board");
        if (board.equals("all"))
            cliCommand.setSCMCommand(false);
        else if (board.equals("scm"))
            cliCommand.setSCMCommand(true);
        else
            cliCommand.addErrorMsg("Error: unsupportted board mode defined: " + board);
        
        String privilege = syntax.getAttribute("privilege");
        cliCommand.setPrivilege(privilege);
        
        String functionName = syntax.getAttribute("function");
        cliCommand.setFunctionName(functionName);
    }

    private boolean   isScmCommand=false;

    public void setSCMCommand(boolean isScm)        { isScmCommand = isScm; }
    
}