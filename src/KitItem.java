
import org.w3c.dom.Node;

public class KitItem extends NonPrintProduct{
        
    public KitItem(Node itemNode, String client, String billingEntity){
        
        count++;
        this.ID = count;
        this.catalog = "1000";
        this.client = client;
        this.item = getValue("ItemData.SKUNumber", itemNode);
        this.description = getValue("ItemData.ItemDescription", itemNode);
        this.price = getValue("ItemData.PricePerUnit", itemNode);
        this.quantity = getValue("ItemData.Quantity", itemNode);
        this.chargeTo = billingEntity;
        
    }
    
}

