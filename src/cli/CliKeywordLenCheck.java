package cli;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import cli.CliExport.Sequence;

class CliKeywordLenCheck extends CliExport {
    
    String keyworkFileName = "/VOBS/MediaGW/tools/various/CliShell/bin/cli_keyword_list.xml";
    
    List<String>    cliExceptionKeywordList;
    boolean         checkResult;  /* whole check result */
    
    
    void exportCliCommand(CliCommand cliCommand) 
    {
        boolean keywordMatched = false;
        
        for (CliNode node: cliCommand.getNodes()) {
            if (!node.isKeywordValid()) {
                keywordMatched = false;
                
                for (int i=0; i<cliExceptionKeywordList.size(); i++) 
                {
                    if (cliExceptionKeywordList.get(i).equals(node.getKeyword()))
                    {
                        keywordMatched = true;
                        break;
                    }
                }
                
                if (keywordMatched == false)
                {
                    System.out.println("\nkeyword <" + node.getKeyword() + "> is too long in CLI: "+cliCommand.getSyntaxString()+"\n");
                    checkResult = false;
                }
            }
        }               
    }
    
    public boolean export(CliCommandTree cliMainCmdTree, CliCommandTree cliDiagCmdTree) 
    {
        checkResult = true;
        
        if (!readExceptionKeywordList())
        {
            System.out.println("Wrong reading: "+keyworkFileName);
            System.exit(-1);
        }
        
        super.setSequence(Sequence.lexical);
        super.walkthrough(cliMainCmdTree);
        super.walkthrough(cliDiagCmdTree);
        
        return checkResult;
    }   
    
    

    boolean readExceptionKeywordList() {
        boolean result = true;
        cliExceptionKeywordList = new ArrayList<String> ();
        
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document xml_doc = db.parse(keyworkFileName);
            xml_doc.normalize();
            
            Element root = xml_doc.getDocumentElement(); 
            if (root == null) return false; 
 
            NodeList cliKeywordList = root.getElementsByTagName("keyword"); 
            if (cliKeywordList == null) return false;
            
            for(int i = 0; i < cliKeywordList.getLength(); i++) 
            {  
                Element cliKeywordElement = (Element) cliKeywordList.item(i);
                if (cliKeywordElement.getNodeType() != Node.ELEMENT_NODE)
                    continue;

                String keyword   = cliKeywordElement.getAttribute("keyname");

                cliExceptionKeywordList.add(keyword);
                //System.out.println("keyword=<" + keyword + ">");
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