package domain;

import java.util.HashMap;
import java.util.Map;

public abstract class AssetBase {

    Map<String, Object> document;

    public AssetBase() {
        this.document = new HashMap<>();
    }

    public void setAttr(String attr, Object value) {
        /*
        """Set the value of an attribute.

        Args:
            attr (str): The attribute name in dot notation format.
                ex: 'foo.bar'
            value (:obj:`object`): value: The value for the particular
                attribute. Can be any json serializable type.
        """
         */

        String[] splittedAttr = attr.split("\\.");
        String finalAttr = splittedAttr[splittedAttr.length - 1];

        if (value instanceof Map)
            document.put(finalAttr, value);
        else if (value instanceof AssetBase)
            document.put(finalAttr, ((AssetBase) value).forJson());
        else document.put(finalAttr, value);
    }


    abstract public Map forJson();


}
