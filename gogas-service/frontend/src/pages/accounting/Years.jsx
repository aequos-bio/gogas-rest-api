/* eslint-disable no-nested-ternary */
/* eslint-disable jsx-a11y/control-has-associated-label */
import React, { useState, useEffect, useMemo, useCallback } from "react";
import { connect } from "react-redux";
import { Container, Row, Col, Table, Alert, Button } from "react-bootstrap";
import _ from "lodash";
import { getJson } from "../../utils/axios_utils";

const Years = () => {
  const [error, setError] = useState(undefined);
  const [years, setYears] = useState([]);
  
  const reload = useCallback(() => {
    getJson("/api/year/all", {}).then(yy => {
      if (yy.error) {
        setError(yy.errorMessage);
      } else {
        setYears(_.orderBy(yy.data, 'year', 'desc'));
      }
    });
  }, []);

  useEffect(() => {
    reload();
  }, [reload]);

  const rows = useMemo(() => {
    return years.map((y,i) => (
      <tr>
        <td>
          {y.year}
        </td>
        <td style={{color:y.closed?'red':'black'}}>
          {y.closed ? "Chiuso" : "Aperto"}
        </td>
        <td style={{textAlign:'center'}}>
          {y.closed || i===0 ? (i===0 ? <i>in corso</i> : null) : 
            <Button size="sm" variant="outline-primary">
              Chiudi
            </Button>
          }
        </td>
      </tr>
    ));
  }, [years]);

  return (
    <Container fluid>
      {error ? <Alert variant="danger">{error}</Alert> : []}
      <Row>
        <Col>
          <h2>
            Anni contabili
          </h2>
        </Col>
      </Row>

      <Row>
        <Col lg={6} md={6} sm={12}>
          <Table striped bordered hover size="sm">
            <thead>
              <tr>
                <th>Anno</th>
                <th>Stato</th>
                <th style={{width:'30%'}}/>
              </tr>
            </thead>

            <tbody>
              {rows}
            </tbody>
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

const mapDispatchToProps = {};

export default connect(
  mapStateToProps, 
  mapDispatchToProps
)(Years);
