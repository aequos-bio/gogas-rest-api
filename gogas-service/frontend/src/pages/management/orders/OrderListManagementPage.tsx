import { Container } from "@material-ui/core"
import PageTitle from "../../../components/PageTitle"
import DataTable, { Column } from "../../../components/DataTable";
import { useOrderListManagementAPI } from "./useOrderListManagementAPI";
import { useCallback, useEffect } from "react";
import { ManagedOrder } from "./types";
import { useHistory } from "react-router";

const columns: Column[] = [
  { label: 'Tipo ordine', type: 'String', alignment: 'Left', property: 'tipoordine' },
  { label: 'Apertura', type: 'String', alignment: 'Center', property: 'dataapertura', hidden: 'smDown' },
  { label: 'Chiusura', type: 'String', alignment: 'Center', property: (order: ManagedOrder) => (`${order.datachiusura} ${order.orachiusura}:00`) },
  { label: 'Consegna', type: 'String', alignment: 'Center', property: 'dataconsegna' },
  { label: 'Stato', type: 'String', alignment: 'Center', property: 'stato' },
  { label: 'Totale ordine', type: 'Amount', alignment: 'Right', property: 'totaleordine' },
  { label: 'Inviato', type: 'Boolean', alignment: 'Right', property: 'inviato' },
];

const OrderManagementPage: React.FC = () => {
  const { loading, orders, reload } = useOrderListManagementAPI();
  const history = useHistory();

  useEffect(() => {
    refresh();
  }, [])

  const refresh = useCallback(() => {
    reload();
  }, [reload])

  return (
    <Container maxWidth={false}>
      <PageTitle title={`Gestione ordini`} />

      <DataTable
        settings={{
          showEdit: false,
          showDelete: false,
          showAdd: false,
          showEnter: true,
          showMenu: true,
          pagination: false,
          showHeader: true,
          showFooter: false,
        }}
        columns={columns}
        rows={orders.map(order => ({ value: order }))}
        loading={loading}
        // onAdd={() => { }}
        // onEdit={() => { }}
        // onDelete={() => { }}
        onEnter={(order: ManagedOrder) => { history.push(`/orders/${order.id}`) }}
        onMenu={() => { }}
      />

    </Container>
  );
}

export default OrderManagementPage;