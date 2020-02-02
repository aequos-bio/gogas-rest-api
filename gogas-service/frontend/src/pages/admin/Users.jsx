/* eslint-disable jsx-a11y/no-static-element-interactions */
/* eslint-disable jsx-a11y/click-events-have-key-events */
/* eslint-disable jsx-a11y/control-has-associated-label */
import React, { useCallback, useEffect, useState } from "react";
import { connect } from "react-redux";
import { Container, Row, Col, Table, Alert } from "react-bootstrap";
import _ from "lodash";
import {createUseStyles} from 'react-jss'
import swal from "sweetalert";
import { getJson } from "../../utils/axios_utils";

const useStyles = createUseStyles(() => ({
  iconbtn: {
    cursor: "pointer"
  },
  tdIcon: {
    color: "red", 
    textAlign: "center",
    width: '30px',
  },
  tdButtons: {
    fontSize: '130%', 
    textAlign: 'center',
  }
}));

function Users({ info }) {
  const classes = useStyles();
  const [users, setUsers] = useState([]);
  const [error, setError] = useState(undefined);
  const sort = info["visualizzazione.utenti"]
    ? info["visualizzazione.utenti"].value
    : "NC";

  const reload = useCallback(() => {
    getJson("/api/user/list", {}).then(uu => {
      if (uu.error) {
        setError(uu.errorMessage);
      } else {
        setUsers(
          _.orderBy(
            uu,
            [
              "attivo",
              sort === "NC" ? "nome" : "cognome",
              sort === "NC" ? "cognome" : "nome"
            ],
            ["desc", "asc", "asc"]
          )
        );
      }
    });
  }, [sort]);

  useEffect(() => {
    reload();
  }, [reload]);

  const mapRoles = useCallback(user => {
    if (user.ruolo === "U") return "Utente";
    if (user.ruolo === "A") return "Amministratore";
    if (user.ruolo === "S") return "Amico";
    return user.ruolo;
  }, []);

  const mapRef = useCallback(
    uid => {
      let friend = "";
      if (uid)
        users.forEach(u => {
          if (u.idUtente === uid)
            friend =
              sort === "NC"
                ? `${u.nome} ${u.cognome}`
                : `${u.cognome} ${u.nome}`;
        });
      return friend;
    },
    [users, sort]
  );

  const editUser = useCallback((id) => {
    console.warn(`Edit user ${id}`);
    swal('Sorry', 'Non implementato')
  }, []);
  
  const deleteUser = useCallback((id) => {
    console.warn(`Delete user ${id}`);
    swal('Sorry', 'Non implementato')
  }, []);

  return (
    <Container fluid>
      {error ? <Alert variant="danger">{error}</Alert> : []}
      <Row>
        <Col>
          <Table striped bordered hover size="sm">
            <thead>
              <tr>
                <th className={classes.tdLink}/>
                <th>{sort === "NC" ? "Nome" : "Cognome"}</th>
                <th>{sort === "NC" ? "Cognome" : "Nome"}</th>
                <th>Username</th>
                <th>Email</th>
                <th>Ruolo</th>
                <th>Ref amico</th>
                <th/>
              </tr>
            </thead>

            <tbody>
              {users
                ? users.map(u => (
                    <tr key={`user-${u.idUtente}`}>
                      <td className={classes.tdIcon}>
                        {u.attivo ? (
                          ""
                        ) : (
                          <span className="fa fa-ban"/>
                        )}
                      </td>
                      <td>{sort === "NC" ? u.nome : u.cognome}</td>
                      <td>{sort === "NC" ? u.cognome : u.nome}</td>
                      <td>{u.username}</td>
                      <td>{u.email}</td>
                      <td>{mapRoles(u)}</td>
                      <td>{mapRef(u.idReferente)}</td>
                      <td className={classes.tdButtons}>
                        <span className={`${classes.iconbtn} fa fa-edit`} onClick={() => { editUser(u.idUtente) }} />
                        {'   '}
                        <span className={`${classes.iconbtn} fa fa-remove`} onClick={() => { deleteUser(u.idUtente) }}/>
                      </td>
                    </tr>
                  ))
                : []}
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

const mapDispatchToProps = {};

export default connect(mapStateToProps, mapDispatchToProps)(Users);
