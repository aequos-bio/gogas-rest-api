package eu.aequos.gogas.excel.products;

import lombok.Data;

@Data
public class ExtractProductError {
    private String type;
    private String message;
    private int rowIndex;
    private int colIndex;

    public String getCompleteMessage() {
        String result = "Errore durante la lettura del file excel";

        if (rowIndex > -1)
            result += " (riga " + rowIndex + ", colonna " + colIndex + ") ";

        result += ": " + type + " " + message;

        return result;
    }
}
