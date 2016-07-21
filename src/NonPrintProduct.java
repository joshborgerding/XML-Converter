
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author jborgerding
 */
public abstract class NonPrintProduct {
    
    protected static int count = 0;
    protected int ID;
    protected String catalog;
    protected String client;
    protected String item;
    protected String description;
    protected String price;
    protected String quantity;
    protected String remarks;
    protected String chargeTo;
    
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
            //System.out.println("Product Check node " + tempNode.getLocalName() + " for " + name);
            if(tempNode.getLocalName().equals(name)){
                //System.out.println("Product Found node " + tempNode.getLocalName());
                return tempNode;
            }
        }
        
        //System.out.println("Product failed to find " + name);
        
        return null;
    }
    
    public String toXML(){
        String temp = printTag("ITEM", "Client=\"" + client + "\"", item);
        temp += "\n" + printTag("DESC", description);
        temp += "\n" + printEmptyTag("UNIT", "Code=\"\"");
        temp += "\n" + printTag("PRICE", price);
        temp += "\n" + printTag("QTY", "ChargeTo=\"" + chargeTo + "\" ShipRef=\"" + ID + "\"", quantity);
        temp += "\n" + printTag("REMARKS", remarks);
        temp = printTag("LINE", "Catalog=\"" + catalog + "\" Number=\"" + ID + "\"", "\n" + temp + "\n");
        
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
    
    public void setRemarks(String remarks){
        this.remarks = remarks;
    }
        
    public static int getCount(){
        return count;
    }
    
    public int getID(){
        return ID;
    }
    
    public String getDesc(){
        return description;
    }
    
    public String getPrice(){
        return price;
    }
    
    public String getQuantity(){
        return quantity;
    }
    
    public String getCatalog(){
        return catalog;
    }
    
    public String getChargeTo(){
        return chargeTo;
    }
    
    public String getClient(){
        return client;
    }
    
    public String getItem(){
        return item;
    }
    
    public String getRemarks(){
        return remarks;
    }
    
}
