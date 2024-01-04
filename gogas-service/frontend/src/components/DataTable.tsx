import React, { useMemo, useCallback, useState } from 'react';
import {
  Fab,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableCell,
  TableBody,
  TablePagination,
  IconButton,
  Checkbox,
  Hidden,
} from '@material-ui/core';
import {
  AddSharp,
  EditSharp,
  DeleteSharp,
  ArrowForwardIosSharp,
  MoreVertSharp,
} from '@material-ui/icons';
import { makeStyles } from '@material-ui/core/styles';
import moment from 'moment-timezone';
import LoadingRow from './LoadingRow';

export interface Column {
  label: string;
  type: 'String' | 'Date' | 'DateTime' | 'Time' | 'Number' | 'Amount' | 'Icon' | 'Boolean';
  alignment: 'Left' | 'Center' | 'Right';
  property: string | ((value: any) => any);
  hidden?: 'xsUp' | 'smUp' | 'mdUp' | 'lgUp' | 'xlUp' | 'xsDown' | 'smDown' | 'mdDown' | 'lgDown' | 'xlDown'
}

export interface Settings {
  showEdit: boolean;
  showDelete: boolean;
  showEnter: boolean;
  showMenu: boolean;
  showAdd: boolean;
  showHeader: boolean;
  showFooter: boolean;
  pagination: boolean;
}

const default_settings: Settings = {
  showEdit: false,
  showDelete: false,
  showEnter: false,
  showMenu: false,
  showAdd: false,
  showHeader: true,
  showFooter: false,
  pagination: false,
};

const useStyles = makeStyles(theme => ({
  cellBold: {
    fontWeight: 'bold'
  },
  cellLeft: {
    textAlign: 'left',
  },
  cellCenter: {
    textAlign: 'center',
  },
  cellRight: {
    textAlign: 'right',
  },
  cellAmount: {
    textAlign: 'right',
  },
  cellNumber: {
    textAlign: 'right',
  },
  cellCommands: {
    textAlign: 'right',
    minWidth: '90px',
    width: '90px',
  },
  fab: {
    position: 'fixed',
    bottom: theme.spacing(2),
    right: theme.spacing(2),
  },
}));

interface Props {
  columns: Column[],
  rows: { value: any }[],
  settings: Settings;
  loading: boolean;
  onAdd?: () => void;
  onEdit?: (value: any) => void;
  onEnter?: (value: any) => void;
  onMenu?: (value: any) => void;
  onDelete?: (value: any) => void;
}

const DataTable: React.FC<Props> = ({
  columns,
  rows,
  settings = default_settings,
  loading = false,
  onAdd,
  onEdit,
  onEnter,
  onMenu,
  onDelete,
}) => {
  const classes = useStyles();
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(
    settings.pagination ? 10 : 999999
  );

  const handleChangePage = (event: any, newPage: number) => {
    setPage(newPage);
  };

  const handleChangeRowsPerPage = (event: any) => {
    setRowsPerPage(parseInt(event.target.value as string, 10));
    setPage(0);
  };

  const cellClasses = useCallback(
    (alignment, type) => {
      let cls = '';
      if (alignment === 'Left') {
        cls += `${classes.cellLeft} `;
      } else if (alignment === 'Right') {
        cls += `${classes.cellRight} `;
      } else if (alignment === 'Center') {
        cls += `${classes.cellCenter} `;
      } else if (type === 'Amount' || type === 'Number') {
        cls += `${classes.cellRight} `;
      } else if (
        type === 'Date' ||
        type === 'DateTime' ||
        type === 'Time' ||
        type === 'Icon'
      ) {
        cls += `${classes.cellCenter} `;
      }
      return cls;
    },
    [classes]
  );

  const headers = useMemo(() => {
    const hh = columns.map((col, i) => {
      return (
        <Hidden
          xsDown={col.hidden === 'xsDown'}
          xsUp={col.hidden === 'xsUp'}
          smDown={col.hidden === 'smDown'}
          smUp={col.hidden === 'smUp'}
          mdDown={col.hidden === 'mdDown'}
          mdUp={col.hidden === 'mdUp'}
          lgDown={col.hidden === 'lgDown'}
          lgUp={col.hidden === 'lgUp'}
          xlDown={col.hidden === 'xlDown'}
          xlUp={col.hidden === 'xlUp'}
        >
          <TableCell
            key={`header-${i}`}
            className={`${classes.cellBold} ${cellClasses(col.alignment, col.type)}`}
          >
            {col.label}
          </TableCell>
        </Hidden>
      );
    });
    if (settings.showEdit || settings.showDelete || settings.showEnter || settings.showMenu) {
      hh.push(<TableCell key='header-commands' className={classes.cellCommands} />);
    }
    return hh;
  }, [columns, cellClasses, settings, classes]);

  const values = useMemo(() => {
    if (loading)
      return (
        <LoadingRow
          colSpan={
            columns.length + (settings.showEdit || settings.showDelete || settings.showEnter || settings.showMenu ? 1 : 0)
          }
        />
      );

    const rr = [];
    for (
      let i = page * rowsPerPage;
      i < Math.min(rows.length, rowsPerPage * (page + 1));
      i += 1
    ) {
      const row = rows[i];
      rr.push(
        <TableRow key={`row-${i}`}>
          {columns.map((col, j) => {
            const value = (typeof col.property === 'string') ? row.value[col.property] : col.property(row.value);
            let val;
            if (col.type === 'Date') {
              let date = moment(value);
              if (!date.isValid()) date = moment(value, 'DD/MM/YYYY');
              val = date.format('DD/MM/YYYY');
            } else if (col.type === 'DateTime') {
              val = moment(value).format('DD/MM/YYYY HH:mm');
            } else if (col.type === 'Time') {
              val = moment(value).format('HH:mm');
            } else if (col.type === 'Amount') {
              val = value.toFixed(2);
            } else if (col.type === 'Boolean') {
              val = <Checkbox disableRipple checked={value === true} />
            } else {
              val = value;
            }

            const cls = cellClasses(col.alignment, col.type);

            return (
              <Hidden
                xsDown={col.hidden === 'xsDown'}
                xsUp={col.hidden === 'xsUp'}
                smDown={col.hidden === 'smDown'}
                smUp={col.hidden === 'smUp'}
                mdDown={col.hidden === 'mdDown'}
                mdUp={col.hidden === 'mdUp'}
                lgDown={col.hidden === 'lgDown'}
                lgUp={col.hidden === 'lgUp'}
                xlDown={col.hidden === 'xlDown'}
                xlUp={col.hidden === 'xlUp'}
              >
                <TableCell key={`cell-${i}-${j}`} className={cls}>
                  {val}
                </TableCell>
              </Hidden>
            );
          })}
          {settings.showEdit || settings.showDelete || settings.showEnter || settings.showMenu ? (
            <TableCell size='small'
              key={`cell-${i}-commands`}
              className={classes.cellCommands}
            >
              {settings.showEdit && onEdit ? (
                <IconButton
                  onClick={() => {
                    onEdit(row.value);
                  }}
                >
                  <EditSharp fontSize="small" />
                </IconButton>
              ) : null}
              {settings.showDelete && onDelete ? (
                <IconButton
                  onClick={() => {
                    onDelete(row.value);
                  }}
                >
                  <DeleteSharp fontSize="small" />
                </IconButton>
              ) : null}
              {settings.showMenu && onMenu ? (
                <IconButton
                  onClick={() => {
                    onMenu(row.value);
                  }}
                >
                  <MoreVertSharp fontSize="small" />
                </IconButton>
              ) : null}
              {settings.showEnter && onEnter ? (
                <IconButton
                  onClick={() => {
                    onEnter(row.value);
                  }}
                >
                  <ArrowForwardIosSharp fontSize="small" />
                </IconButton>
              ) : null}
            </TableCell>
          ) : null}
        </TableRow>
      );
    }
    return rr;
  }, [
    rows,
    loading,
    columns,
    cellClasses,
    page,
    rowsPerPage,
    settings,
    classes,
    onEdit,
    onDelete,
  ]);

  return (
    <>
      {settings.showAdd ? (
        <Fab className={classes.fab} color="secondary" onClick={onAdd}>
          <AddSharp />
        </Fab>
      ) : null}

      <TableContainer>
        <Table size="small">
          <TableHead>
            <TableRow>{headers}</TableRow>
          </TableHead>

          <TableBody>{values}</TableBody>
        </Table>
      </TableContainer>

      {settings.pagination ? (
        <TablePagination
          component="div"
          count={rows.length}
          page={page}
          onPageChange={handleChangePage}
          rowsPerPage={rowsPerPage}
          onChangeRowsPerPage={handleChangeRowsPerPage}
          backIconButtonText="Pagina precedente"
          nextIconButtonText="Pagina successiva"
          labelRowsPerPage="Righe per pagina"
        />
      ) : null}
    </>
  );
};

export default DataTable;
