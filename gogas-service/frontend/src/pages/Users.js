import React, { useCallback, useEffect, useState } from 'react';
import { connect } from "react-redux";
import { Container, Row, Col, Table, Alert } from 'react-bootstrap';
import _ from 'lodash';
import { getJson } from '../utils/axios_utils';

function Users({authentication, info}) {
  const [users, setUsers] = useState([]);
  const [error, setError] = useState(undefined);
  const sort = info['visualizzazione.utenti'] ? info['visualizzazione.utenti'].value : 'NC';

  const reload = useCallback(() => {
    getJson('/api/user/list', {}, authentication.jwtToken).then(users => {
      if (users.error) {
        setError(users.errorMessage);
      } else {
        setUsers( _.orderBy(users, ['attivo', sort==='NC' ? 'nome' : 'cognome', sort==='NC' ? 'cognome' : 'nome'], ['desc', 'asc', 'asc']) );
      }
    });
  }, [authentication, info]);

  useEffect(() => {
    reload();
  }, []);

  const mapRoles = useCallback((user) => {
    if (user.ruolo === 'U')
      return 'Utente';
    else if (user.ruolo === 'A')
      return 'Amministratore';
    else if (user.ruolo === 'S')
      return 'Amico';
    else
      return user.ruolo;
  }, []);

  const mapRef = useCallback((uid) => {
    let friend = '';
    if (uid)
      users.forEach(u => {
        if (u.idUtente === uid)
          friend = sort === 'NC' ? u.nome + ' ' + u.cognome : u.cognome + ' ' + u.nome;
      });
    return friend;
  }, [users]);
console.log('users', users)
  return (
    <Container fluid style={{backgroundColor: 'white'}}>
      {error ?
        <Alert variant='danger'>{error}</Alert>
        : []}
      <Row>
        <Col>
          <Table striped bordered hover size="sm">
            <thead>
              <tr>
                <th>{sort === 'NC' ? 'Nome' : 'Cognome'}</th>
                <th>{sort === 'NC' ? 'Cognome' : 'Nome'}</th>
                <th>Username</th>
                <th>Email</th>
                <th>Ruolo</th>
                <th>Ref amico</th>
                <th style={{ width: '50px' }}>Inattivo</th>
                {/*                <th style={{ width: '70px' }} /> */}
              </tr>
            </thead>

            <tbody>
              {users ? users.map((u, i) => (
                <tr key={'user-' + u.idUtente}>
                  <td>{sort === 'NC' ? u.nome : u.cognome}</td>
                  <td>{sort === 'NC' ? u.cognome : u.nome}</td>
                  <td>{u.username}</td>
                  <td>{u.email}</td>
                  <td>{mapRoles(u)}</td>
                  <td>{mapRef(u.idReferente)}</td>
                  <td style={{ textAlign: 'center' }}>
                    {/*<Switch disabled={true} checked={u.active} />*/}
                    {u.attivo ? '' : <span className='fa fa-ban' style={{ color: 'red' }}></span>}
                  </td>
                  {/*<td style={{ fontSize: '130%', textAlign: 'center' }}>
                    <span className='fa fa-edit' onClick={() => { console.log('Edit user ' + u.id) }} style={styles.iconbtn} />
                    {'   '}
                    <span className='fa fa-remove' onClick={() => { console.log('Delete user ' + u.id) }} style={styles.iconbtn} />
                  </td>*/}
                </tr>
              )) : []}
            </tbody>
          </Table>
        </Col>
      </Row>
    </Container>
  );
}

const mapStateToProps = state => {
  return {
    authentication: state.authentication,
    info: state.info
  };
};

const mapDispatchToProps = {
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(Users);
