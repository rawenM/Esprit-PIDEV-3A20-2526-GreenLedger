package Models.dto.external;

/**
 * Response DTO for Carbon Interface API estimates.
 * Represents calculated carbon emissions for an activity.
 */
public class CarbonEstimateResponse {
    
    private String id;
    private String type;
    private CarbonAttributes attributes;
    
    public CarbonEstimateResponse() {}
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public CarbonAttributes getAttributes() {
        return attributes;
    }
    
    public void setAttributes(CarbonAttributes attributes) {
        this.attributes = attributes;
    }
    
    @Override
    public String toString() {
        return "CarbonEstimate{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", attributes=" + attributes +
                '}';
    }
}
