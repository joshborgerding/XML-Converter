
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import java.util.ArrayList;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;


public class Convert_XML {
    
    private final Document doc;
    private final DateFormat dateFormat = new SimpleDateFormat("yyyMMdd");
    private final Date date;
    private final String fn;
    private final String path;
    private String dsf_id;
    private boolean processCorrectly;
    private String user;
    private String client;
    private String email;
    private String billingEntity, billingPO;
    private int outputDir;
    private ArrayList<NonPrintProduct> products;
    private ArrayList<ShipLoc> shippingLocations;
    private ArrayList<Kit> kits;
    private String processLog;
    private String xmlOutput;
    
    public Convert_XML(Document doc, String fn, String path){
        this.doc = doc;
        this.fn = fn;
        this.path = path;
        outputDir = 1;
        date = new Date();
    }
    
    public boolean processXML(){
        processLog = "";
        processCorrectly = true;
        
        processLog += logEntry(date.toString());
        processLog += logEntry("Input File is: " + fn);
        processLog += logEntry("Located in: " + path);
        dsf_id = getValue("OrderInformation.Order.IdData.DSF.Id", doc);
        
        if(dsf_id != null && !dsf_id.isEmpty()){
            processLog += logEntry("Grabbed Reqnum: " + dsf_id +"\nThis is also the Client_PO");
        } else {
            processLog += logEntry("Failed to grab Reqnum");
            processCorrectly = false;
        }
        
        user = getValue("OrderInformation.GeneralOrderInfo.Buyer.User.NameComponents.First", doc) + " " + getValue("OrderInformation.GeneralOrderInfo.Buyer.User.NameComponents.Last", doc);
        client = getValue("OrderInformation.GeneralOrderInfo.Buyer.Company.IdData.MIS.Id", doc);
        
        if(client.equals("7000CRIT")){
            client = "D2703";
            outputDir = 2;
        } else if(client.equals("7000THC")){
            client = "D1180";
            outputDir = 2;
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
        
        email = getValue("OrderInformation.GeneralOrderInfo.Buyer.Address.Email", doc);
        
        billingEntity = "";
        billingPO = "";
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
                    String billString;

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
        products = new ArrayList<>();
        shippingLocations = new ArrayList<>();
        kits = new ArrayList<>();
        
        // If we've grabbed everything correctly so far, start to look at the product info
        if(processCorrectly){

            // Get the list of products
            NodeList productList = doc.getElementsByTagName("pcpsi:Product");

            if(productList.getLength() > 0){

                for (int i = 0; i < productList.getLength(); i++) {
                    Node productNode = productList.item(i);
                    productNode.normalize();
                    if (productNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element productElement = (Element) productNode;
                        if (getValue("ProductData.ProductClassification", productNode).equals("NonPrintedProduct")){

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

                        } else if(getValue("ProductData.ProductClassification", productNode).equals("PrintedProductTicketTree")){
                            Kit myKit = new Kit(productNode, client, billingEntity, billingPO);

                            if(myKit.getHasNonPrinted()){
                                ArrayList<KitItem> items = myKit.getItems();
                                for(int h = 0; h < items.size(); h++){
                                    products.add(myKit.getItem(h));
                                    shippingLocations.add(myKit.getShip(h));
                                }

                                kits.add(myKit);

                            }

                        }
                    }
                }

                processLog += logEntry(productList.getLength() + " individual products found");
                processLog += logEntry(Kit.getCount() + " products are kits");
                processLog += logEntry(NonPrintProduct.getCount() + " actual nonprint products found");
                processLog += logEntry(Kit.getProductCount() + " nonprint products in kits");

            } else {
                processLog += logEntry("Failed to grab products");
                processCorrectly = false;
            }
            
            for(int j = 0; j < products.size(); j++){
                processLog += logEntry("Product " + (j+1) + ": (" + products.get(j).getQuantity() + ") " + products.get(j).getDesc(), 0);
            }
            
            
            if(products.size() > 0){
                processLog += logEntry("");
            }
            
        }
        
        return processCorrectly;
        
    }
    
    public Node getXMLOutput(){
        try{
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            // Make sure that the document builder is aware that the XML document using namespace prefixing
            dbFactory.setNamespaceAware(true);
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

            // Make a new document for the output
            Document newDoc = dBuilder.newDocument();
            newDoc.setXmlStandalone(true);
            
            Element rootElem = newDoc.createElement("QNET");
            rootElem.setAttribute("Version", "Download");
            newDoc.appendChild(rootElem);
            
            Element orderElem = newDoc.createElement("ORDER");
            orderElem.setAttribute("BillShipTo", "Y");
            orderElem.setAttribute("Reqnum", dsf_id);
            rootElem.appendChild(orderElem);
            
            Element clientElem = newDoc.createElement("CLIENT");
            clientElem.setTextContent(client);
            orderElem.appendChild(clientElem);
            
            Element userElem = newDoc.createElement("USER");
            userElem.setTextContent(user);
            orderElem.appendChild(userElem);
            
            Element dateElem = newDoc.createElement("DATE");
            dateElem.setTextContent(dateFormat.format(date));
            orderElem.appendChild(dateElem);
            
            Element loginElem = newDoc.createElement("LOGIN_CC");
            loginElem.setTextContent("NOT_ON_FILE");
            orderElem.appendChild(loginElem);
            
            Element clientPOElem = newDoc.createElement("CLIENT_PO");
            clientPOElem.setTextContent(dsf_id);
            orderElem.appendChild(clientPOElem);
            
            Element emailElem = newDoc.createElement("EMAIL");
            emailElem.setTextContent(email);
            orderElem.appendChild(emailElem);
            
            Element shipLocElem = newDoc.createElement("SHIP_LOCS");
            orderElem.appendChild(shipLocElem);
            
            for(int i = 0; i < shippingLocations.size(); i++){
                ShipLoc myShip = shippingLocations.get(i);
                Element ccElem = newDoc.createElement("CC");
                ccElem.setAttribute("Ref", String.valueOf(myShip.getID()));
                ccElem.setAttribute("Id", dsf_id);
                ccElem.setAttribute("Type", "C");
                
                
                Element addr1Elem = newDoc.createElement("ADDR_LINE1");
                addr1Elem.setTextContent(myShip.getCompany());
                ccElem.appendChild(addr1Elem);
                
                Element addr2Elem = newDoc.createElement("ADDR_LINE2");
                addr2Elem.setTextContent(myShip.getAdd1());
                ccElem.appendChild(addr2Elem);
                
                Element addr3Elem = newDoc.createElement("ADDR_LINE3");
                if(myShip.getAdd2().length() > 0 && myShip.getAdd3().length() > 0){
                    addr3Elem.setTextContent(myShip.getAdd2() + " - " + myShip.getAdd3());
                } else {
                    if(myShip.getAdd2().length() > 0){
                        addr3Elem.setTextContent(myShip.getAdd2());
                    } else {
                        addr3Elem.setTextContent(myShip.getAdd3());
                    }
                }
                ccElem.appendChild(addr3Elem);
                
                Element cityElem = newDoc.createElement("ADDR_CITY");
                cityElem.setTextContent(myShip.getCity());
                ccElem.appendChild(cityElem);
                
                Element stateElem = newDoc.createElement("ADDR_STATE");
                stateElem.setTextContent(myShip.getState());
                ccElem.appendChild(stateElem);
                
                Element zipElem = newDoc.createElement("ADDR_ZIP");
                zipElem.setTextContent(myShip.getZip());
                ccElem.appendChild(zipElem);
                
                Element attnElem = newDoc.createElement("ADDR_ATTN");
                attnElem.setTextContent(myShip.getAttn());
                ccElem.appendChild(attnElem);
                
                shipLocElem.appendChild(ccElem);
            }
            
            Element lineItemElem = newDoc.createElement("LINE_ITEMS");
            orderElem.appendChild(lineItemElem);
            
            for(int j = 0; j < products.size(); j ++){
                NonPrintProduct myProd = products.get(j);
                Element lineElem = newDoc.createElement("LINE");
                lineElem.setAttribute("Catalog", myProd.getCatalog());
                lineElem.setAttribute("Number", String.valueOf(myProd.getID()));
                
                Element itemElem = newDoc.createElement("ITEM");
                itemElem.setAttribute("Client", myProd.getClient());
                itemElem.setTextContent(myProd.getItem());
                lineElem.appendChild(itemElem);
                
                Element descElem = newDoc.createElement("DESC");
                descElem.setTextContent(myProd.getDesc());
                lineElem.appendChild(descElem);
                
                Element unitElem = newDoc.createElement("UNIT");
                unitElem.setAttribute("Code", "");
                lineElem.appendChild(unitElem);
                
                Element priceElem = newDoc.createElement("PRICE");
                priceElem.setTextContent(myProd.getPrice());
                lineElem.appendChild(priceElem);
                
                Element qtyElem = newDoc.createElement("QTY");
                qtyElem.setAttribute("ChargeTo", billingEntity);
                // SHOULD REALLY BE FIXED IN THE FUTURE TO BE TIED DIRECTLY TO 
                // THE ACTUALLY SHIP ID, NOT THE PRODUCT ID.
                qtyElem.setAttribute("ShipRef", String.valueOf(myProd.getID()));
                qtyElem.setTextContent(myProd.getQuantity());
                lineElem.appendChild(qtyElem);
                
                Element remarkElem = newDoc.createElement("REMARKS");
                if(billingPO.length() > 0 && shippingLocations.get(j).getRemarks().length() > 0){
                    remarkElem.setTextContent(billingPO + " - " + shippingLocations.get(j).getRemarks());
                } else {
                    if(billingPO.length() > 0){
                        remarkElem.setTextContent(billingPO);
                    } else if (shippingLocations.get(j).getRemarks().length() > 0){
                        remarkElem.setTextContent(shippingLocations.get(j).getRemarks());
                    } else {
                        remarkElem.setTextContent("");
                    }
                }
                lineElem.appendChild(remarkElem);
                
                lineItemElem.appendChild(lineElem);
            }            

            return newDoc;
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    public String getStringOutput(){
        // Start building the string that will hold the output XML
        // *******************************************************
        String finalString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
        xmlOutput = "";
        
        xmlOutput = printTag("CLIENT", client);

        // Again, of we've grabbed everything correctly so far, start to
        // look build the string for the output file
        if(processCorrectly){
            xmlOutput += printTag("USER", user);
            xmlOutput += printTag("DATE", dateFormat.format(date));
            xmlOutput += printTag("LOGIN_CC", "NOT_ON_FILE");
            xmlOutput += printTag("CLIENT_PO", dsf_id);
            xmlOutput += printTag("EMAIL", email);

            String shippingString = "";

            for(int j = 0; j < shippingLocations.size(); j++){
                shippingString += shippingLocations.get(j).toXML() + "\n";
            }

            xmlOutput += printTag("SHIP_LOCS", "\n" + shippingString);

            String productString = "\n";

            for(int j = 0; j < products.size(); j++){
                productString += products.get(j).toXML() + "\n";
            }

            xmlOutput += printTag("LINE_ITEMS", productString);

            xmlOutput = printTag("ORDER", "BillShipTo=\"Y\" Reqnum=\"" + dsf_id + "\"", "\n" + xmlOutput);
            xmlOutput = printTag("QNET", "Version=\"Download\"", "\n" + xmlOutput);
            // *******************************************************
        }
        return finalString + xmlOutput;
    }
    
    public String getProcessLog(){
        return processLog;
    }
    
    // Return an XML tag with the given tag name, attributes, and value
    private String printTag(String name, String attributes, String value){
        String temp = "<" + name;
        if(attributes != null && !attributes.isEmpty()){
            temp += " " + attributes;
        }
        temp = temp + ">" + value + "</" + name + ">\n";
        return temp;
    }

    // Return an XML tag with the given tag name and value
    // If the value is empty, returns an empty XML tag
    private String printTag(String name, String value){
        if(value != null && !value.isEmpty()){
            return printTag(name, "", value);
        } else {
            return printEmptyTag(name);
        }
    }

    // Returns an empty XML tag
    private String printEmptyTag(String name){
        return "<" + name + "/>\n";
    }
    
    // Returns an empty XML tag with the given attributes
    private String printEmptyTag(String name, String attributes){
        return "<" + name + " " + attributes + "/>\n";
    }
    
    // Returns a value from the given document with the given field address and prefix
    private String getValue(String address, Node node){
        String[] locs = address.split("\\.");
        
        
        Node tempNode = node;
        
        for(String loc: locs){
            NodeList childNodes = tempNode.getChildNodes();
            tempNode = getNode(childNodes, loc);
            if(tempNode == null){
                return "";
            }
        }
        
        Element elem = (Element) tempNode;
        
        return elem.getTextContent();
    }
    
    private Node getNode(NodeList nodes, String name){
        Node tempNode;
        
        for(int i = 0; i < nodes.getLength(); i++){
            tempNode = nodes.item(i);
            if(tempNode.getLocalName().equals(name)){
                return tempNode;
            }
        }
        
        return null;
    }

    private Node getNode(Node node, String name){
        
        NodeList nodes = node.getChildNodes();
        return getNode(nodes, name);

    }
    
    // Returns a string of splitting up entries into a log
    private String logEntry(String entry){
        return logEntry(entry, 2);
    }
    
    private String logEntry(String entry, int lines){
        String tempString = entry + "\n";
        if(lines > 0){
            tempString += "------------------------------------------------------";
        }
        for(int j = 0; j < lines; j++){
            tempString += "\n";
        }
        return tempString;
    }
    
    public boolean processedCorrectly(){
        return processCorrectly;
    }
    
    public int getProductCount(){
        return NonPrintProduct.getCount();
    }
    
    public int getOutputDir(){
        return outputDir;
    }
}