import java.io.File;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
//import javax.xml.xpath.XPath;
//import javax.xml.xpath.XPathConstants;
//import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import java.util.ArrayList;
import java.io.PrintWriter;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;


public class Test_Converter {
    
    public static void main(String[] args){
        
        String processLog = "";
        boolean processCorrectly = true;
        
        String fullName = args[0];
        String outputDir = "";
        String outputDir2 = "";
        
        if(args != null && args.length > 1 ){
            if(args[1].equals("test")){
                outputDir = "/Volumes/q2000/Landing/0001/";
                outputDir2 = "/Volumes/q2000/Landing/0002/";
            } else if(args[1].equals("local")){
                outputDir = "/Users/jborgerding/Documents/XML-Converter/output/";
                outputDir2 = "/Users/jborgerding/Documents/XML-Converter/output/";
            } else {
                outputDir = "/Volumes/q2000/Inbox/qnet/0001/";
                outputDir2 = "/Volumes/q2000/Inbox/qnet/0002/";
            }
        } else {
            outputDir = "/Volumes/q2000/Inbox/qnet/0001/";
            outputDir2 = "/Volumes/q2000/Inbox/qnet/0002/";
        }
        
        try {   
            DateFormat dateFormat = new SimpleDateFormat("yyyMMdd");
            Date date = new Date();
            
            processLog += logEntry(date.toString());
            
            // The input file (will eventually be args[0], when passed from calling bash script)
            // Specify manually for explicit file testing
            //String fullName = "/Users/jborgerding/Documents/XML-Converter/input/0001803095.xml";
            
            File inputFile = new File(fullName);
            
            String fileName = inputFile.getName();
            String extension = fileName.substring(fileName.lastIndexOf('.')+1);
            String fn = fileName.substring(0, fileName.lastIndexOf('.'));
            String path = inputFile.getPath();
            String absPath = inputFile.getAbsolutePath();
            String justPath = absPath.substring(0, absPath.lastIndexOf(fileName));
            String endPath = justPath.substring(0, justPath.lastIndexOf('/'));
            endPath = endPath.substring(0, endPath.lastIndexOf('/'));
                        
            processLog += logEntry("Input File is: " + fn);
            processLog += logEntry("Located in: " + justPath);
            
            // Create a new instance of a document builder
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            // Make sure that the document builder is aware that the XML document using namespace prefixing
            dbFactory.setNamespaceAware(true);
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            // Make a document by parsing the input file
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();
            
            // Make a new document for the output
            Document newDoc = dBuilder.newDocument();
            Element rootElement = newDoc.createElement("QNET");
            rootElement.setAttribute("Version", "Download");
            doc.appendChild(rootElement);
            
            Element orderElement = doc.createElement("ORDER");
            orderElement.setAttribute("BillShipTo", "Y");
            
            
            // Print out the local name of the root element
            // doc.getDocumentElement gets the root element of the document
            // .getLocalName gets the name of the element without the namespace prefix
            
            String dsf_id = getValue("Order.IdData.DSF.Id", doc, "pcpsi");
            
            if(dsf_id != null && !dsf_id.isEmpty()){
                processLog += logEntry("Grabbed Reqnum: " + dsf_id +"\nThis is also the Client_PO");
            } else {
                processLog += logEntry("Failed to grab Reqnum");
                processCorrectly = false;
            }
            
            orderElement.setAttribute("Reqnum", dsf_id);
            rootElement.appendChild(orderElement);
            
            String user = getValue("GeneralOrderInfo.Buyer.User.NameComponents.First", doc, "pcpsi") + " " + getValue("GeneralOrderInfo.Buyer.User.NameComponents.Last", doc, "pcpsi");
            String client = getValue("GeneralOrderInfo.Buyer.Company.IdData.MIS.Id", doc, "pcpsi");
            
            if(client.equals("7000CRIT")){
                client = "D2703";
                outputDir = outputDir2;
            } else if(client.equals("7000THC")){
                client = "D1180";
                outputDir = outputDir2;
            }
            
            if(client != null && !client.isEmpty()){
                processLog += logEntry("Grabbed Client: " + client);
            } else {
                processLog += logEntry("Failed to grab Client");
                processCorrectly = false;
            }
            
            if(user != null && !user.isEmpty()){
                processLog += logEntry("Grabbed User: " + user);
            } else {
                processLog += logEntry("Failed to grab User");
                processCorrectly = false;
            }
            
            Element clientElement = doc.createElement("CLIENT");
            clientElement.appendChild(doc.createTextNode(client));
            Element userElement = doc.createElement("USER");
            userElement.appendChild(doc.createTextNode(user));
            
            orderElement.appendChild(clientElement);
            orderElement.appendChild(userElement);
            
            Element dateElement = doc.createElement("DATE");
            dateElement.appendChild(doc.createTextNode(dateFormat.format(date)));
            orderElement.appendChild(dateElement);
            
            Element loginElement = doc.createElement("LOGIN_CC");
            loginElement.appendChild(doc.createTextNode("NOT_ON_FILE"));
            orderElement.appendChild(loginElement);
            
            
            
            
            // This is a temporary workaround for getting the email address used on the order
            // The issue lies in the fact that the Address object that contains the email address
            // is not the first Address object in the Buyer object.  Even though it is the highest
            // branch, it is not the correct Address object chosen by item(0).
            Node buyerNode = doc.getElementsByTagName("pcpsi:Buyer").item(0);
            buyerNode.normalize();
            Element buyerElement = (Element) buyerNode;
            Node addressNode = buyerElement.getElementsByTagName("pcpsi:Address").item(1);
            addressNode.normalize();
            Element addressElement = (Element) addressNode;
            String email = addressElement.getElementsByTagName("pcpsi:Email").item(0).getTextContent();
            
            
            // This is also probably not the best way to get the BillingEntityAccount number.  But
            // considering that the node structure for billing details are <key> & <value> pairs, not
            // nodes and text for that node <BillingEntity...>, this is the best way I've been able
            // to come up with at the time.
            String billingEntity = "";
            String billingPO = "";
            Node paymentDetailNode = doc.getElementsByTagName("pcpsi:PaymentMethodDetailAsText").item(0);
            if(paymentDetailNode != null){
                paymentDetailNode.normalize();
                NodeList paymentDetails = paymentDetailNode.getChildNodes();

                for(int k = 0; k < paymentDetails.getLength(); k++){
                    Node tempNode = paymentDetails.item(k);
                    tempNode.normalize();

                    try{
                        Element tempElem = (Element) tempNode;
                        // We're looking for both the billing account number (should ALWAYS be present), and the billing PO number
                        // If the billing PO number exists, it will be prepended to the remarks for each product line item.
                        String billString = "";

                        try{
                            billString = tempElem.getElementsByTagName("pcpsi:Key").item(0).getTextContent();
                        } catch (NullPointerException e){
                            processLog += logEntry("No billing info found");
                            processCorrectly = false;
                            break;
                        }

                        if(billString.equals("BillingEntityPONumber")){
                            billingPO = tempElem.getElementsByTagName("pcpsi:Value").item(0).getTextContent();
                        } else if(billString.equals("BillingEntityAccountNumber")){
                            billingEntity = tempElem.getElementsByTagName("pcpsi:Value").item(0).getTextContent();
                        }
                    } catch (ClassCastException e){
                        ;
                    }
                }
            } else {
                processLog += logEntry("No PaymentMethodDetailAsText element found");
                processCorrectly = false;
            }
            
            if(billingEntity != null && !billingEntity.isEmpty()){
                processLog += logEntry("Grabbed Cost Center #: " + billingEntity);
            } else {
                processLog += logEntry("Failed to grab Cost Center #");
                processCorrectly = false;
            }
            
            // Create an ArrayList for all of the products and shipping locations
            // Opted for ArrayLists rather than just arrays because while there
            // might be 9 products, not all of those products may be "NonPrintedProducts".
            // And only "NonPrintedProducts" should be added to the output XML.
            ArrayList<Product> products = new ArrayList<>();
            ArrayList<ShipLoc> shippingLocations = new ArrayList<>();
            
            // If we've grabbed everything correctly so far, start to look at the product info
            if(processCorrectly){
                
                // Get the list of products
                NodeList productList = doc.getElementsByTagName("pcpsi:Product");

                if(productList.getLength() > 0){
                    processLog += logEntry(productList.getLength() + " products found");
                    int actualProducts = 0;

                    for (int i = 0; i < productList.getLength(); i++) {
                        Node productNode = productList.item(i);
                        productNode.normalize();
                        if (productNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element productElement = (Element) productNode;
                            if (productElement.getElementsByTagName("pcpsi:ProductClassification").item(0).getTextContent().equals("NonPrintedProduct")) {

                                actualProducts++;

                                Product myProd = new Product(productNode, client, billingEntity);


                                Node shipNode = productElement.getElementsByTagName("pcpsi:Shipment").item(0);
                                shipNode.normalize();
                                ShipLoc myShip = new ShipLoc(shipNode, billingEntity);

                                if(billingPO.length() > 0){
                                    if(myShip.getRemarks().length() > 0){
                                        myProd.setRemarks(billingPO + " - " + myShip.getRemarks());
                                    } else {
                                        myProd.setRemarks(billingPO);
                                    }
                                } else {
                                    myProd.setRemarks(myShip.getRemarks());
                                }

                                products.add(myProd);
                                shippingLocations.add(myShip);

                            }
                        }
                    }

                    processLog += logEntry(actualProducts + " actual (non-printed) products found");

                    // If there are no actual products (non-printed products), then write the log file, and
                    // exit successfully.  No need to create an output file for a record with 0 entries.
                    if(actualProducts == 0){
                        writeFile(processLog, endPath + "/Logs/processed_logs/" + fn +"_log.txt", processLog, 0);
                    }

                } else {
                    processLog += logEntry("Failed to grab products");
                    processCorrectly = false;
                }
            }
            
            // Start building the string that will hold the output XML
            // *******************************************************
            String finalString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
            String printString = printTag("CLIENT", client);
            
            // Again, of we've grabbed everything correctly so far, start to
            // look build the string for the output file
            if(processCorrectly){
                printString += printTag("USER", user);
                printString += printTag("DATE", dateFormat.format(date));
                printString += printTag("LOGIN_CC", "NOT_ON_FILE");
                printString += printTag("CLIENT_PO", dsf_id);
                printString += printTag("EMAIL", email);

                String shippingString = "";

                for(int j = 0; j < shippingLocations.size(); j++){
                    shippingString += shippingLocations.get(j).toXML() + "\n";
                }

                printString += printTag("SHIP_LOCS", "\n" + shippingString);

                String productString = "\n";

                for(int j = 0; j < products.size(); j++){
                    productString += products.get(j).toXML() + "\n";
                    processLog += logEntry("Product " + (j+1) + ": " + products.get(j).getDesc(), 0);
                }
                processLog += logEntry("");

                printString += printTag("LINE_ITEMS", productString);

                printString = printTag("ORDER", "BillShipTo=\"Y\" Reqnum=\"" + dsf_id + "\"", "\n" + printString);
                printString = printTag("QNET", "Version=\"Download\"", "\n" + printString);
                // *******************************************************
            }
            
            // Write the files (both output file and log file) to their final locations
            // Only write the output file if pulling all of the data was successful
            writeFile(processLog, endPath + "/Logs/processed_logs/", fn +"_log.txt");
            
            if(processCorrectly){
                writeFile(finalString + printString, outputDir, fn +"_output.txt", 0);
            } else {
                System.exit(1);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Return an XML tag with the given tag name, attributes, and value
    private static String printTag(String name, String attributes, String value){
        String temp = "<" + name;
        if(attributes != null && !attributes.isEmpty()){
            temp += " " + attributes;
        }
        temp = temp + ">" + value + "</" + name + ">\n";
        return temp;
    }

    // Return an XML tag with the given tag name and value
    // If the value is empty, returns an empty XML tag
    private static String printTag(String name, String value){
        if(value != null && !value.isEmpty()){
            return printTag(name, "", value);
        } else {
            return printEmptyTag(name);
        }
    }

    // Returns an empty XML tag
    private static String printEmptyTag(String name){
        return "<" + name + "/>\n";
    }
    
    // Returns an empty XML tag with the given attributes
    private static String printEmptyTag(String name, String attributes){
        return "<" + name + " " + attributes + "/>\n";
    }
    
    // Returns a value from the given document with the given field address
    private static String getValue(String address, Document doc){
        return getValue(address, doc, "");
    }
    // Returns a value from the given document with the given field address and prefix
    private static String getValue(String address, Document doc, String prefix){
        String[] locs = address.split("\\.");
        
        Element tempElem = doc.getDocumentElement();
        
        String tempAdd = "";
        
        for(int i = 0; i < locs.length; i++){
            tempAdd = locs[i];
            if(prefix.length() > 0){
                tempAdd = prefix + ":" + tempAdd;
            }
            Node tempNode = tempElem.getElementsByTagName(tempAdd).item(0);
            tempNode.normalize();
            tempElem = (Element) tempNode;
        }
        
        String returnString = "";
        
        try {
            returnString = tempElem.getTextContent();
        } catch (NullPointerException e){
            returnString = "";
        }
        
        return returnString;
    }
    
    // Returns a string of splitting up entries into a log
    private static String logEntry(String entry){
        return logEntry(entry, 2);
    }
    
    private static String logEntry(String entry, int lines){
        String tempString = entry + "\n";
        if(lines > 0){
            tempString += "------------------------------------------------------";
        }
        for(int j = 0; j < lines; j++){
            tempString += "\n";
        }
        return tempString;
    }
    
    private static void writeFile(String data, String loc, String fileName){
        writeFile(data, loc, fileName, 2);
    }
    
    private static void writeFile(String data, String loc, String fileName, int exit){
        try(  PrintWriter out = new PrintWriter( loc + fileName )  ){
            out.println( data );
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(exit != 2){
            System.exit(exit);
        }
    }
}