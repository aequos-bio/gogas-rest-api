import moment from "moment-timezone";
import { UserMovementView } from "../useraccountingdetail/types";
import { BalanceRow } from "./types";
import { GasMovementView } from "../gasmovements/types";

export const MapUserMovement = (movement: UserMovementView): BalanceRow => {
  if (movement.segno === '+') {
    // versamenti
    return {
      data: moment(movement.data).format('YYYY-MM-DD'),
      descrizione: `${movement.nomecausale} (${movement.nomeutente}): ${movement.descrizione}`,
      importo: movement.importo,
      contoDare: '1000',
      contoAvere: 'C_XXX',
      type: 4,
    };
  }
  return {
    // addebiti
    data: moment(movement.data).format('YYYY-MM-DD'),
    descrizione: `${movement.nomecausale} (${movement.nomeutente}): ${movement.descrizione}`,
    importo: movement.importo,
    contoDare: 'C_XXX',
    contoAvere: '3000',
    type: 3,
  };
}

export const MapGasMovement = (movement: GasMovementView): BalanceRow | undefined => {
  if (movement.codicecausale) {
    // movimento manuale GAS
    return {
      data: movement.data,
      descrizione: movement.descrizione,
      importo: movement.importo,
      contoDare: movement.segnocausale === '-' ? movement.codicecontabile : '4000',
      contoAvere: movement.segnocausale === '-' ? '1000' : movement.codicecontabile,
      type: movement.type
    };
  }

  if (movement.type === 1) {
    // fattura
    return {
      data: movement.data,
      descrizione: movement.descrizione,
      importo: movement.importo,
      contoDare: '4000',
      contoAvere: movement.codicecontabile,
      type: movement.type
    };
  }
  if (movement.type === 2) {
    // pagamento fattura fornitore (type===2)
    return {
      data: movement.data,
      descrizione: `Pagamento ${movement.descrizione}`,
      importo: -1 * movement.importo,
      contoDare: movement.codicecontabile,
      contoAvere: '1000',
      type: movement.type
    };
  }
  if (movement.type === 3) {
    // addebito ordine ai gasisti
    return {
      data: movement.data,
      descrizione: movement.descrizione,
      importo: movement.importo,
      contoDare: movement.codicecontabile,
      contoAvere: '3000',
      type: movement.type
    };
  }
  return undefined;

}
