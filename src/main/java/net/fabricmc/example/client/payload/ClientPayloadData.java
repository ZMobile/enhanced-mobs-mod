package net.fabricmc.example.client.payload;

public class ClientPayloadData {
    private String type;
    private Object data;

public ClientPayloadData(String type, Object data) {
        this.type = type;
        this.data = data;
    }

    public String getType() {
        return type;
    }

    public Object getData() {
        return data;
    }
}
