/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author jborgerding
 */


import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import java.util.ArrayList;


public class Kit {
    
    private static int count = 0;
    private static int products = 0;
    private int ID;
    private boolean hasNonPrinted;
    private ArrayList<KitItem> items;
    private ArrayList<ShipLoc> shippingLocations;
    private ShipLoc shippingLoc;
    private Element eElement;
    
    public Kit(Node kit, String client, String billingEntity, String billingPO){
        count++;
        ID = count;
        
        items = new ArrayList<>();
        shippingLocations = new ArrayList<>();
        
        hasNonPrinted = false;
        
        eElement = (Element) kit;

        Node shipNode = eElement.getElementsByTagName("pcpsi:Shipment").item(0);
        shipNode.normalize();
                
        NodeList list = eElement.getElementsByTagName("pcpsi:Item");
        
        for(int i = 0; i < list.getLength(); i++){
            Node item = list.item(i);
            if(getValue("ItemData.CatalogItem.IdData.DSF.IdTypeName", item).equals("NonPrintedProduct")){
                hasNonPrinted = true;
                products++;
                
                shippingLoc = new ShipLoc(shipNode, billingEntity);
                
                KitItem myItem = new KitItem(item, client, billingEntity);
                
                if(billingPO.length() > 0){
                    if(shippingLoc.getRemarks().length() > 0){
                        myItem.setRemarks(billingPO + " - " + shippingLoc.getRemarks());
                    } else {
                        myItem.setRemarks(billingPO);
                    }
                } else {
                    myItem.setRemarks(shippingLoc.getRemarks());
                }
                
                items.add(myItem);
                shippingLocations.add(shippingLoc);
                
            }
        }
        
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
            //System.out.println("Check node " + tempNode.getLocalName() + " for " + name);
            if(tempNode.getLocalName().equals(name)){
                //System.out.println("Found node " + tempNode.getLocalName());
                return tempNode;
            }
        }
        
        return null;
    }
    
    public static int getCount(){
        return count;
    }
    
    public static int getProductCount(){
        return products;
    }
    
    public ArrayList<KitItem> getItems(){
        return items;
    }
    
    public KitItem getItem(int i){
        return items.get(i);
    }
    
    public ShipLoc getShip(int i){
        return shippingLocations.get(i);
    }
        
    public boolean getHasNonPrinted(){
        return hasNonPrinted;
    }
    
}
