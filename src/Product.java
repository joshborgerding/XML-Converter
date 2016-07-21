
import org.w3c.dom.Node;

public class Product extends NonPrintProduct{
  
    public Product(Node product, String client, String billingEntity){
        
        count++;
        this.ID = count;
        this.catalog = "1000";
        this.client = client;
        this.item = getValue("ProductData.SKUNumber", product);
        this.description = getValue("ProductData.ProductDescription", product);
        this.price = getValue("ProductData.Items.Item.ItemData.PricePerUnit", product);
        this.quantity = getValue("ProductData.Quantity", product);
        this.chargeTo = billingEntity;
        
    }

}
