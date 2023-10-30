import { List, Typography } from "@material-ui/core";
import { makeStyles } from '@material-ui/core/styles';
import { MenuChapter as MenuChapterType, MenuItem as MenuItemType } from "./types";
import { useAppSelector } from "../../store/store";
import useJwt from "../../hooks/JwtHooks";
import MenuItem from "./MenuItem";

const drawerWidth = '280px';
const useStyles = makeStyles((theme) => ({
  menuContainer: {
    width: drawerWidth,
    padding: theme.spacing(1, 0),
    display: 'flex',
    flexDirection: 'column',
  },
  menuChapter: {
    paddingLeft: theme.spacing(2),
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

  const menus = chapter.items.filter((menu) => {
    if (!menu.restrictions) return true;
    const matching = menu.restrictions.filter((r) => r === jwt?.role);
    if (matching.length > 0) {
      return true;
    }
    return false;
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
    </>
  );
}

export default MenuChapter;