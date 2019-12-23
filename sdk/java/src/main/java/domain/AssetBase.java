package domain;

import java.util.HashMap;
import java.util.Map;

public abstract class AssetBase {

    Map<String, Object> document;

    public AssetBase() {
        this.document = new HashMap<>();
    }

    /**
     * Set the value of an attribute.
     *
     * @param attr The attribute name in dot notation format. ex: 'foo.bar'
     * @param value The value for the particular attribute. Can be any json serializable type.
     *
     */
    public void setAttr(String attr, Object value) {

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
