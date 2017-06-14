package norm.dao;

public class AdditionalProperty {
    private String key;
    private Object value;

    public AdditionalProperty(String key, Object value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }
}

