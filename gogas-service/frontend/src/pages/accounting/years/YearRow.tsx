import { Button, TableCell, TableRow } from "@material-ui/core";
import { Year } from "./types";
import { useAppDispatch } from "../../../store/store";
import moment from "moment-timezone";

interface Props {
  year: Year;
  isCurrent?: boolean;
  onClose: (year: Year) => void;
  onSetCurrent: (year: Year) => void;
}

const YearRow: React.FC<Props> = ({ year, isCurrent = false, onClose, onSetCurrent }) => {
  const dispatch = useAppDispatch();
  const currentYear = Number.parseInt(moment().format('YYYY'), 10);

  return (
    <TableRow hover>
      <TableCell>{year.year}</TableCell>
      <TableCell style={{ color: year.closed ? 'red' : 'black' }}>
        {year.closed ? 'Chiuso' : `Aperto`}
        {isCurrent ? ', corrente' : ''}
      </TableCell>
      <TableCell align='right'>
        {year.closed || year.year === currentYear ? null : (
          <Button
            variant='outlined'
            size='small'
            onClick={() => onClose(year)}
          >
            Chiudi
          </Button>
        )}
        {isCurrent ? null : (
          <Button
            variant='outlined'
            size='small'
            onClick={() => {
              dispatch(() => onSetCurrent(year));
            }}
            style={{ marginLeft: '5px' }}
          >
            Seleziona
          </Button>
        )}
      </TableCell>
    </TableRow>

  );
}

export default YearRow;