


import java.util.*;


public class ConfigTree {

    List<ConfigModule> moduleList;
    
    HashMap<String , ConfndTypedef> typeDefs;
    
    
    ConfigTree() {
        moduleList = new ArrayList<ConfigModule> ();
        
        typeDefs = new HashMap<String , ConfndTypedef> ();
        
        //addBuitinTypes();
    }
    
    void addConfigModule(ConfigModule _mod) {
        moduleList.add(_mod);
    }
    
    void addTypeDef(String moduleName, String typeName, ConfndTypedef typedef) {
        String key = typeName;
        
        if (!ConfndBuiltin.isBuiltinType(typeName))
            key = moduleName + ":" + typeName;
        
        typeDefs.put(key, typedef);
        //System.out.println("type add to configure tree:" + typedef);
    }
    
    
    ConfndTypedef getTypeDef(ConfndType dataType) {
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
        if (dataType.definedModule==null || dataType.definedModule.length()==0)
            return typeDefs.get(dataType.typeName);
        
        return typeDefs.get(dataType.definedModule + ":" + dataType.typeName);
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
