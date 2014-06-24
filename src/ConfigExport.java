

//import java.util.*;
import java.io.File;
import java.io.IOException;




import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableCellFormat;
import jxl.write.WritableFont;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;


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
    void exportOneLeafNode(ConfigNode configNode) {
        
    }
    
    /* do nothing, this function to be over-writtn by sub-classes */ 
    void exportOneLeafListNode(ConfigNode configNode) {
        
    }
    
    /* do nothing, this function to be over-writtn by sub-classes */ 
    void exportOneListNode(ConfigNode configNode) {
        
        
    }
    
    /* do nothing, this function to be over-writtn by sub-classes */ 
    void exportOneContainerNode(ConfigNode configNode) {
        
    }
    
    final void exportOneNode(ConfigNode configNode) {
        if (configNode.type == ConfigNode.NodeType.leaf) {
            exportOneLeafNode(configNode);
        }
        
        else if (configNode.type == ConfigNode.NodeType.leaf_list) {
            exportOneLeafListNode(configNode);
        }
        
        else if (configNode.type == ConfigNode.NodeType.container) {
            exportOneContainerNode(configNode);
            
            for (ConfigNode node:configNode.children) {
                exportOneNode(node);
            }
        }
        
        else if (configNode.type == ConfigNode.NodeType.list) {
            exportOneListNode(configNode);

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




class ConfigExportPDD extends ConfigExport{

    final static int COLUMN_MODULE     = 0;
    final static int COLUMN_NODE_TYPE  = 1;
    final static int COLUMN_HIERARCHY  = 2;
    final static int COLUMN_NAME       = 3;
    final static int COLUMN_CONF_ABLE  = 4;
    final static int COLUMN_SCOPE      = 5;
    final static int COLUMN_DESCP      = 6;
    final static int COLUMN_FORMAT     = 7;
    final static int COLUMN_UNITS      = 8;
    final static int COLUMN_RANGE      = 9;
    final static int COLUMN_DEFAULT    =10;
    final static int COLUMN_ADD_REL    =12;
    final static int COLUMN_MOD_REL    =13;
    final static int COLUMN_NOTES      =14;


    WritableWorkbook wwb;
    WritableSheet    sysParamSheet;
    

    //WritableCellFormat syntaxFormat;
    WritableCellFormat normalFormat;
    WritableCellFormat headerFormat;

    int curRow;
            

    String getDescription(ConfigNode configNode) {
        StringBuffer sb = new StringBuffer();
        
        sb.append(configNode.description.replace("\n", " "));
        
        
        if ((configNode.dataType.enumValList != null) &&
            (configNode.dataType.getName().contentEquals("enumeration"))) {
            for (ConfigDataEnum choice: configNode.dataType.enumValList) {
                
                if (choice.descr.length() == 0)
                    continue;
                
                if (sb.length() == 0) {
                    sb.append("'");
                } else {
                    sb.append("\n");
                }
               
                sb.append("-" + choice.name + ":");
                sb.append(choice.descr.replace("\n", " "));
                //sb.append("\n");
            }
        }
        
        return sb.toString();
    }
    
    @Override
    void exportOneLeafNode(ConfigNode configNode) {
        
        if (configNode.type != ConfigNode.NodeType.leaf)
            return;
        
        /* only configurable parameter should be export to PDD document */
        /*
        if (!configNode.isConfigurable()) {
            return;
        }
        */
                
        Label label;
            
        try {

            curRow++;
            
            //System.out.println(configNode);
            
            String moduleName = configNode.getYangModule().configModuleName;
            label = new Label(COLUMN_MODULE,curRow, moduleName,normalFormat);
            sysParamSheet.addCell(label);
            
            //label = new Label(COLUMN_NAME,curRow, configNode.getName(), normalFormat);
            label = new Label(COLUMN_NAME,curRow, configNode.getName(), normalFormat);
            sysParamSheet.addCell(label);
            
            label = new Label(COLUMN_HIERARCHY,curRow, configNode.getHierarchyName(), normalFormat);
            sysParamSheet.addCell(label);
            
            label = new Label(COLUMN_CONF_ABLE,curRow, configNode.configurable, normalFormat);
            sysParamSheet.addCell(label);
            
            label = new Label(COLUMN_NODE_TYPE,curRow, configNode.type.toString(), normalFormat);
            sysParamSheet.addCell(label);
            
            label = new Label(COLUMN_SCOPE,curRow, configNode.getScopeName(), normalFormat);
            sysParamSheet.addCell(label);
            
            label = new Label(COLUMN_DESCP,curRow, getDescription(configNode), normalFormat);
            sysParamSheet.addCell(label);
            
            label = new Label(COLUMN_FORMAT,curRow, configNode.dataType.getGwBuiltinName(), normalFormat);
            sysParamSheet.addCell(label);
            
            //label = new Label(COLUMN_UNITS,curRow, configNode.dataType.getUnits(), normalFormat);
            label = new Label(COLUMN_UNITS,curRow, configNode.getUnits(), normalFormat);
            sysParamSheet.addCell(label);
            
            label = new Label(COLUMN_RANGE,curRow, configNode.dataType.getRange(), normalFormat);
            sysParamSheet.addCell(label);
            
            label = new Label(COLUMN_DEFAULT,curRow, configNode.getDefaultVal(), normalFormat);
            sysParamSheet.addCell(label);
            
            label = new Label(COLUMN_ADD_REL,curRow, configNode.getAddRelease(), normalFormat);
            sysParamSheet.addCell(label);
            
            label = new Label(COLUMN_MOD_REL,curRow, configNode.getModRelease(), normalFormat);
            sysParamSheet.addCell(label);
            
            label = new Label(COLUMN_NOTES,curRow, configNode.getNotes(), normalFormat);
            sysParamSheet.addCell(label);
        } catch (WriteException e) {   
            e.printStackTrace();   
        }

    }

    
    void exportOneLeafListNode(ConfigNode configNode) {
        
        if (configNode.type != ConfigNode.NodeType.leaf_list)
            return;
        
        Label label;
        
        ConfigLeafList leaflist = (ConfigLeafList)configNode;
            
        try {

            curRow++;
            
            //System.out.println(configNode);
            String moduleName = configNode.getYangModule().configModuleName;
            label = new Label(COLUMN_MODULE,curRow, moduleName,normalFormat);
            sysParamSheet.addCell(label);
            
            label = new Label(COLUMN_NAME,curRow, configNode.getHierarchyName(), normalFormat);
            sysParamSheet.addCell(label);

            label = new Label(COLUMN_NODE_TYPE,curRow, configNode.type.toString(), normalFormat);
            sysParamSheet.addCell(label);

            if (leaflist.maxElements.length() != 0) {
                String range = "size: 0.." + leaflist.maxElements;
                
                label = new Label(COLUMN_RANGE,curRow, range, normalFormat);
                sysParamSheet.addCell(label);
            }
            
        } catch (WriteException e) {   
            e.printStackTrace();   
        }
    }
    
    
    void exportOneListNode(ConfigNode configNode) {
        
        if (configNode.type != ConfigNode.NodeType.list)
            return;
        
        Label label;
        ConfigList list = (ConfigList)configNode;
            
        try {

            curRow++;
            
            //System.out.println(configNode);
            String moduleName = configNode.getYangModule().configModuleName;
            label = new Label(COLUMN_MODULE,curRow, moduleName,normalFormat);
            sysParamSheet.addCell(label);
            
            label = new Label(COLUMN_NAME,curRow, configNode.getHierarchyName(), normalFormat);
            sysParamSheet.addCell(label);
            
            label = new Label(COLUMN_CONF_ABLE,curRow, configNode.configurable, normalFormat);
            sysParamSheet.addCell(label);
            
            label = new Label(COLUMN_SCOPE,curRow, configNode.getScopeName(), normalFormat);
            sysParamSheet.addCell(label);
            
            label = new Label(COLUMN_NODE_TYPE,curRow, configNode.type.toString(), normalFormat);
            sysParamSheet.addCell(label);

            label = new Label(COLUMN_DESCP,curRow, list.getDescription(), normalFormat);
            sysParamSheet.addCell(label);
            
            /*
            label = new Label(COLUMN_FORMAT,curRow, configNode.dataType.getTypeName(), normalFormat);
            sysParamSheet.addCell(label);
            
            
            label = new Label(COLUMN_UNITS,curRow, configNode.getUnits(), normalFormat);
            sysParamSheet.addCell(label);
            */
            
            if (list.maxElements.length() != 0) {
                String range = "size: 0.." + list.maxElements;
                label = new Label(COLUMN_RANGE,curRow, range, normalFormat);
                sysParamSheet.addCell(label);
            }            

            
            /*
            label = new Label(COLUMN_DEFAULT,curRow, configNode.getDefaultVal(), normalFormat);
            sysParamSheet.addCell(label);
            */
            
            label = new Label(COLUMN_ADD_REL,curRow, configNode.getAddRelease(), normalFormat);
            sysParamSheet.addCell(label);
            
            label = new Label(COLUMN_MOD_REL,curRow, configNode.getModRelease(), normalFormat);
            sysParamSheet.addCell(label);
            
            label = new Label(COLUMN_NOTES,curRow, configNode.getNotes(), normalFormat);
            sysParamSheet.addCell(label);

            
            
        } catch (WriteException e) {   
            e.printStackTrace();   
        }
    }
    
    
    void exportOneContainerNode(ConfigNode configNode) {
        
        if (configNode.type != ConfigNode.NodeType.container)
            return;
        
        Label label;
            
        try {

            curRow++;
            
            //System.out.println(configNode);
            String moduleName = configNode.getYangModule().configModuleName;
            label = new Label(COLUMN_MODULE,curRow, moduleName,normalFormat);
            sysParamSheet.addCell(label);
            
            label = new Label(COLUMN_NAME,curRow, configNode.getHierarchyName(), normalFormat);
            sysParamSheet.addCell(label);
            
            label = new Label(COLUMN_NODE_TYPE,curRow, configNode.type.toString(), normalFormat);
            sysParamSheet.addCell(label);
            
        } catch (WriteException e) {   
            e.printStackTrace();   
        }
    }
    
    void printHeadline() 
    {
        try { 
            // get the first sheet   
            sysParamSheet = wwb.createSheet("7510-PDD", 0);
    
            WritableFont Font1 = new WritableFont(WritableFont.ARIAL, 10); 
            WritableFont Font2 = new WritableFont(WritableFont.TAHOMA, 10, 
                                                  WritableFont.BOLD);
            
            //WritableFont.TAHOMA;
            //WritableFont.COURIER;   ARIAL
            //WritableFont.TIMES

            //syntaxFormat  = new WritableCellFormat(Font3);
            normalFormat  = new WritableCellFormat(Font1);
            headerFormat  = new WritableCellFormat(Font2);
    
            //syntaxFormat.setBackground(jxl.format.Colour.GRAY_25);
            headerFormat.setBackground(jxl.format.Colour.PALE_BLUE);
            normalFormat.setWrap(true);
            
            curRow = 0;
            
            sysParamSheet.setColumnView(COLUMN_MODULE,   10);
            sysParamSheet.setColumnView(COLUMN_NODE_TYPE,10);
            sysParamSheet.setColumnView(COLUMN_NAME,     15);
            sysParamSheet.setColumnView(COLUMN_HIERARCHY,30);
            sysParamSheet.setColumnView(COLUMN_CONF_ABLE,10);
            sysParamSheet.setColumnView(COLUMN_SCOPE,    10);
            sysParamSheet.setColumnView(COLUMN_DESCP,    50);
            sysParamSheet.setColumnView(COLUMN_FORMAT,   15);
            sysParamSheet.setColumnView(COLUMN_UNITS,     8);
            sysParamSheet.setColumnView(COLUMN_RANGE,    15);
            sysParamSheet.setColumnView(COLUMN_DEFAULT,  14);
            sysParamSheet.setColumnView(COLUMN_ADD_REL,  10);
            sysParamSheet.setColumnView(COLUMN_MOD_REL,  10);
            sysParamSheet.setColumnView(COLUMN_NOTES,    30);
    
            Label labelHead;
            labelHead = new Label(COLUMN_MODULE,curRow, "Module", headerFormat);
            sysParamSheet.addCell(labelHead);
            labelHead = new Label(COLUMN_NODE_TYPE,curRow, "NodeType", headerFormat);
            sysParamSheet.addCell(labelHead);
            labelHead = new Label(COLUMN_HIERARCHY,curRow, "hierarchy", headerFormat);
            sysParamSheet.addCell(labelHead);
            labelHead = new Label(COLUMN_NAME,curRow, "Name", headerFormat);  
            sysParamSheet.addCell(labelHead);
            labelHead = new Label(COLUMN_CONF_ABLE,curRow, "config", headerFormat);  
            sysParamSheet.addCell(labelHead);
            labelHead = new Label(COLUMN_SCOPE,curRow, "Scope", headerFormat);  
            sysParamSheet.addCell(labelHead);
            labelHead = new Label(COLUMN_DESCP,curRow, "Description", headerFormat);  
            sysParamSheet.addCell(labelHead);
            labelHead = new Label(COLUMN_FORMAT,curRow, "Format", headerFormat);  
            sysParamSheet.addCell(labelHead);
            labelHead = new Label(COLUMN_UNITS,curRow, "Units", headerFormat);  
            sysParamSheet.addCell(labelHead);
            labelHead = new Label(COLUMN_RANGE,curRow, "Range", headerFormat);  
            sysParamSheet.addCell(labelHead);
            labelHead = new Label(COLUMN_DEFAULT,curRow, "Default", headerFormat);  
            sysParamSheet.addCell(labelHead);
            labelHead = new Label(COLUMN_ADD_REL,curRow, "add_release", headerFormat);  
            sysParamSheet.addCell(labelHead);
            labelHead = new Label(COLUMN_MOD_REL,curRow, "mod_release", headerFormat);
            sysParamSheet.addCell(labelHead);
            labelHead = new Label(COLUMN_NOTES,curRow, "notes", headerFormat);
            sysParamSheet.addCell(labelHead);
        
        } catch (WriteException e) {   
            e.printStackTrace();   
        }
    }

    
    public void export(String fileName, ConfigTree _confTree)
    {
        try {
            wwb = Workbook.createWorkbook(new File(fileName));   
        } catch (IOException e) {   
            e.printStackTrace();   
        }   
        
        if(wwb!=null){
            printHeadline();
            setConfigTree(_confTree);
            super.walkthrough();
        }
  
        try {   
            wwb.write();   
            wwb.close();   
        } catch (IOException e) {   
            e.printStackTrace();   
        } catch (WriteException e) {   
            e.printStackTrace();   
        }   
    } 
}



