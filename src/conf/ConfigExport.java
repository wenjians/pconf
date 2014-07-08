package conf;


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
    void exportLeafEnter(ConfigNode configNode) {
        
    }

    void exportLeafExit(ConfigNode configNode) {
        
    }
    
    /* do nothing, this function to be over-writtn by sub-classes */ 
    void exportLeafListEnter(ConfigNode configNode) {
        
    }
    
    void exportLeafListExit(ConfigNode configNode) {
        
    }
    
    /* do nothing, this function to be over-writtn by sub-classes */ 
    void exportListEnter(ConfigNode configNode) {
        
    }
    
    void exportListExit(ConfigNode configNode) {
        
    }
    
    /* do nothing, this function to be over-writtn by sub-classes */ 
    void exportContainerEnter(ConfigNode configNode) {
        
    }

    void exportContainerExit(ConfigNode configNode) {
        
    }

    final void exportOneNode(ConfigNode configNode) {
        if (configNode.type == ConfigNode.NodeType.LEAF) {
            exportLeafEnter(configNode);
            exportLeafExit(configNode);
        }
        
        else if (configNode.type == ConfigNode.NodeType.LEAF_LIST) {
            exportLeafListEnter(configNode);
            exportLeafListExit(configNode);
        }
        
        else if (configNode.type == ConfigNode.NodeType.CONTAINER) {
            exportContainerEnter(configNode);
            
            for (ConfigNode node:configNode.children) {
                exportOneNode(node);
            }
            
            exportContainerExit(configNode);
        }
        
        else if (configNode.type == ConfigNode.NodeType.LIST) {
            exportListEnter(configNode);

            for (ConfigNode node:configNode.children) {
                exportOneNode(node);
            }
            
            exportListExit(configNode);
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



