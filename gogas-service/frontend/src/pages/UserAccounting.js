import React, { useCallback, useEffect, useState, useMemo } from 'react';
import { connect } from "react-redux";
import { Container, Row, Col, Table, Alert, Button } from 'react-bootstrap';
import { getJson } from '../utils/axios_utils';
import Excel from '../excel-50.png';
import swal from 'sweetalert';
import _ from 'lodash';

const styles = {
  iconbtn: {
    cursor: 'pointer'
  },
  excelbtn: {
    width: '35px',
    height: '35px',
    cursor: 'pointer'
  },
  button: {
    marginRight: '15px',
    marginTop: '2px'
  }
}

function UserAccounting({history}) {
  const [total, setTotal] = useState(0);
  const [totals, setTotals] = useState([]);
  const [error, setError] = useState(undefined);

  const reload = useCallback(() => {
    getJson('/api/useraccounting/userTotals', {}).then(totals => {
      if (totals.error) {
        setError(totals.errorMessage);
      } else {
        let tot = 0;
        totals.data.forEach(t => tot += t.total);
        setTotals(totals.data);
        setTotal(tot);
      }
    });
  }, []);

  const downloadXls = useCallback(() => {
    swal('Esportazione dati', 'Selezionare il tipo di esportazione', {
      buttons: {
        cancel: 'Annulla',
        simple: 'Situaz. contabile',
        full: 'Situaz. contab. + dettaglio'
      }
    }).then(value => {
      if (value === 'simple')
        window.open('/api/useraccounting/exportUserTotals', '_blank');
      else if (value === 'full')
        window.open('/api/useraccounting/exportUserTotals?includeUsers=true', '_blank');
    });

  }, [total, totals])

  useEffect(() => {
    reload();
  }, []);

  const rows = useMemo(() => {
    if(!totals) return null;

    const tt = _.orderBy(totals, ['user.enabled', 'user.firstName', 'user.lastName'], ['desc', 'asc', 'asc']);
    return tt.map((t, i) => (
      <tr key={'user-' + t.user.id}>
        <td style={{ color: 'red', textAlign: 'center' }}>{t.user.enabled ? [] : <span className='fa fa-ban'></span>}</td>
        <td>{t.user.firstName + ' ' + t.user.lastName}</td>
        <td style={{ textAlign: 'right', color: t.total < 0 ? 'red' : 'inheried', width: '150px' }}>{t.total.toFixed(2)}</td>
        <td style={{ textAlign: 'center', width: '70px' }}>
          <span className='fa fa-edit' onClick={() => history.push('/useraccountingdetails?userId=' + t.user.id)} style={styles.iconbtn} />
        </td>
      </tr>
    )) 
  }, [totals]);

  return (
    <Container fluid>
      {error ?
        <Alert variant='danger'>{error}</Alert>
        : []}
      <Row>
        <Col>
          <h2>
            Situazione contabile utenti
            <img className='pull-right' src={Excel} alt='excel' title='Esporta dati su file Excel' style={styles.excelbtn} onClick={downloadXls} />
            <Button className='pull-right' size='sm' style={styles.button} variant='outline-primary'>Nuovo movimento</Button>
          </h2>
          <Table striped bordered hover size="sm">
            <thead>
              <tr>
                <th style={{ width: '30px' }}></th>
                <th>Utente</th>
                <th>Saldo</th>
                <th></th>
              </tr>
            </thead>

            <tbody>
              {rows}
            </tbody>

            <tfoot>
              <tr>
                <th></th>
                <th>TOTALE</th>
                <th style={{ textAlign: 'right', color: total < 0 ? 'red' : 'inherited' }}>{total.toFixed(2)}</th>
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
)(UserAccounting);
