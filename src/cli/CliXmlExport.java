package cli;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import cli.CliExport.Sequence;

class CliXmlExport extends CliExport{

    Document document;
    Element root;
    
    void exportCliCommand(CliCommand cliCommand) {
        
        Comment comment = document.createComment(cliCommand.getKeywords());
        root.appendChild(comment);
        
        Element cliElement = document.createElement("command"); 
        
        Element keyword = document.createElement("keyword"); 
        keyword.appendChild(document.createTextNode(cliCommand.getKeywords())); 
        cliElement.appendChild(keyword); 
        
        Element syntax = document.createElement("syntax");
        syntax.setAttribute("mode", cliCommand.getCliCmdMode().toString());
        if (cliCommand.isSCMCommand())
            syntax.setAttribute("board", "scm");
        else
            syntax.setAttribute("board", "all");

        //System.out.println("keyword: " + cliCommand.getKeywords() + " sys_parameter:" + cliCommand.isSystemParameter());        
        syntax.setAttribute("display",   cliCommand.getDisplayModeName());
        syntax.setAttribute("privilege", cliCommand.getPrivilegeName());
        syntax.setAttribute("function", cliCommand.getFunctionName());
        syntax.setAttribute("source", cliCommand.getSource().toString());
        cliElement.appendChild(syntax);
        
        if (cliCommand.isXMLCommand()) {
            Element helpElement;
            for (String helpMsg: cliCommand.getHelp()) {
                 helpElement = document.createElement("help");
                 helpElement.appendChild(document.createTextNode(helpMsg)); 
                 cliElement.appendChild(helpElement); 
            }
        }
        
        for (CliNode cliNode: cliCommand.getNodes()) {
            if (cliNode.isParameterNode()) {
                Element cliParamElement = document.createElement("param"); 
                CliNodeParameter cliParameter = (CliNodeParameter)cliNode;
                
                cliParamElement.setAttribute("required", cliParameter.getRequiredName());
                cliParamElement.setAttribute("datatype", cliParameter.getDataType());

                //System.out.println("keyword: " + cliCommand.getKeywords() + " unit:" + cliParameter.getUnit());
                if (!cliParameter.getUnit().isEmpty()) {
                    cliParamElement.setAttribute("unit", cliParameter.getUnit());
                }
                
                Element paramKeyword = document.createElement("keyword"); 
                paramKeyword.appendChild(document.createTextNode(cliParameter.getKeyword())); 
                cliParamElement.appendChild(paramKeyword); 

                if (cliParameter.isRangeSet()) {
                    Element paramRange = document.createElement("range"); 
                    paramRange.appendChild(document.createTextNode(cliParameter.getMinValue() + "," + cliParameter.getMaxValue())); 
                    cliParamElement.appendChild(paramRange);
                }

                if (!cliParameter.getDefValue().isEmpty()) {
                    Element paramDef = document.createElement("default"); 
                    paramDef.appendChild(document.createTextNode(cliParameter.getDefValue())); 
                    cliParamElement.appendChild(paramDef);
                }
                
                if (cliCommand.isXMLCommand()) {
                    Element helpElement;
                    for (String helpMsg: cliParameter.getHelps()) {
                         helpElement = document.createElement("help");
                         helpElement.appendChild(document.createTextNode(helpMsg)); 
                         cliParamElement.appendChild(helpElement); 
                    }
                }
                
                cliElement.appendChild(cliParamElement);
            }
        }
        
        root.appendChild(cliElement);
    }
   
    public void export(String fileName, CliCommandTree cliMainCmdTree, CliCommandTree cliDiagCmdTree)
    { 
        document=null; 
        try { 
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); 
            DocumentBuilder builder = factory.newDocumentBuilder(); 
            document = builder.newDocument(); 
        } catch (ParserConfigurationException e) { 
            System.out.println(e.getMessage()); 
        } 
        
        if (document == null) return;
        
        root = document.createElement("ui_definition_file"); 
        document.appendChild(root); 

        super.setSequence(Sequence.lexical);
        if (cliMainCmdTree != null)
            super.walkthrough(cliMainCmdTree);
        if (cliDiagCmdTree != null)
            super.walkthrough(cliDiagCmdTree);

        
        TransformerFactory tf = TransformerFactory.newInstance(); 
        try { 
            Transformer transformer = tf.newTransformer(); 
            DOMSource source = new DOMSource(document); 
            transformer.setOutputProperty(OutputKeys.ENCODING, "gb2312"); 
            transformer.setOutputProperty(OutputKeys.INDENT, "yes"); 
            transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            
            PrintWriter pw = new PrintWriter(new FileOutputStream(fileName)); 
            StreamResult result = new StreamResult(pw); 
            //XMLOutputter out;
            transformer.transform(source, result); 
            pw.close();
            System.out.println("create XML file <"+ fileName + "> successful!"); 
        } catch (TransformerConfigurationException e) { 
        System.out.println(e.getMessage()); 
        } catch (IllegalArgumentException e) { 
        System.out.println(e.getMessage()); 
        } catch (FileNotFoundException e) { 
        System.out.println(e.getMessage()); 
        } catch (TransformerException e) { 
        System.out.println(e.getMessage()); 
        } 
    }   
}