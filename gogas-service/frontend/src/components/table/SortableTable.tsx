import React, { useMemo, useCallback } from 'react';
import {
  Fab,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableCell,
  TableSortLabel,
  TableBody,
  TablePagination,
  IconButton,
} from '@material-ui/core';
import {
  AddSharp as PlusIcon,
  EditSharp as EditIcon,
  DeleteSharp as DeleteIcon,
} from '@material-ui/icons';
import { makeStyles } from '@material-ui/core/styles';
import moment from 'moment-timezone';
import LoadingRow from '../LoadingRow';
import { useTableSort, SortType } from './useTableSort';

/**
 * Column definition for the Tier A sortable table. A superset of the original
 * DataTable column: adds `sortable` (default true) so columns can opt out.
 */
export interface Column {
  label: string;
  type: 'String' | 'Date' | 'DateTime' | 'Time' | 'Number' | 'Amount' | 'Icon' | 'Boolean';
  alignment: 'Left' | 'Center' | 'Right';
  property: string;
  sortable?: boolean;
  /** Optional custom cell renderer; overrides the type-based formatting. */
  render?: (row: any) => React.ReactNode;
}

export interface Settings {
  canEdit: boolean;
  canDelete: boolean;
  canAdd: boolean;
  showHeader: boolean;
  showFooter: boolean;
  pagination: boolean;
  rowsPerPage?: number;
}

const default_settings: Settings = {
  canEdit: true,
  canDelete: true,
  canAdd: true,
  showHeader: true,
  showFooter: false,
  pagination: false,
};

const useStyles = makeStyles((theme) => ({
  cellLeft: { textAlign: 'left' },
  cellCenter: { textAlign: 'center' },
  cellRight: { textAlign: 'right' },
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
  headerCell: { fontWeight: 600 },
}));

interface Row {
  value: any;
}

interface Props {
  columns: Column[];
  rows: Row[];
  settings?: Settings;
  loading?: boolean;
  /** Enables per-column header sorting (client-side). Default true. */
  sortable?: boolean;
  /** Decide per-row whether edit/delete actions are available. Default: always. */
  canEditRow?: (row: any) => boolean;
  canDeleteRow?: (row: any) => boolean;
  onAdd?: () => void;
  onEdit?: (value: any) => void;
  onDelete?: (value: any) => void;
}

const alignmentClass = (
  classes: ReturnType<typeof useStyles>,
  alignment: Column['alignment'],
  type: Column['type'],
) => {
  if (alignment === 'Left') return classes.cellLeft;
  if (alignment === 'Right') return classes.cellRight;
  if (alignment === 'Center') return classes.cellCenter;
  if (type === 'Amount' || type === 'Number') return classes.cellRight;
  if (type === 'Date' || type === 'DateTime' || type === 'Time' || type === 'Icon' || type === 'Boolean') {
    return classes.cellCenter;
  }
  return classes.cellLeft;
};

const formatValue = (value: any, type: Column['type']) => {
  if (value === undefined || value === null) return '';
  switch (type) {
    case 'Date': {
      // The backend sends dates as DD/MM/YYYY strings; parse that explicitly
      // first so days <= 12 aren't misread as MM/DD by moment's loose parser.
      let date = moment(value, ['DD/MM/YYYY', 'YYYY-MM-DD', moment.ISO_8601], true);
      if (!date.isValid()) date = moment(value);
      return date.isValid() ? date.format('DD/MM/YYYY') : '';
    }
    case 'DateTime':
      return moment(value).format('DD/MM/YYYY HH:mm');
    case 'Time':
      return moment(value).format('HH:mm');
    case 'Amount': {
      const num = typeof value === 'number' ? value : parseFloat(value);
      if (isNaN(num)) return value;
      return new Intl.NumberFormat('it-IT', { style: 'currency', currency: 'EUR' }).format(num);
    }
    case 'Boolean':
      return value ? 'Sì' : 'No';
    default:
      return value;
  }
};

/**
 * Tier A table: the original DataTable plus client-side column sorting.
 * Pagination remains client-side; rows are plain in-memory arrays. Suitable
 * for the CRUD list pages of the legacy migration.
 */
const SortableTable: React.FC<Props> = ({
  columns,
  rows,
  settings = default_settings,
  loading = false,
  sortable = true,
  canEditRow,
  canDeleteRow,
  onAdd,
  onEdit,
  onDelete,
}) => {
  const classes = useStyles();
  const [page, setPage] = React.useState(0);
  const [rowsPerPage, setRowsPerPage] = React.useState(
    settings.pagination ? settings.rowsPerPage ?? 10 : 999999,
  );

  const getValue = useCallback((row: Row, property: string) => row.value[property], []);
  const typeOf = useCallback(
    (property: string): SortType | undefined => {
      const col = columns.find((c) => c.property === property);
      if (!col || col.type === 'Icon') return undefined;
      return col.type as SortType;
    },
    [columns],
  );

  const { sortedRows, sort, toggleSort } = useTableSort<Row>(rows, getValue, typeOf);

  const handleChangePage = (_event: unknown, newPage: number) => setPage(newPage);
  const handleChangeRowsPerPage = (event: React.ChangeEvent<HTMLInputElement>) => {
    setRowsPerPage(parseInt(event.target.value, 10));
    setPage(0);
  };

  const showCommands = settings.canEdit || settings.canDelete;

  const headers = useMemo(() => {
    const hh = columns.map((col, i) => {
      const cls = alignmentClass(classes, col.alignment, col.type);
      const canSort = sortable && col.sortable !== false && col.type !== 'Icon';
      return (
        <TableCell key={`header-${i}`} className={`${cls} ${classes.headerCell}`}>
          {canSort ? (
            <TableSortLabel
              active={sort.property === col.property}
              direction={sort.property === col.property ? sort.direction : 'asc'}
              onClick={() => toggleSort(col.property)}
            >
              {col.label}
            </TableSortLabel>
          ) : (
            col.label
          )}
        </TableCell>
      );
    });
    if (showCommands) {
      hh.push(<TableCell key="header-commands" className={classes.cellCommands} />);
    }
    return hh;
  }, [columns, classes, sortable, sort, toggleSort, showCommands]);

  const body = useMemo(() => {
    if (loading) {
      return <LoadingRow colSpan={columns.length + (showCommands ? 1 : 0)} />;
    }

    const start = page * rowsPerPage;
    const end = Math.min(sortedRows.length, rowsPerPage * (page + 1));
    const slice = sortedRows.slice(start, end);

    return slice.map((row, i) => {
      const rowEditable = canEditRow ? canEditRow(row.value) : true;
      const rowDeletable = canDeleteRow ? canDeleteRow(row.value) : true;
      return (
        <TableRow key={`row-${i}`}>
          {columns.map((col, j) => {
            const cls = alignmentClass(classes, col.alignment, col.type);
            const content = col.render
              ? col.render(row.value)
              : formatValue(row.value[col.property], col.type);
            return (
              <TableCell key={`cell-${i}-${j}`} className={cls}>
                {content}
              </TableCell>
            );
          })}
          {showCommands ? (
            <TableCell key={`cell-${i}-commands`} className={classes.cellCommands}>
              {settings.canEdit && onEdit && rowEditable ? (
                <IconButton size="small" onClick={() => onEdit(row.value)}>
                  <EditIcon fontSize="small" />
                </IconButton>
              ) : null}
              {settings.canDelete && onDelete && rowDeletable ? (
                <IconButton size="small" onClick={() => onDelete(row.value)}>
                  <DeleteIcon fontSize="small" />
                </IconButton>
              ) : null}
            </TableCell>
          ) : null}
        </TableRow>
      );
    });
  }, [
    sortedRows,
    loading,
    columns,
    classes,
    page,
    rowsPerPage,
    settings,
    showCommands,
    canEditRow,
    canDeleteRow,
    onEdit,
    onDelete,
  ]);

  return (
    <>
      {settings.canAdd ? (
        <Fab className={classes.fab} color="secondary" onClick={onAdd}>
          <PlusIcon />
        </Fab>
      ) : null}

      <TableContainer>
        <Table size="small">
          {settings.showHeader ? (
            <TableHead>
              <TableRow>{headers}</TableRow>
            </TableHead>
          ) : null}
          <TableBody>{body}</TableBody>
        </Table>
      </TableContainer>

      {settings.pagination ? (
        <TablePagination
          component="div"
          count={sortedRows.length}
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

export default SortableTable;
