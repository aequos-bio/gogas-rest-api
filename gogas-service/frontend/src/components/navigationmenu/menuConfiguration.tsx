import {
  HomeSharp as HomeIcon,
  EventSharp as EventIcon,
  EuroSharp as EuroIcon,
  GroupSharp as GroupIcon,
  Settings as SettingsIcon,
  ViewListSharp as ListIcon,
  ExploreSharp as ExploreIcon,
  CodeSharp as CodeIcon,
  AccountBalanceSharp as AccountBalanceIcon,
  ReceiptSharp as BillIcon,
} from '@material-ui/icons';
import { MenuChapter } from './types';

export const menuIcons = [
  <HomeIcon />,
  <EventIcon />,
  <EuroIcon />,
  <GroupIcon />,
  <SettingsIcon />,
  <ListIcon />,
  <ExploreIcon />,
  <CodeIcon />,
  <AccountBalanceIcon />,
  <BillIcon />,
];

export const menuItems: MenuChapter[] = [
  {
    items: [{ label: 'Home', url: '/', icon: 0 }],
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