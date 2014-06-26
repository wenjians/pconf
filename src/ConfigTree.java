


import java.util.*;


public class ConfigTree {

    List<ConfigModule> moduleList;
    
    HashMap<String , ConfigTypedef> typeDefs;
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
        String key = typeName;
        
        if (!ConfigBuiltin.isYangBuiltin(typeName))
            key = moduleName + ":" + typeName;
        
        typeDefs.put(key, typedef);
        //System.out.println("type add to configure tree:" + typedef);
    }
    
    
    ConfigTypedef getTypeDef(ConfigType dataType) {
        //System.out.println("getTypeDef, fullname=<" + dataType.definedModule + ":" + dataType.typeName + ">");
        if (dataType.defModule==null || dataType.defModule.length()==0)
            return typeDefs.get(dataType.getName());
        
        return typeDefs.get(dataType.defModule + ":" + dataType.getName());
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
