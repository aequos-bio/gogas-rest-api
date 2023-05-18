import { IconButton, TableCell, TableRow } from "@material-ui/core";
import { makeStyles } from '@material-ui/core/styles';
import {
  ArrowForwardIosSharp as EditIcon,
  BlockSharp as BlockIcon,
} from '@material-ui/icons';
import { UserAccountingTotal } from "./types";

const useStyles = makeStyles((theme) => ({
  fab: {
    position: 'fixed',
    bottom: theme.spacing(2),
    right: theme.spacing(2),
  },
  tdIcon: {
    color: 'red',
    textAlign: 'center',
    width: '30px',
  },
  tdAmount: {
    textAlign: 'right',
    width: '90px',
  },
  tdLink: {
    textAlign: 'center',
    width: '70px',
  },
  footercell: {
    '& td': {
      fontSize: '.875rem',
    },
  },
}));

interface Props {
  userTotal: UserAccountingTotal;
  onOpenDetail: (userId: string) => void;
}

const UserAccountingTotalRow: React.FC<Props> = ({ userTotal, onOpenDetail }) => {
  const classes = useStyles();

  return (
    <TableRow key={`user-${userTotal.user.id}`} hover>
      <TableCell className={classes.tdIcon}>
        {userTotal.user.enabled ? [] : <BlockIcon fontSize='small' />}
      </TableCell>
      <TableCell>{`${userTotal.user.firstName} ${userTotal.user.lastName}`}</TableCell>
      <TableCell
        className={classes.tdAmount}
        style={{ color: userTotal.total < 0 ? 'red' : 'inheried' }}
      >
        {userTotal.total.toFixed(2)}
      </TableCell>
      <TableCell className={classes.tdLink}>
        <IconButton
          onClick={() => onOpenDetail(userTotal.user.id)}
          size='small'
        >
          <EditIcon fontSize='small' />
        </IconButton>
      </TableCell>
    </TableRow>

  )
}

export default UserAccountingTotalRow;