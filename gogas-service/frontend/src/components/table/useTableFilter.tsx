import { useState, useMemo, useCallback } from 'react';

/**
 * Client-side filtering hook for the Tier A table toolkit.
 *
 * Keeps a free-text search term and returns the rows that match it on any of
 * the provided searchable properties. Matching is case-insensitive substring.
 *
 * For structured/column-specific filters, pages can compose this with their
 * own predicates via the `extraPredicate` argument.
 */
export function useTableFilter<T>(
  rows: T[],
  getValue: (row: T, property: string) => any,
  searchableProperties: string[],
  extraPredicate?: (row: T) => boolean,
) {
  const [term, setTerm] = useState('');

  const filteredRows = useMemo(() => {
    const normalized = term.trim().toLowerCase();

    return rows.filter((row) => {
      if (extraPredicate && !extraPredicate(row)) {
        return false;
      }
      if (!normalized) {
        return true;
      }
      return searchableProperties.some((property) => {
        const value = getValue(row, property);
        if (value === undefined || value === null) {
          return false;
        }
        return `${value}`.toLowerCase().includes(normalized);
      });
    });
  }, [rows, term, searchableProperties, getValue, extraPredicate]);

  const clear = useCallback(() => setTerm(''), []);

  return { filteredRows, term, setTerm, clear };
}
