/* eslint-disable no-use-before-define */
/* eslint-disable no-alert */
/* eslint-disable no-nested-ternary */
/* eslint-disable jsx-a11y/control-has-associated-label */
import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { connect } from 'react-redux';
import {
  Container,
  Button,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableCell,
  TableBody,
} from '@material-ui/core';
import { withSnackbar } from 'notistack';
import _ from 'lodash';
import moment from 'moment-timezone';
import { apiGetJson, apiPut, apiPost } from '../../utils/axios_utils';
import PageTitle from '../../components/PageTitle';
import LoadingRow from '../../components/LoadingRow';
import ActionDialog from '../../components/ActionDialog';
import { setAccountingYear } from '../../store/actions';

const Years = ({ enqueueSnackbar, accounting, setAccountingYear }) => {
  const [years, setYears] = useState([]);
  const [loading, setLoading] = useState(false);
  const [confirmDlgOpen, setConfigDlgOpen] = useState(false);
  const [selectedYear, setSelectedYear] = useState();

  const reload = useCallback(() => {
    const checkCurrentYear = yy => {
      const currentYear = Number.parseInt(moment().format('YYYY'), 10);
      const existing = yy.filter(y => y.year === currentYear);
      if (!existing.length) {
        // eslint-disable-next-line no-restricted-globals
        const result = confirm(
          `Vuoi aprire un nuovo anno contabile per il ${currentYear}?`
        );
        if (result) {
          apiPost(`/api/year/${currentYear}`).then(y => {
            if (y.error) {
              enqueueSnackbar(y.errorMessage, { variant: 'error' });
            } else {
              alert(`Creato nuovo anno contabile ${y.data.data.year}`);
              // eslint-disable-next-line no-use-before-define
              reload();
            }
          });
        }
      }
    };

    setLoading(true);
    apiGetJson('/api/year/all', {}).then(yy => {
      setLoading(false);
      if (yy.error) {
        enqueueSnackbar(yy.errorMessage, { variant: 'error' });
      } else {
        setYears(_.orderBy(yy.data, 'year', 'desc'));
        checkCurrentYear(yy.data);
      }
    });
  }, [enqueueSnackbar]);

  useEffect(() => {
    reload();
  }, [reload]);

  const closeYear = useCallback(y => {
    setSelectedYear(y);
    setConfigDlgOpen(true);
    console.warn('closing year', y);
  }, []);

  const doCloseYear = useCallback(() => {
    apiPut(`/api/year/close/${selectedYear.year}`)
      .then(() => {
        setConfigDlgOpen(false);
        setSelectedYear();
        reload();
        enqueueSnackbar('Anno chiuso', { variant: 'success' });
      })
      .catch(err => {
        enqueueSnackbar(
          err.response?.statusText ||
            "Errore nell'eliminazione della categoria",
          { variant: 'error' }
        );
      });
  }, [enqueueSnackbar, reload, selectedYear]);

  const rows = useMemo(() => {
    return loading ? (
      <LoadingRow colSpan={3} />
    ) : (
      years.map((y, i) => (
        <TableRow key={`year-${y.year}`} hover>
          <TableCell>{y.year}</TableCell>
          <TableCell style={{ color: y.closed ? 'red' : 'black' }}>
            {y.closed ? 'Chiuso' : `Aperto${i === 0 ? ', in corso' : ''}`}
            {y.year === accounting.currentYear ? ', corrente' : ''}
          </TableCell>
          <TableCell align="right">
            {y.closed || i === 0 ? null : (
              <Button
                variant="outlined"
                size="small"
                onClick={() => closeYear(y)}
              >
                Chiudi
              </Button>
            )}
            {y.year === accounting.currentYear ? null : (
              <Button
                variant="outlined"
                size="small"
                onClick={() => {
                  setAccountingYear(y.year);
                }}
                style={{ marginLeft: '5px' }}
              >
                Seleziona
              </Button>
            )}
          </TableCell>
        </TableRow>
      ))
    );
  }, [years, closeYear, loading, accounting, setAccountingYear]);

  return (
    <Container maxWidth={false}>
      <PageTitle title="Anni contabili" />

      <TableContainer>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Anno</TableCell>
              <TableCell>Stato</TableCell>
              <TableCell style={{ width: '30%' }} />
            </TableRow>
          </TableHead>

          <TableBody>{rows}</TableBody>
        </Table>
      </TableContainer>

      <ActionDialog
        open={confirmDlgOpen}
        title="Conferma chiusura"
        message={`Sei sicuro di voler chiudere l'anno ${selectedYear?.year}?`}
        onCancel={() => setConfigDlgOpen(false)}
        onAction={doCloseYear}
        actions={['Ok']}
      />
    </Container>
  );
};

const mapStateToProps = state => {
  return {
    accounting: state.accounting,
  };
};

const mapDispatchToProps = {
  setAccountingYear,
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(withSnackbar(Years));
