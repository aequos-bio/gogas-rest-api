export interface InfoState {
  'gas.nome': string;
  'colli.soglia_arrotondamento': string;
  'aequos.password': string;
  'aequos.username': string;
  'visualizzazione.utenti': 'NC' | 'CN';
  'friends.enabled': string | undefined;
}

export interface AccountingState {
  currentYear: number;
}

export interface AuthenticationState {
  running: boolean;
  error_message?: string;
  jwtToken?: string;
  userDetails: any;

}

export interface JwtToken {
  id: string;
  tenant: string;
  firstname: string;
  lastname: string;
  manager: boolean;
  role: string;
  exp: number;
  expired?: boolean;
  sub: string;
}

export interface ErrorResponse {
  error: any;
  errorMessage: string;
}

export interface DataResponse<T> extends ErrorResponse {
  data: T;
}
