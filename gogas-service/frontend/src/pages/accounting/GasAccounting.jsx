/* eslint-disable no-constant-condition */
/* eslint-disable react/no-array-index-key */
import React, { useState, useCallback, useEffect, useMemo } from 'react';
import { connect } from 'react-redux';
import { Container, Button } from '@material-ui/core';
import { SaveAltSharp as SaveIcon } from '@material-ui/icons';
import { withSnackbar } from 'notistack';
import _ from 'lodash';
import moment from 'moment-timezone';
import Excel from 'exceljs';
import { apiGetJson } from '../../utils/axios_utils';
import PageTitle from '../../components/PageTitle';
import DataTable from '../../components/DataTable';

const GasAccounting = ({ enqueueSnackbar, accounting }) => {
  const [loading, setLoading] = useState(false);
  const [userEntries, setUserEntries] = useState([]);
  const [gasEntries, setGasEntries] = useState([]);

  const columns = [
    { label: 'Data', type: 'Date', alignment: 'Left', property: 'data' },
    {
      label: 'Descrizione',
      type: 'String',
      alignment: 'Left',
      property: 'descrizione',
    },
    { label: 'Importo', type: 'Amount', property: 'importo' },
    {
      label: 'Conto Dare',
      type: 'String',
      alignment: 'Right',
      property: 'dare',
    },
    {
      label: 'Conto Avere',
      type: 'String',
      alignment: 'Right',
      property: 'avere',
    },
    { label: 'TYPE', type: 'Number', property: 'type' },
  ];

  const checkDateFormat = data => {
    return data.includes('/')
      ? moment(data, 'DD/MM/YYYY').format('YYYY-MM-DD')
      : data;
  };

  const reload = useCallback(() => {
    setLoading(true);

    Promise.all([
      apiGetJson(`/api/accounting/user/entry/list`, {
        dateFrom: `01/01/${accounting.currentYear}`,
        dateTo: `31/12/${accounting.currentYear}`,
      }),
      apiGetJson(`/api/accounting/gas/report/${accounting.currentYear}`, {}),
    ]).then(([user, gas]) => {
      setLoading(false);
      if (user.error || gas.error) {
        enqueueSnackbar(user.errorMessage || gas.errorMessage, {
          variant: 'error',
        });
      } else {
        setUserEntries(
          user.map(u => ({
            ...u,
            data: checkDateFormat(u.data),
          }))
        );
        setGasEntries(
          gas.map(g => ({
            ...g,
            data: checkDateFormat(g.data),
          }))
        );
      }
    });
  }, [enqueueSnackbar, accounting]);

  useEffect(() => {
    reload();
  }, [reload]);

  const rows = useMemo(() => {
    const userRows = userEntries.map(e => {
      // movimenti manuali registrati sui gasisti
      if (e.segno === '+') {
        // versamenti
        return {
          ...e,
          descrizione: `${e.nomecausale} (${e.nomeutente}): ${e.descrizione}`,
          data: moment(e.data).format('YYYY-MM-DD'),
          dare: '1000',
          avere: 'C_XXX',
          type: 4,
        };
      }
      return {
        // addebiti
        ...e,
        descrizione: `${e.nomecausale} (${e.nomeutente}): ${e.descrizione}`,
        data: moment(e.data).format('YYYY-MM-DD'),
        dare: 'C_XXX',
        avere: '3000',
        type: 3,
      };
    });

    const gasRows = gasEntries.map(e => {
      if (e.codicecausale) {
        // movimento manuale GAS
        return {
          ...e,
          dare: e.segnocausale === '-' ? e.codicecontabile : '4000',
          avere: e.segnocausale === '-' ? '1000' : e.codicecontabile,
        };
      }

      if (e.type === 1) {
        // fattura
        return {
          ...e,
          dare: '4000',
          avere: e.codicecontabile,
        };
      }
      if (e.type === 2) {
        // pagamento fattura fornitore (type===2)
        return {
          ...e,
          descrizione: `Pagamento ${e.descrizione}`,
          importo: -1 * e.importo,
          dare: e.codicecontabile,
          avere: '1000',
        };
      }
      if (e.type === 3) {
        // addebito ordine ai gasisti
        return {
          ...e,
          dare: e.codicecontabile,
          avere: '3000',
        };
      }
      return null;
    });

    const rr = _.orderBy(gasRows.concat(userRows), ['data'], ['asc']);
    return rr.map(v => ({ value: v }));
  }, [gasEntries, userEntries]);

  const exportXls = useCallback(() => {
    const wb = new Excel.Workbook();
    wb.calcProperties.fullCalcOnLoad = true;
    const sh = wb.addWorksheet('Contabilità GAS');

    const headerRow = sh.getRow(1);
    headerRow.style = { font: { size: 12, bold: true } };
    for (let c = 0; c < columns.length; c++) {
      headerRow.getCell(c + 1).value = columns[c].label;
    }

    let rowNum = 2;
    rows.forEach(row => {
      const valueRow = sh.getRow(rowNum);
      for (let c = 0; c < columns.length; c++) {
        valueRow.getCell(c + 1).value = row.value[columns[c].property];
      }

      rowNum += 1;
    });

    wb.xlsx
      .writeBuffer({
        base64: true,
      })
      .then(buffer => {
        const base64 = buffer.toString('base64');

        const a = document.createElement('a');
        a.href = `data:application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;base64,${base64}`;
        a.target = '_blank';
        a.download = 'contabilita.xlsx';

        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
      });
  }, [rows, columns]);

  return (
    <Container maxWidth={false}>
      <PageTitle
        title={`Situazione contabile del GAS - ${accounting.currentYear}`}
      >
        <Button onClick={exportXls} variant="outlined" startIcon={<SaveIcon />}>
          Esporta XLS
        </Button>
      </PageTitle>

      <DataTable
        settings={{
          canEdit: false,
          canDelete: false,
          canAdd: false,
          pagination: false,
        }}
        columns={columns}
        rows={rows}
        loading={loading}
      />
    </Container>
  );
};

const mapStateToProps = state => {
  return {
    accounting: state.accounting,
  };
};

const mapDispatchToProps = {};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(withSnackbar(GasAccounting));
