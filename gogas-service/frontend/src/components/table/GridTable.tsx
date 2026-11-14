import React from 'react';
import {
  useReactTable,
  getCoreRowModel,
  getSortedRowModel,
  getExpandedRowModel,
  getGroupedRowModel,
  flexRender,
  ColumnDef,
  SortingState,
  GroupingState,
} from '@tanstack/react-table';
import {
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableCell,
  TableBody,
  TableFooter,
  TableSortLabel,
  IconButton,
} from '@material-ui/core';
import {
  KeyboardArrowRight as CollapsedIcon,
  KeyboardArrowDown as ExpandedIcon,
} from '@material-ui/icons';
import { makeStyles } from '@material-ui/core/styles';

const useStyles = makeStyles((theme) => ({
  groupCell: {
    display: 'flex',
    alignItems: 'center',
  },
  groupCount: {
    marginLeft: theme.spacing(1),
    color: theme.palette.text.secondary,
  },
  footerCell: {
    fontWeight: 600,
  },
}));

interface Props<T> {
  data: T[];
  columns: ColumnDef<T, any>[];
  /** Enable client-side column sorting. Default true. */
  enableSorting?: boolean;
  /** Grouping column ids; enables grouped/aggregated rendering when provided. */
  grouping?: GroupingState;
  /** Render a footer row driven by column aggregates. Default false. */
  showFooter?: boolean;
  size?: 'small' | 'medium';
}

/**
 * Tier B table for the complex order-management grids. Uses TanStack Table
 * (headless) for sorting / grouping / aggregation logic and renders the result
 * through MUI table components, so it stays on MUI v4 and the app theme.
 *
 * This covers the jqxGrid features the referenti order-detail pages rely on
 * (grouping, aggregates/footers) without adopting a visual datagrid.
 */
function GridTable<T>({
  data,
  columns,
  enableSorting = true,
  grouping,
  showFooter = false,
  size = 'small',
}: Props<T>) {
  const classes = useStyles();
  const [sorting, setSorting] = React.useState<SortingState>([]);

  const table = useReactTable<T>({
    data,
    columns,
    state: {
      sorting,
      grouping: grouping ?? [],
    },
    enableSorting,
    onSortingChange: setSorting,
    getCoreRowModel: getCoreRowModel(),
    getSortedRowModel: getSortedRowModel(),
    getGroupedRowModel: grouping && grouping.length ? getGroupedRowModel() : undefined,
    getExpandedRowModel: grouping && grouping.length ? getExpandedRowModel() : undefined,
  });

  return (
    <TableContainer>
      <Table size={size}>
        <TableHead>
          {table.getHeaderGroups().map((headerGroup) => (
            <TableRow key={headerGroup.id}>
              {headerGroup.headers.map((header) => {
                const canSort = header.column.getCanSort();
                const sorted = header.column.getIsSorted();
                return (
                  <TableCell key={header.id} style={{ fontWeight: 600 }}>
                    {header.isPlaceholder ? null : canSort ? (
                      <TableSortLabel
                        active={!!sorted}
                        direction={sorted === 'desc' ? 'desc' : 'asc'}
                        onClick={header.column.getToggleSortingHandler()}
                      >
                        {flexRender(header.column.columnDef.header, header.getContext())}
                      </TableSortLabel>
                    ) : (
                      flexRender(header.column.columnDef.header, header.getContext())
                    )}
                  </TableCell>
                );
              })}
            </TableRow>
          ))}
        </TableHead>

        <TableBody>
          {table.getRowModel().rows.map((row) => (
            <TableRow key={row.id}>
              {row.getVisibleCells().map((cell) => {
                if (cell.getIsGrouped()) {
                  return (
                    <TableCell key={cell.id}>
                      <span className={classes.groupCell}>
                        <IconButton size="small" onClick={row.getToggleExpandedHandler()}>
                          {row.getIsExpanded() ? <ExpandedIcon /> : <CollapsedIcon />}
                        </IconButton>
                        {flexRender(cell.column.columnDef.cell, cell.getContext())}
                        <span className={classes.groupCount}>({row.subRows.length})</span>
                      </span>
                    </TableCell>
                  );
                }
                if (cell.getIsAggregated()) {
                  return (
                    <TableCell key={cell.id}>
                      {flexRender(
                        cell.column.columnDef.aggregatedCell ?? cell.column.columnDef.cell,
                        cell.getContext(),
                      )}
                    </TableCell>
                  );
                }
                if (cell.getIsPlaceholder()) {
                  return <TableCell key={cell.id} />;
                }
                return (
                  <TableCell key={cell.id}>
                    {flexRender(cell.column.columnDef.cell, cell.getContext())}
                  </TableCell>
                );
              })}
            </TableRow>
          ))}
        </TableBody>

        {showFooter ? (
          <TableFooter>
            {table.getFooterGroups().map((footerGroup) => (
              <TableRow key={footerGroup.id}>
                {footerGroup.headers.map((header) => (
                  <TableCell key={header.id} className={classes.footerCell}>
                    {header.isPlaceholder
                      ? null
                      : flexRender(header.column.columnDef.footer, header.getContext())}
                  </TableCell>
                ))}
              </TableRow>
            ))}
          </TableFooter>
        ) : null}
      </Table>
    </TableContainer>
  );
}

export default GridTable;
