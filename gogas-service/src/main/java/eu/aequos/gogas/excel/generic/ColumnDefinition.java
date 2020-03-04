package eu.aequos.gogas.excel.generic;

import java.util.function.Function;

public class ColumnDefinition<T> {
    public enum DataType {
        Text,
        TextCenter,
        Date,
        Currency,
        Integer,
        Float,
        SubTotal
    }

    private final String header;
    private final DataType dataType;
    private Function<T, Object> extract;
    private boolean showTotal;

    public ColumnDefinition(String header, DataType dataType) {
        this.header = header;
        this.dataType = dataType;
    }

    public String getHeader() {
        return header;
    }

    public DataType getDataType() {
        return dataType;
    }

    public boolean showTotal() {
        return showTotal;
    }

    public ColumnDefinition<T> withShowTotal() {
        this.showTotal = true;
        return this;
    }

    public Object extract(T dataItem) {
        return extract.apply(dataItem);
    }

    public ColumnDefinition<T> withExtract(Function<T, Object> extract) {
        this.extract = extract;
        return this;
    }
}
