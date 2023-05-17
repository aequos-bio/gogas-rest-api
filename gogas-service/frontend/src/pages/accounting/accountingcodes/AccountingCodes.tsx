import React, { useEffect, useState, useCallback, useMemo } from 'react';
import {
  Container,
  TableContainer,
  Table,
  TableHead,
  TableBody,
  TableRow,
  TableCell,
  IconButton,
} from '@material-ui/core';
import { makeStyles } from '@material-ui/core/styles';
import { orderBy } from 'lodash';
import { apiGetJson } from '../../../utils/axios_utils';
import PageTitle from '../../../components/PageTitle';
import LoadingRow from '../../../components/LoadingRow';
import EditAccountingCodeDialog from './EditAccountingCodeDialog';
import AccountingCodeRow from './AccountingcodeRow';
import { useAccountingCodesAPI } from './useAccountingCodesAPI';
import { AccountingCode } from './types';

const useStyles = makeStyles((theme) => ({
  tdButtons: {
    textAlign: 'center',
    minWidth: '44px',
    width: '44px',
  },
  cellHeader: {
    paddingLeft: theme.spacing(1),
    paddingRight: theme.spacing(1),
    verticalAlign: 'bottom',
  },
}));

const AccountingCodes: React.FC = () => {
  const classes = useStyles();
  const [dialogOpen, setDialogOpen] = useState(false);
  const [selectedAccountingCode, setSelectedAccountingCode] = useState<AccountingCode | undefined>(undefined);
  const { accountingCodes, loading, reload } = useAccountingCodesAPI();

  useEffect(() => {
    reload();
  }, [reload]);

  const dialogClosed = useCallback(
    (needrefresh) => {
      setDialogOpen(false);
      if (needrefresh) {
        reload();
      }
    },
    [reload],
  );

  const editAccountingCode = useCallback((orderType) => {
    setSelectedAccountingCode(orderType);
    setDialogOpen(true);
  }, []);

  const rows = useMemo(() => {
    return loading ? (
      <LoadingRow colSpan={8} />
    ) : (
      accountingCodes.map((accountingCode) => (
        <AccountingCodeRow key={`accountingCode-${accountingCode.id}`} accountingCode={accountingCode} onEdit={() => editAccountingCode(accountingCode)} />
      ))
    );
  }, [loading, accountingCodes, editAccountingCode]);

  return (
    <Container maxWidth={false}>
      <PageTitle title='Gestione dei codici contabili' />

      <TableContainer>
        <Table size='small'>
          <TableHead>
            <TableRow>
              <TableCell>Tipo ordine</TableCell>
              <TableCell style={{ width: '10%' }}>Codice contabile</TableCell>
              <TableCell className={classes.tdButtons} />
            </TableRow>
          </TableHead>

          <TableBody>{rows}</TableBody>
        </Table>
      </TableContainer>
      <EditAccountingCodeDialog
        open={dialogOpen}
        onClose={dialogClosed}
        code={selectedAccountingCode?.accountingCode}
        orderTypeId={selectedAccountingCode?.id}
      />
    </Container>
  );
};

export default AccountingCodes;
