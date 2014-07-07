package conf;

//import YangFileModule;

import java.util.*;

/* used for import and include
 * include: 
 *      a) module include submodule
 *      b) submodule include other submodule with same modules
 *  import:
 *      a) module reference other modules
 *      b) submodule reference other modules      
 */
class ReferYangFile {
    public enum ReferType { refer_include, refer_import }
    
    // prefix used in this yang file
    public String      prefix;
    
    // which yang file which include/import
    public YangFile    yangFile;
    
    // it is include or import
    public ReferType   referType;
    
    ReferYangFile() {
        //moduleName = "";
        prefix     = "";
        yangFile   = null;
    }
    
    
    public String getModuleName()  {        
        return yangFile.getModuleName();    
    }

    @Override
    public String toString() {
        return "ReferYangFile [ReferType="+ referType 
                + ", moduleName=" + getModuleName() 
                + ", prefix=" + prefix + "]\n";
    }
    
    
}

/**
 * @author wenjians
 *
 */
class YangFile {
    
    
    /* for every file, there are three status:
     * yang: the file is still in yang state, not processed yet
     * yin : the yang file is already re-formated to yin (XML) with pyang command
     * conf: the yin  file is already parsed by pConf and put to parameter tree (ConfParamTree) 
     */
    public enum FileState { yang, yin, conf }
    
    /* whether the current Yang File is module or submodule */
    public enum FileType  { module, submodule}
    
    
    private FileState  fileState;
    private FileType   fileType;
    
    /* this name include the extension of yang, e.g. "aaa.yang" */
    private String     yangFileName; 
    
    // only valid for submodule
    private YangFileModule parentModule;    
    
    // used for include and import list
    private List <ReferYangFile> referFiles;
    
    
    YangFile() {
        yangFileName = "";
        fileState = FileState.yang;
        fileType  = FileType.module;
        parentModule = null;
        
        referFiles = new ArrayList<ReferYangFile> ();
    }
    
    YangFile(FileType _type, String fileName) {
        this();

        this.yangFileName = fileName;
        this.fileType     = _type;
    }
    
    public void setParentModule(YangFileModule parent) { 
        parentModule = parent; 
    }
    
    public YangFileModule getParentModule() {         
    	return parentModule; 
    }
    
    public String getParentModuleName()  { 
        return parentModule.getModuleName(); 
    }
    
    public void setYangFileName(String fileName) { 
        yangFileName = fileName;  
    }
    
    public String getYangFileName() { 
        return yangFileName;      
    }
    
    public String getModuleName() { 
        return yangFileName.substring(0, yangFileName.length()-5).trim(); 
    }
    
    // translate gw_module.yang => ge_module.xml
    public String getYinFileName() {
        return getModuleName() + ".xml"; 
    }
    
    public void setFileState (FileState state) { 
        fileState = state;
    }
    
    public FileState getFileState()  { 
        return fileState; 
    }
    
    public void setFileType (FileType type) { 
        fileType = type;  
    }
    
    public FileType getFileType() { 
        return fileType;  
    }
    
    
    public List<ReferYangFile> getReferFiles() {
        return referFiles;
    }

    public void setReferFiles(List<ReferYangFile> referFiles) {
        this.referFiles = referFiles;
    }

    public boolean  isMoudle() { 
        return fileType == FileType.module; 
    }
    
    public boolean  isSubmodule() { 
        return fileType == FileType.submodule; 
    }

    @Override
    public String toString() {
        return "YangFile [yangFileName=" + yangFileName 
        		+ ", fileType=" + fileType 
        		+ ", fileState=" + fileState 
                + ",refer_files" + referFiles + "]\n";
    }
}

class YangFileModule extends YangFile {
    
    public enum ModuleGroup {extension, external, common, internal}
    
    private ModuleGroup     moduleGroup;
    private String          directory;  // which module located for module and submodules

    private List <YangFile> submoduleFiles;
    
    String configModuleName;  /* config module name used in config.xml */
    
    public YangFileModule () {
        moduleGroup    = ModuleGroup.internal;
        directory      = "";
        //moduleFile     = new YangFile();
        submoduleFiles = new ArrayList<YangFile> ();
        setParentModule(this);
    }
    
    public boolean isExtensionModule() { 
        return moduleGroup == ModuleGroup.extension; 
    }
    
    public boolean isExternalModule() { 
        return moduleGroup == ModuleGroup.external; 
    }
    
    public boolean isCommonModule()  { 
        return moduleGroup == ModuleGroup.common; 
    }
    
    public boolean isInternalModule() { 
        return moduleGroup == ModuleGroup.internal; 
    }
    
    public void setModuleGroup(ModuleGroup group) { 
        moduleGroup = group; 
    }
    public void setDirectory(String dir) { 
        directory = dir; 
    }
    
    public String getDirectory() { 
        return directory; 
    }
    
    public void addSubmodule(YangFile submodule) {
        submoduleFiles.add(submodule);
    }
    
    public  List <YangFile> getSubmoduleList() {
        return submoduleFiles;
    }
    

    // TODO
    public boolean validation()
    {
        return true;
    }

    @Override
    public String toString() {
        return "YangFileModule [moduleGroup=" + moduleGroup + ", directory=" + directory + ", \nModule=" + super.toString()
                + ", submoduleFiles=" + submoduleFiles + "]\n\n";
    }
}


public final class YangFileTree {

    /* it is a list, the add is accoring to the sequence added 
     * this is import for Yin Parser, because when do YinParse, it is assumed the
     * need yang file is already parsed
     */
    
    private List<YangFileModule> yangModuleList;
    
    private HashMap<String, YangFile> yangFileTable;
    
    public YangFileTree() {
        yangModuleList = new ArrayList<YangFileModule> ();
        
        yangFileTable = new HashMap<String, YangFile> ();
    }
    
    
    public YangFile getYangFile(String fileName) {
        return yangFileTable.get(fileName);
    }
    
    public List<YangFileModule> getModuleList()  { 
        return yangModuleList; 
    }

    public boolean addYangModule(YangFileModule yangModule) {
        yangModuleList.add(yangModule);
        yangFileTable.put(yangModule.getModuleName(), yangModule);
        
        for (YangFile yangFile: yangModule.getSubmoduleList()) {
            yangFileTable.put(yangFile.getModuleName(), yangFile);
        }
        
        return true;
    }
    
    
    public void addDefaultYangFile() {
        /* add two default extension to yang File Tree */
        YangFileModule extCOM = new YangFileModule();
        extCOM.setModuleGroup(YangFileModule.ModuleGroup.extension);
        extCOM.setYangFileName("ALUYangExtensions.yang");
        addYangModule(extCOM);
        
        YangFileModule fileMGW = new YangFileModule();
        fileMGW.setModuleGroup(YangFileModule.ModuleGroup.extension);
        fileMGW.setYangFileName("MGWYangExtensions.yang");
        addYangModule(fileMGW);        
    }
    
    // TODO 
    public boolean validation() {
        for (YangFileModule yangModule: yangModuleList) {
            if (!yangModule.validation())
                return false;
        }
        
        return true;
    }

    @Override
    public String toString() {
        return "YangFileTree [\nextYangModuleList=" + yangModuleList + "\n"
                + "Yang Hash Tables="  + yangFileTable
                + "]\n";
    }
    
    
}
