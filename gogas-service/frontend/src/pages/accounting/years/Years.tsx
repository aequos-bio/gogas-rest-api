import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { useSelector, useDispatch } from 'react-redux';
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
import PageTitle from '../../../components/PageTitle';
import LoadingRow from '../../../components/LoadingRow';
import ActionDialog from '../../../components/ActionDialog';
import { setAccountingYear } from '../../../store/features/accounting.slice';
import { RootState, useAppDispatch } from '../../../store/store';
import { useYearsAPI } from './useYearsAPI';
import { Year } from './types';
import YearRow from './YearRow';

const Years: React.FC = () => {
  const [confirmDlgOpen, setConfigDlgOpen] = useState(false);
  const [selectedYear, setSelectedYear] = useState<Year | undefined>(undefined);
  const accounting = useSelector((state: RootState) => state.accounting);
  const dispatch = useAppDispatch();
  const { years, loading, reload, closeYear } = useYearsAPI();

  useEffect(() => {
    reload();
  }, []);

  const _closeYear = useCallback((y: Year) => {
    setSelectedYear(y);
    setConfigDlgOpen(true);
  }, []);

  const doCloseYear = useCallback(() => {
    if (!selectedYear) return;
    closeYear(selectedYear.year).then(() => {
      setConfigDlgOpen(false);
      setSelectedYear(undefined);
    });
  }, [selectedYear]);

  const rows = useMemo(() => {
    return;
  }, [years, loading]);

  return (
    <Container maxWidth={false}>
      <PageTitle title='Anni contabili' />

      <TableContainer>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Anno</TableCell>
              <TableCell>Stato</TableCell>
              <TableCell style={{ width: '30%' }} />
            </TableRow>
          </TableHead>

          <TableBody>
            {loading ? (
              <LoadingRow colSpan={3} />
            ) : (
              years.map((y, i) => (
                <YearRow
                  key={`year-${y.year}`}
                  year={y}
                  isCurrent={y.year === accounting.currentYear}
                  onClose={_closeYear}
                  onSetCurrent={(year) => dispatch(setAccountingYear(year.year))}
                />
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>

      <ActionDialog
        open={confirmDlgOpen}
        title='Conferma chiusura'
        message={`Sei sicuro di voler chiudere l'anno ${selectedYear?.year}?`}
        onCancel={() => setConfigDlgOpen(false)}
        onAction={doCloseYear}
        actions={['Ok']}
      />
    </Container>
  );
};

export default Years;
