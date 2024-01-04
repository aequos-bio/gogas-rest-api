export interface MenuChapter {
  label?: string;
  items: MenuItem[];
}

export interface MenuItem {
  label: string;
  url: string;
  icon: number;
  restrictions?: Restrictions;
}

export interface Restrictions {
  roles?: string[];
  orderManager?: boolean;
  friendsEnabled?: boolean;
}

