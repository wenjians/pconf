



import java.io.*;

import org.w3c.dom.*;
import org.xml.sax.*; 

import javax.xml.parsers.*;


/**
 * @author wenjians
 *
 */
public class YangParser {

    String yangFilePath;
    String yinFilePath;
    
    YangFileTree yangFileTree;
    
    boolean bYang2Yin;
    
    /* it is file of gw-yang-list.xml, which list all the yang files */
    String yangXMLFile = "gw-yang-list.xml";
    
    
    YangParser() {
        yangFileTree = null;
        bYang2Yin    = false;
    }
    
    void enabletYang2Yin()      {bYang2Yin = true;}
    
    
    void setYangFileTree(YangFileTree yangTree) { yangFileTree = yangTree; }

    boolean executeShell(String shellCommand) {
        String lineStr;
        boolean result=true;
        Runtime run = Runtime.getRuntime();
        
        try {
            Process process = run.exec(shellCommand);

            BufferedInputStream in  = new BufferedInputStream(process.getInputStream());
            BufferedInputStream out = new BufferedInputStream(process.getErrorStream());
            
            BufferedReader inBuf = new BufferedReader(new InputStreamReader(in));
            BufferedReader errBuf = new BufferedReader(new InputStreamReader(out));
            
            // get the process normal output and print them
            while ((lineStr = inBuf.readLine()) != null)
                System.out.println(lineStr);
            
            // in case there is something wrong, output the error message
            if (process.waitFor() != 0) {
                if (process.exitValue() == 1) // exitValue(), 0: normal, 1: error
                {
                    System.err.print("Error:");
                    
                    while ((lineStr = errBuf.readLine()) != null)
                        System.err.println(lineStr);
                    
                    result = false;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }       
        
        return result;  
    } 
    
   
  //pyang -f yin gw-system-cli.yang -o ../yin/gw-system-cli.xml -p ../external -p ../extensions -p ../common
    boolean parseOneFile(YangFileModule yangModule, YangFile yangFile) {
        
        if (!bYang2Yin) {
            return true;
        }
        
        String fullYangFileName;
        if (yangModule.getDirectory().length() == 0)
            fullYangFileName = yangFilePath + "/" + yangFile.getYangFileName();
        else
            fullYangFileName = yangFilePath + "/" + yangModule.getDirectory() + 
                               "/" + yangFile.getYangFileName();
            
        String fullYinFileName  = yinFilePath  + "/" + yangFile.getYinFileName();
        
        String command = String.format("pyang -f yin %s -o %s -p %s/external -p %s/extensions -p %s/common", 
                                       fullYangFileName, fullYinFileName, yangFilePath, yangFilePath, yangFilePath);
        if (yangModule.getDirectory().length() != 0)
            command = command + " -p " + yangFilePath + "/" + yangModule.getDirectory();
        
        System.out.println(command);
        
        return executeShell(command);
    }
    
    boolean parseOneModule(YangFileModule yangModule) {
        
        //System.out.println(yangModule);
        
        boolean result = false;
        
        result = parseOneFile(yangModule, yangModule);
        if (!result)
            return result;
        
        if (yangModule.isInternalModule()) {
            for (YangFile yangFile: yangModule.getSubmoduleList()) {
                result = parseOneFile(yangModule, yangFile);
                if (!result)
                    break;
            }
        }
        
        result = yangFileTree.addYangModule(yangModule);
        if (!result)
            return result;
        
        
        return result;
    }


    /**
     * @param yangFiles
     * @param yangFileTree
     * @param yangPath
     * @param yinPath
     */
    public boolean parseYangFiles(String yangPath, String yinPath) {
    
        boolean  result = true;
        
        yangFilePath = yangPath;
        yinFilePath  = yinPath;
        yangXMLFile  = yangPath + "/" + yangXMLFile;
        
        
        
        //System.out.println("yang file path: " + yangPath + "...");
        //System.out.println("yin  file path: " + yinPath  + "...");
        System.out.println("\nprocess Yang XML file list: " + yangXMLFile + "...");
        
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document xml_doc = db.parse(yangXMLFile);
            xml_doc.normalize();
            
            Element root = xml_doc.getDocumentElement(); 
            if (root == null) 
                return false; 
     
            NodeList moduleList = root.getElementsByTagName("all-module-list");            
            if (moduleList == null) 
                return false;
            
            
            /*
             * the following code is used to parse the gw common type definition modules
             */
            NodeList extYangGroupList = root.getElementsByTagName("gw-external");
            if (extYangGroupList.getLength() > 1) {
                System.out.println("all external yang file must be in same group!\n");
                return false;
            }
            
            for(int i = 0; i < extYangGroupList.getLength(); i++) 
            {  
                Element extYangGroupElement = (Element) extYangGroupList.item(i);
                String directory = extYangGroupElement.getAttribute("directory").trim();
                
                NodeList extModuleList = extYangGroupElement.getElementsByTagName("module");
                if (extModuleList == null)
                    continue;
                
                for (int modId = 0; modId < extModuleList.getLength(); modId++) 
                {
                    Element yangModuleElement = (Element)extModuleList.item(modId);
                    YangFileModule yangModule = new YangFileModule();
                    
                    yangModule.setModuleGroup(YangFileModule.ModuleGroup.external);
                    yangModule.setDirectory(directory);
                    yangModule.setYangFileName(yangModuleElement.getAttribute("name").trim());
                    
                    result = parseOneModule(yangModule);
                    if (!result)
                        return result;
                    
                    //yangFileTree.addYangFile(yangModule.moduleFile.yangFileName, yangModule.moduleFile);
                    
                }
            }
            
            
            /*
             * the following code is used to parse the gw common type definition modules
             */
            NodeList comYangGroupList = root.getElementsByTagName("gw-common");
            if (comYangGroupList.getLength() > 1) {
                System.out.println("all common yang file must be in same group!\n");
                return false;
            }
            
            for(int i = 0; i < comYangGroupList.getLength(); i++) 
            {  
                Element comYangGroupElement = (Element) comYangGroupList.item(i);
                
                String directory = comYangGroupElement.getAttribute("directory").trim();
                
    
                NodeList comModuleList = comYangGroupElement.getElementsByTagName("module");
                if (comModuleList == null)
                    continue;
                
                for (int modId = 0; modId < comModuleList.getLength(); modId++) 
                {
                    Element yangModuleElement = (Element)comModuleList.item(modId);
                    YangFileModule yangModule = new YangFileModule();
                    
                    yangModule.setModuleGroup(YangFileModule.ModuleGroup.common);
                    yangModule.setDirectory(directory);
                    yangModule.setYangFileName(yangModuleElement.getAttribute("name").trim());
                    
                    result = parseOneModule(yangModule);
                    if (!result) 
                        return result;
                    
                    //yangFileTree.addYangFile(yangModule.moduleFile.yangFileName, yangModule.moduleFile);
                }
            }            
            
            /*
             * the following code is used to parse the gw related modules
             */
    
            NodeList mgwYangGroupList = root.getElementsByTagName("gw-module");
            for(int i = 0; i < mgwYangGroupList.getLength(); i++) 
            {  
                Element mgwYangModuleElement = (Element) mgwYangGroupList.item(i);
                
                YangFileModule yangModule = new YangFileModule();
                
                yangModule.setModuleGroup(YangFileModule.ModuleGroup.internal);
                yangModule.setYangFileName(mgwYangModuleElement.getAttribute("name").trim());
                yangModule.setDirectory(mgwYangModuleElement.getAttribute("directory").trim());
                
    
                NodeList mgwSubmoduleList = mgwYangModuleElement.getElementsByTagName("sub-module");
                if (mgwSubmoduleList == null)
                    continue;
                
                for (int submodId = 0; submodId < mgwSubmoduleList.getLength(); submodId++) 
                {
                    
                    Element yangModuleElement = (Element)mgwSubmoduleList.item(submodId);
                    String  submoduleName = yangModuleElement.getAttribute("name").trim();
                    
                    YangFile subModule = new YangFile(YangFile.FileType.submodule, submoduleName);
                    subModule.setParentModule(yangModule);
                    
                    yangModule.addSubmodule(subModule);
                }
                
                result = parseOneModule(yangModule);
                if (!result)
                    return result;
                
                //asdfsfd  add yang File to yang File tree;
            }    
    
        } catch (ParserConfigurationException e) {  
            e.printStackTrace();  
            result = false;
        } catch (FileNotFoundException e) {  
            e.printStackTrace();
            result = false;
        } catch (SAXException e) {  
            e.printStackTrace();
            result = false;
        } catch (IOException e) {  
            e.printStackTrace();
            result = false;
        } 
        
        return result;        
    }
}


