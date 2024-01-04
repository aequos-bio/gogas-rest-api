import { CircularProgress, Container } from "@material-ui/core"
import { useParams } from "react-router";
import { useOrderDetailManagementAPI } from "./useOrderDetailManagementAPI";
import { useEffect } from "react";
import PageTitle from "../../../components/PageTitle";
import DataTable, { Column } from "../../../components/DataTable";

const columns: Column[] = [
  { label: 'Prodotto', type: 'String', alignment: 'Left', property: 'nomeProdotto' },
  { label: 'Prezzo unitario', type: 'Amount', alignment: 'Right', property: 'prezzoKg' },
  { label: 'Peso collo', type: 'Number', alignment: 'Right', property: 'pesoCollo' },
  { label: 'Colli da ordinare', type: 'Number', alignment: 'Right', property: 'colliOrdinati' },
  { label: 'Colli risultanti', type: 'Number', alignment: 'Right', property: 'colliRisultanti' },
  { label: 'Q.tà ordinata', type: 'Number', alignment: 'Right', property: 'qtaOrdinata' },
  { label: 'Q.tà rimanente', type: 'Number', alignment: 'Right', property: 'rimanenze' },
  { label: 'Ordinanti', type: 'Number', alignment: 'Right', property: 'numeroOrdinanti' },
  { label: 'Importo totale', type: 'Amount', alignment: 'Right', property: 'totale' },
];

const OrderDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const { loading, order, products, reload } = useOrderDetailManagementAPI(id);

  useEffect(() => {
    reload();
  }, [id]);

  if (loading) return (
    <CircularProgress />
  )

  return (
    <Container maxWidth={false}>
      <PageTitle title={`Ordine ${order?.tipoordine} in consegna il ${order?.dataconsegna}`} />

      <DataTable
        settings={{
          showEdit: false,
          showDelete: false,
          showAdd: false,
          showEnter: false,
          showMenu: false,
          pagination: false,
          showHeader: true,
          showFooter: false,
        }}
        columns={columns}
        rows={products.map(product => ({ value: product }))}
        loading={loading}
      />
    </Container>
  )
}

export default OrderDetailPage;