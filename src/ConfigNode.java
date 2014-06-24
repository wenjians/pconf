


//import java.io.File;
//import java.io.IOException;
import java.util.*;

/*
import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableCellFormat;
import jxl.write.WritableFont;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
*/

/**
 * 
 */

/**
 * @author wenjians
 *
 */
class ConfigNode {
    static enum NodeType { invalid, module, container, leaf, leaf_list, list, data_type, type_def }
    
    static enum Scope { invalid, system, customer, element }
    
    /* the node type, which is not support dynamic set later
     * it can not be set during construction file by descend classes
     */
    NodeType type;  
    
    // point to the YangFileModule, which also include submodule
    YangFileModule yangModule;  
    
    // which Yang File (module or submodule) define this node
    YangFile       yangFile;  
    
    // the Configure Parameter is arranged in tree, these are all the children
    List<ConfigNode> children;
    ConfigNode       parent;
    
    
    // the following list the value of one Yang Node
    
    /* module or keyword name, 
     * module name: defined in <module name=... >, for configure client usage
     * leaf, node, ...: defined <leaf/node name=... >
     */
    String name;
    String description;
    String defaultVal;
    String limit_must;
    String limit_when;
    String units;
    String configurable;
    
    ConfndType dataType;
    
    /* the following is gw specific defined attribution */
    Scope  gw_scope;
    String gw_add_release;
    String gw_mod_release;
    String gw_service_impact;
    String gw_notes;
    String gw_feature_id;
        
    
    protected ConfigNode() {
        type            = NodeType.invalid;
        name            = "";
        children        = null; // some node don't have child
        parent          = null;
        description     = "";
        defaultVal      = "";
        dataType        = null;
        configurable    = "true";
        gw_scope        = Scope.invalid;
        gw_add_release  = "";
        gw_mod_release  = "";
        gw_service_impact = "";
        gw_notes        = "";
        gw_feature_id   = "";
        
    }
    
    boolean isConfigParameter()  {
        return ((type == NodeType.container) ||
                (type == NodeType.leaf)      ||
                (type == NodeType.leaf_list) ||
                (type == NodeType.list));
    }
    
    //invalid, module, container, leaf, leaf_list, list, data_type, type_def
    
    boolean isConfigModule()    { return type == NodeType.module;       }
    boolean isContainer()       { return type == NodeType.container;    }
    boolean isLeaf()            { return type == NodeType.leaf;         }
    boolean isLeafList()        { return type == NodeType.leaf_list;    }
    boolean isDataType()        { return type == NodeType.data_type;    }
    boolean isTypeDef()         { return type == NodeType.type_def;     }
    
    
    boolean isConfigurable() {
        return configurable.contentEquals("true");
    }
    
    
    public Scope getScope() {
        return gw_scope;
    }
    
    public String getScopeName() {
        String _scopeName;
        
        if (gw_scope == Scope.invalid) {
            _scopeName = "";
        } else {
            _scopeName = gw_scope.toString();
        }
        
        return _scopeName;
    }

    public void setScope(String scope) {
        if (scope.contentEquals("system")) {
            this.gw_scope = Scope.system;
        }
        else if (scope.contentEquals("customer")) {
            this.gw_scope = Scope.customer;
        }
        else if (scope.contentEquals("element")) {
            this.gw_scope = Scope.element;
        }
        else {
            String errMsg;
            errMsg = String.format("unknow scope <%s> defined in node <%s> of yang file<%s>\n", 
                          scope, getName(), yangFile.getModuleName());
            PConfError.getInstance().addMessage(errMsg);
        }
            
    }
    
    

    public String getUnits() {
        return units;
    }

    public void setUnits(String units) {
        this.units = units;
    }

    public String getAddRelease() {
        return gw_add_release;
    }

    public void setAddRelease(String gw_add_release) {
        this.gw_add_release = gw_add_release;
    }

    public String getModRelease() {
        return gw_mod_release;
    }

    public void setModRelease(String gw_mod_release) {
        this.gw_mod_release = gw_mod_release;
    }

    public String getServiceImpact() {
        return gw_service_impact;
    }

    public void setServiceImpact(String gw_serivce_impact) {
        this.gw_service_impact = gw_serivce_impact;
    }

    public String getNotes() {
        return gw_notes;
    }

    public void setNotes(String gw_notes) {
        this.gw_notes = gw_notes;
    }

    public String getFeatureId() {
        return gw_feature_id;
    }

    public void setFeatureId(String gw_feature_id) {
        this.gw_feature_id = gw_feature_id;
    }


    String getDefaultVal() {  
        return defaultVal;           
    }
    
    void setDefaultVal(String val) {  
        this.defaultVal = val;       
    }

    String getDescription()  {  
        return description;          
    }
    
    void setDescription(String _desc)   {  
        this.description = _desc;    
    }

    void setYangModule(YangFileModule _yang) { 
        yangModule = _yang;      
    }
    
    YangFileModule getYangModule() { 
        return yangModule;       
    }
    
    YangFile getYangFile()  {  
        return yangFile;             
    }
    
    void setYangFile(YangFile yangFile) {  
        this.yangFile = yangFile;    
    }

    List<ConfigNode> getChildren()  { 
        return children;          
    }
    
    void addChild(ConfigNode child) { 
        this.children.add(child); 
    }
    

    public ConfigNode getParent() {
        return parent;
    }

    public void setParent(ConfigNode _parent) {
        this.parent = _parent;
    }

    void setName(String _name)  { 
        name = _name;             
    }
    
    String getName() { 
        return name;              
    }
    
    
    /* this function return the full path name, not include the module name
     * e.g. realm-table/ip-if/ip
     */
    String getFullPathName() {
        //String[] keyword;
        List<String> keyword = new ArrayList<String> ();
        String fullPathName="";
        
        
        for (ConfigNode parent=getParent();
             parent.type != ConfigNode.NodeType.module;
             parent = parent.getParent()
            ) {
            keyword.add(parent.getName());
        }
        
        for (int i=keyword.size()-1; i>=0; i--) {
            fullPathName += keyword.get(i) + "/";
        }
        
        return fullPathName + getName();
    }    
    
    /*
    String getFullPathName() {
        
    }
    */

    @Override
    public String toString() {
        return "ConfigNode [yangFile=" + yangFile.getModuleName() 
                + ", type=" + type + ", name=" + name + ", description=" + description 
                + ", defaultVal=" + defaultVal + ", scopeVal=" + getScope()
                + ", feature id = " + gw_feature_id
                + ", add release=" + gw_add_release + ", mod release=" + gw_mod_release
                + ", service impact=" + gw_service_impact + "notes=" + gw_notes
                + ", children=" + children + "]\n"; 
    }
    
}

/**
 * @author diag
 *
 */
class ConfigModule extends ConfigNode {
    
    String          namespace;
    
    ConfigModule() {
        super();
        type = NodeType.module;
        children = new ArrayList<ConfigNode> ();
    }
    
    String getNamespace()  { 
        return namespace;    
    }
    
    void setNamespace(String namespace) { 
        this.namespace = namespace;    
    }

    boolean validate() {
        if (name.length() == 0) {
            System.out.println("module name is null");
            return false;
        }
        return true;
    }
    
    String toStringAllConfigParam() {
        StringBuffer sb = new StringBuffer();
        sb.append("Module name<" + getName() + ">\n");
        for (ConfigNode configNode: children) {
            if (configNode.isConfigParameter()) {
                
                sb.append(configNode);
                sb.append("\n");
            }
            
        }
        
        return sb.toString();
    }

    @Override
    public String toString() {
        return "ConfigModule [namespace=" + namespace + ", name=" + name + "]";
    }
    
    
}

/**
 * @author diag
 *
 */
class ConfndContainer extends ConfigNode {

    
    ConfndContainer() {
        super();
        
        type = NodeType.container;
        children = new ArrayList<ConfigNode> ();
    }

    @Override
    public String toString() {
        return "ConfndContainer [" + super.toString() +"]";
    }
    
    
}

/**
 * @author diag
 *
 */
class ConfndLeaf extends ConfigNode {
    
    ConfndLeaf() {
        super();
        type     = NodeType.leaf;
        children = null;
    }

    @Override
    public String toString() {
        return "ConfndLeaf [name=" + name + "]";
    }
    
}


/**
 * @author diag
 *
 */
class ConfndLeafList extends ConfigNode {

    String maxElements;
    ConfndLeafList() {
        super();
        type        = NodeType.leaf_list;
        children    = null;
        maxElements = "";
    }

    @Override
    public String toString() {
        return "ConfndLeafList [name=" + name + ", dataType=" + dataType.getName() + "]";
    }
}


class ConfndList extends ConfigNode {

    String listKey;
    String maxElements;
    
    ConfndList() {
        super();
        
        listKey = "";
        maxElements = "";
        
        type = NodeType.list;
        children = new ArrayList<ConfigNode> ();
    }
    
    //String get
    @Override
    public String toString() {
        return "ConfndList [key=" + listKey
                + ", maxElements=" + maxElements
                + ", " + super.toString()
                + "]";
    }

    
}



class ConfndTypedef extends ConfigNode {
    
	//private String typeName;
	
    ConfndTypedef() {
        super();
        
        //typeName = "";
        type = NodeType.type_def;
    }
    
    
    /*
    public String getTypeName() {        
    	return typeName;
	}

    public void setTypeName(String typeName) { 
    	this.typeName = typeName;    
    }
    */
    
    
    @Override
    public String toString() {
        return "ConfndTypedef [yangFile=" + yangFile.getYangFileName() 
        	 + ", name=" + getName()
        	 + ", description (len)=" + description.length()
        	 + ", dataType = " + dataType
             + "]\n\n";
    }
}


class ConfndBuiltin extends ConfndTypedef {
    
    /* where does the type defined, in module or submodule */
    static String builtinTypes[] = { "enumeration", "union", "string", "int32", "uint32" };
    
    
    ConfndBuiltin(String _name) {
    	super();
    	
    	assert (isBuiltinType(name));
    	
        type = NodeType.type_def;
        setName(_name);
    }
    
    static boolean isBuiltinType(String typeName) {
        for (String type: builtinTypes) {
            if (typeName.contentEquals(type))
                return true;
        }
        
        return false;
    }
    
    static String[] getBuiltinTypes() {
    	return builtinTypes;
    }
    
    @Override
    public String toString() {
        return "ConfndBuiltin [builtin type name=" + getName() + "]\n\n";
    }
    
}

class DataTypeChoice {
    String name;
    String value;
    String descr;
    
    DataTypeChoice() {
        name = "";
        value= "";
        descr = "";
    }

    @Override
    public String toString() {
        return "DataTypeChoice [name=" + name + ", value=" + value
                + ", description=" + descr + "]";
    }
    
    
}

class ConfndType extends ConfigNode {

    static Set<String> yangTypeSet = new HashSet<String> ();
    static Set<String> gwTypeSet   = new HashSet<String> ();
    static boolean inited = false;
    
    static void Init() {
        if (inited)
            return;
        yangTypeSet.add("enumeration");
        yangTypeSet.add("union");
        yangTypeSet.add("string");
        yangTypeSet.add("int32");
        yangTypeSet.add("uint32");
        
        gwTypeSet.addAll(yangTypeSet);
        gwTypeSet.add("ip-address");
        gwTypeSet.add("ipv6-address");
        gwTypeSet.add("ipv4-address");
        //gwTypeSet.add("");
        //gwTypeSet.add("enumeration");
        //gwTypeSet.add("enumeration");
        
        inited = true;
    }
    
    /*
    static String gw_builtinTypes[] = { 
        "enumeration", 
        "union", 
        "string", 
        "int32", 
        "uint32" };
    */
    ConfndTypedef typeDefinition;
    
    String typeName;
    String definedModule;   // where define this data type
    
    List <DataTypeChoice> enumValList;
    List <String>         patternList;
    String                range;
    String                length;
    
    ConfndType() {
        
        super();
        
        if (!inited) {
            Init();
            inited = true;
        }
            
        type = NodeType.data_type;
        
        typeDefinition     = null;
        typeName     = "";
        definedModule   = "";
        enumValList  = new ArrayList<DataTypeChoice> ();
        patternList  = new ArrayList<String> ();
        range        = "";
        length       = "";
    }
    
    
    public ConfndTypedef getTypeDefinition() {
        return typeDefinition;
    }

    public void setTypeDefinition(ConfndTypedef dataType) {
        this.typeDefinition = dataType;
    }

    boolean isEnum() {
        return typeName.contentEquals("enumeration");
    }
    
    boolean isString() {
        return typeName.contentEquals("string");
    }

    
    String getRange() {
        StringBuffer _range=new StringBuffer();
        
        if (isEnum()) {
            for (DataTypeChoice choice: getEnumValList()) {
                if (_range.length() != 0)
                    _range.append("\n");
                _range.append("("+choice.name + ":" + choice.value + ")");
            }
        }
        else if (isString()) {
            for (String pattern: patternList) {
                if (_range.length() != 0)
                    _range.append("\n");
                _range.append("pattern:" + pattern);                
            }
            
            if (!length.isEmpty()) {
                if (_range.length() != 0)
                    _range.append("\n");
                _range.append("length: " + length);
            }
        }
        else {
            _range.append(range);
        }
        
        return _range.toString();
    }

    
    String getTypeName() {
        Init();
        
        if (gwTypeSet.contains(typeName)) {
            return typeName;
        }
        
        if (typeDefinition.children==null) {
            System.out.println("type definition is null, typeName=" + typeName);
            return typeName;
        }
        
       
        ConfndTypedef _type = typeDefinition;
        
        while (!gwTypeSet.contains(_type.getName())) {
            if (typeDefinition.children.isEmpty())
                return _type.getName();
            
            for (ConfigNode tmp: typeDefinition.children) {
                if (tmp.type == ConfigNode.NodeType.type_def) {
                    _type = (ConfndTypedef)(tmp);
                    break;
                }
            }
        }
        
        return _type.getName();
    }

    public List<DataTypeChoice> getEnumValList() {
        return enumValList;
    }


    public void setEnumValList(List<DataTypeChoice> enumValList) {
        this.enumValList = enumValList;
    }


    boolean parseFullName(String fullTypeName) {
        String[] keywords = fullTypeName.split(":");
        
        if (keywords.length == 0) {
            return false;
        }
        
        else if (keywords.length == 1) {
            typeName = keywords[0];
            
            if (!ConfndBuiltin.isBuiltinType(typeName))
                definedModule = yangFile.getModuleName();
        }
        
        if (keywords.length == 2) {
            typeName = keywords[1];
        
            String prefix = keywords[0];
            for (ReferYangFile refer: yangFile.getReferFiles()) {
                //System.out.println("prefix=<" + prefix + ">, refer-prefix=<" + refer.prefix + ">");
                if (refer.prefix.contentEquals(prefix)) {
                    definedModule = refer.getModuleName();
                    break;
                }
            }            
        }
        
        return true;
    }
    
    /*
    String getModuleName(String fullTypeName, YangFile yangFile) {
        String[] keywords = fullTypeName.split(":");
        String prefix="";
        if (keywords.length == 1) {
            return "";
        }
        else if (keywords.length == 2) {
            prefix = keywords[0];
            
        }
        return "";
    }
*/
    @Override
    public String toString() {
        return "\nConfndType [typeName=" + typeName + ", moduleName=" + definedModule
                + ", range=" + range + ", length=" + length
                + ", enumValList=" + enumValList 
                + ", patternList=" + patternList + "]";
    }    
}

