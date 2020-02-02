/* eslint-disable jsx-a11y/control-has-associated-label */
/* eslint-disable jsx-a11y/no-noninteractive-element-interactions */
/* eslint-disable jsx-a11y/no-static-element-interactions */
/* eslint-disable jsx-a11y/click-events-have-key-events */
import React, { useCallback, useEffect, useState, useMemo } from "react";
import { connect } from "react-redux";
import { Container, Row, Col, Table, Alert, Button } from "react-bootstrap";
import swal from "sweetalert";
import _ from "lodash";
import {createUseStyles} from 'react-jss'
import { getJson } from "../../utils/axios_utils";
import Excel from "../../excel-50.png";
import AddTransactionDialog from "./components/AddTransactionDialog";

const useStyles = createUseStyles(() => ({
  excelbtn: {
    width: "35px",
    height: "35px",
    cursor: "pointer"
  },
  button: {
    marginRight: "15px",
    marginTop: "2px"
  },
  iconbtn: {
    cursor: "pointer"
  },
  tdIcon: {
    color: "red", 
    textAlign: "center",
    width: '30px',
  },
  tdAmount: {
    textAlign: "right",
    width: "90px",
  },
  tdLink: {
    textAlign: "center", 
    width: "70px"  
  },
}));

function UserAccounting({ history }) {
  const classes = useStyles();
  const [total, setTotal] = useState(0);
  const [totals, setTotals] = useState([]);
  const [error, setError] = useState(undefined);
  const [showDlg, setShowDlg] = useState(false);

  const reload = useCallback(() => {
    getJson("/api/useraccounting/userTotals", {}).then(tt => {
      if (tt.error) {
        setError(tt.errorMessage);
      } else {
        let tot = 0;
        tt.data.forEach(t => {
          tot += t.total;
        });
        setTotals(tt.data);
        setTotal(tot);
      }
    });
  }, []);

  const downloadXls = useCallback(() => {
    swal("Esportazione dati", "Selezionare il tipo di esportazione", {
      buttons: {
        cancel: "Annulla",
        simple: "Situaz. contabile",
        full: "Situaz. contab. + dettaglio"
      }
    }).then(value => {
      if (value === "simple")
        window.open("/api/useraccounting/exportUserTotals", "_blank");
      else if (value === "full")
        window.open(
          "/api/useraccounting/exportUserTotals?includeUsers=true",
          "_blank"
        );
    });
  }, []);

  useEffect(() => {
    reload();
  }, [reload]);

  const rows = useMemo(() => {
    if (!totals) return null;

    const tt = _.orderBy(
      totals,
      ["user.enabled", "user.firstName", "user.lastName"],
      ["desc", "asc", "asc"]
    );
    return tt.map(t => (
      <tr key={`user-${t.user.id}`}>
        <td className={classes.tdIcon}>
          {t.user.enabled ? [] : <span className="fa fa-ban" />}
        </td>
        <td>{`${t.user.firstName} ${t.user.lastName}`}</td>
        <td className={classes.tdAmount}
          style={{color: t.total < 0 ? "red" : "inheried"}}
        >
          {t.total.toFixed(2)}
        </td>
        <td className={classes.tdLink}>
          <span
            className={`${classes.iconbtn} fa fa-edit`}
            onClick={() =>
              history.push(`/useraccountingdetails?userId=${t.user.id}`)
            }
          />
        </td>
      </tr>
    ));
  }, [totals, history, classes]);

  return (
    <Container fluid>
      {error ? <Alert variant="danger">{error}</Alert> : []}
      <Row>
        <Col>
          <h2>
            Situazione contabile utenti
            <img
              className={`${classes.excelbtn} pull-right`}
              src={Excel}
              alt="excel"
              title="Esporta dati su file Excel"
              onClick={downloadXls}
            />
            <Button
              className={`${classes.button} pull-right`}
              size="sm"
              variant="outline-primary"
              onClick={() => setShowDlg(true)}
            >
              Nuovo movimento
            </Button>
          </h2>
          <Table striped bordered hover size="sm">
            <thead>
              <tr>
                <th className={classes.tdIcon}/>
                <th>Utente</th>
                <th>Saldo</th>
                <th />
              </tr>
            </thead>

            <tbody>{rows}</tbody>

            <tfoot>
              <tr>
                <th />
                <th>TOTALE</th>
                <th className={classes.tdAmount}
                  style={{color: total < 0 ? "red" : "inherited"}}
                >
                  {total.toFixed(2)}
                </th>
                <th />
              </tr>
            </tfoot>
          </Table>
        </Col>
      </Row>

      <AddTransactionDialog
        title="Nuovo movimento"
        show={showDlg}
        onClose={refresh => {
          setShowDlg(false);
          if (refresh) reload();
        }}
      />
    </Container>
  );
}

const mapStateToProps = state => {
  return {
    authentication: state.authentication
  };
};

const mapDispatchToProps = {};

export default connect(mapStateToProps, mapDispatchToProps)(UserAccounting);
