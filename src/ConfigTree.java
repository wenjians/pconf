


import java.util.*;


public class ConfigTree {

    List<ConfigModule> moduleList;
    
    /* 
     * typeDefs include all the type definitions
     *      key: is the fullYangPath type name, e.g. gw-ip-type:vlan
     */
    HashMap<String , ConfigTypedef> typeDefs;
    
    /*
     * configTable include all the parameter configuration, which is used to quickly
     * each each of parameter definition
     *      Key: is the full path name e.g. /syslog/save-mode
     */
    Hashtable<String, ConfigNode> configTable;
    
    
    ConfigTree() {
        moduleList = new ArrayList<ConfigModule> ();
        
        typeDefs = new HashMap<String , ConfigTypedef> ();
        
        configTable = new Hashtable<String, ConfigNode>();
        //addBuitinTypes();
    }
    
    void addConfigModule(ConfigModule _mod) {
        moduleList.add(_mod);
    }
    
    boolean addConfig(String configPathName, ConfigNode configNode) {
        if (!configNode.isLeaf() && !configNode.isLeafList())
            return false;
        
        if (findConfig(configPathName) != null) {
            return false;
        }
        
        configTable.put(configPathName, configNode);
        //System.out.println("ConfigTree.addConfig, add configuration: " + configPathName);
        return true;
    }
    
    
    ConfigNode findConfig(String configPathName) {
        return configTable.get(configPathName);
    }
    
    void addTypedef(String moduleName, String typeName, ConfigTypedef typedef) {
        
    	String fullTypeName = ConfigTypedef.getFullTypeName(moduleName, typeName);
        
        typeDefs.put(fullTypeName, typedef);
        //System.out.println("type add to configure tree:" + typedef);
    }
    
    
    void addAllBuiltinTypedef() {
        /* Yang built in types */
        addTypedef("", "enumeration", new ConfigTypedefYang("enumeration"));
        addTypedef("", "union", new ConfigTypedefYang("union"));
        addTypedef("", "string", new ConfigTypedefYang("string"));
        addTypedef("", "int32", new ConfigTypedefYang("int32"));
        addTypedef("", "uint32", new ConfigTypedefYang("uint32"));
        
        /* the following is the gw builtin types */
        addTypedef("MGWYangExtensions", "ip-address", new ConfigTypedefGw("MGWYangExtensions", "ip-address"));
        addTypedef("MGWYangExtensions", "ipv6-address", new ConfigTypedefGw("MGWYangExtensions", "ipv6-address"));
        addTypedef("MGWYangExtensions", "ipv4-address", new ConfigTypedefGw("MGWYangExtensions", "ipv4-address"));
        addTypedef("MGWYangExtensions", "string-word", new ConfigTypedefGw("MGWYangExtensions", "string-word"));
        addTypedef("MGWYangExtensions", "mac-addres", new ConfigTypedefGw("MGWYangExtensions", "mac-addres"));
        addTypedef("MGWYangExtensions", "mem-address", new ConfigTypedefGw("MGWYangExtensions", "mem-address"));
    }
    
    ConfigTypedef getTypeDef(ConfigType dataType) {
        
    	
    	/*
        if (dataType.defModule==null || dataType.defModule.trim().length()==0)
            return typeDefs.get(dataType.getName());
       	*/
        
        String fullTypeName = ConfigTypedef.getFullTypeName(dataType.defModule, dataType.getName());
        //System.out.println("getTypeDef, fullname=<" + fullTypeName + ">");
        
        return typeDefs.get(fullTypeName);
    }
    
    String toStringTypedef() {
        return typeDefs.toString();
    }
    
    
    
    String toStringAllConfigModule() {
        StringBuffer sb = new StringBuffer();
        for (ConfigModule configModule: moduleList) {
            sb.append(configModule.toStringAllConfigParam());
        }
        
        return sb.toString();
    }
}
