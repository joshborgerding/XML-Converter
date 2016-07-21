
import java.io.File;
import java.io.PrintWriter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author jborgerding
 */
public class MainClass {
    
    public static void main(String[] args){
        
        String fullName = args[0];
        String outputDir;
        
        if(args != null && args.length > 1 ){
            switch (args[1]) {
                case "test":
                    outputDir = "/Volumes/q2000/Landing/";
                    break;
                case "local":
                    outputDir = "/Users/jborgerding/Documents/XML-Converter/output/";
                    break;
                default:
                    outputDir = "/Volumes/q2000/Inbox/qnet/";
                    break;
            }
        } else {
            outputDir = "/Volumes/q2000/Inbox/qnet/";
        }
        
        try {   
                        
            File inputFile = new File(fullName);
            
            String fileName = inputFile.getName();
            String extension = fileName.substring(fileName.lastIndexOf('.')+1);
            String fn = fileName.substring(0, fileName.lastIndexOf('.'));
            String path = inputFile.getPath();
            String absPath = inputFile.getAbsolutePath();
            String justPath = absPath.substring(0, absPath.lastIndexOf(fileName));
            String endPath = justPath.substring(0, justPath.lastIndexOf('/'));
            endPath = endPath.substring(0, endPath.lastIndexOf('/'));
            
            // Create a new instance of a document builder
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            // Make sure that the document builder is aware that the XML document using namespace prefixing
            dbFactory.setNamespaceAware(true);
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            // Make a new document through parsing the input file
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();
            
            // Create a new XML object for the given input XML object
            Convert_XML xml = new Convert_XML(doc, fn, justPath);
            
            // Process the XML object
            xml.processXML();
            
            // Write the logfile for the processing of the XML object
            try(  PrintWriter out = new PrintWriter( endPath + "/Logs/processed_logs/" + fn +"_log.txt" )  ){
                out.println( xml.getProcessLog() );
            }
            
            // If the client number in the XML object is anything special,
            // then the output dir will need to be changed.
            String subdir = "0001/";
            if(xml.getOutputDir() > 1){
                subdir = "0002/";
            }
            
            // If the XML object was able to process the data correctly,
            // then try to write the output file.
            if(xml.processedCorrectly() && xml.getProductCount() > 0){
                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                
                
                try(  PrintWriter out = new PrintWriter( outputDir + subdir + fn +"_output.txt" )  ){
                    out.println( xml.getStringOutput() );
                }


                // ******************************************
                // Testing writing using DOM instead of String
                
                DOMSource source = new DOMSource(xml.getXMLOutput());
//                StreamResult result = new StreamResult (new File(outputDir + subdir + fn +"_output.txt"));
//                
//                
//                transformer.transform(source, result);
                
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");                
                StreamResult result = new StreamResult (new File(outputDir + "Hold/" + fn + "_output.txt"));
                transformer.transform(source, result);
                // ******************************************
                
                
                System.exit(0);
            } else {
                if(xml.processedCorrectly()){
                    // If the XML object was able to process the data correctly,
                    // but there are no actual products, exit without writing any output
                    System.exit(0);
                } else {
                    // If the XML object wasn't able to process correctly,
                    // then exit with error code 1.
                    System.exit(1);
                }
            }
            
            
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
    
}
