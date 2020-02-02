/* eslint-disable jsx-a11y/control-has-associated-label */
/* eslint-disable jsx-a11y/no-noninteractive-element-interactions */
/* eslint-disable jsx-a11y/click-events-have-key-events */
import React, { useCallback, useEffect, useState, useMemo } from "react";
import { connect } from "react-redux";
import { Container, Row, Col, Table, Alert, Button } from "react-bootstrap";
import queryString from "query-string";
import moment from "moment-timezone";
import _ from "lodash";
import Jwt from "jsonwebtoken";
import { getJson } from "../../utils/axios_utils";
import Excel from "../../excel-50.png";
import AddTransactionDialog from "./components/AddTransactionDialog";

const styles = {
  excelbtn: {
    width: "35px",
    height: "35px",
    cursor: "pointer"
  },
  button: {
    marginRight: "15px",
    marginTop: "2px"
  }
};

function UserAccountingDetails({ authentication, location }) {
  const search = queryString.parse(location.search);
  const [user, setUser] = useState({});
  const [transactions, setTransactions] = useState([]);
  const [totals, setTotals] = useState({ accrediti: 0, addebiti: 0 });
  const [error, setError] = useState(undefined);
  const [showDlg, setShowDlg] = useState(false);

  const reload = useCallback(() => {
    getJson(`/api/user/${search.userId}`, {}).then(u => {
      if (u.error) {
        setError(u.errorMessage);
      } else setUser(u);
    });

    getJson(
      `/api/useraccounting/userTransactions?userId=${search.userId}`,
      {}
    ).then(t => {
      if (t.error) {
        setError(user.errorMessage);
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
  }, [search.userId, user.errorMessage]);

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
    if (!transactions) return [];
    const rr = [];
    let lastYear = '';
    let lastYearPlus;
    let lastYearMinus;

    transactions.forEach((t, i) => {
      const year = moment(t.date).format("YYYY");
      if (year!==lastYear) {

        if (lastYearPlus!==undefined || lastYearMinus!==undefined) {
          rr.push(
            <tr key={`initialamt-${lastYear}`}>
              <td style={{textAlign:'center'}}>01/01/{lastYear}</td>
          <td>Saldo iniziale {lastYear}</td>
              <td/>
              <td/>
              <td
            style={{
              textAlign: "right",
              width: "90px",
              color: t.saldo < 0 ? "red" : "inherited"
            }}
          >
            {t.saldo >= 0 ? "+ " : ""}
            {t.saldo.toFixed(2)}
          </td>
            </tr>    
          );

          rr.push(
            <tr key={`totals-${lastYear}`}>
              <td colSpan='2' style={{textAlign: 'right'}}>
                <strong>Totale anno {lastYear}</strong>
              </td>
              <td style={{textAlign:'right'}}>
                <strong>{Math.abs(lastYearPlus).toFixed(2)}</strong>
              </td>
              <td style={{textAlign:'right'}}>
                <strong>{Math.abs(lastYearMinus).toFixed(2)}</strong>
              </td>
              <td/>
            </tr>
          )
        }
          rr.push(
            <tr key={`year-${year}`}>
              <td colSpan='5'>
                <strong>Anno {year}</strong>
              </td>
            </tr>
          );
        lastYearPlus = 0;
        lastYearMinus = 0;  
        lastYear = year;
      }
      lastYearPlus += (t.sign === "+" ? Math.abs(t.amount) : 0);
      lastYearMinus += (t.sign === "-" ? Math.abs(t.amount) : 0);

      rr.push(
        // eslint-disable-next-line react/no-array-index-key
        <tr key={`transaction-${i}`}>
          <td style={{ textAlign: "center", width: "120px" }}>
            {moment(t.date).format("DD/MM/YYYY")}
          </td>
          <td>
            {t.reason ? `${t.reason} - ` : ""}
            {t.friend ? `(${t.friend}) ` : ""}
            {t.description}
          </td>
          <td style={{ textAlign: "right", width: "90px" }}>
            {t.sign === "+" || t.amount < 0
              ? Math.abs(t.amount).toFixed(2)
              : ""}
          </td>
          <td style={{ textAlign: "right", width: "90px" }}>
            {t.sign === "-" && t.amount >= 0
              ? Math.abs(t.amount).toFixed(2)
              : ""}
          </td>
          <td
            style={{
              textAlign: "right",
              width: "90px",
              color: t.saldo < 0 ? "red" : "inherited"
            }}
          >
            {t.saldo >= 0 ? "+ " : ""}
            {t.saldo.toFixed(2)}
          </td>
        </tr>
      );
    });
    
    rr.push(
      <tr key={`initialamt-${lastYear}`}>
        <td style={{textAlign:'center'}}>01/01/{lastYear}</td>
        <td>Saldo iniziale {lastYear}</td>
        <td/>
        <td/>
        <td style={{textAlign: "right"}}>
          0.00
        </td>
      </tr>
    );

    rr.push(
      <tr key={`totals-${lastYear}`}>
        <td colSpan='2' style={{textAlign: 'right'}}>
          <strong>Totale anno {lastYear}</strong>
        </td>
        <td style={{textAlign:'right'}}>
          <strong>{Math.abs(lastYearPlus).toFixed(2)}</strong>
        </td>
        <td style={{textAlign:'right'}}>
          <strong>{Math.abs(lastYearMinus).toFixed(2)}</strong>
        </td>
        <td/>
      </tr>
    )

    return rr;
  }, [transactions]);

  return (
    <Container fluid>
      {error ? <Alert variant="danger">{error}</Alert> : []}
      <Row>
        <Col>
          <h2>
            Dettaglio situazione contabile di{" "}
            {`${user.nome || ""} ${user.cognome || ""}`}
            <img
              className="pull-right"
              src={Excel}
              alt="excel"
              title="Esporta dati su file Excel"
              style={styles.excelbtn}
              onClick={downloadXls}
            />
            {jwt && jwt.sub && jwt.role === "A" ? (
              <Button
                className="pull-right"
                size="sm"
                style={styles.button}
                variant="outline-primary"
                onClick={() => setShowDlg(true)}
              >
                Nuovo movimento
              </Button>
            ) : null}
          </h2>
          <Table striped bordered hover size="sm">
            <thead>
              <tr>
                <th>Data</th>
                <th>Descrizione</th>
                <th style={{ textAlign: "center" }}>Accrediti</th>
                <th style={{ textAlign: "center" }}>Addebiti</th>
                <th style={{ textAlign: "center" }}>Saldo</th>
              </tr>
            </thead>

            <tbody>
              {rows}
            </tbody>

            <tfoot>
              <tr>
                <th />
                <th>TOTALE</th>
                <th style={{ textAlign: "right" }}>
                  {totals.accrediti.toFixed(2)}
                </th>
                <th style={{ textAlign: "right" }}>
                  {totals.addebiti.toFixed(2)}
                </th>
                <th />
              </tr>
            </tfoot>
          </Table>
        </Col>
      </Row>
      <AddTransactionDialog
        title="Nuovo movimento"
        user={user}
        show={showDlg}
        onClose={dialogClosed}
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

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(UserAccountingDetails);
