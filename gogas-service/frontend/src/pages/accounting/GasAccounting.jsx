import React, { useState, useCallback, useEffect, useMemo } from 'react';
import { connect } from 'react-redux';
import {
  Container,
  Fab,
  Button,
  IconButton,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableCell,
  TableBody,
} from '@material-ui/core';
import { makeStyles } from '@material-ui/core/styles';
import { withSnackbar } from 'notistack';
import _ from 'lodash';
import moment from 'moment-timezone';
import { apiGetJson } from '../../utils/axios_utils';
import PageTitle from '../../components/PageTitle';
import LoadingRow from '../../components/LoadingRow';

const useStyles = makeStyles(theme => ({
  cellAmount: {
    textAlign: 'right',
  },
  cellCode: {
    textAlign: 'right',
  },
}));

const GasAccounting = ({ enqueueSnackbar }) => {
  const classes = useStyles();
  const [year, setYear] = useState(moment().format('YYYY'));
  const [loading, setLoading] = useState(false);
  const [entries, setEntries] = useState([]);

  const reload = useCallback(() => {
    setLoading(true);
    apiGetJson(`/api/accounting/gas/report/${year}`, {}).then(tt => {
      setLoading(false);
      if (tt.error) {
        enqueueSnackbar(tt.errorMessage, { variant: 'error' });
      } else {
        setEntries(tt);
      }
    });
  }, [enqueueSnackbar, year]);

  useEffect(() => {
    reload();
  }, [reload]);

  const rows = useMemo(() => {
    if (loading) {
      return <LoadingRow colSpan={6} />;
    }
    return entries.map(e => {
      const dateStr = moment(e.data).format('DD/MM/YYYY');
      if (e.codicecausale) {
        // movimento manuale
        return (
          <TableRow>
            <TableCell>{dateStr}</TableCell>
            <TableCell>{e.descrizione}</TableCell>
            <TableCell className={classes.cellAmount}>
              {e.importo.toFixed(2)}
            </TableCell>
            <TableCell className={classes.cellCode}>
              {e.segnocausale === '-' ? e.codicecontabile : '4000'}
            </TableCell>
            <TableCell className={classes.cellCode}>
              {e.segnocausale === '-' ? '1000' : e.codicecontabile}
            </TableCell>
            <TableCell />
          </TableRow>
        );
      }
      if (e.importo < 0) {
        // pagamento fattura
        return (
          <TableRow>
            <TableCell>{dateStr}</TableCell>
            <TableCell>Pagamento {e.descrizione}</TableCell>
            <TableCell className={classes.cellAmount}>
              {(-1 * e.importo).toFixed(2)}
            </TableCell>
            <TableCell className={classes.cellCode}>
              {e.codicecontabile}
            </TableCell>
            <TableCell className={classes.cellCode}>1000</TableCell>
            <TableCell />
          </TableRow>
        );
      }
      // fattura
      return (
        <TableRow>
          <TableCell>{dateStr}</TableCell>
          <TableCell>{e.descrizione}</TableCell>
          <TableCell className={classes.cellAmount}>
            {e.importo.toFixed(2)}
          </TableCell>
          <TableCell className={classes.cellCode}>4000</TableCell>
          <TableCell className={classes.cellCode}>
            {e.codicecontabile}
          </TableCell>
          <TableCell />
        </TableRow>
      );
    });
  }, [loading, entries]);

  return (
    <Container maxWidth={false}>
      <PageTitle title="Situazione contabile del GAS">
        {/* <Button onClick={() => setExportDlgOpen(true)} startIcon={<SaveIcon />}>
          Esporta XLS
        </Button> */}
      </PageTitle>

      {/* <Fab
        className={classes.fab}
        color="secondary"
        onClick={() => setShowDlg(true)}
      >
        <PlusIcon />
      </Fab> */}

      <TableContainer>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Data</TableCell>
              <TableCell>Descrizione</TableCell>
              <TableCell className={classes.cellAmount}>Importo</TableCell>
              <TableCell className={classes.cellCode}>Conto Dare</TableCell>
              <TableCell className={classes.cellCode}>Conto Avere</TableCell>
              <TableCell />
            </TableRow>
          </TableHead>

          <TableBody>{rows}</TableBody>
        </Table>
      </TableContainer>
    </Container>
  );
};

const mapStateToProps = state => {
  return {
    authentication: state.authentication,
  };
};

const mapDispatchToProps = {};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(withSnackbar(GasAccounting));
