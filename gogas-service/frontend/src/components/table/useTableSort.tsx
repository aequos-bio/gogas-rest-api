import { useState, useMemo, useCallback } from 'react';
import moment from 'moment-timezone';

export type SortDirection = 'asc' | 'desc';

export interface SortState {
  property?: string;
  direction: SortDirection;
}

/**
 * Column value types understood by the comparator. Mirrors the `type`
 * used by DataTable columns so sorting matches the rendered formatting.
 */
export type SortType = 'String' | 'Number' | 'Amount' | 'Date' | 'DateTime' | 'Time' | 'Boolean';

const toComparable = (value: any, type?: SortType): number | string => {
  if (value === undefined || value === null) {
    // Push empty values to the end regardless of direction handling below.
    return type === 'Number' || type === 'Amount' ? Number.NEGATIVE_INFINITY : '';
  }

  switch (type) {
    case 'Number':
    case 'Amount':
      return typeof value === 'number' ? value : parseFloat(value) || 0;
    case 'Date': {
      // DD/MM/YYYY strings from the backend; parse explicitly to avoid
      // MM/DD misreads for days <= 12.
      const d = moment(value, ['DD/MM/YYYY', 'YYYY-MM-DD', moment.ISO_8601], true);
      return d.isValid() ? d.valueOf() : 0;
    }
    case 'DateTime':
    case 'Time': {
      const d = moment(value);
      return d.isValid() ? d.valueOf() : 0;
    }
    case 'Boolean':
      return value ? 1 : 0;
    default:
      return `${value}`.toLowerCase();
  }
};

/**
 * Client-side sorting hook for the Tier A table toolkit.
 *
 * Given a list of rows, returns the sorted rows plus the current sort state
 * and a toggle handler suitable for wiring to a sortable column header.
 *
 * The sort is stable and non-mutating (operates on a copy of `rows`).
 */
export function useTableSort<T>(
  rows: T[],
  getValue: (row: T, property: string) => any,
  typeOf?: (property: string) => SortType | undefined,
  initial?: SortState,
) {
  const [sort, setSort] = useState<SortState>(initial ?? { direction: 'asc' });

  const toggleSort = useCallback((property: string) => {
    setSort((prev) => {
      if (prev.property !== property) {
        return { property, direction: 'asc' };
      }
      // Same column: asc -> desc -> cleared
      if (prev.direction === 'asc') {
        return { property, direction: 'desc' };
      }
      return { property: undefined, direction: 'asc' };
    });
  }, []);

  const sortedRows = useMemo(() => {
    if (!sort.property) {
      return rows;
    }
    const property = sort.property;
    const type = typeOf?.(property);
    const factor = sort.direction === 'asc' ? 1 : -1;

    // Decorate-sort-undecorate keeps the sort stable.
    return rows
      .map((row, index) => ({ row, index }))
      .sort((a, b) => {
        const va = toComparable(getValue(a.row, property), type);
        const vb = toComparable(getValue(b.row, property), type);
        if (va < vb) return -1 * factor;
        if (va > vb) return 1 * factor;
        return a.index - b.index;
      })
      .map((entry) => entry.row);
  }, [rows, sort, getValue, typeOf]);

  return { sortedRows, sort, toggleSort, setSort };
}
