package conf;


//import java.util.*;







import util.PConfError;


public class ConfigExport {
    
    ConfigTree configTree;
    PConfError errProc;
    

    String transferHTML(String str) {
        str = str.replace("&", "&amp;");
        str = str.replace("<", "&lt;");
        str = str.replace(">", "&gt;");
        str = str.replace("\"", "&quot;");
        str = str.replace(" ", "&#32;");
        //str = str.replace(" ", "&nbsp;");
        //str = str.replace("\n", "<br>");
        
        return str;
    }
    
    ConfigExport() {
        configTree = null;
        
        errProc = PConfError.getInstance();
    }
    
    
    
    void setConfigTree(ConfigTree _configTree) {
        configTree = _configTree;
    }
    
    /* do nothing, this function to be over-writtn by sub-classes */ 
    void exportLeaf(ConfigNode configNode) {
        
    }
    
    /* do nothing, this function to be over-writtn by sub-classes */ 
    void exportLeafList(ConfigNode configNode) {
        
    }
    
    /* do nothing, this function to be over-writtn by sub-classes */ 
    void exportList(ConfigNode configNode) {
        
        
    }
    
    /* do nothing, this function to be over-writtn by sub-classes */ 
    void exportContainer(ConfigNode configNode) {
        
    }
    
    final void exportOneNode(ConfigNode configNode) {
        if (configNode.type == ConfigNode.NodeType.LEAF) {
            exportLeaf(configNode);
        }
        
        else if (configNode.type == ConfigNode.NodeType.LEAF_LIST) {
            exportLeafList(configNode);
        }
        
        else if (configNode.type == ConfigNode.NodeType.CONTAINER) {
            exportContainer(configNode);
            
            for (ConfigNode node:configNode.children) {
                exportOneNode(node);
            }
        }
        
        else if (configNode.type == ConfigNode.NodeType.LIST) {
            exportList(configNode);

            for (ConfigNode node:configNode.children) {
                exportOneNode(node);
            }
        }
        
        else {
            String errMsg;
            
            errMsg = "un-supported type node type (" + configNode.type.toString()
                   + "%s) when exporting to PDD\n"
                   + "Name:" + configNode.getName() 
                   + " in Yang file <" + configNode.getYangFile().getYangFileName() + ">"; 
            errProc.addMessage(errMsg);
        }        
    }
    
    protected void walkthrough() 
    {
        for (ConfigModule module: configTree.moduleList) {
            for (ConfigNode configNode: module.getChildren()) {
                exportOneNode(configNode);
            }               
        }
    }
}



