export interface Reason {
  accountingCode: string;
  description: string;
  reasonCode: string;
  sign: '-' | '+';
  signEnum?: 'MINUS' | 'PLUS';
}