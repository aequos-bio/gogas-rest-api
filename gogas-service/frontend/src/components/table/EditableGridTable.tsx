import React, { useCallback, useMemo, useRef, useState } from 'react';
import {
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableCell,
  TableBody,
  TableFooter,
  InputBase,
} from '@material-ui/core';
import { makeStyles } from '@material-ui/core/styles';

export interface EditableColumn<T> {
  /** Header label. */
  label: React.ReactNode;
  /** Stable column id. */
  id: string;
  align?: 'left' | 'center' | 'right';
  width?: number | string;
  /** Read-only cell content. Ignored when `editable` is true. */
  render?: (row: T) => React.ReactNode;
  /** Marks this column as an editable numeric column. */
  editable?: boolean;
  /** For editable columns: current numeric value for the row. */
  getValue?: (row: T) => number | undefined;
  /** For editable columns: decide per-row whether the cell can be edited. */
  canEdit?: (row: T) => boolean;
  /** Decimal digits allowed (editable columns). Default 2. */
  decimals?: number;
  /** Footer content (e.g. an aggregate). */
  footer?: (rows: T[]) => React.ReactNode;
}

interface Props<T> {
  columns: EditableColumn<T>[];
  rows: T[];
  rowId: (row: T) => string;
  /** Commit a new value for an editable cell. Return true if accepted. */
  onCommit: (row: T, columnId: string, value: number) => Promise<boolean> | boolean;
  /** Optional per-row CSS class (e.g. for cancelled rows). */
  rowClassName?: (row: T) => string | undefined;
  /** Optional row-click handler (e.g. master/detail selection). */
  onRowClick?: (row: T) => void;
  /** Row id currently selected (for highlight). */
  selectedRowId?: string;
  size?: 'small' | 'medium';
  emptyMessage?: string;
}

const useStyles = makeStyles((theme) => ({
  editableCell: {
    cursor: 'text',
    backgroundColor: 'rgba(76, 175, 80, 0.08)',
    padding: theme.spacing(0, 1),
  },
  input: {
    font: 'inherit',
    width: '100%',
    '& input': {
      textAlign: 'right',
      padding: theme.spacing(0.25, 0),
    },
  },
  editing: {
    outline: `2px solid ${theme.palette.primary.main}`,
    outlineOffset: '-2px',
  },
  empty: {
    textAlign: 'center',
    color: theme.palette.text.secondary,
    padding: theme.spacing(2),
  },
  clickableRow: {
    cursor: 'pointer',
  },
  selectedRow: {
    backgroundColor: 'rgba(33, 150, 243, 0.12)',
  },
}));

/**
 * A table with keyboard-driven editing of numeric cells, replacing the legacy
 * jqxGrid fast-entry behaviour for order quantities.
 *
 * Editable cells support:
 *  - type-to-edit (focus a cell and start typing);
 *  - Enter / Tab  -> commit the value and move to the editable cell in the next row;
 *  - Shift+Tab    -> commit and move to the previous row;
 *  - Escape       -> revert to the original value.
 *
 * Editing is limited to the designated numeric column(s); there is no full
 * spreadsheet-style arrow navigation (by design).
 */
function EditableGridTable<T>({
  columns,
  rows,
  rowId,
  onCommit,
  rowClassName,
  onRowClick,
  selectedRowId,
  size = 'small',
  emptyMessage = 'Nessun dato',
}: Props<T>) {
  const classes = useStyles();

  // Which cell is being edited, and its draft text.
  const [editing, setEditing] = useState<{ rowId: string; columnId: string } | null>(null);
  const [draft, setDraft] = useState('');
  const inputRefs = useRef<Record<string, HTMLInputElement | null>>({});
  // When a key handler has already decided how to leave the cell (commit via
  // Enter/Tab, or discard via Esc/Arrow), suppress the onBlur auto-commit so it
  // doesn't double-handle or commit a draft we meant to discard.
  const skipBlurRef = useRef(false);

  const editableColumns = useMemo(() => columns.filter((c) => c.editable), [columns]);

  const cellKey = (rid: string, cid: string) => `${rid}::${cid}`;

  const startEditing = useCallback(
    (row: T, column: EditableColumn<T>, initial?: string) => {
      if (!column.editable) return;
      if (column.canEdit && !column.canEdit(row)) return;
      const rid = rowId(row);
      const current = column.getValue?.(row);
      setEditing({ rowId: rid, columnId: column.id });
      setDraft(
        initial !== undefined
          ? initial
          : current !== undefined && current !== null
            ? String(current)
            : '',
      );
    },
    [rowId],
  );

  const parseDraft = useCallback((text: string, decimals: number): number | undefined => {
    const normalized = text.replace(',', '.').trim();
    if (normalized === '') return undefined;
    const n = Number(normalized);
    if (isNaN(n) || n < 0) return undefined;
    const factor = Math.pow(10, decimals);
    return Math.round(n * factor) / factor;
  }, []);

  const moveTo = useCallback(
    (targetRowIndex: number, columnId: string, direction: 1 | -1) => {
      const column = editableColumns.find((c) => c.id === columnId);
      if (!column) return;
      // Skip rows that cannot be edited in that column, scanning in `direction`.
      let idx = targetRowIndex;
      while (idx >= 0 && idx < rows.length) {
        const row = rows[idx];
        if (!column.canEdit || column.canEdit(row)) {
          startEditing(row, column);
          const key = cellKey(rowId(row), columnId);
          // Focus after the input mounts.
          setTimeout(() => inputRefs.current[key]?.focus(), 0);
          return;
        }
        idx += direction;
      }
    },
    [rows, editableColumns, rowId, startEditing],
  );

  const commit = useCallback(
    async (row: T, column: EditableColumn<T>): Promise<boolean> => {
      const value = parseDraft(draft, column.decimals ?? 2);
      const current = column.getValue?.(row);
      if (value === undefined) {
        // Empty/invalid -> treat as no change (revert).
        return false;
      }
      if (value === current) {
        return true; // no-op, accept
      }
      const ok = await onCommit(row, column.id, value);
      return ok;
    },
    [draft, parseDraft, onCommit],
  );

  const handleKeyDown = useCallback(
    async (event: React.KeyboardEvent, row: T, column: EditableColumn<T>, rowIndex: number) => {
      if (event.key === 'Escape') {
        event.preventDefault();
        skipBlurRef.current = true;
        setEditing(null);
        return;
      }
      // Arrow Up/Down: discard the current draft (no commit) and move the edit
      // to the adjacent editable row.
      if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
        event.preventDefault();
        skipBlurRef.current = true;
        const direction: 1 | -1 = event.key === 'ArrowDown' ? 1 : -1;
        setEditing(null);
        moveTo(rowIndex + direction, column.id, direction);
        return;
      }
      if (event.key === 'Enter' || event.key === 'Tab') {
        event.preventDefault();
        skipBlurRef.current = true;
        const ok = await commit(row, column);
        setEditing(null);
        if (ok) {
          const direction: 1 | -1 = event.shiftKey ? -1 : 1;
          moveTo(rowIndex + direction, column.id, direction);
        }
      }
    },
    [commit, moveTo],
  );

  const renderEditableCell = useCallback(
    (row: T, column: EditableColumn<T>, rowIndex: number) => {
      const rid = rowId(row);
      const key = cellKey(rid, column.id);
      const isEditing = editing?.rowId === rid && editing?.columnId === column.id;
      const canEdit = !column.canEdit || column.canEdit(row);
      const value = column.getValue?.(row);
      const display =
        value !== undefined && value !== null && !isNaN(value) ? value.toFixed(column.decimals ?? 2) : '';

      if (!canEdit) {
        return <span>{display}</span>;
      }

      if (isEditing) {
        return (
          <InputBase
            className={`${classes.input} ${classes.editing}`}
            value={draft}
            autoFocus
            inputProps={{ inputMode: 'decimal' }}
            inputRef={(el: HTMLInputElement | null) => {
              inputRefs.current[key] = el;
            }}
            onChange={(e) => setDraft(e.target.value)}
            onBlur={async () => {
              // A key handler (Enter/Tab/Esc/Arrow) already decided the outcome.
              if (skipBlurRef.current) {
                skipBlurRef.current = false;
                return;
              }
              await commit(row, column);
              setEditing(null);
            }}
            onKeyDown={(e) => handleKeyDown(e, row, column, rowIndex)}
          />
        );
      }

      // Not editing: show the value; typing starts editing with that character.
      return (
        <span
          tabIndex={0}
          style={{ display: 'block', width: '100%', minHeight: '1.2em' }}
          onFocus={() => undefined}
          onClick={() => startEditing(row, column)}
          onKeyDown={(e) => {
            if (e.key === 'Enter') {
              e.preventDefault();
              startEditing(row, column);
            } else if (/^[0-9.,]$/.test(e.key)) {
              e.preventDefault();
              startEditing(row, column, e.key);
            }
          }}
        >
          {display}
        </span>
      );
    },
    [editing, draft, classes, rowId, startEditing, commit, handleKeyDown],
  );

  const hasFooter = useMemo(() => columns.some((c) => c.footer), [columns]);

  return (
    <TableContainer>
      <Table size={size}>
        <TableHead>
          <TableRow>
            {columns.map((c) => (
              <TableCell key={c.id} align={c.align} style={{ width: c.width, fontWeight: 600 }}>
                {c.label}
              </TableCell>
            ))}
          </TableRow>
        </TableHead>

        <TableBody>
          {rows.length === 0 ? (
            <TableRow>
              <TableCell colSpan={columns.length} className={classes.empty}>
                {emptyMessage}
              </TableCell>
            </TableRow>
          ) : (
            rows.map((row, rowIndex) => {
              const rid = rowId(row);
              const classNames = [
                rowClassName?.(row),
                onRowClick ? classes.clickableRow : undefined,
                selectedRowId === rid ? classes.selectedRow : undefined,
              ]
                .filter(Boolean)
                .join(' ');
              return (
                <TableRow
                  key={rid}
                  className={classNames || undefined}
                  onClick={onRowClick ? () => onRowClick(row) : undefined}
                >
                  {columns.map((c) => (
                    <TableCell
                      key={c.id}
                      align={c.align}
                      className={c.editable ? classes.editableCell : undefined}
                    >
                      {c.editable
                        ? renderEditableCell(row, c, rowIndex)
                        : c.render
                          ? c.render(row)
                          : null}
                    </TableCell>
                  ))}
                </TableRow>
              );
            })
          )}
        </TableBody>

        {hasFooter ? (
          <TableFooter>
            <TableRow>
              {columns.map((c) => (
                <TableCell key={c.id} align={c.align}>
                  {c.footer ? c.footer(rows) : null}
                </TableCell>
              ))}
            </TableRow>
          </TableFooter>
        ) : null}
      </Table>
    </TableContainer>
  );
}

export default EditableGridTable;
