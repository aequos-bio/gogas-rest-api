import {
  HomeSharp,
  EventSharp,
  EuroSharp,
  GroupSharp,
  SettingsSharp,
  ViewListSharp,
  ExploreSharp,
  CodeSharp,
  AccountBalanceSharp,
  ReceiptSharp,
  StoreSharp,
  ShoppingBasketSharp,
  ShoppingCartSharp,
  HelpOutlineSharp
} from '@material-ui/icons';
import { MenuChapter } from './types';

const menuIcons: { [key: string]: JSX.Element } = {
  home: <HomeSharp />,
  years: <EventSharp />,
  accounting: <EuroSharp />,
  friends: <GroupSharp />,
  settings: < SettingsSharp />,
  ordertypes: <ViewListSharp />,
  managers: <ExploreSharp />,
  codes: <CodeSharp />,
  gasaccounting: <AccountBalanceSharp />,
  invoices: <ReceiptSharp />,
  suppliers: <StoreSharp />,
  orders: <ShoppingBasketSharp />,
  products: <ShoppingCartSharp />,
  help: <HelpOutlineSharp />
}

export const menuItems: MenuChapter[] = [
  {
    label: 'Utente',
    items: [
      {
        label: 'Home',
        url: '/',
        icon: menuIcons.home
      },
      {
        label: 'Situazione contabile',
        url: `/useraccountingdetails?userId=:userId`,
        icon: menuIcons.accounting,
      },
      {
        label: 'Guida',
        url: '/documentation/GoGas.pdf',
        icon: menuIcons.help,
        newWindow: true
      },
      {
        label: 'Guida Smistamento',
        url: '/documentation/Smisto.pdf',
        icon: menuIcons.help,
        newWindow: true
      },
    ],
  },
  {
    label: 'Amici',
    items: [
      {
        label: 'Gestione amici',
        url: '/legacy/managefriends',
        restrictions: { friendsEnabled: true, roles: ['U'] },
        icon: menuIcons.friends
      },
      {
        label: 'Situazione contabile amici',
        url: '/friendsaccounting',
        restrictions: { friendsEnabled: true, roles: ['U'] },
        icon: menuIcons.accounting
      },
    ],
  },
  {
    label: 'Contabilità [year]',
    items: [
      {
        label: 'Anni contabili',
        url: '/years',
        restrictions: { roles: ['A'] },
        icon: menuIcons.years
      },
      {
        label: 'Causali',
        url: '/reasons',
        restrictions: { roles: ['A'] },
        icon: menuIcons.settings
      },
      {
        label: 'Codici contabili',
        url: '/accountingcodes',
        restrictions: { roles: ['A'] },
        icon: menuIcons.codes,
      },
      {
        label: 'Movimenti del gas',
        url: '/gasmovements',
        restrictions: { roles: ['A'] },
        icon: menuIcons.accounting,
      },
      {
        label: 'Situazione utenti',
        url: '/useraccounting',
        restrictions: { roles: ['A'] },
        icon: menuIcons.accounting,
      },
      {
        label: 'Fatture',
        url: '/invoices',
        restrictions: { roles: ['A'] },
        icon: menuIcons.invoices,
      },
      {
        label: 'Contabilità del GAS',
        url: '/gasaccounting',
        restrictions: { roles: ['A'] },
        icon: menuIcons.gasaccounting,
      },
    ],
  },
  {
    label: 'Gestione',
    items: [
      {
        label: 'Utenti',
        url: '/users',
        restrictions: { roles: ['A'] },
        icon: menuIcons.friends
      },
      {
        label: 'Tipi ordine',
        url: '/ordertypes',
        restrictions: { roles: ['A'] },
        icon: menuIcons.ordertypes,
      },
      {
        label: 'Referenti',
        url: '/managers',
        restrictions: { roles: ['A'] },
        icon: menuIcons.managers
      },
      {
        label: 'Produttori',
        url: '/legacy/suppliers',
        restrictions: { roles: ['A'] },
        icon: menuIcons.suppliers
      },
      {
        label: 'Configurazione generale',
        url: '/legacy/configuration',
        restrictions: { roles: ['A'] },
        icon: menuIcons.settings
      },
    ],
  },
  {
    label: 'Referente',
    items: [
      {
        label: 'Prodotti',
        url: '/legacy/products',
        restrictions: { orderManager: true },
        icon: menuIcons.products
      },
      {
        label: 'Gestione ordini',
        url: `/orders`,
        restrictions: { orderManager: true },
        icon: menuIcons.orders
      },
      {
        label: 'Tool smistamento (exe)',
        url: '/tools/smistamento.exe',
        restrictions: { orderManager: true },
        icon: menuIcons.settings,
        newWindow: true
      },
      {
        label: 'Tool smistamento (jar)',
        url: '/tools/smistamento.jar',
        restrictions: { orderManager: true },
        icon: menuIcons.settings,
        newWindow: true
      },
    ],
  },
];