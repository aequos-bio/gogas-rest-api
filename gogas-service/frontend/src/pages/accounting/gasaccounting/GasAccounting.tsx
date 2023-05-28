import React, { useState, useCallback, useEffect, useMemo } from 'react';
import { useSelector } from 'react-redux';
import { Container, Button } from '@material-ui/core';
import { SaveAltSharp as SaveIcon } from '@material-ui/icons';
import { withSnackbar } from 'notistack';
import { orderBy } from 'lodash';
import moment from 'moment-timezone';
import Excel from 'exceljs';
import { apiGetJson } from '../../../utils/axios_utils';
import PageTitle from '../../../components/PageTitle';
import DataTable from '../../../components/DataTable';
import { columns } from './columns';
import { useGasAccountingAPI } from './useGasAccountingAPI';
import { useAppSelector } from '../../../store/store';
import { MapGasMovement, MapUserMovement } from './GasAccountingMapper';
import { GasMovement } from '../gasmovements/types';

const GasAccounting: React.FC = () => {
  const accounting = useAppSelector((state) => state.accounting);
  const { userMovements, gasMovements, loading, reload } = useGasAccountingAPI(accounting.currentYear);

  useEffect(() => {
    reload();
  }, [reload]);

  const rows = useMemo(() => {
    // movimenti manuali registrati sui gasisti
    const userRows = userMovements.map(MapUserMovement);
    const gasRows: GasMovement[] = gasMovements.map(MapGasMovement).filter((row) => !!row) as GasMovement[];

    const balanceRows = gasRows.concat(userRows);
    const rr = orderBy(balanceRows, ['data'], ['asc']);
    return rr.map((v) => ({ value: v }));
  }, [gasMovements, userMovements]);

  const exportXls = useCallback(() => {
    debugger
    const wb = new Excel.Workbook();
    wb.calcProperties.fullCalcOnLoad = true;
    const sh = wb.addWorksheet('Contabilità GAS');

    const headerRow = sh.getRow(1);
    (headerRow as any).style = { font: { size: 12, bold: true } };
    for (let c = 0; c < columns.length; c++) {
      headerRow.getCell(c + 1).value = columns[c].label;
    }

    let rowNum = 2;
    rows.forEach((row) => {
      const valueRow = sh.getRow(rowNum);
      for (let c = 0; c < columns.length; c++) {
        const property = columns[c].property;
        valueRow.getCell(c + 1).value = row.value[property as keyof GasMovement];
      }

      rowNum += 1;
    });

    wb.xlsx
      .writeBuffer() // { base64: true }
      .then((buffer) => {
        const base64 = buffer.toString();

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
        <Button onClick={exportXls} variant='outlined' startIcon={<SaveIcon />}>
          Esporta XLS
        </Button>
      </PageTitle>

      <DataTable
        settings={{
          canEdit: false,
          canDelete: false,
          canAdd: false,
          pagination: false,
          showHeader: true,
          showFooter: false,
        }}
        columns={columns}
        rows={rows}
        loading={loading}
      />
    </Container>
  );
};

export default GasAccounting;
