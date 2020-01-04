import React, { useCallback, useEffect, useState } from 'react';
import { connect } from "react-redux";
import { Container, Row, Col, Table, Alert } from 'react-bootstrap';
import { getJson } from '../utils/axios_utils';
import queryString from 'query-string';
import moment from 'moment-timezone';
import _ from 'lodash';
import Excel from '../excel-50.png';

const styles = {
  excelbtn: {
    width: '35px',
    height: '35px',
    cursor: 'pointer'
  }
}

function UserAccountingDetails({location}) {
  const search = queryString.parse(location.search);
  const [user, setUser] = useState({});
  const [transactions, setTransactions] = useState([]);
  const [totals, setTotals] = useState({ accrediti: 0, addebiti: 0 })
  const [error, setError] = useState(undefined);

  const reload = useCallback(() => {
    getJson('/api/user/' + search.userId, {})
      .then(user => {
        if (user.error) {
          setError(user.errorMessage);
        } else
          setUser(user)
      });

    getJson('/api/useraccounting/userTransactions?userId=' + search.userId, {})
      .then(transactions => {
        if (transactions.error) {
          setError(user.errorMessage);
          setTransactions([]);
          setTotals({ accrediti: 0, addebiti: 0 })
        } else {
          let tt = _.orderBy(transactions.data, 'date', 'desc');
          let saldo = 0;
          let accrediti = 0;
          let addebiti = 0;
          if (tt.length)
            for (let f = tt.length - 1; f >= 0; f--) {
              let m = tt[f].amount * (tt[f].sign === '-' ? -1 : 1);
              tt[f].saldo = saldo + m;
              saldo = tt[f].saldo;
              if (m < 0)
                addebiti += -1 * m;
              else
                accrediti += m;
            }
          setTransactions(tt);
          setTotals({ accrediti, addebiti })
        }
      });
  }, []);

  const downloadXls = useCallback(() => {
    window.open('/api/useraccounting/exportUserDetails?userId=' + user.idUtente, '_blank');
  }, [user, transactions])

  useEffect(() => {
    reload();
  }, []);

  return (
    <Container fluid>
      {error ?
        <Alert variant='danger'>{error}</Alert>
        : []}
      <Row>
        <Col>
          <h2>
            Dettaglio situazione contabile di {(user.nome || '') + ' ' + (user.cognome || '')}
            <img class='pull-right' src={Excel} alt='excel' title='Esporta dati su file Excel' style={styles.excelbtn} onClick={downloadXls} />
          </h2>
          <Table striped bordered hover size="sm">
            <thead>
              <tr>
                <th>Data</th>
                <th>Descrizione</th>
                <th style={{ textAlign: 'center' }}>Accrediti</th>
                <th style={{ textAlign: 'center' }}>Addebiti</th>
                <th style={{ textAlign: 'center' }}>Saldo</th>
              </tr>
            </thead>

            <tbody>
              {transactions ? transactions.map((t, i) => (
                <tr key={'transaction-' + i}>
                  <td style={{ textAlign: 'center', width: '120px' }}>
                    {moment(t.date).format('DD/MM/YYYY')}
                  </td>
                  <td>
                    {t.reason ? t.reason + ' - ' : ''}{t.friend ? '(' + t.friend + ') ' : ''}{t.description}
                  </td>
                  <td style={{ textAlign: 'right', width: '90px' }}>
                    {t.sign === '+' || t.amount < 0 ? Math.abs(t.amount).toFixed(2) : ''}
                  </td>
                  <td style={{ textAlign: 'right', width: '90px' }}>
                    {t.sign === '-' && t.amount >= 0 ? Math.abs(t.amount).toFixed(2) : ''}
                  </td>
                  <td style={{ textAlign: 'right', width: '90px', color: t.saldo < 0 ? 'red' : 'inherited' }}>
                    {t.saldo >= 0 ? '+ ' : ''}{t.saldo.toFixed(2)}
                  </td>
                </tr>
              )) : []}
            </tbody>

            <tfoot>
              <tr>
                <th></th>
                <th>TOTALE</th>
                <th style={{ textAlign: 'right' }}>{totals.accrediti.toFixed(2)}</th>
                <th style={{ textAlign: 'right' }}>{totals.addebiti.toFixed(2)}</th>
                <th></th>
              </tr>
            </tfoot>
          </Table>
        </Col>
      </Row>
    </Container>
  );
}

const mapStateToProps = state => {
  return {
    authentication: state.authentication
  };
};

const mapDispatchToProps = {
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(UserAccountingDetails);
