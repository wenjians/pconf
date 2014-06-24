


import java.util.*;


public class ConfigTree {

    List<ConfigModule> moduleList;
    
    HashMap<String , ConfigTypedef> typeDefs;
    
    
    ConfigTree() {
        moduleList = new ArrayList<ConfigModule> ();
        
        typeDefs = new HashMap<String , ConfigTypedef> ();
        
        //addBuitinTypes();
    }
    
    void addConfigModule(ConfigModule _mod) {
        moduleList.add(_mod);
    }
    
    void addTypedef(String moduleName, String typeName, ConfigTypedef typedef) {
        String key = typeName;
        
        if (!ConfigBuiltin.isYangBuiltin(typeName))
            key = moduleName + ":" + typeName;
        
        typeDefs.put(key, typedef);
        //System.out.println("type add to configure tree:" + typedef);
    }
    
    
    ConfigTypedef getTypeDef(ConfigType dataType) {
        /*
        String moduleName = "";
        
        if (dataType.definedModule == null || 
            dataType.definedModule.length() == 0) {
            if (!ConfndBuiltin.isBuiltinType(dataType.typeName))
                moduleName = dataType.getYangFile().getYangFileName();
            else
                moduleName = "";
        } else {
            moduleName = dataType.definedModule;
        }
        */
        
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
