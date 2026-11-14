export interface Supplier {
  id: string;
  /** Ragione sociale (JSON: ragionesociale) */
  ragionesociale: string;
  /** Provincia (JSON: provincia) */
  provincia: string;
  /**
   * External id (JSON: idesterno). When present the supplier was imported
   * automatically and cannot be edited or deleted.
   */
  idesterno?: string | null;
}
