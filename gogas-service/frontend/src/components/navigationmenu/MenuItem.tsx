import { ListItem, ListItemIcon, ListItemText } from "@material-ui/core";
import { makeStyles } from '@material-ui/core/styles';
import { MenuItem as MenuItemType } from "./types";
import { menuIcons } from "./menuConfiguration";

const useStyles = makeStyles((theme) => ({
  menuItem: {
    padding: theme.spacing(0.5, 2),
  },
  menuItemIcon: {
    minWidth: theme.spacing(6),
  },
}));

interface Props {
  menu: MenuItemType;
  onMenuClick: (menu: MenuItemType) => void;
}
const MenuItem: React.FC<Props> = ({ menu, onMenuClick }) => {
  const classes = useStyles();

  return (
    <ListItem
      className={classes.menuItem}
      button
      onClick={() => onMenuClick(menu)}
    >
      <ListItemIcon className={classes.menuItemIcon}>
        {menuIcons[menu.icon]}
      </ListItemIcon>
      <ListItemText>{menu.label}</ListItemText>
    </ListItem>

  )
}

export default MenuItem;