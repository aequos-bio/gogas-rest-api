import { IconButton, TableCell, TableRow } from "@material-ui/core";
import { EditSharp as EditIcon } from '@material-ui/icons';
import { AccountingCode } from "./types";

interface Props {
  accountingCode: AccountingCode;
  onEdit: () => void;
}
const AccountingCodeRow: React.FC<Props> = ({ accountingCode, onEdit }) => {

  return (
    <TableRow>
      <TableCell>{accountingCode.description}</TableCell>
      <TableCell>{accountingCode.accountingCode}</TableCell>
      <TableCell>
        <IconButton onClick={onEdit}>
          <EditIcon />
        </IconButton>
      </TableCell>
    </TableRow>

  )
}

export default AccountingCodeRow;