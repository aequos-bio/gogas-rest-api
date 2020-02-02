import React, { useCallback, useMemo } from "react";
import {
  Container,
  Row,
  Col,
  Form,
  FormGroup,
  InputGroup,
  Button,
  Alert
} from "react-bootstrap";
import moment from 'moment-timezone';
import Jwt from 'jsonwebtoken';
import { Redirect } from "react-router-dom";
import { connect } from "react-redux";
import "../style/login.scss";
import { login } from "../store/actions";
import Logo from "../logo_aequos.png";

function Login({ authentication, location, history, info, ...props }) {
  const dologin = useCallback(
    e => {
      e.preventDefault();
      const username = e.target.username.value;
      const password = e.target.password.value;
      props.login(username, password);
      history.push("/");
      return false;
    },
    [props, history]
  );

  const validJwt = useMemo(() => {
    if (authentication && authentication.jwtToken) {
      const jwt = Jwt.decode(authentication.jwtToken);
			if (moment(jwt.exp * 1000).isBefore(moment())) {
				return false;
			}
			return true;
    } 
    return false;

  }, [authentication])

  return validJwt ? (
    <Redirect to={location.state ? location.state.from : "/"} />
  ) : (
    <Container>
      <Row style={{ marginTop: "100px" }}>
        <Col
          xs={{ span: 8, offset: 2 }}
          sm={{ span: 8, offset: 2 }}
          md={{ span: 8, offset: 2 }}
          lg={{ span: 6, offset: 3 }}
          style={{ padding: 0 }}
        >
          <Form className="form-signin" name="loginForm" onSubmit={dologin}>
            <div
              style={{
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                marginBottom: "30px"
              }}
            >
              <img src={Logo} width="50px" height="50px" alt="" />
              <h1 style={{ margin: "0px 15px" }}>
                {info["gas.nome"] ? info["gas.nome"].value : "GoGas"}
              </h1>
            </div>

            <FormGroup controlId="username">
              <Form.Label className="sr-only">Username</Form.Label>
              <InputGroup>
                <InputGroup.Prepend>
                  <InputGroup.Text id="inputGroupPrepend">
                    <span className="fa fa-user" />
                  </InputGroup.Text>
                </InputGroup.Prepend>
                <Form.Control
                  type="text"
                  placeholder="Username"
                  style={{ marginBottom: 0 }}
                />
              </InputGroup>
            </FormGroup>

            <FormGroup controlId="password">
              <Form.Label className="sr-only">Password</Form.Label>
              <InputGroup>
                <InputGroup.Prepend>
                  <InputGroup.Text id="inputGroupPrepend2">
                    <span className="fa fa-lock" />
                  </InputGroup.Text>
                </InputGroup.Prepend>
                <Form.Control
                  type="password"
                  placeholder="Password"
                  style={{ marginBottom: 0 }}
                />
              </InputGroup>
            </FormGroup>

            <Button variant="primary" type="submit">
              Login
            </Button>
          </Form>
        </Col>
        <Col
          xs={{ span: 8, offset: 2 }}
          sm={{ span: 8, offset: 2 }}
          md={{ span: 8, offset: 2 }}
          lg={{ span: 6, offset: 3 }}
          style={{
            marginTop: "10px",
            textAlign: "center",
            fontWeight: "lighter"
          }}
        >
          Copyright 2019-2020 Cooperativa Aequos
          <br />
          Tutti i diritti riservati
        </Col>
      </Row>

      {authentication.error_message ? (
        <Row style={{ marginTop: "10px" }}>
          <Col md={{ span: 6, offset: 3 }}>
            <Alert variant="danger">
              Username/password non validi o nessun diritto di accedere
            </Alert>
          </Col>
        </Row>
      ) : (
        []
      )}
    </Container>
  );
}

const mapStateToProps = state => {
  return {
    info: state.info,
    authentication: state.authentication
  };
};

const mapDispatchToProps = {
  login
};

export default connect(mapStateToProps, mapDispatchToProps)(Login);
