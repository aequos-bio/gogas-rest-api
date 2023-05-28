import { Column } from "../../../components/DataTable";

export const columns: Column[] = [
  {
    label: "Data",
    type: "Date",
    alignment: "Left",
    property: "data"
  },
  {
    label: "Descrizione",
    type: "String",
    alignment: "Left",
    property: "descrizione"
  },
  {
    label: "Importo",
    type: "Amount",
    alignment: "Left",
    property: "importo"
  },
  {
    label: "Conto Dare",
    type: "String",
    alignment: "Right",
    property: "contoDare"
  },
  {
    label: "Conto Avere",
    type: "String",
    alignment: "Right",
    property: "contoAvere"
  }
]