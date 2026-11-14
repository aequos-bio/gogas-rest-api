import axios from 'axios';
import ExcelJS from 'exceljs';

/**
 * Trigger a browser download for an in-memory Blob by creating a temporary
 * object URL and clicking a synthetic anchor.
 */
export function saveBlob(blob: Blob, filename: string): void {
  const url = window.URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = filename;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  window.URL.revokeObjectURL(url);
}

/**
 * Download a file from an authenticated API endpoint. Unlike the legacy
 * `document.location.href` approach, this goes through axios so the request
 * carries the usual credentials/headers, then saves the response as a file.
 *
 * The filename is taken from the Content-Disposition header when present,
 * otherwise the provided fallback is used.
 */
export async function downloadFile(url: string, fallbackFilename: string): Promise<void> {
  const response = await axios.get(url, { responseType: 'blob' });

  let filename = fallbackFilename;
  const disposition = response.headers['content-disposition'];
  if (disposition) {
    const match = /filename\*?=(?:UTF-8''|")?([^";]+)/i.exec(disposition);
    if (match && match[1]) {
      filename = decodeURIComponent(match[1].replace(/"/g, ''));
    }
  }

  saveBlob(response.data as Blob, filename);
}

export interface ExcelColumn<T> {
  header: string;
  /** Property key or accessor returning the cell value for a row. */
  value: keyof T | ((row: T) => any);
  width?: number;
}

/**
 * Build and download an .xlsx file from a set of rows, replacing the legacy
 * jqxGrid export. Standardizes column headers, widths, and a bold header row.
 */
export async function exportToExcel<T>(
  rows: T[],
  columns: ExcelColumn<T>[],
  filename: string,
  sheetName = 'Dati',
): Promise<void> {
  const workbook = new ExcelJS.Workbook();
  const sheet = workbook.addWorksheet(sheetName);

  sheet.columns = columns.map((col) => ({
    header: col.header,
    key: col.header,
    width: col.width ?? 20,
  }));

  rows.forEach((row) => {
    const values = columns.map((col) =>
      typeof col.value === 'function' ? col.value(row) : (row as any)[col.value],
    );
    sheet.addRow(values);
  });

  sheet.getRow(1).font = { bold: true };

  const buffer = await workbook.xlsx.writeBuffer();
  const blob = new Blob([buffer as ArrayBuffer], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  });
  saveBlob(blob, filename.endsWith('.xlsx') ? filename : `${filename}.xlsx`);
}
