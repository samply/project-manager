package de.samply.form;

import com.fasterxml.jackson.databind.ObjectMapper;

public enum DataType {

    INTEGER(Integer.class),
    DOUBLE(Double.class),
    BOOLEAN(Boolean.class),
    STRING(String.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final Class<?> clazz;

    DataType(Class<?> clazz) {
        this.clazz = clazz;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public static DataType fromString(String type) {
        for (DataType dataType : values()) {
            if (dataType.name().equalsIgnoreCase(type)) {
                return dataType;
            }
        }
        throw new IllegalArgumentException("Unsupported dataType: " + type);
    }

    public static Object convert(String dataType, String value) throws Exception {
        DataType type = fromString(dataType);
        return objectMapper.readValue(value, type.getClazz());
    }

    public Object convert(String value) throws Exception {
        return objectMapper.readValue(value, this.getClazz());
    }

}
