package conf;



import java.util.*;

import util.PConfError;


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

public class ConfigNode {

	static enum NodeType { 
		INVALID, MODULE, CONTAINER, LEAF, LEAF_LIST, LIST, 
		DATA_TYPE, TYPE_DEF, TYPEDEF_BUILTIN 	
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
    String gw_retrieval;
    String gw_critical;
    String gw_service_impact;   /* service impact for the parameter changes */
    String gw_external_impact;
    String gw_internal_impact;
    String gw_add_rel;
    String gw_mod_rel;
    
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
    
    public boolean isConfigModule()    { return type == NodeType.MODULE;       }
    public boolean isContainer()       { return type == NodeType.CONTAINER;    }
    public boolean isList()            { return type == NodeType.LIST;         }
    public boolean isLeaf()            { return type == NodeType.LEAF;         }
    public boolean isLeafList()        { return type == NodeType.LEAF_LIST;    }
    public boolean isDataType()        { return type == NodeType.DATA_TYPE;    }
    public boolean isTypeDef()         { return type == NodeType.TYPE_DEF;     }
    public boolean isBuiltin()         { return type == NodeType.TYPEDEF_BUILTIN; } 
    
    
    
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
    public String getBuiltinName() {
        /*
        System.out.println(dataType.type.toString() + ", type name=" + dataType.getName() 
                + ", GW builtin:" + dataType.typeDefinition.isBuiltin());
        System.out.println(dataType.typeDefinition);
        System.out.println("isBultin:" + isBuiltin());
        */
        
        if (isBuiltin()) {
            return getName().trim();
        }
        
        if (dataType.typeDefinition.isBuiltin()) {
            //System.out.println("dataTypeName: <" + dataType.getName().trim() + ">");
            return dataType.getName().trim();
        }
        
        
        return dataType.typeDefinition.getBuiltinName();
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

    
    public String getRetrieval() {
        return gw_retrieval;
    }

    public void setRetrieval(String gw_retrieval) {
        this.gw_retrieval = gw_retrieval;
    }

    public String getCritical() {
        return gw_critical;
    }

    public void setCritical(String gw_critical) {
        this.gw_critical = gw_critical;
    }


    public String getExternalImpact() {
        return gw_external_impact;
    }

    public void setExternalImpact(String gw_external_impact) {
        this.gw_external_impact = gw_external_impact;
    }

    public String getInternalImpact() {
        return gw_internal_impact;
    }

    public void setInternalImpact(String gw_internal_impact) {
        this.gw_internal_impact = gw_internal_impact;
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

    public String getDescription()  {  
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

    public void setName(String _name)  { 
        name = _name;             
    }
    
    public String getName() { 
        return name;              
    }
    
    public String getCliName(boolean isRequired) {
        String cliName="";
        if (dataType.isEnum()) {
            for (ConfigDataEnum enumVal: dataType.enumValList) {
                if (cliName.length()!=0) {
                    cliName += "|";
                }
                cliName += enumVal.name;
            }
            
            cliName = "{" + cliName + "}";
        } else if (isRequired) {
            cliName = "<" + getName() + ">";
        } else {
            cliName = "[" + getName() + "]";
        }
        
        return cliName.trim();
    }

    public String getRangeMin() {
        /* set the minimum and maximum value from ConfigNode */
        String minValue="0";
        String[] range_list = null;
        
        if (dataType.isNumber() || dataType.isString()) {
            // split with "|" or ".." with regular express
            //System.out.println("nodename=" + configNode.getName() + "range=" + dataType.getCliRange());
            range_list = dataType.getCliRange().split("(\\|)|(\\.\\.)");
            
            if ((range_list != null) && (range_list.length >= 1)) {
                minValue = range_list[0].trim();
            }
        }
        
        return minValue.trim();
    }
    
    public String getRangeMax() {
        String maxValue="0";
        String[] range_list = null;
        if (dataType.isNumber() || dataType.isString()) {
            // split with "|" or ".." with regular express
            //System.out.println("nodename=" + configNode.getName() + "range=" + dataType.getCliRange());
            range_list = dataType.getCliRange().split("(\\|)|(\\.\\.)");
            
            if ((range_list != null) && (range_list.length >= 1)) {
                maxValue = range_list[range_list.length-1].trim();
            }
        }
        
        return maxValue;
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
    
    
    public String getListKey() {
        return listKey;
    }



    public void setListKey(String listKey) {
        this.listKey = listKey;
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
    
    static String getFullTypeName(String yangFile, String typeName) {
        String fullTypeName = typeName.trim();
        
        if ((yangFile != null) && (!yangFile.trim().isEmpty())) {
            fullTypeName = yangFile.trim() + ":" + fullTypeName;
        }

        return fullTypeName;
    }
    
    
    @Override
    public String toString() {
        return "ConfndTypedef [yangFile=" + yangFile.getYangFileName()
        	 + ", name=" + getName()
        	 //+ ", description (len)=" + description.length()
        	 //+ ", dataType = " + dataType
             + "]\n";
    }
}


/* include GW builtin and Yang Builtin */
class ConfigTypeBuiltin extends ConfigTypedef {
    String defYangFileName;
    /* the formst of string type in set is combined with yang file
     *  Yang builtin type: typename, e.g. "uint32"
     *  GW   builtin type: yangFile:typeName, e.g. "MGWExtension:ip-address" 
     */
    static Set <String> gwBuiltin = new HashSet<String> ();

    static boolean isBuiltin(String yangFile, String typeName) {
        String fullTypeName = getFullTypeName(yangFile, typeName);
        if (gwBuiltin.contains(fullTypeName))
            return true;
        
        return false;
    }
    
    ConfigTypeBuiltin(String yangFile, String _name) {
    	super();
    	
        type = NodeType.TYPEDEF_BUILTIN;
        
        defYangFileName = yangFile.trim();
        
        setName(_name.trim());
        gwBuiltin.add(getName());
        //System.out.println("GW built-in added: <" + defYangFileName + ":" + getName() + ">");
    }
    
    
    @Override
    public String toString() {
        return "ConfigTypeBuiltin [type name=" + getName() + "]";
    }
}


/* include GW builtin and Yang Builtin */
/*
class ConfigTypedefYang extends ConfigTypedefGw {
    
    static Set<String> yangBuiltin = new HashSet<String> ();

    static boolean isBuiltin(String yangFile, String typeName) {
        String fullTypeName = getFullTypeName(yangFile, typeName);
        if (yangBuiltin.contains(fullTypeName))
            return true;
        
        return false;
    }
    
    ConfigTypedefYang(String _name) {

        super("", _name);
        
        type = NodeType.TYPEDEF_YANG_BUILTIN;
        
        yangBuiltin.add(getName());
        //System.out.println("Yang builtin added: <" + getName() + ">");
    }
    
    @Override
    public String toString() {
        return "ConfigTypedefYang [type name=" + getName() + "]";
    }
    
}
*/

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

    /* go through the definition, till it is GW bultin type definition */
    /*
    public String getBuiltinName() {
        if (typeDefinition.isBuiltin()) {
            return typeDefinition.getName();
        }
        
        if (typeDefinition.dataType != null) {
            return typeDefinition.dataType.getBuiltinName();
        }
            
        return getName();
    }
    */
    
    boolean isEnum() {
        /*
        if (typeDefinition == null) {
            System.out.println("typedef is null");
        }
        */
        
        //System.out.println("\n\n\nisEnum: Node:" + getName() + ", builtin typeName=" + typeDefinition.getBuiltinName());
        return typeDefinition.getBuiltinName().contentEquals("enumeration");
    }
    
    boolean isString() {
        String builtinTypeName = typeDefinition.getBuiltinName().trim();
        return builtinTypeName.contentEquals("string") || 
               builtinTypeName.contentEquals("string-word");
    }
    
    boolean isNumber() {
        String builtinTypeName = typeDefinition.getBuiltinName().trim();
        return builtinTypeName.contentEquals("uint32") ||
               builtinTypeName.contentEquals("int32");
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
        
        else  if (typeDefinition.dataType != null){
            return typeDefinition.dataType.getRange();
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
        
        else if (!typeDefinition.isBuiltin()) {
            return typeDefinition.dataType.getRange();
        }
        
        return "";
    }
    
    /* go through the definition, till it is GW bultin type definition */
    /*
    String getBuiltinName() {
        if (typeDefinition.isBuiltin()) {
            return typeDefinition.getName();
        }
        
        if (typeDefinition.dataType != null) {
            return typeDefinition.dataType.getBuiltinName();
        }
            
        return getName();
    }
    */
    
    

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
            
            if (ConfigTypeBuiltin.isBuiltin("", getName())) {
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

