package conf;

import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;


public class ConfigExportPDD extends ConfigExport{

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
    final static int COLUMN_ADD_REL    =11;
    final static int COLUMN_MOD_REL    =12;
    final static int COLUMN_NOTES      =13;
    
    FileOutputStream outFile;
    Workbook workBook;
    Sheet workSheet;
    
    CellStyle csTitle ;
    CellStyle csGroup ;
    CellStyle csNormal;
    CellStyle csKeywd;
    
    Font ftTitle;
    Font ftGroup;
    Font ftNormal;
    Font ftKeywd;
    
    Row row ;
    Cell cell;
    
    int curRow;
            
    public ConfigExportPDD () {
        curRow = 0;
    }
    
    private void writeCurRowCell(int column, CellStyle style, String value) {
        cell = row.createCell(column);
        cell.setCellStyle(style);
        cell.setCellValue(value);
    }
    
    @Override
    void exportLeaf(ConfigNode configNode) {
        if (!configNode.isLeaf())
            return;
        
        curRow++;
        row = workSheet.createRow(curRow);
        
        //System.out.println(configNode.getName());
        writeCurRowCell(COLUMN_MODULE, csNormal, configNode.getYangModule().configModuleName);
        writeCurRowCell(COLUMN_NODE_TYPE, csNormal, configNode.type.toString().toLowerCase());
        writeCurRowCell(COLUMN_HIERARCHY, csNormal, configNode.getMiddleName());
        writeCurRowCell(COLUMN_NAME, csNormal, configNode.getName());
        writeCurRowCell(COLUMN_CONF_ABLE, csNormal, configNode.configurable);
        writeCurRowCell(COLUMN_SCOPE, csNormal, configNode.getScopeName());
        writeCurRowCell(COLUMN_DESCP, csNormal, configNode.getDescription());
        writeCurRowCell(COLUMN_FORMAT, csNormal, configNode.getBuiltinName());
        writeCurRowCell(COLUMN_UNITS, csNormal, configNode.getUnits());
        writeCurRowCell(COLUMN_RANGE, csNormal, configNode.dataType.getRange());
        writeCurRowCell(COLUMN_DEFAULT, csNormal, configNode.getDefaultVal());
        writeCurRowCell(COLUMN_ADD_REL, csNormal, configNode.getAddRelease());
        writeCurRowCell(COLUMN_MOD_REL, csNormal, configNode.getModRelease());
        writeCurRowCell(COLUMN_NOTES, csNormal, configNode.getNotes());
    }

    
    void exportLeafList(ConfigNode configNode) {
        if (!configNode.isLeafList())
            return;
        
        curRow++;
        row = workSheet.createRow(curRow);
        
        //System.out.println(configNode.getName());
        writeCurRowCell(COLUMN_MODULE, csNormal, configNode.getYangModule().configModuleName);
        writeCurRowCell(COLUMN_NODE_TYPE, csNormal, configNode.type.toString().toLowerCase());
        writeCurRowCell(COLUMN_HIERARCHY, csNormal, configNode.getMiddleName());
        writeCurRowCell(COLUMN_NAME, csNormal, configNode.getName());
        writeCurRowCell(COLUMN_CONF_ABLE, csNormal, configNode.configurable);
        writeCurRowCell(COLUMN_SCOPE, csNormal, configNode.getScopeName());
        writeCurRowCell(COLUMN_DESCP, csNormal, configNode.getDescription());
        
        if (configNode.maxElements.length() != 0) {
            String range = "size: 0.." + configNode.maxElements;
            cell = row.createCell(COLUMN_RANGE);
            cell.setCellStyle(csNormal);
            cell.setCellValue(range);
        }
            
    }
    
    
    void exportList(ConfigNode configNode) {
        
        if (!configNode.isList())
            return;
        
        curRow++;
        row = workSheet.createRow(curRow);
        
        //System.out.println(configNode.getName());
        writeCurRowCell(COLUMN_MODULE, csGroup, configNode.getYangModule().configModuleName);
        writeCurRowCell(COLUMN_NODE_TYPE, csGroup, configNode.type.toString().toLowerCase());
        writeCurRowCell(COLUMN_HIERARCHY, csGroup, configNode.getMiddleName());
        writeCurRowCell(COLUMN_NAME, csGroup, configNode.getName());
        writeCurRowCell(COLUMN_CONF_ABLE, csGroup, configNode.configurable);
        writeCurRowCell(COLUMN_SCOPE, csGroup, configNode.getScopeName());
        writeCurRowCell(COLUMN_DESCP, csGroup, configNode.getDescription());
        
        if (configNode.maxElements.length() != 0) {
            String range = "size: 0.." + configNode.maxElements;
            cell = row.createCell(COLUMN_RANGE);
            cell.setCellStyle(csNormal);
            cell.setCellValue(range);
        }
    }
    
    
    void exportContainer(ConfigNode configNode) {
        if (!configNode.isContainer()) {
            return;
        }
        
        curRow++;
        row = workSheet.createRow(curRow);

        //System.out.println(configNode.getName());
        writeCurRowCell(COLUMN_MODULE, csGroup, configNode.getYangModule().configModuleName);
        writeCurRowCell(COLUMN_NODE_TYPE, csGroup, configNode.type.toString().toLowerCase());
        writeCurRowCell(COLUMN_HIERARCHY, csGroup, configNode.getMiddleName());
        writeCurRowCell(COLUMN_NAME, csGroup, configNode.getName());
        writeCurRowCell(COLUMN_CONF_ABLE, csGroup, configNode.configurable);
        writeCurRowCell(COLUMN_SCOPE, csGroup, configNode.getScopeName());
        writeCurRowCell(COLUMN_DESCP, csGroup, configNode.getDescription());
    }
    
    void printTitle() 
    {
        curRow = 0;
        
        row = workSheet.createRow(curRow);

        writeCurRowCell(COLUMN_MODULE, csTitle, "Module");
        writeCurRowCell(COLUMN_NODE_TYPE, csTitle, "NodeType");
        writeCurRowCell(COLUMN_HIERARCHY, csTitle, "hierarchy");
        writeCurRowCell(COLUMN_NAME, csTitle, "Name");
        writeCurRowCell(COLUMN_CONF_ABLE, csTitle, "config");
        writeCurRowCell(COLUMN_SCOPE, csTitle, "Scope");
        writeCurRowCell(COLUMN_DESCP, csTitle, "Description");
        writeCurRowCell(COLUMN_FORMAT, csTitle, "Format");
        writeCurRowCell(COLUMN_UNITS, csTitle, "units");
        writeCurRowCell(COLUMN_RANGE, csTitle, "Range");
        writeCurRowCell(COLUMN_DEFAULT, csTitle, "Default");
        writeCurRowCell(COLUMN_ADD_REL, csTitle, "add_release");
        writeCurRowCell(COLUMN_MOD_REL, csTitle, "mod_release");
        writeCurRowCell(COLUMN_NOTES, csTitle, "notes");
    }

    
    public void export(String fileName, ConfigTree _confTree)
    {
        try {
            outFile = new FileOutputStream(fileName);
            workBook = new HSSFWorkbook();
            workSheet = workBook.createSheet();
        } catch (IOException e) {   
            e.printStackTrace();   
        }   
        
        if(workBook!=null){

            setConfigTree(_confTree);
            
            workBook.setSheetName(0, "7510-PDD");
            
            ftTitle = workBook.createFont();
            ftTitle.setColor(HSSFColor.WHITE.index);
            ftTitle.setFontName("Tahoma");
            ftTitle.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);

            csTitle = workBook.createCellStyle();
            csTitle.setFillForegroundColor(HSSFColor.LIGHT_BLUE.index);
            csTitle.setFillPattern(CellStyle.SOLID_FOREGROUND);
            csTitle.setFont(ftTitle);
            
            
            ftNormal = workBook.createFont();
            csNormal = workBook.createCellStyle();
            csNormal.setWrapText(true);


            ftGroup = workBook.createFont();
            csGroup = workBook.createCellStyle();
            csGroup.setWrapText(true);
            csGroup.setFillForegroundColor(HSSFColor.LIGHT_CORNFLOWER_BLUE.index);
            csGroup.setFillPattern(CellStyle.SOLID_FOREGROUND);
            
            ftKeywd = workBook.createFont();
            csKeywd = workBook.createCellStyle();
            
            workSheet.setColumnWidth(COLUMN_MODULE,   10*256);
            workSheet.setColumnWidth(COLUMN_NODE_TYPE,10*256);
            workSheet.setColumnWidth(COLUMN_NAME,     15*256);
            workSheet.setColumnWidth(COLUMN_HIERARCHY,25*256);
            workSheet.setColumnWidth(COLUMN_CONF_ABLE,10*256);
            workSheet.setColumnWidth(COLUMN_SCOPE,    10*256);
            workSheet.setColumnWidth(COLUMN_DESCP,    50*256);
            workSheet.setColumnWidth(COLUMN_FORMAT,   15*256);
            workSheet.setColumnWidth(COLUMN_UNITS,     8*256);
            workSheet.setColumnWidth(COLUMN_RANGE,    15*256);
            workSheet.setColumnWidth(COLUMN_DEFAULT,  14*256);
            workSheet.setColumnWidth(COLUMN_ADD_REL,  10*256);
            workSheet.setColumnWidth(COLUMN_MOD_REL,  10*256);
            workSheet.setColumnWidth(COLUMN_NOTES,    30*256);
            
            printTitle();
            
            super.walkthrough();
        }
  
        try {   
            workBook.write(outFile);
            outFile.close();
        } catch (IOException e) {   
            e.printStackTrace();   
        }   
    } 
}