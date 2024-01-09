import { List, Typography, Divider } from "@material-ui/core";
import { makeStyles } from '@material-ui/core/styles';
import { MenuChapter as MenuChapterType, MenuItem as MenuItemType } from "./types";
import { useAppSelector } from "../../store/store";
import useJwt from "../../hooks/JwtHooks";
import MenuItem from "./MenuItem";

const drawerWidth = '280px';
const useStyles = makeStyles((theme) => ({
  menuContainer: {
    width: drawerWidth,
    padding: theme.spacing(0, 0, 1),
    display: 'flex',
    flexDirection: 'column',
  },
  menuChapter: {
    paddingLeft: theme.spacing(2),
    fontSize: '70%'
  },
  menuItemList: {
    padding: 0,
  },
}));

interface Props {
  chapter: MenuChapterType;
  onMenuClick: (menu: MenuItemType) => void;
}

const MenuChapter: React.FC<Props> = ({ chapter, onMenuClick }) => {
  const classes = useStyles();
  const jwt = useJwt();
  const accounting = useAppSelector((state) => state.accounting);
  const info = useAppSelector((state) => state.info);

  const menus = chapter.items.filter((menu) => {
    var restrictions = menu.restrictions;

    if (!restrictions) {
      return true;
    }

    if (restrictions.roles) {
      const matchingRoles = restrictions.roles.filter((r) => r === jwt?.role);
      if (matchingRoles.length <= 0) {
        return false;
      };
    }

    if ((restrictions.orderManager ?? false) && !jwt?.manager) {
      return false;
    }

    if ((restrictions.friendsEnabled ?? false) && !info['friends.enabled']) {
      return false;
    }

    return true;
  });

  if (!menus.length) return <></>;

  return (
    <>
      <div className={classes.menuContainer}>
        {chapter.label ? (
          <Typography
            variant='overline'
            display='block'
            gutterBottom
            color='textSecondary'
            className={classes.menuChapter}
          >
            {chapter.label.replace('[year]', `${accounting.currentYear}`)}
          </Typography>
        ) : null}
        <List className={classes.menuItemList}>
          {menus.map((m, j) => (
            <MenuItem key={`menuitem-${m.label}-${j}`} menu={m} onMenuClick={onMenuClick} />
          ))}

        </List>
      </div>
      <Divider />
    </>
  );
}

export default MenuChapter;