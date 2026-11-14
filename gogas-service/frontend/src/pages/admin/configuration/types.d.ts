export interface ConfigurationItem {
  /** Configuration key — read-only, identifies the item. */
  key: string;
  /** Current value — the only editable field. */
  value: string;
  /** Human-readable description — read-only. */
  description: string;
}
