


import java.util.*;


/**
 * @author wenjians
 *
 */
/* it is used for "range" and "must" in Yang */
class ConfigCondition {
    String description;
    String condition;
    String errMsg;
    
    ConfigCondition() {
        description = "";
        condition = "";
        errMsg = "";
    }

    @Override
    public String toString() {
        return "XPathMust [description=" + description + ", condition=" + condition 
             + ", errMsg=" + errMsg + "]";
    }
}

class ConfigNode {

	static enum NodeType { 
		INVALID, MODULE, CONTAINER, LEAF, LEAF_LIST, LIST, 
		DATA_TYPE, TYPE_DEF, TYPE_BUILTIN, TYPE_GWBUILTIN 	
	}
    
    static enum Scope { INVALID, SYSTEM, CUSTOMER, ELEMENT }
    
    static PConfError errProc = PConfError.getInstance();
    
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
    String limit_when;
    String units;
    String configurable;
    String status;
    String mandatory; /* only for leaf */
    
    /* max and min elements is only valid for list and leaf-list */
    String maxElements;
    String minElements;
    String orderedBy;
    
    ConfigType dataType;
    List <ConfigCondition> limit_must;
    
    /* the following is gw specific defined attribution */
    Scope  gw_scope;
    String gw_add_rel;
    String gw_mod_rel;
    String gw_service_impact;	/* service impact for the parameter changes */
    String gw_notes;
    String gw_feature_id;
        
    
    protected ConfigNode() {
        type            = NodeType.INVALID;
        name            = "";
        children        = null; // some node don't have child
        parent          = null;
        description     = "";
        defaultVal      = "";
        dataType        = null;
        configurable    = "true";
        status			= "";
        limit_must      = null;
        maxElements = "";
        minElements = "";
        orderedBy = "";
        mandatory = "false";
        units = "";
        
        gw_scope        = Scope.INVALID;
        gw_add_rel  	= "";
        gw_mod_rel  	= "";
        gw_service_impact = "";
        gw_notes        = "";
        gw_feature_id   = "";
        
        
    }
    
    boolean isConfigParameter()  {
        return ((type == NodeType.CONTAINER) ||
                (type == NodeType.LEAF)      ||
                (type == NodeType.LEAF_LIST) ||
                (type == NodeType.LIST));
    }
    
    
    //invalid, module, container, leaf, leaf_list, list, data_type, type_def
    
    boolean isConfigModule()    { return type == NodeType.MODULE;       }
    boolean isContainer()       { return type == NodeType.CONTAINER;    }
    boolean isLeaf()            { return type == NodeType.LEAF;         }
    boolean isLeafList()        { return type == NodeType.LEAF_LIST;    }
    boolean isDataType()        { return type == NodeType.DATA_TYPE;    }
    boolean isTypeDef()         { return type == NodeType.TYPE_DEF;     }
    boolean isBuiltin()			{ return type == NodeType.TYPE_BUILTIN;	}
    
    
    boolean isConfigurable() {
        return configurable.contentEquals("true");
    }
    
    
    public Scope getScope() {
        return gw_scope;
    }
    
    public String getScopeName() {
        String _scopeName;
        
        if (gw_scope == Scope.INVALID) {
            _scopeName = "";
        } else {
            _scopeName = gw_scope.toString().toLowerCase();
        }
        
        return _scopeName;
    }

    public void setScope(String scope) {
        if (scope.contentEquals("system")) {
            this.gw_scope = Scope.SYSTEM;
        }
        else if (scope.contentEquals("customer")) {
            this.gw_scope = Scope.CUSTOMER;
        }
        else if (scope.contentEquals("element")) {
            this.gw_scope = Scope.ELEMENT;
        }
        else {
            String errMsg;
            errMsg = String.format("unknow scope <%s> defined in node <%s> of yang file<%s>\n", 
                          scope, getName(), yangFile.getModuleName());
            PConfError.getInstance().addMessage(errMsg);
        }
            
    }
    
    
    /* here only assume that only two level is derived type definition */
    public ConfigType getGwBuiltinType() {
        //System.out.println(dataType.type.toString() + ", type name=" + dataType.getName());
        
        if (ConfigTypeBuiltin.isGwBuiltin(dataType.getName())) {
            return dataType;
        }
        
        if (dataType.typeDefinition == null) {
            System.out.println("dataType.typeDefinition is null");
        } else {
            return dataType.typeDefinition.getGwBuiltinType();
        }
        
        return null;
    }
    
    public String getRecursionDefault() {
        /* if the derived type/node already define the default value, then return */
        if (getDefaultVal().length() != 0) {
            return getDefaultVal();
        }

        /* get the default defined in type definition */
        if ((dataType != null) && (dataType.typeDefinition != null)) {
            dataType.typeDefinition.getRecursionDefault();
        }
        
        return "";
    }

    /*
    public String getRecursionRange() {
        if (isNumber)
        
        return null;
    }
    */
    
    public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getUnits() {
        return units;
    }
	
	public String getRecursionUnits() {
	    //System.out.println("node:" + getName() + ", units=" + getUnits());
	    if (getUnits().trim().length() != 0) {
	        return getUnits();
	    }
	    
	    if ((dataType != null) && (dataType.typeDefinition!=null)) {
	        return dataType.typeDefinition.getRecursionUnits();
	    }
	    
	    return "";
	}

    public void setUnits(String units) {
        this.units = units;
    }

    public String getAddRelease() {
        return gw_add_rel;
    }

    public void setAddRelease(String gw_add_release) {
        this.gw_add_rel = gw_add_release;
    }

    public String getModRelease() {
        return gw_mod_rel;
    }

    public void setModRelease(String gw_mod_release) {
        this.gw_mod_rel = gw_mod_release;
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
        StringBuffer sb = new StringBuffer();
        
        sb.append(description.replace("\n", " "));
        
        if ((dataType != null) &&
            (dataType.enumValList != null) &&
            (dataType.getName().contentEquals("enumeration"))) {
            for (ConfigDataEnum choice: dataType.enumValList) {
                
                if (choice.descr.length() == 0)
                    continue;
                
                if (sb.length() == 0) {
                    sb.append("'");
                } else {
                    sb.append("\n");
                }
               
                sb.append("-" + choice.name + ":");
                sb.append(choice.descr.replace("\n", " "));
            }
        }
        
        return sb.toString();
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

    /* set the parent of the current node, it will also inherit parameters
     * from the parent.
     */
    public void setParent(ConfigNode _parent) {
        this.parent = _parent;
        
        configurable = _parent.configurable;
        status       = _parent.getStatus();
        
        gw_scope 		= _parent.gw_scope;
        gw_add_rel 		= _parent.gw_add_rel;
        gw_mod_rel 		= _parent.gw_mod_rel;
        gw_feature_id 	= _parent.gw_feature_id;
        gw_service_impact = _parent.gw_service_impact;
    }

    void setName(String _name)  { 
        name = _name;             
    }
    
    String getName() { 
        return name;              
    }
    
    
    /* this function return the middle name (remove the module name and leaf name
     * which is easy to show the hierarchy, e.g. 
     *     realm/realm-table/ip-if/ip => realm-table/ip-if 
     */
    String getMiddleName() {
    	
        List<String> keyword = new ArrayList<String> ();
        
        ConfigNode curNode=getParent();
        while (!curNode.isConfigModule()) {
        	keyword.add(curNode.getName());
        	curNode = curNode.getParent();
        }
            
        String middleName="";
        for (int i=keyword.size()-1; i>=0; i--) {
        	middleName += keyword.get(i) ;
        	
        	if (i>0)
        		middleName += '/';
        }
        
        return middleName ;
    }    
    
    /* this function return the full path or the unique system level path name
     *     e.g. realm/realm-table/ip-if/ip  
     */
    String getFullPathName() {
        List<String> keyword = new ArrayList<String> ();
        
        String fullPathName="/";
        ConfigNode curNode=this;
        
        
        if (isConfigModule()) {
        	return getName();
        }
        
        
        do {
        	curNode = curNode.getParent();
        	keyword.add(curNode.getName());
        } while (!curNode.isConfigModule());
            

        
        for (int i=keyword.size()-1; i>=0; i--) {
            fullPathName += keyword.get(i) + "/";
        }
        
        return fullPathName + getName();
    }   

    @Override
    public String toString() {
        return "ConfigNode [type=" + type.toString() + ", yangFile=" + yangFile.getModuleName() 
                + ", name=" + name + ", parent=" + parent.getName() 
                + ", description=" + description 
                + ", type=" + dataType.getName() + ", units=" + units
                + ", defaultVal=" + defaultVal 
                + ", configurable" + configurable + ", status=" + status
                + ", mandatory=" + mandatory
                + ", scopeVal=" + getScope()
                + ", add release=" + gw_add_rel + ", feature id = " + gw_feature_id 
                + ", mod release=" + gw_mod_rel + ", gw_notes" + gw_notes
                + ", service impact=" + gw_service_impact + "notes=" + gw_notes
                + ", children=" + children
                + ", must=" + limit_must
                + ", when=" + limit_when
                + "]\n"; 
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
        type = NodeType.MODULE;
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
class ConfigContainer extends ConfigNode {

    
    ConfigContainer() {
        super();
        
        type = NodeType.CONTAINER;
        children = new ArrayList<ConfigNode> ();
        limit_must = new ArrayList <ConfigCondition> ();
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
class ConfigLeaf extends ConfigNode {
    
    ConfigLeaf() {
        super();
        
        type = NodeType.LEAF;
        children = null;
        limit_must = new ArrayList <ConfigCondition> () ;
    }

    @Override
    public String toString() {
        return "ConfndLeaf [name=" + name + super.toString() + "]" ;
    }
    
}


/**
 * @author diag
 *
 */
class ConfigLeafList extends ConfigNode {

    
    ConfigLeafList() {
        super();
        
        type        = NodeType.LEAF_LIST;
        children    = null;
        
        limit_must = new ArrayList <ConfigCondition> ();
    }

    
    @Override
    public String toString() {
        return "ConfndLeafList [name=" + name + ", dataType=" + dataType.getName() 
              + super.toString() + "]";
    }
}


class ConfigList extends ConfigNode {

    String listKey;
    
    ConfigList() {
        super();
        
        listKey = "";
        type = NodeType.LIST;
        children = new ArrayList<ConfigNode> ();
        limit_must = new ArrayList <ConfigCondition> () ;
    }
    
    @Override
    public String toString() {
        return "ConfndList [key=" + listKey + ", " + super.toString() + "]";
    }

    
}


class ConfigTypedef extends ConfigNode {
    
    ConfigTypedef() {
        super();
        
        type 	 = NodeType.TYPE_DEF;
        children = null;
    }
    
    
    @Override
    public String toString() {
        return "ConfndTypedef [yangFile=" + yangFile.getYangFileName()
        	 + ", name=" + getName()
        	 + ", description (len)=" + description.length()
        	 + ", dataType = " + dataType
             + "]\n\n";
    }
}

/* include GW builtin and Yang Builtin */
class ConfigTypeBuiltin extends ConfigTypedef {
    
	static boolean inited = false;
	
	static Set<String> yangBuiltin = new HashSet<String> ();
    static Set<String> gwBuiltin = new HashSet<String> ();

    static void Init() {
        if (inited)
            return;

        /* Yang built in types */
        yangBuiltin.add("enumeration");
        yangBuiltin.add("union");
        yangBuiltin.add("string");
        yangBuiltin.add("int32");
        yangBuiltin.add("uint32");
        
        /* gateway built in types */
        gwBuiltin.addAll(yangBuiltin);
        gwBuiltin.add("ip-address");
        gwBuiltin.add("ipv6-address");
        gwBuiltin.add("ipv4-address");
        gwBuiltin.add("string-word");
        gwBuiltin.add("mac-address");
        gwBuiltin.add("mem-address");
        
        inited = true;
    }
    
    static boolean isYangBuiltin(String typeName) {
        if (yangBuiltin.contains(typeName))
            return true;
        
        return false;
    }
    
    static Set<String> getYangBuiltinTypes() {
    	return yangBuiltin;
    }
    
    
    static boolean isGwBuiltin(String typeName) {
        if (gwBuiltin.contains(typeName))
            return true;
        
        return false;
    }
    
    ConfigTypeBuiltin(String _name) {
    	super();
    	
    	assert (isGwBuiltin(name));
    	
        type = NodeType.TYPE_BUILTIN;
        setName(_name);
    }
    
    @Override
    public String toString() {
        return "ConfndBuiltin [builtin type name=" + getName() + "]\n\n";
    }
    
}

class ConfigDataEnum {
    String name;
    String value;
    String descr;
    String status;
    
    ConfigDataEnum() {
        name = "";
        value= "";
        descr = "";
        status = "";
    }

    @Override
    public String toString() {
        return "DataTypeChoice [name=" + name + ", value=" + value
                + ", description=" + descr + ", status=" + status + "]";
    }
    
    
}

class ConfigType extends ConfigNode {

    String defModule;   // where define this data type
    String range;
    String length;

    List <String> patternList;
    List <ConfigDataEnum> enumValList;
    ConfigTypedef typeDefinition;
    
    ConfigType() {
        
        super();
            
        type = NodeType.DATA_TYPE;
        
        typeDefinition = null;
        defModule    = "";
        enumValList  = new ArrayList<ConfigDataEnum> ();
        patternList  = new ArrayList<String> ();
        range        = "";
        length       = "";
    }
    
    
    public ConfigTypedef getTypeDefinition() {
        return typeDefinition;
    }

    public void setTypeDefinition(ConfigTypedef dataType) {
        this.typeDefinition = dataType;
    }

    boolean isEnum() {
        return getGwBuiltinName().contentEquals("enumeration");
    }
    
    boolean isString() {
        return getGwBuiltinName().contentEquals("string") || getName().contentEquals("string-word");
    }
    
    boolean isNumber() {
        return getGwBuiltinName().contentEquals("uint32") ||getName().contentEquals("int32");
    }

    String getRange() {
        StringBuffer _range=new StringBuffer();
        
        if (isEnum()) {
            for (ConfigDataEnum choice: enumValList) {
                
                if (_range.length() != 0)
                    _range.append(", ");
                
                //_range.append("("+choice.name + ":" + choice.value + ")");
                _range.append(choice.name);
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
        
        else if (isNumber()){
            if (_range.length() != 0)
                _range.append("\n");
            
            _range.append(range);
        }
        
        else if (!ConfigTypeBuiltin.isGwBuiltin(getName())) {
            if (typeDefinition != null) {
                return typeDefinition.dataType.getRange();
            }
        }
        
        return _range.toString();
    }

    /* get the range of CLI, which only apply for number and string,
     * and ONLY have the following range defined
     *      string: length
     *      number: range
     */
    String getCliRange() {
       
        if (isString() && !length.trim().isEmpty()) {
            return length;
        }
        
        else if (isNumber() && !range.trim().isEmpty()){
            return range;
        }
        
        else if (!ConfigTypeBuiltin.isGwBuiltin(getName())) {
            if (typeDefinition != null) {
                return typeDefinition.dataType.getRange();
            }
        }
        
        return "";
    }
    
    /* go through the definition, till it is GW bultin type definition */
    String getGwBuiltinName() {
        if (ConfigTypeBuiltin.isGwBuiltin(getName())) {
            return getName();
        }
        
        ConfigTypedef type_def = typeDefinition;
        if ((type_def != null) && (type_def.dataType != null)) {
            return type_def.dataType.getGwBuiltinName();
        }
            
        return getName();
    }
    
    

    /*
    public List<DataTypeEnum> getEnumValList() {
        return enumValList;
    }
    */

    /*
    public void setEnumValList(List<DataTypeEnum> enumValList) {
        this.enumValList = enumValList;
    }
    */


    boolean parseFullName(String fullTypeName) {
        String[] keywords = fullTypeName.split(":");
        
        /* at least one stream */
        if (keywords.length == 0) {
            errProc.addMessage("type name error in Node <" + fullTypeName
                             + "> in Yang file " + yangFile.getYangFileName());
            return false;
        }
        
        /* should be YANG built in type or type defined in same Yang file */
        else if (keywords.length == 1) {
            setName(keywords[0]);
            
            if (ConfigTypeBuiltin.isYangBuiltin(getName())) {
                defModule = "";
            }
            else {
                defModule = yangFile.getModuleName();
            }
        }
        
        else if (keywords.length == 2) {
            setName(keywords[1]);
        
            String prefix = keywords[0];
            for (ReferYangFile refer: yangFile.getReferFiles()) {
                if (refer.prefix.contentEquals(prefix)) {
                    defModule = refer.getModuleName();
                    break;
                }
            }            
        }
        
        return true;
    }
    
    @Override
    public String toString() {
        return "\nConfndType [typeName=" + getName() + ", moduleName=" + defModule
                + ", range=" + range + ", length=" + length
                + ", enumValList=" + enumValList 
                + ", patternList=" + patternList + "]";
    }    
}

