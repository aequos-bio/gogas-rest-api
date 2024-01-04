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
    items: [
      { label: 'Home', url: '/', icon: 0 },
      { label: 'Guida', url: '/documentation/GoGas.pdf', icon: 13, newWindow: true }
    ],
  },
  {
    label: 'Contabilità [year]',
    items: [
      { label: 'Anni contabili', url: '/years', restrictions: ['A'], icon: 1 },
      { label: 'Causali', url: '/reasons', restrictions: ['A'], icon: 4 },
      {
        label: 'Codici contabili',
        url: '/accountingcodes',
        restrictions: ['A'],
        icon: 7,
      },
      {
        label: 'Movimenti del gas',
        url: '/gasmovements',
        restrictions: ['A'],
        icon: 2,
      },
      {
        label: 'Situazione utenti',
        url: '/useraccounting',
        restrictions: ['A'],
        icon: 2,
      },
      {
        label: 'Fatture',
        url: '/invoices',
        restrictions: ['A'],
        icon: 9,
      },
      {
        label: 'Contabilità del GAS',
        url: '/gasaccounting',
        restrictions: ['A'],
        icon: 8,
      },
    ],
  },
  {
    label: 'Gestione',
    items: [
      { label: 'Utenti', url: '/users', restrictions: ['A'], icon: 3 },
      {
        label: 'Tipi ordine',
        url: '/ordertypes',
        restrictions: ['A'],
        icon: 5,
      },
      { label: 'Referenti', url: '/managers', restrictions: ['A'], icon: 6 },
      { label: 'Produttori', url: '/legacy/suppliers', restrictions: ['A'], icon: 10 },
      { label: 'Configurazione generale', url: '/legacy/configuration', restrictions: ['A'], icon: 4 },
    ],
  },
  {
    label: 'Referente',
    items: [
      { label: 'Prodotti', url: '/legacy/products', restrictions: ['A'], icon: 12 },
      { label: 'Gestione ordini', url: `/legacy/orderslist`, icon: 11 },
    ],
  },
  {
    label: 'Utente',
    items: [
      {
        label: 'Situazione contabile',
        url: `/useraccountingdetails?userId=:userId`,
        icon: 2,
      },
    ],
  },
];