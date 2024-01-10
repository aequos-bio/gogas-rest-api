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

export const menuIcons = [
  <HomeSharp />,
  <EventSharp />,
  <EuroSharp />,
  <GroupSharp />,
  <SettingsSharp />,
  <ViewListSharp />,
  <ExploreSharp />,
  <CodeSharp />,
  <AccountBalanceSharp />,
  <ReceiptSharp />,
  <StoreSharp />,
  <ShoppingBasketSharp />,
  <ShoppingCartSharp />,
  <HelpOutlineSharp />
];

export const menuItems: MenuChapter[] = [
  {
    label: 'Utente',
    items: [
      {
        label: 'Home',
        url: '/',
        icon: 0
      },
      {
        label: 'Situazione contabile',
        url: `/useraccountingdetails?userId=:userId`,
        icon: 2,
      },
      {
        label: 'Guida',
        url: '/documentation/GoGas.pdf',
        icon: 13,
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
        icon: 3
      },
      {
        label: 'Situazione contabile amici',
        url: '/friendsaccounting',
        restrictions: { friendsEnabled: true, roles: ['U'] },
        icon: 3
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
        icon: 1
      },
      {
        label: 'Causali',
        url: '/reasons',
        restrictions: { roles: ['A'] },
        icon: 4
      },
      {
        label: 'Codici contabili',
        url: '/accountingcodes',
        restrictions: { roles: ['A'] },
        icon: 7,
      },
      {
        label: 'Movimenti del gas',
        url: '/gasmovements',
        restrictions: { roles: ['A'] },
        icon: 2,
      },
      {
        label: 'Situazione utenti',
        url: '/useraccounting',
        restrictions: { roles: ['A'] },
        icon: 2,
      },
      {
        label: 'Fatture',
        url: '/invoices',
        restrictions: { roles: ['A'] },
        icon: 9,
      },
      {
        label: 'Contabilità del GAS',
        url: '/gasaccounting',
        restrictions: { roles: ['A'] },
        icon: 8,
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
        icon: 3
      },
      {
        label: 'Tipi ordine',
        url: '/ordertypes',
        restrictions: { roles: ['A'] },
        icon: 5,
      },
      {
        label: 'Referenti',
        url: '/managers',
        restrictions: { roles: ['A'] },
        icon: 6
      },
      {
        label: 'Produttori',
        url: '/legacy/suppliers',
        restrictions: { roles: ['A'] },
        icon: 10
      },
      {
        label: 'Configurazione generale',
        url: '/legacy/configuration',
        restrictions: { roles: ['A'] },
        icon: 4
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
        icon: 12
      },
      {
        label: 'Gestione ordini',
        url: `/legacy/orderslist`,
        restrictions: { orderManager: true },
        icon: 11
      },
    ],
  },
];