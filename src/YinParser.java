
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;
import org.w3c.dom.*;



public final class YinParser {
    
    public static final int SUB_ELE_ATTR = 0;
    public static final int SUB_ELE_CTXT = 1;
    
    String yinFileDirectory;
    YangFileTree yangFileTree;
    ConfigTree configTree;
    PConfError errProc;
    
    
    YinParser() {
        
        yinFileDirectory = null;
        yangFileTree     = null;
        configTree       = null;
        
        errProc = PConfError.getInstance();
    }

    void setConfigTree(ConfigTree _configTree)  { configTree = _configTree; }
    
    void setYinFileDirectory(String yinDir) { yinFileDirectory = yinDir; }
    void setYangFileTree(YangFileTree _yangTree) { yangFileTree = _yangTree; }
    
    String getYinFileName(YangFile yangFile)  {return yinFileDirectory+"/"+yangFile.getYinFileName() ; }
    

    public boolean parseYangFileTree(){
        
        boolean result=true;
        
        for (YangFileModule yangModule: yangFileTree.getModuleList()) {
            
            //System.out.println("beging parseYangFileModule file <" + yangModule.getModuleName() + "> ...");
            
            /* do not need to parse the extension module, which only for attribution keywords */
            if (yangModule.isExtensionModule()) {
                continue;
            }
            
            ConfigModule configMod = new ConfigModule();

            configMod.setYangModule(yangModule);
            //configMod.setName(yangModule.moduleFile.getYangFileName());
            result = parseOneYinFile(configMod, yangModule);
            if (!result) 
                return result;
            
            if (yangModule.isInternalModule()) {
                for (YangFile yangFile: yangModule.getSubmoduleList()) {
                    result = parseOneYinFile(configMod, yangFile);
                    if (!result) 
                        return result;
                }            
            }
            
            configTree.addConfigModule(configMod);
        }
        
        return result;
    }


    public boolean parseOneYinFile(ConfigModule configMod, YangFile yangFile) {
        boolean result = true;
        
        String xmlFileName = getYinFileName(yangFile);
        System.out.println("process yin XML file: " + xmlFileName + "...");
        
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document xml_doc = db.parse(xmlFileName);
            xml_doc.normalize();
            
            Element root = xml_doc.getDocumentElement(); 
            if (root == null) 
                return false; 
     
            
            NodeList nodeList = root.getChildNodes();            
            if (nodeList == null) 
                return false;
            
            for (int i=0; i<nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                result = parseModule(configMod, yangFile, node);
                if (!result)
                    break;
            }
            
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
        
        return result;        
    }
    
    
    private String getSubelementValue(Element xmlElement, int type, String subElementName, String attrName) {
        
        String local_value="";
        
        if ((type != SUB_ELE_ATTR) && (type != SUB_ELE_CTXT)) {
            return local_value;
        }
        
        NodeList stmntList = xmlElement.getChildNodes();
        if (stmntList == null) {
            return local_value;
        }
        
        for (int i=0; i<stmntList.getLength(); i++){

            Node xmlStmntNode = stmntList.item(i);
            if (xmlStmntNode.getNodeType() != Node.ELEMENT_NODE)
                continue;
            
            Element xmlStmnt = (Element)xmlStmntNode;
            String keyword   = xmlStmnt.getNodeName();
            
            /*
            System.out.println("getSubelementValue, keyword=" + keyword
                             + ", attribution:" + attrName
                             + ", type=" + type);
            */
            
            if (keyword.contentEquals(subElementName)) {
                if (type == SUB_ELE_ATTR)
                    local_value  = xmlStmnt.getAttribute(attrName).trim();
                else
                    local_value = getSubelementContext(xmlStmnt, attrName).trim();
                
                //System.out.println("getSubelementValue:local_value=" + local_value);
            }
        }
        
        return local_value;
    }
    
    private String getSubelementContext(Element xmlElement, String subElementName) {
        
        String ctxtValue="";

        NodeList stmntList = xmlElement.getElementsByTagName(subElementName);
        if (stmntList == null) {
            return ctxtValue;
        }
        
        /*
        System.out.println("getSubelementContext(" + stmntList.getLength() 
                         + "): name(" + xmlElement.getLocalName()
                         + "), context(" + subElementName 
                         + "):"+ stmntList.item(0).getTextContent());
        */
        
        return stmntList.item(0).getTextContent();
    }
    
    
    private boolean parseModule(ConfigModule configModule, YangFile yangFile, Node node) {
        boolean result = true;
        //System.out.println("[" + node.getNodeName() + "," + node.getNodeType() + ", " + node.getNodeValue() + "] ");
        
        String keyword = node.getNodeName();
        
        if (node.getNodeType() != Node.ELEMENT_NODE)
            return result;
        
        //System.out.println("parseOneModuleFile: parse node:" + node.getNodeName());
        Element xmlStmnt  = (Element)node;
        
        if (keyword.contentEquals("namespace")) {
            if (configModule.type == ConfigNode.NodeType.MODULE) {
                configModule.setNamespace(xmlStmnt.getAttribute("uri").trim());
            }
        }
        
        // module prefix take as the first module
        else if (keyword.contentEquals("prefix")) { 
            String configClientModuleName = xmlStmnt.getAttribute("value").trim(); 
            configModule.setName(configClientModuleName);
            configModule.getYangModule().configModuleName = configClientModuleName;
        } 
        
        else if (keyword.contentEquals("import")) {
            ReferYangFile refer = new ReferYangFile();
            
            String moduleName = xmlStmnt.getAttribute("module").trim();
            
            refer.referType = ReferYangFile.ReferType.refer_import;
            refer.prefix    = getSubelementValue(xmlStmnt, SUB_ELE_ATTR, "prefix", "value");
            refer.yangFile  = yangFileTree.getYangFile(moduleName);
            
            if (refer.yangFile == null) {
                errProc.addMessage(String.format("module <%s> is not parsed yet!\n", moduleName));
                return false;
            }
            yangFile.getReferFiles().add(refer);
        } 
        
        else if (keyword.contentEquals("include")) {
            ReferYangFile refer = new ReferYangFile();
            
            refer.referType = ReferYangFile.ReferType.refer_include;
            refer.prefix    = getSubelementValue(xmlStmnt, SUB_ELE_ATTR, "prefix", "value");
            refer.yangFile  = yangFileTree.getYangFile(refer.getModuleName());
            
            if (refer.yangFile == null) {
                errProc.addMessage(String.format("module <%s> is not parsed yet!\n", 
                                                 refer.getModuleName()));
                return false;
            }
            yangFile.getReferFiles().add(refer);
        }
        
        else if (keyword.contentEquals("typedef")   ||
                 keyword.contentEquals("leaf")      ||
                 keyword.contentEquals("list")      ||
                 keyword.contentEquals("leaf-list") ||
                 keyword.contentEquals("container")) {
            result = parseOneNode(configModule, yangFile, node);
        }

        else {
        	// do nothing for unsupported node type
        }
        
        return result;
    }

    
    private boolean parseOneNode(ConfigNode configNode, YangFile yangFile, Node node) {
        boolean result = true;
        //System.out.println("[" + node.getNodeName() + "," + node.getNodeType() + ", " + node.getNodeValue() + "] ");
        
        String keyword = node.getNodeName();
        
        if (node.getNodeType() != Node.ELEMENT_NODE)
            return result;
        
        //System.out.println("parse node:" + node.getNodeName() + ", name=" + configNode.getName());
        //System.out.println("parseOneYangNode:" + configNode);
        Element xmlStmnt  = (Element)node;
        
        if (keyword.contentEquals("typedef")) {
            result = parseTypedef(configNode, yangFile, xmlStmnt);
        }
        
        else if (keyword.contentEquals("leaf")) {
            result = parseLeaf(configNode, yangFile, xmlStmnt);
        }
        
        else if (keyword.contentEquals("list")) {
            //System.out.println("\n\nthe list is not support yet!\n\n");
            result = parseList(configNode, yangFile, xmlStmnt);
        }
        
        else if (keyword.contentEquals("leaf-list")) {
            //System.out.println("\n\nthe leaf-list is not support yet!\n\n");
            result = parseLeafList(configNode, yangFile, xmlStmnt);
        }
        
        else if (keyword.contentEquals("container")) {
            result = parseContainer(configNode, yangFile, xmlStmnt);
        }
        
        // unsupported type, do nothing here
        else {
         
        }
        
        
        return result;
    }

    private boolean parseTypedef(ConfigNode configNode, YangFile yangFile, Element xmlElement) {
        
    	boolean result = true;
    	//System.out.println("\n\nparseYangTypeDef\n");
    	ConfigTypedef type_def = new ConfigTypedef();
        
    	type_def.setParent(configNode);
        type_def.setYangFile(yangFile);
        type_def.setYangModule(yangFile.getParentModule());
        type_def.setName(xmlElement.getAttribute("name").trim());
        
        result = parseStatements(type_def, yangFile, xmlElement);
        
        //configNode.addChild(typeDef);
        configTree.addTypedef(yangFile.getModuleName(), type_def.getName(), type_def);
        //System.out.println("name=" + typeDef.getName() + ", YangFile=" + typeDef.getYangFile().getYangFileName());
        //System.out.println(typeDef);
        
        //System.out.println("in parseYangTypeDef: def_name:"+ typeDef.getName() + ":"+ typeDef.dataType);
        
        return result;
        
    }
    
    
    private boolean parseLeaf(ConfigNode configNode, YangFile yangFile, Element xmlElement) {
        
        //System.out.println("parse leaf, parent:" + configNode.getName());
        //System.out.println("parse leaf, parent:" + configNode);
        
        boolean result = false;
        
        ConfigLeaf leaf = new ConfigLeaf();
        
        leaf.setParent(configNode);
        leaf.setYangFile(yangFile);
        leaf.setYangModule(yangFile.getParentModule());
        leaf.setName(xmlElement.getAttribute("name").trim());
        
        result = parseStatements(leaf, yangFile, xmlElement);
        
        configNode.addChild(leaf);
        
        return result;
    }
    
    
    private boolean parseLeafList(ConfigNode configNode, YangFile yangFile, Element xmlElement) {
        
        boolean result = false;
        ConfigLeafList leaflist = new ConfigLeafList();
        
        leaflist.setParent(configNode);
        leaflist.setYangFile(yangFile);
        leaflist.setYangModule(yangFile.getParentModule());
        leaflist.setName(xmlElement.getAttribute("name").trim());
        
        result = parseStatements(leaflist, yangFile, xmlElement);
        
        configNode.addChild(leaflist);
        
        return result;
    }
    
    private boolean parseList(ConfigNode configNode, YangFile yangFile, Element xmlElement) {
        
        boolean result = false;
        ConfigList confList = new ConfigList();
        
        confList.setYangFile(yangFile);
        confList.setYangModule(yangFile.getParentModule());
        confList.setParent(configNode);
        confList.setName(xmlElement.getAttribute("name").trim());
        
        configNode.addChild(confList);
        
        //System.out.println("list: " + confList.getName());
        // firstly will parse the statements directly belong to list
        result = parseStatements(confList, yangFile, xmlElement);
        if (!result) {
            return result;
        }
        
        /* go through all the sub-node, and parse the sub-node, 
         * including leaf, leaf-list, list, container, typedef
         */
        NodeList nodeList = xmlElement.getChildNodes();            
        if (nodeList == null) 
            return false;
        
        for (int i=0; i<nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE)
                continue;
            
            //System.out.println("parse list sub-node:" + node.getNodeName());
            Element xmlStmnt  = (Element)node;
            String keyword = node.getNodeName();
            
            if (keyword.contentEquals("key")) {
                confList.listKey = xmlStmnt.getAttribute("value").trim();
            }
            
            else {
                result = parseOneNode(confList, yangFile, node);
            }

            if (!result) {
                break;
            }
        }

        return result;
    }
    
    
    private boolean parseContainer(ConfigNode configNode, YangFile yangFile, Element xmlElement) {
        
        boolean result = false;
        ConfigContainer container = new ConfigContainer();
        
        container.setParent(configNode);
        container.setYangFile(yangFile);
        container.setYangModule(yangFile.getParentModule());
        container.setName(xmlElement.getAttribute("name").trim());
        
        configNode.addChild(container);
        
        // first parse the statement which belong to container directly
        result = parseStatements(container, yangFile, xmlElement);
        if (!result)
            return result;
        
        // then parse the sub-nodes, including leaf, list, container, leaf-list and typedef
        NodeList nodeList = xmlElement.getChildNodes();            
        if (nodeList == null) 
            return false;
        
        for (int i=0; i<nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            
            if (node.getNodeType() != Node.ELEMENT_NODE)
                continue;

            result = parseOneNode(container, yangFile, node);
            if (!result)
                break;
        }
        
        return result;
    }
    
    private boolean parseStatements(ConfigNode configNode, YangFile yangFile, Element xmlElement) {

        NodeList stmntList = xmlElement.getChildNodes();
        if (stmntList == null) 
            return false;
        
        for (int i=0; i<stmntList.getLength(); i++){

            Node xmlStmntNode = stmntList.item(i);
            if (xmlStmntNode.getNodeType() != Node.ELEMENT_NODE)
                continue;
            
            Element xmlStmnt = (Element)xmlStmntNode;
            String keyword   = xmlStmnt.getNodeName();
            
            //System.out.println("parseStatements: key=" + keyword);

            if (keyword.contentEquals("description")) {
                configNode.setDescription(getSubelementContext(xmlStmnt, "text"));
            }
            
            else if (keyword.contentEquals("type")){
                parseType(configNode, (Element)xmlStmntNode, yangFile);
            }
            
            else if (keyword.contentEquals("units")){
                configNode.setUnits(xmlStmnt.getAttribute("name").trim());
            }
            
            else if (keyword.contentEquals("default")) {
                configNode.setDefaultVal(xmlStmnt.getAttribute("value").trim());
            }
            
            else if (keyword.contentEquals("min-elements")) {
                configNode.minElements = xmlStmnt.getAttribute("value").trim();
            }
            
            else if (keyword.contentEquals("max-elements")) {
                configNode.maxElements = xmlStmnt.getAttribute("value").trim();
            }
            
            else if (keyword.contentEquals("config")) {
                configNode.configurable = xmlStmnt.getAttribute("value").trim();
            }            
            
            else if (keyword.contentEquals("status")) {
                configNode.setStatus(xmlStmnt.getAttribute("value").trim());
            }
            
            else if (keyword.contentEquals("ordered-by")) {
                configNode.orderedBy = xmlStmnt.getAttribute("value").trim();
            }

            else if (keyword.contentEquals("mandatory")) {
                configNode.mandatory = xmlStmnt.getAttribute("value").trim();
            }
            
            else if (keyword.contentEquals("must")) {
                //System.out.println("\n\n\nattribute 'must' not finished yet\n\n\n\n");
                
                parseMust(configNode, (Element)xmlStmntNode, yangFile);
            }
            
            else if (keyword.contentEquals("when")) {
                System.out.println("\nattribute 'when' not finished yet\n");
                //configNode.setDefaultVal(xmlStmnt.getAttribute("value").trim());
            }
            
            else if (keyword.contentEquals("pattern")) {
                //System.out.println("\n\n\nattribute 'pattern' not finished yet\n\n\n\n");
                //configNode.setDefaultVal(xmlStmnt.getAttribute("value").trim());
                //if (configNode.type != ConfigNode.NodeType.data_type) {
                errProc.addMessage("Pattern is ONLY support in type description, "
                                   + "error in Yang file <" + yangFile.getYangFileName() 
                                   + "> with node of " + configNode.getName());
                break;
            }
            
            else if (keyword.endsWith("gw:scope")) {
                configNode.setScope(xmlStmnt.getAttribute("string").trim());
            }
            
            else if (keyword.endsWith("gw:add-release")) {
                configNode.setAddRelease(xmlStmnt.getAttribute("release").trim());
            }
            
            else if (keyword.endsWith("gw:mod-release")) {
                configNode.setModRelease(xmlStmnt.getAttribute("release").trim());
            }
            
            else if (keyword.endsWith("gw:service-impact")) {
                configNode.setServiceImpact(xmlStmnt.getAttribute("string").trim());
            }
            
            else if (keyword.endsWith("gw:notes")) {
                configNode.setNotes(xmlStmnt.getAttribute("string").trim());
            }
        }
        
        if (errProc.hasErrorMsg())
            return false;
        
        return true;
    }


     
    private boolean parseType(ConfigNode configNode, Element xmlStmnt, YangFile yangFile) {
        NodeList stmntList = xmlStmnt.getChildNodes();
        if (stmntList == null) 
            return false;
        
        String fullTypeName = xmlStmnt.getAttribute("name").trim();
        //System.out.println("parseStatementType fullTypeName=" + fullTypeName);

        ConfigType dataType = new ConfigType();
        
        dataType.setParent(configNode);
        dataType.setYangFile(yangFile);
        dataType.setYangModule(yangFile.getParentModule());
        dataType.parseFullName(fullTypeName);
        
        configNode.dataType = dataType;
        
        ConfigTypedef typedef = configTree.getTypeDef(dataType);
        if (typedef == null) {
            errProc.addMessage(String.format("type (%s) not defined in Yang file (%s)\n", 
                                             fullTypeName, yangFile.getYangFileName()));
            
            errProc.addMessage("module name: " + dataType.defModule 
            		         + ", type_name=" + dataType.getName());
            
            //System.out.println(configTree.typeDefs);
            //System.out.println(yangFile);
            
            return false;
        }

        dataType.setName(typedef.getName());
        dataType.typeDefinition = typedef;
        
        if (!ConfigBuiltin.isGwBuiltin(typedef.getName())) {
        	configNode.dataType = typedef.dataType;
        }
        
        for (int i=0; i<stmntList.getLength(); i++){

            Node xmlStmntNode = stmntList.item(i);
            if (xmlStmntNode.getNodeType() != Node.ELEMENT_NODE)
                continue;
            
            Element xmlSubStmnt = (Element)xmlStmntNode;
            String keyword   = xmlSubStmnt.getNodeName();
            
            if (keyword.contentEquals("pattern")) {
                dataType.patternList.add(xmlSubStmnt.getAttribute("value").trim());
            }
            
            else if (keyword.contentEquals("enum")){
                ConfigDataEnum typeChoice = new ConfigDataEnum();
                
                typeChoice.name  = xmlSubStmnt.getAttribute("name").trim();
                typeChoice.value = getSubelementValue(xmlSubStmnt, SUB_ELE_ATTR, "value", "value");
                typeChoice.descr = getSubelementValue(xmlSubStmnt, SUB_ELE_CTXT, "description", "text");
                typeChoice.status = getSubelementValue(xmlSubStmnt, SUB_ELE_ATTR, "status", "value");
                
                /*
                if (typeChoice.status.length() != 0)
                    System.out.println("parseType: enum: " + typeChoice);
             	*/
               
                dataType.enumValList.add(typeChoice);
            }
            
            else if (keyword.contentEquals("length")) {
                dataType.length = xmlSubStmnt.getAttribute("value").trim();
            }
            
            else if (keyword.contentEquals("range")) {
                dataType.range = xmlSubStmnt.getAttribute("value").trim();
            }
            
        }
        
        //System.out.println(configNode.dataType);
        //configNode.addChild(dataType);

        return true;
    }

    /*
       <must condition="../primary-ip != 0.0.0.0">
          <error-message>
            <value>primary IP address must be configure</value>
          </error-message>
        </must>
    */
    private boolean parseMust(ConfigNode configNode, Element xmlStmnt, YangFile yangFile) {

        //System.out.println("parseMust =" + xmlStmnt.getNodeName());
        
        if (!configNode.isConfigParameter()) {
            errProc.addMessage("Unsupportted 'must' in config node " + configNode.getName()
                             + " in Yang file <" + yangFile.getYangFileName() + ">\n");
            return false;
        }

        
        ConfigDataMust dataMust = new ConfigDataMust();

        dataMust.condition = xmlStmnt.getAttribute("condition").trim();
        dataMust.errMsg = getSubelementValue(xmlStmnt, SUB_ELE_CTXT, "error-message", "value");
        dataMust.description = getSubelementValue(xmlStmnt, SUB_ELE_CTXT, "description", "text");
        configNode.limit_must.add(dataMust);
        
        //System.out.println("parseMust, node: " + configNode.getName() + " " + dataMust);

        return true;
    }    
}
