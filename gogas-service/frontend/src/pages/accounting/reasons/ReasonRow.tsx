import { IconButton, TableCell, TableRow } from "@material-ui/core";
import {
  EditSharp as EditIcon,
  DeleteSharp as DeleteIcon,
  AddSharp as PlusIcon,
  RemoveSharp as RemoveIcon,
} from '@material-ui/icons';
import { makeStyles } from '@material-ui/core/styles';
import { Reason } from "./types";

const useStyles = makeStyles((theme) => ({
  tdIcon: {
    color: 'red',
    textAlign: 'center',
    width: '30px',
  },
  tdButtons: {
    fontSize: '130%',
    textAlign: 'center',
  },
}));

interface Props {
  reason: Reason;
  onEdit: () => void;
  onDelete: () => void;
}

const ReasonRow: React.FC<Props> = ({ reason, onEdit, onDelete }) => {
  const classes = useStyles();

  return (
    <TableRow key={`reason-${reason.reasonCode}`} hover>
      <TableCell>{reason.reasonCode}</TableCell>
      <TableCell>{reason.description}</TableCell>
      <TableCell>
        {reason.sign === '+' ? (
          <PlusIcon fontSize='small' />
        ) : (
          <RemoveIcon fontSize='small' />
        )}
      </TableCell>
      <TableCell>{reason.accountingCode}</TableCell>
      <TableCell className={classes.tdButtons}>
        <IconButton
          onClick={onEdit}
        >
          <EditIcon fontSize='small' />
        </IconButton>
        <IconButton
          onClick={onDelete}
        >
          <DeleteIcon fontSize='small' />
        </IconButton>
      </TableCell>
    </TableRow>
  )
}

export default ReasonRow;