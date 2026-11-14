import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Container, Grid, Button, Link } from '@material-ui/core';
import { Search as SearchIcon } from '@material-ui/icons';
import { makeStyles } from '@material-ui/core/styles';
import { useHistory } from 'react-router-dom';
import moment from 'moment-timezone';
import PageTitle from '../../../components/PageTitle';
import SortableTable, { Column } from '../../../components/table/SortableTable';
import SelectField from '../../../components/form/SelectField';
import MultiSelectField from '../../../components/form/MultiSelectField';
import DatePickerField from '../../../components/form/DatePickerField';
import useJwt from '../../../hooks/JwtHooks';
import { useUserOrdersAPI } from './useUserOrdersAPI';
import { OrderSearchFilter, UserOrder } from './types';

const useStyles = makeStyles((theme) => ({
  filter: {
    marginBottom: theme.spacing(2),
  },
  searchRow: {
    display: 'flex',
    justifyContent: 'flex-end',
  },
  commands: {
    display: 'flex',
    gap: theme.spacing(2),
  },
}));

/** Default: orders whose due date is within the last ~2 months (as in legacy). */
const defaultDueDateFrom = () => moment().subtract(2, 'months').format('YYYY-MM-DD');

const UserOrdersListPage: React.FC = () => {
  const classes = useStyles();
  const history = useHistory();
  const jwt = useJwt();
  const { orders, orderTypes, statuses, hasFriends, loading, loadFilters, search } = useUserOrdersAPI();

  const [orderType, setOrderType] = useState<string | undefined>(undefined);
  const [dueDateFrom, setDueDateFrom] = useState<string | undefined>(defaultDueDateFrom());
  const [dueDateTo, setDueDateTo] = useState<string | undefined>(undefined);
  const [deliveryDateFrom, setDeliveryDateFrom] = useState<string | undefined>(undefined);
  const [deliveryDateTo, setDeliveryDateTo] = useState<string | undefined>(undefined);
  // Legacy default selection: Aperto (0) + Chiuso (1)
  const [selectedStatuses, setSelectedStatuses] = useState<string[]>(['0', '1']);

  const buildFilter = useCallback((): OrderSearchFilter => {
    const toItDate = (iso?: string) => (iso ? moment(iso, 'YYYY-MM-DD').format('DD/MM/YYYY') : undefined);
    return {
      orderType,
      dueDateFrom: toItDate(dueDateFrom),
      dueDateTo: toItDate(dueDateTo),
      deliveryDateFrom: toItDate(deliveryDateFrom),
      deliveryDateTo: toItDate(deliveryDateTo),
      status: selectedStatuses.map((s) => parseInt(s, 10)),
    };
  }, [orderType, dueDateFrom, dueDateTo, deliveryDateFrom, deliveryDateTo, selectedStatuses]);

  const doSearch = useCallback(() => {
    search(buildFilter());
  }, [search, buildFilter]);

  // Load filter options once, and run the initial search.
  useEffect(() => {
    loadFilters();
  }, [loadFilters]);

  useEffect(() => {
    doSearch();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const statusOptions = useMemo(
    () => statuses.map((s) => ({ value: s.id, label: s.description })),
    [statuses],
  );
  const orderTypeOptions = useMemo(
    () => orderTypes.map((t) => ({ value: t.id, label: t.description })),
    [orderTypes],
  );

  const goToDetails = useCallback(
    (order: UserOrder) => {
      history.push(`/legacy/ordersdetails?orderId=${order.id}&userId=${jwt?.id ?? ''}`);
    },
    [history, jwt],
  );

  const goToFriendSplit = useCallback(
    (order: UserOrder) => {
      history.push(`/legacy/friendorders?orderId=${order.id}`);
    },
    [history],
  );

  const columns: Column[] = useMemo(() => {
    const cols: Column[] = [
      { label: 'Tipo Ordine', type: 'String', alignment: 'Left', property: 'tipoordine' },
      { label: 'Data Apertura', type: 'Date', alignment: 'Center', property: 'dataapertura' },
      { label: 'Data Chiusura', type: 'Date', alignment: 'Center', property: 'datachiusura' },
      { label: 'Ora Chiusura', type: 'Number', alignment: 'Center', property: 'orachiusura' },
      { label: 'Data Consegna', type: 'Date', alignment: 'Center', property: 'dataconsegna' },
      { label: 'Stato', type: 'String', alignment: 'Center', property: 'stato' },
      { label: 'Num. Articoli', type: 'Number', alignment: 'Right', property: 'numarticoli' },
      { label: 'Importo', type: 'Amount', alignment: 'Right', property: 'totaleordine' },
    ];

    if (hasFriends) {
      cols.push({
        label: 'Contab.to amici',
        type: 'Boolean',
        alignment: 'Center',
        property: 'contabilizzato',
      });
    }

    cols.push({
      label: '',
      type: 'String',
      alignment: 'Left',
      property: 'actions',
      sortable: false,
      render: (order: UserOrder) => (
        <span className={classes.commands}>
          <Link
            component="button"
            variant="body2"
            onClick={() => goToDetails(order)}
            title="Visualizza il dettaglio dell'ordine"
          >
            dettaglio
          </Link>
          {order.amici && order.contabilizzabile ? (
            <Link
              component="button"
              variant="body2"
              onClick={() => goToFriendSplit(order)}
              title="Gestisci la ripartizione con gli amici delle quantità ritirate"
            >
              Gestisci ripartizione amici
            </Link>
          ) : null}
        </span>
      ),
    });

    return cols;
  }, [hasFriends, classes.commands, goToDetails, goToFriendSplit]);

  const rows = useMemo(() => orders.map((value) => ({ value })), [orders]);

  return (
    <Container maxWidth={false}>
      <PageTitle title="Storico ordini" />

      <Grid container spacing={2} className={classes.filter} alignItems="flex-end">
        <Grid item xs={12} sm={6} md={3}>
          <SelectField
            label="Tipologia ordine"
            options={orderTypeOptions}
            value={orderType}
            onChange={setOrderType}
            placeholder="Tutte le tipologie..."
            isClearable
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MultiSelectField
            label="Stato"
            options={statusOptions}
            value={selectedStatuses}
            onChange={setSelectedStatuses}
          />
        </Grid>
        <Grid item xs={6} sm={3} md={2}>
          <DatePickerField label="Chiusura dal" value={dueDateFrom} onChange={setDueDateFrom} />
        </Grid>
        <Grid item xs={6} sm={3} md={2}>
          <DatePickerField label="Chiusura al" value={dueDateTo} onChange={setDueDateTo} />
        </Grid>
        <Grid item xs={6} sm={3} md={2}>
          <DatePickerField label="Consegna dal" value={deliveryDateFrom} onChange={setDeliveryDateFrom} />
        </Grid>
        <Grid item xs={6} sm={3} md={2}>
          <DatePickerField label="Consegna al" value={deliveryDateTo} onChange={setDeliveryDateTo} />
        </Grid>
        <Grid item xs={12} className={classes.searchRow}>
          <Button
            variant="contained"
            color="primary"
            startIcon={<SearchIcon />}
            onClick={doSearch}
          >
            Ricerca
          </Button>
        </Grid>
      </Grid>

      <SortableTable
        columns={columns}
        rows={rows}
        loading={loading}
        settings={{
          canAdd: false,
          canEdit: false,
          canDelete: false,
          showHeader: true,
          showFooter: false,
          pagination: true,
          rowsPerPage: 25,
        }}
      />
    </Container>
  );
};

export default UserOrdersListPage;
