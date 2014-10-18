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


public class ConfigExportPdd extends ConfigExport{

    //final static int COLUMN_MODULE     = 0;
    //final static int COLUMN_NODE_TYPE  = 1;
    //final static int COLUMN_HIERARCHY  = 2;
    final static int COLUMN_NAME       = 0;
    //final static int COLUMN_CONF_ABLE  = 4;
    //final static int COLUMN_SCOPE      = 5;
    final static int COLUMN_DESCP      = 1;
    //final static int COLUMN_FORMAT     = 7;
    final static int COLUMN_RANGE      = 2;
    final static int COLUMN_DEFAULT    = 3;
    final static int COLUMN_RETRIEVAL  = 4;
    final static int COLUMN_CRITICAL   = 5;
    final static int COLUMN_SERVICE_IMPACT = 6;
    final static int COLUMN_INTERNAL_IMPACT= 7;
    final static int COLUMN_EXTERNAL_IMPACT= 8;
    final static int COLUMN_NOTES          = 9;
    final static int COLUMN_IMS_RELEASE    =10;
    
    FileOutputStream outFile;
    Workbook workBook;
    Sheet workSheet;
    
    CellStyle csTitle;
    CellStyle csGroup;
    CellStyle csGroupExit;
    CellStyle csNormal;
    CellStyle csKeywd;
    
    Font ftTitle;
    Font ftGroup;
    Font ftGroupExit;
    Font ftNormal;
    Font ftKeywd;
    
    Row row ;
    Cell cell;
    
    int curRow;
            
    public ConfigExportPdd () {
        curRow = 0;
    }
    
    private void writeCurRowCell(int column, CellStyle style, String value) {
        cell = row.createCell(column);
        cell.setCellStyle(style);
        cell.setCellValue(value);
    }
    
    /*
    private String getSpacedName(String hieraryName, String name) {
        String spacedName = "";
        String[] keywords = hieraryName.split("/");
        for (String key: keywords) {
            if (!spacedName.isEmpty()) {
                spacedName += " ";
            }
            spacedName += key;
        }
        
        return spacedName + " " + name;
    }
    */
    
    @Override
    void exportLeafEnter(ConfigNode configNode) {
        if (!configNode.isLeaf())
            return;

        CellStyle csName = csNormal;

        if (configNode.getParent().isList()) {
            ConfigList list = (ConfigList) configNode.getParent();

            if (list.getListKey().contentEquals(configNode.getName())) {
                csName = csKeywd;
            }
        }

        curRow++;
        row = workSheet.createRow(curRow);
        
        writeCurRowCell(COLUMN_NAME, csName, configNode.getName());
        writeCurRowCell(COLUMN_DESCP, csNormal, configNode.getDescription());
        writeCurRowCell(COLUMN_RANGE, csNormal, configNode.dataType.getRange() + " " + configNode.getUnits());
        writeCurRowCell(COLUMN_DEFAULT, csNormal, configNode.getDefaultVal());
        writeCurRowCell(COLUMN_RETRIEVAL, csNormal, configNode.getRetrieval());
        writeCurRowCell(COLUMN_CRITICAL, csNormal, configNode.getCritical());
        writeCurRowCell(COLUMN_SERVICE_IMPACT, csNormal, configNode.getServiceImpact());
        writeCurRowCell(COLUMN_INTERNAL_IMPACT, csNormal, configNode.getInternalImpact());
        writeCurRowCell(COLUMN_EXTERNAL_IMPACT, csNormal, configNode.getExternalImpact());
        writeCurRowCell(COLUMN_NOTES, csNormal, configNode.getNotes());
        
    }

    
    void exportLeafListEnter(ConfigNode configNode) {
        if (!configNode.isLeafList())
            return;
        
        curRow++;
        row = workSheet.createRow(curRow);
        
        writeCurRowCell(COLUMN_NAME, csNormal, configNode.getName());
        writeCurRowCell(COLUMN_DESCP, csNormal, configNode.getDescription());
        
        if (configNode.maxElements.length() != 0) {
            String range = "size: 0.." + configNode.maxElements;
            cell = row.createCell(COLUMN_RANGE);
            cell.setCellStyle(csNormal);
            cell.setCellValue(range);
        }
        
        writeCurRowCell(COLUMN_RETRIEVAL, csNormal, configNode.getRetrieval());
        writeCurRowCell(COLUMN_CRITICAL, csNormal, configNode.getCritical());
        writeCurRowCell(COLUMN_SERVICE_IMPACT, csNormal, configNode.getServiceImpact());
        writeCurRowCell(COLUMN_INTERNAL_IMPACT, csNormal, configNode.getInternalImpact());
        writeCurRowCell(COLUMN_EXTERNAL_IMPACT, csNormal, configNode.getExternalImpact());
        writeCurRowCell(COLUMN_NOTES, csNormal, configNode.getNotes());
            
    }
    

    void exportListEnter(ConfigNode configNode) {
        
        if (!configNode.isList())
            return;
        
        curRow++;
        row = workSheet.createRow(curRow);
        
        //System.out.println(configNode.getName());
        writeCurRowCell(COLUMN_NAME, csGroup, configNode.getFullPathName());
        writeCurRowCell(COLUMN_DESCP, csGroup, configNode.getDescription());
        
        if (configNode.maxElements.length() != 0) {
            String range = "size: 0.." + configNode.maxElements;
            cell = row.createCell(COLUMN_RANGE);
            cell.setCellStyle(csNormal);
            cell.setCellValue(range);
        }
    }

    void exportListExit(ConfigNode configNode) {
        
        if (!configNode.isList())
            return;
        
        curRow++;
        row = workSheet.createRow(curRow);
        
        //System.out.println(configNode.getName());
        //writeCurRowCell(COLUMN_NODE_TYPE, csGroupExit, "End List");
        writeCurRowCell(COLUMN_NAME, csGroupExit, "End List " + configNode.getFullPathName());
        writeCurRowCell(COLUMN_DESCP, csGroupExit, null);
    }
    
    void exportContainerEnter(ConfigNode configNode) {
        if (!configNode.isContainer()) {
            return;
        }
        
        curRow++;
        row = workSheet.createRow(curRow);

        //System.out.println(configNode.getName());
        writeCurRowCell(COLUMN_NAME, csGroup, configNode.getFullPathName());
        writeCurRowCell(COLUMN_DESCP, csGroup, configNode.getDescription());
    }
    
    void exportContainerExit(ConfigNode configNode) {
        if (!configNode.isContainer()) {
            return;
        }
        
        curRow++;
        row = workSheet.createRow(curRow);

        //System.out.println(configNode.getName());
        writeCurRowCell(COLUMN_NAME, csGroupExit, "End Group " + configNode.getFullPathName());
        writeCurRowCell(COLUMN_DESCP, csGroupExit, null);
    }
    
    void printTitle() 
    {
        curRow = 0;
        
        row = workSheet.createRow(curRow);

        writeCurRowCell(COLUMN_NAME, csTitle, "Parameter");
        writeCurRowCell(COLUMN_DESCP, csTitle, "Description");
        writeCurRowCell(COLUMN_RANGE, csTitle, "Range");
        writeCurRowCell(COLUMN_DEFAULT, csTitle, "Default Value");
        writeCurRowCell(COLUMN_RETRIEVAL, csTitle, "Retrieval Mechanism");
        writeCurRowCell(COLUMN_CRITICAL, csTitle, "Critical (Y/N)");
        writeCurRowCell(COLUMN_SERVICE_IMPACT, csTitle, "Service Impact");
        writeCurRowCell(COLUMN_INTERNAL_IMPACT, csTitle, "Internal Impact");
        writeCurRowCell(COLUMN_EXTERNAL_IMPACT, csTitle, "External Impact");
        writeCurRowCell(COLUMN_NOTES, csTitle, "notes");
        writeCurRowCell(COLUMN_IMS_RELEASE, csTitle, "add_release");
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
            csNormal.setFont(ftNormal);


            ftGroup = workBook.createFont();
            csGroup = workBook.createCellStyle();
            csGroup.setWrapText(true);
            csGroup.setFillForegroundColor(HSSFColor.AQUA.index);
            csGroup.setFillPattern(CellStyle.SOLID_FOREGROUND);
            csGroup.setFont(ftGroup);
            
            
            csGroupExit = workBook.createCellStyle();
            csGroupExit.setFillForegroundColor(HSSFColor.AQUA.index);
            csGroupExit.setFillPattern(CellStyle.SOLID_FOREGROUND);
            
            
            ftKeywd = workBook.createFont();
            ftKeywd.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
            ftKeywd.setColor(HSSFColor.DARK_RED.index);
            ftKeywd.setUnderline(Font.U_SINGLE);
            csKeywd = workBook.createCellStyle();
            csKeywd.setFont(ftKeywd);
            
            workSheet.setColumnWidth(COLUMN_NAME,     15*256);
            workSheet.setColumnWidth(COLUMN_DESCP,    60*256);
            workSheet.setColumnWidth(COLUMN_RANGE,    15*256);
            workSheet.setColumnWidth(COLUMN_DEFAULT,  20*256);
            workSheet.setColumnWidth(COLUMN_RETRIEVAL,40*256);
            workSheet.setColumnWidth(COLUMN_CRITICAL, 10*256);
            workSheet.setColumnWidth(COLUMN_SERVICE_IMPACT,  30*256);
            workSheet.setColumnWidth(COLUMN_INTERNAL_IMPACT, 20*256);
            workSheet.setColumnWidth(COLUMN_EXTERNAL_IMPACT, 30*256);
            workSheet.setColumnWidth(COLUMN_NOTES,    30*256);
            workSheet.setColumnWidth(COLUMN_IMS_RELEASE, 20*256);
            
            printTitle();
            
            super.walkthrough();
            
            //workSheet.groupRow(1,  10);
        }
  
        try {   
            workBook.write(outFile);
            outFile.close();
        } catch (IOException e) {   
            e.printStackTrace();   
        }   
    } 
}