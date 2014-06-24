
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
            result = parseYangFileOneXML(configMod, yangModule);
            if (!result) 
                return result;
            
            if (yangModule.isInternalModule()) {
                for (YangFile yangFile: yangModule.getSubmoduleList()) {
                    result = parseYangFileOneXML(configMod, yangFile);
                    if (!result) 
                        return result;
                }            
            }
            
            configTree.addConfigModule(configMod);
        }
        
        return result;
    }


    public boolean parseYangFileOneXML(ConfigModule configMod, YangFile yangFile) {
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
                result = parseOneModuleFile(configMod, yangFile, node);
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
    
    
    private boolean parseOneModuleFile(ConfigModule configModule, YangFile yangFile, Node node) {
        boolean result = true;
        //System.out.println("[" + node.getNodeName() + "," + node.getNodeType() + ", " + node.getNodeValue() + "] ");
        
        String keyword = node.getNodeName();
        
        if (node.getNodeType() != Node.ELEMENT_NODE)
            return result;
        
        //System.out.println("parseOneModuleFile: parse node:" + node.getNodeName());
        Element xmlStmnt  = (Element)node;
        
        if (keyword.contentEquals("namespace")) {
            if (configModule.type == ConfigNode.NodeType.module) {
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
            
            refer.prefix    = getSubelementValue(xmlStmnt, SUB_ELE_ATTR, "prefix", "value");
            refer.yangFile  = yangFileTree.getYangFile(moduleName);
            refer.referType = ReferYangFile.ReferType.refer_import;
            if (refer.yangFile == null) {
                errProc.addMessage(String.format("module <%s> is not parsed yet!\n", moduleName));
                return false;
            }
            yangFile.getReferFiles().add(refer);
        } 
        
        else if (keyword.contentEquals("include")) {
            ReferYangFile refer = new ReferYangFile();
            
            refer.prefix    = getSubelementValue(xmlStmnt, SUB_ELE_ATTR, "prefix", "value");
            refer.yangFile  = yangFileTree.getYangFile(refer.getModuleName());
            refer.referType = ReferYangFile.ReferType.refer_include;
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
                     
            result = parseOneYangNode(configModule, yangFile, node);
        }
        
        else {
            //System.out.println("unknow items" + keyword + " in yang file " + yangFile.getYangFileName());
        }
        
        
        /*
        else if (keyword.contentEquals("typedef") ) {
            result = parseYangTypeDef(configModule, yangFile, xmlStmnt);
        }
        
        else if (keyword.contentEquals("leaf")) {
            result = parseYangLeaf(configModule, yangFile, xmlStmnt);
        }
        
        else if (keyword.contentEquals("list")) {
            System.out.println("\n\nthe list is not support yet!\n\n");
            result = parseYangList(configModule, yangFile, xmlStmnt);
        }
        
        else if (keyword.contentEquals("leaf-list")) {
            System.out.println("\n\nthe leaf-list is not support yet!\n\n");
            //result = parseYangLeaf(configModule, yangFile, xmlStmnt);
        }
        
        else if (keyword.contentEquals("container")) {
            System.out.println("\n\nthe container is not support yet!\n\n");
            //result = parseYangLeaf(configModule, yangFile, xmlStmnt);
        }
        */
        
        return result;
    }

    
    private boolean parseOneYangNode(ConfigNode configNode, YangFile yangFile, Node node) {
        boolean result = true;
        //System.out.println("[" + node.getNodeName() + "," + node.getNodeType() + ", " + node.getNodeValue() + "] ");
        
        String keyword = node.getNodeName();
        
        if (node.getNodeType() != Node.ELEMENT_NODE)
            return result;
        
        //System.out.println("parse node:" + node.getNodeName() + ", name=" + configNode.getName());
        //System.out.println("parseOneYangNode:" + configNode);
        Element xmlStmnt  = (Element)node;
        
        
        if (keyword.contentEquals("typedef")) {
            result = parseYangTypeDef(configNode, yangFile, xmlStmnt);
        }
        
        else if (keyword.contentEquals("leaf")) {
            //System.out.println(configNode);
            result = parseYangLeaf(configNode, yangFile, xmlStmnt);
        }
        
        else if (keyword.contentEquals("list")) {
            //System.out.println("\n\nthe list is not support yet!\n\n");
            result = parseYangList(configNode, yangFile, xmlStmnt);
        }
        
        else if (keyword.contentEquals("leaf-list")) {
            //System.out.println("\n\nthe leaf-list is not support yet!\n\n");
            result = parseYangLeafList(configNode, yangFile, xmlStmnt);
        }
        
        else if (keyword.contentEquals("container")) {
            //System.out.println("\n\nthe container is not support yet!\n\n");
            result = parseYangContainer(configNode, yangFile, xmlStmnt);
        }
        
        
        return result;
    }

    private boolean parseYangTypeDef(ConfigNode configNode, YangFile yangFile, Element xmlElement) {
        
    	//System.out.println("\n\nparseYangTypeDef\n");
    	ConfndTypedef typeDef = new ConfndTypedef();
        
        typeDef.setYangFile(yangFile);
        typeDef.setYangModule(yangFile.getParentModule());
        typeDef.setParent(configNode);
        
        typeDef.setName(xmlElement.getAttribute("name").trim());
        
        //System.out.println("name=" + typeDef.getName() + ", YangFile=" + typeDef.getYangFile().getYangFileName());
                
        parseStatements(typeDef, yangFile, xmlElement);
        
        //configNode.addChild(typeDef);
        configTree.addTypeDef(yangFile.getModuleName(), typeDef.getName(), typeDef);
        //System.out.println("name=" + typeDef.getName() + ", YangFile=" + typeDef.getYangFile().getYangFileName());
        //System.out.println(typeDef);
        
        //System.out.println("in parseYangTypeDef: def_name:"+ typeDef.getName() + ":"+ typeDef.dataType);
        
        return true;
        
    }
    
    
    private boolean parseYangLeaf(ConfigNode configNode, YangFile yangFile, Element xmlElement) {
        
        //System.out.println("parse leaf, parent:" + configNode.getName());
        //System.out.println("parse leaf, parent:" + configNode);
        
        boolean result = false;
        ConfndLeaf leaf = new ConfndLeaf();
        
        leaf.setYangFile(yangFile);
        leaf.setYangModule(yangFile.getParentModule());
        leaf.setName(xmlElement.getAttribute("name").trim());
        leaf.setParent(configNode);
        
        
        result = parseStatements(leaf, yangFile, xmlElement);
        
        configNode.addChild(leaf);
        
        return result;
    }
    
    
    private boolean parseYangLeafList(ConfigNode configNode, YangFile yangFile, Element xmlElement) {
        
        boolean result = false;
        ConfndLeafList leaflist = new ConfndLeafList();
        
        leaflist.setYangFile(yangFile);
        leaflist.setYangModule(yangFile.getParentModule());
        leaflist.setName(xmlElement.getAttribute("name").trim());
        leaflist.setParent(configNode);
        
        
        result = parseStatements(leaflist, yangFile, xmlElement);
        
        configNode.addChild(leaflist);
        
        return result;
    }
    
    private boolean parseYangList(ConfigNode configNode, YangFile yangFile, Element xmlElement) {
        
        boolean result = false;
        ConfndList confList = new ConfndList();
        
        confList.setYangFile(yangFile);
        confList.setYangModule(yangFile.getParentModule());
        confList.setParent(configNode);
        confList.setName(xmlElement.getAttribute("name").trim());
        
        //System.out.println("list: " + confList.getName());
        
        NodeList nodeList = xmlElement.getChildNodes();            
        if (nodeList == null) 
            return false;
        
        for (int i=0; i<nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            
            String keyword = node.getNodeName();
            
            if (node.getNodeType() != Node.ELEMENT_NODE)
                continue;
            
            //System.out.println("parse list sub-node:" + node.getNodeName());
            Element xmlStmnt  = (Element)node;
            
            if (keyword.contentEquals("key")) {
                confList.listKey = xmlStmnt.getAttribute("value").trim();
            }
            
            else if (keyword.contentEquals("max-elements")){
                confList.maxElements = xmlStmnt.getAttribute("value").trim();
                //System.out.println("max-elements=" + confList.maxElements);
            }
            
            result = parseStatements(confList, yangFile, xmlElement);
            if (!result)
                break;
                
            //System.out.println(confList);
            result = parseOneYangNode(confList, yangFile, node);
            if (!result)
                break;
        }
        
        configNode.addChild(confList);
        
        return result;
    }
    
    
    private boolean parseYangContainer(ConfigNode configNode, YangFile yangFile, Element xmlElement) {
        
        boolean result = false;
        ConfndContainer container = new ConfndContainer();
        
        container.setYangFile(yangFile);
        container.setYangModule(yangFile.getParentModule());
        container.setParent(configNode);
        container.setName(xmlElement.getAttribute("name").trim());
        
        //System.out.println("list: " + confList.getName());
        
        NodeList nodeList = xmlElement.getChildNodes();            
        if (nodeList == null) 
            return false;
        
        for (int i=0; i<nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            
            //String keyword = node.getNodeName();
            
            if (node.getNodeType() != Node.ELEMENT_NODE)
                continue;
            
            //System.out.println("parse list sub-node:" + node.getNodeName());
            //Element xmlStmnt  = (Element)node;
            
            /*
            if (keyword.contentEquals("key")) {
                confList.listKey = xmlStmnt.getAttribute("value").trim();
            }
            
            else if (keyword.contentEquals("max-elements")){
                confList.maxElements = xmlStmnt.getAttribute("value").trim();
            }
            */
            
            result = parseStatements(container, yangFile, xmlElement);
            if (!result)
                break;
                
            //System.out.println(confList);
            result = parseOneYangNode(container, yangFile, node);
            if (!result)
                break;
        }
        
        configNode.addChild(container);
        
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
            	//System.out.println(" parse the type");
                parseStatementType(configNode, (Element)xmlStmntNode, yangFile);
            }
            
            else if (keyword.contentEquals("units")){
                //System.out.println(" parse the type");
                configNode.setUnits(xmlStmnt.getAttribute("name").trim());
                //System.out.println(" unit" + configNode.getUnits());
            }
            
            else if (keyword.contentEquals("default")) {
                configNode.setDefaultVal(xmlStmnt.getAttribute("value").trim());
            }
            
            else if (keyword.contentEquals("config")) {
                configNode.configurable = xmlStmnt.getAttribute("value").trim();
            }            
            
            else if (keyword.contentEquals("must")) {
                System.out.println("\n\n\nattribute 'must' not finished yet\n\n\n\n");
                //configNode.setDefaultVal(xmlStmnt.getAttribute("value").trim());
            }
            
            else if (keyword.contentEquals("when")) {
                System.out.println("\n\n\nattribute 'when' not finished yet\n\n\n\n");
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
                //}
                
                /*
                ConfndType type = (ConfndType)configNode;
                type.patternList.add(xmlStmnt.getAttribute("value").trim());
                
                System.out.println("patter value=" + xmlStmnt.getAttribute("value").trim() );
                System.out.println(type);
                */
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
            
            if (errProc.hasErrorMsg())
                return false;
        }
        
        return true;
    }


     
    private boolean parseStatementType(ConfigNode configNode, Element xmlStmnt, YangFile yangFile) {
        NodeList stmntList = xmlStmnt.getChildNodes();
        if (stmntList == null) 
            return false;
        
        String fullTypeName = xmlStmnt.getAttribute("name").trim();
        //System.out.println("parseStatementType fullTypeName=" + fullTypeName);
        ConfndType dataType = new ConfndType();
        dataType.setYangFile(yangFile);
        dataType.setParent(configNode);
        dataType.parseFullName(fullTypeName);
        
        //System.out.println("parseStatementType type_name=" + dataType.definedModule + ":" + dataType.typeName);
        
        configNode.dataType = dataType;
        
        ConfndTypedef typedef = configTree.getTypeDef(dataType);
        if (typedef == null) {
            errProc.addMessage(String.format("type (%s) not defined in Yang file (%s)\n", 
                                             fullTypeName, yangFile.getYangFileName()));
            
            errProc.addMessage("module name: " + dataType.definedModule 
            		         + ", type_name=" + dataType.typeName);
            
            //System.out.println(configTree.typeDefs);
            //System.out.println(yangFile);
            
            return false;
        }

        dataType.setName(typedef.getName());
        dataType.typeDefinition = typedef;
        
        //String Name = "type:"+dataType.getName();
        //while 
        //System.out.println("type:"+dataType.getName() + ", definition=" + typedef.getName());
        //System.out.if (typedef.dataType.typeDefinition.getName())
        
        for (int i=0; i<stmntList.getLength(); i++){

            Node xmlStmntNode = stmntList.item(i);
            if (xmlStmntNode.getNodeType() != Node.ELEMENT_NODE)
                continue;
            
            Element xmlSubStmnt = (Element)xmlStmntNode;
            String keyword   = xmlSubStmnt.getNodeName();
            
            //System.out.println("parseStatementType: keyword=" + keyword );
            
            if (keyword.contentEquals("pattern")) {
                //ConfndType type = (ConfndType)configNode;
                dataType.patternList.add(xmlSubStmnt.getAttribute("value").trim());
                
                //System.out.println("patter value=" + xmlSubStmnt.getAttribute("value").trim() );
                //System.out.println(type);
                
                /*
                errProc.addMessage(String.format("error happened in typ of leaf (%s) of Yang file (%s)\n", 
                                   configNode.getName(), yangFile.getYangFileName()));
                errProc.addMessage("patter is not supported yet");
                return false;
                */
            }
            
            else if (keyword.contentEquals("enum")){
                DataTypeChoice typeChoice = new DataTypeChoice();
                
                typeChoice.name  = xmlSubStmnt.getAttribute("name").trim();
                typeChoice.value = getSubelementValue(xmlSubStmnt, SUB_ELE_ATTR, "value", "value");
                typeChoice.descr = getSubelementValue(xmlSubStmnt, SUB_ELE_CTXT, "description", "text");
                
                dataType.enumValList.add(typeChoice);
                
                //System.out.println(typeChoice);
            }
            
            else if (keyword.contentEquals("length")) {
                dataType.length = xmlSubStmnt.getAttribute("value").trim();
                /*
                errProc.addMessage(String.format("error happened in typ of leaf (%s) of Yang file (%s)\n", 
                        configNode.getName(), yangFile.getYangFileName()));
                errProc.addMessage(" length is not defined yet");
                return false;
                */
            }
            
            else if (keyword.contentEquals("range")) {
                dataType.range = xmlSubStmnt.getAttribute("value").trim();
                /*
                errProc.addMessage(String.format("error happened in typ of leaf (%s) of Yang file (%s)\n", 
                        configNode.getName(), yangFile.getYangFileName()));
                errProc.addMessage("range is not defined yet");
                return false;
                */
            }
            
        }
        
        //System.out.println(configNode.dataType);
        //configNode.addChild(dataType);

        return true;
    }
        
}
