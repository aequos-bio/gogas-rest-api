import React, { useCallback, useEffect, useState, useMemo } from "react";
import { connect } from "react-redux";
import {
  Container,
  Fab,
  Button,
  TableContainer,
  Table,
  TableHead,
  TableFooter,
  TableRow,
  TableCell,
  TableBody,
} from '@material-ui/core';
import { 
  AddSharp as PlusIcon,
  SaveAltSharp as SaveIcon 
} from '@material-ui/icons';
import { makeStyles } from '@material-ui/core/styles';
import { withSnackbar } from 'notistack';
import queryString from "query-string";
import moment from "moment-timezone";
import _ from "lodash";
import Jwt from "jsonwebtoken";
import { getJson } from "../../utils/axios_utils";
import NewTransactionDialog from "./components/NewTransactionDialog";
import PageTitle from '../../components/PageTitle';

const useStyles = makeStyles((theme) => ({
  fab: {
    position:'fixed',
    bottom: theme.spacing(2),
    right: theme.spacing(2),
  },
  tdAmount: {
    textAlign: "right",
    width: "90px",
  },
  footercell: {
    '& td': {
      fontSize: '.875rem'
    }
  }
}));

function UserAccountingDetails({ authentication, location, enqueueSnackbar }) {
  const classes = useStyles();
  const search = queryString.parse(location.search);
  const [user, setUser] = useState({});
  const [transactions, setTransactions] = useState([]);
  const [totals, setTotals] = useState({ accrediti: 0, addebiti: 0 });
  const [showDlg, setShowDlg] = useState(false);

  const reload = useCallback(() => {
    getJson(`/api/user/${search.userId}`, {}).then(u => {
      if (u.error) {
        enqueueSnackbar(u.errorMessage, { variant: 'error' })
      } else setUser(u);
    });

    getJson(
      `/api/useraccounting/userTransactions?userId=${search.userId}`,
      {}
    ).then(t => {
      if (t.error) {
        enqueueSnackbar(t.errorMessage, { variant: 'error' })
        setTransactions([]);
        setTotals({ accrediti: 0, addebiti: 0 });
      } else {
        const tt = _.orderBy(t.data, "date", "desc");
        let saldo = 0;
        let accrediti = 0;
        let addebiti = 0;
        if (tt.length)
          for (let f = tt.length - 1; f >= 0; f--) {
            const m = tt[f].amount * (tt[f].sign === "-" ? -1 : 1);
            tt[f].saldo = saldo + m;
            saldo = tt[f].saldo;
            if (m < 0) {
              addebiti += -1 * m;
            } else {
              accrediti += m;
            }
          }
        setTransactions(tt);
        setTotals({ accrediti, addebiti });
      }
    });
  }, [search.userId, enqueueSnackbar]);

  const downloadXls = useCallback(() => {
    window.open(
      `/api/useraccounting/exportUserDetails?userId=${user.idUtente}`,
      "_blank"
    );
  }, [user]);

  const jwt = useMemo(() => {
    if (authentication.jwtToken) {
      const j = Jwt.decode(authentication.jwtToken);
      return j;
    }
    return null;
  }, [authentication]);

  useEffect(() => {
    reload();
  }, [reload]);

  const dialogClosed = useCallback(
    refresh => {
      setShowDlg(false);
      if (refresh) reload();
    },
    [setShowDlg, reload]
  );

  const rows = useMemo(() => {
    if (!transactions || !transactions.length) return [];
    const rr = [];
    let lastYear = '';
    let lastYearPlus;
    let lastYearMinus;

    transactions.forEach((t, i) => {
      const year = moment(t.date).format("YYYY");
      if (year !== lastYear) {

        if (lastYearPlus !== undefined || lastYearMinus !== undefined) {
          rr.push(
            <TableRow key={`initialamt-${lastYear}`}>
              <TableCell align='center'>01/01/{lastYear}</TableCell>
              <TableCell>Saldo iniziale {lastYear}</TableCell>
              <TableCell />
              <TableCell />
              <TableCell className={classes.tdAmount} style={{ color: t.saldo < 0 ? "red" : "inherited" }}
              >
                {t.saldo >= 0 ? "+ " : ""}
                {t.saldo.toFixed(2)}
              </TableCell>
            </TableRow>
          );

          rr.push(
            <TableRow key={`totals-${lastYear}`}>
              <TableCell colSpan='2' align='right'>
                <strong>Totale anno {lastYear}</strong>
              </TableCell>
              <TableCell className={classes.tdAmount} >
                <strong>{Math.abs(lastYearPlus).toFixed(2)}</strong>
              </TableCell>
              <TableCell className={classes.tdAmount} >
                <strong>{Math.abs(lastYearMinus).toFixed(2)}</strong>
              </TableCell>
              <TableCell />
            </TableRow>
          )
        }
        rr.push(
          <TableRow key={`year-${year}`}>
            <TableCell colSpan='5'>
              <strong>Anno {year}</strong>
            </TableCell>
          </TableRow>
        );
        lastYearPlus = 0;
        lastYearMinus = 0;
        lastYear = year;
      }
      lastYearPlus += (t.sign === "+" ? Math.abs(t.amount) : 0);
      lastYearMinus += (t.sign === "-" ? Math.abs(t.amount) : 0);

      rr.push(
        // eslint-disable-next-line react/no-array-index-key
        <TableRow key={`transaction-${i}`}>
          <TableCell align='center'>
            {moment(t.date).format("DD/MM/YYYY")}
          </TableCell>
          <TableCell>
            {t.reason ? `${t.reason} - ` : ""}
            {t.friend ? `(${t.friend}) ` : ""}
            {t.description}
          </TableCell>
          <TableCell className={classes.tdAmount} >
            {t.sign === "+" || t.amount < 0
              ? Math.abs(t.amount).toFixed(2)
              : ""}
          </TableCell>
          <TableCell className={classes.tdAmount} >
            {t.sign === "-" && t.amount >= 0
              ? Math.abs(t.amount).toFixed(2)
              : ""}
          </TableCell>
          <TableCell className={classes.tdAmount} style={{ color: t.saldo < 0 ? "red" : "inherited" }}>
            {t.saldo >= 0 ? "+ " : ""}
            {t.saldo.toFixed(2)}
          </TableCell>
        </TableRow>
      );
    });

    rr.push(
      <TableRow key={`initialamt-${lastYear}`}>
        <TableCell align='center'>01/01/{lastYear}</TableCell>
        <TableCell>Saldo iniziale {lastYear}</TableCell>
        <TableCell />
        <TableCell />
        <TableCell className={classes.tdAmount}>
          0.00
        </TableCell>
      </TableRow>
    );

    rr.push(
      <TableRow key={`totals-${lastYear}`}>
        <TableCell colSpan='2' align='right'>
          <strong>Totale anno {lastYear}</strong>
        </TableCell>
        <TableCell className={classes.tdAmount}>
          <strong>{Math.abs(lastYearPlus).toFixed(2)}</strong>
        </TableCell>
        <TableCell className={classes.tdAmount}>
          <strong>{Math.abs(lastYearMinus).toFixed(2)}</strong>
        </TableCell>
        <TableCell />
      </TableRow>
    )

    return rr;
  }, [transactions, classes]);

  return (
    <Container maxWidth='xl'>
      <PageTitle title={`Dettaglio situazione contabile di ${user.nome || ""} ${user.cognome || ""}`}>
        <Button onClick={downloadXls} startIcon={<SaveIcon/>}>
          Esporta XLS
        </Button>
      </PageTitle>

      {jwt && jwt.sub && jwt.role === "A" ? (
        <Fab className={classes.fab} color='secondary' onClick={() => setShowDlg(true)}>
          <PlusIcon />
        </Fab>
      ) : null}

      <TableContainer>
        <Table size='small'>
          <TableHead>
            <TableRow>
              <TableCell>Data</TableCell>
              <TableCell>Descrizione</TableCell>
              <TableCell align='center'>Accrediti</TableCell>
              <TableCell align='center'>Addebiti</TableCell>
              <TableCell align='center'>Saldo</TableCell>
            </TableRow>
          </TableHead>

          <TableBody>
            {rows}
          </TableBody>

          <TableFooter className={classes.footercell}>
            <TableRow>
              <TableCell />
              <TableCell><strong>TOTALE</strong></TableCell>
              <TableCell className={classes.tdAmount}>
                <strong>{totals && !Number.isNaN(totals.accrediti) ? totals.accrediti.toFixed(2) : ''}</strong>
              </TableCell>
              <TableCell className={classes.tdAmount}>
                <strong>{totals && !Number.isNaN(totals.addebiti) ? totals.addebiti.toFixed(2) : ''}</strong>
              </TableCell>
              <TableCell />
            </TableRow>
          </TableFooter>
        </Table>
      </TableContainer>

      <NewTransactionDialog user={user} open={showDlg} onClose={dialogClosed} />
    </Container>
  );
}

const mapStateToProps = state => {
  return {
    authentication: state.authentication
  };
};

const mapDispatchToProps = {};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(withSnackbar(UserAccountingDetails));
