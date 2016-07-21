
/**
 * Write a description of class ShipLoc here.
 * 
 * @author (your name) 
 * @version (a version number or a date)
 */

import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
//import java.text.DecimalFormat;

public class ShipLoc
{
    // instance variables - replace the example below with your own
    private static int count = 0;
    private int ID;
    private String catalog;
    private String company;
    private String addr1;
    private String addr2;
    private String addr3;
    private String city;
    private String state;
    private String zip;
    private String attn;
    private String country;
    private String remarks;
//    private Element shipEl;

    /**
     * Constructor for objects of class Product
     */
    public ShipLoc(Node ship, String billingEntity)
    {
        // initialise instance variables
        count++;
        ID = count;

//        shipEl = (Element) ship;
        catalog = billingEntity;
        
        company = getValue("DestinationAddress.CompanyName", ship);
        addr1 = getValue("DestinationAddress.Address1", ship);
        addr2 = getValue("DestinationAddress.Address2", ship);
        addr3 = getValue("DestinationAddress.Address3", ship);
        city = getValue("DestinationAddress.City", ship);
        state = getValue("DestinationAddress.State_Province_Region", ship);
        zip = getValue("DestinationAddress.Zip_PostalCode", ship);
        country = getValue("DestinationAddress.Country", ship);
        remarks = getValue("ShippingInstructions", ship);
        attn = getValue("DestinationAddress.NameFirst", ship) + " " + getValue("DestinationAddress.NameLast", ship);
        
    }
    
    public static String getValue(String address, Node node){
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
    
    private static Node getNode(NodeList nodes, String name){
        Node tempNode;
        
        for(int i = 0; i < nodes.getLength(); i++){
            tempNode = nodes.item(i);
            //System.out.println("Shipping Check node " + tempNode.getLocalName() + " for " + name);
            if(tempNode.getLocalName().equals(name)){
                //System.out.println("Found node " + tempNode.getLocalName());
                return tempNode;
            }
        }
        
        //System.out.println("Product failed to find " + name);
        
        return null;
    }

    public String printAddress(){
        String temp = attn + "\n" + addr1;
        if(addr2 != null && !addr2.isEmpty()) {
            temp += "\n" + addr2;
        }
        if(addr3 != null && !addr3.isEmpty()) {
            temp += "\n" + addr3;
        }
        temp += "\n" + city + ", " + state + " " + zip;
        
        return temp;
    }
    
    public String toXML(){
        //  CompanyName should really be Address Line 1 according to Brad and Kristy.
        //  So pushing address lines down one step, concatenating 2 & 3 into 3,
        //  and making CompanyName be address line 1
        
        String temp = printTag("ADDR_LINE1", company);
        temp += "\n" + printTag("ADDR_LINE2", addr1);
        temp += "\n" + printTag("ADDR_LINE3", addr2 + " " + addr3);
        temp += "\n" + printTag("ADDR_CITY", city) + "\n" + printTag("ADDR_STATE", state) + "\n" + printTag("ADDR_ZIP", zip);
        temp += "\n" + printTag("ATTN", attn);
        temp = printTag("CC", "Ref=\""+ID+"\" Id=\"" + catalog + "\" Type=\"C\"",  "\n" + temp + "\n");
        return temp;
    }
    
    private String printTag(String name, String attributes, String value){
        String temp = "<" + name;
        if(attributes != null && !attributes.isEmpty()){
            temp += " " + attributes;
        }
        temp = temp + ">" + value + "</" + name + ">";
        return temp;
    }
    
    private String printTag(String name, String value){
        if(value != null && !value.isEmpty() && value.trim().length() > 0){
            return printTag(name, "", value);
        } else {
            return printEmptyTag(name);
        }
    }
    
    private String printEmptyTag(String name){
        return "<" + name + "/>";
    }
    
    private static String printEmptyTag(String name, String attributes){
        return "<" + name + " " + attributes + "/>";
    }
    
    public String getRemarks(){
        return remarks;
    }
    
    public static int getCount(){
        return count;
    }
    
    public int getID(){
        return ID;
    }
    
    public String getCompany(){
        return company;
    }
    
    public String getAdd1(){
        return addr1;
    }
    
    public String getAdd2(){
        return addr2;
    }
    
    public String getAdd3(){
        return addr3;
    }
    
    public String getCity(){
        return city;
    }
    
    public String getState(){
        return state;
    }
    
    public String getZip(){
        return zip;
    }
    
    public String getAttn(){
        return attn;
    }
}
