import React, { useCallback } from 'react';
import { Container, Row, Col, Form, FormGroup, Button, Alert } from 'react-bootstrap';
import '../style/login.scss';
import { Redirect } from 'react-router-dom';
import { connect } from 'react-redux';
import { login } from '../store/actions';
import Logo from '../logo_aequos.png';

function Login({authentication, location, login, history, info}) {
  if (authentication && authentication.jwtToken)
    return <Redirect to={location.state ? location.state.from : '/'} />;
  
  const dologin = useCallback(e => {
    e.preventDefault();
    const username = e.target.username.value;
    const password = e.target.password.value;
    login(username, password);
    history.push('/');
    return false;
  }, [login, history]);

  return (
    <Container>
      <Row className="title-row">
        <Col md={{ span: 6, offset: 3 }} style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: '15px' }}>
          <img src={Logo} width="50px" height="50px" alt="" />
          <h1 style={{ margin: '0px 15px' }}>
            {info['gas.nome'] ? info['gas.nome'].value : 'GoGas'}
          </h1>
        </Col>
      </Row>

      <Row>
        <Col md={{ span: 6, offset: 3 }}>
          <Form className="form-signin" name="loginForm" onSubmit={dologin}>
            <h2 className="form-signin-heading" style={{ marginTop: '0px' }}>
              Autenticazione
            </h2>

            <FormGroup controlId="username" style={{ marginBottom: 0 }}>
              <Form.Label className="sr-only">Username</Form.Label>
              <Form.Control type="text" placeholder="Username" style={{ marginBottom: 0 }} />
            </FormGroup>

            <FormGroup controlId="password">
              <Form.Label className="sr-only">Password</Form.Label>
              <Form.Control type="password" placeholder="Password" />
            </FormGroup>

            <Button variant="outline-primary" type="submit">
              Login
            </Button>
          </Form>
        </Col>
      </Row>

      {authentication.error_message ?
        <Row style={{ marginTop: '10px' }}>
          <Col md={{ span: 6, offset: 3 }}>
            <Alert variant='danger'>
              Username/password non validi o nessun diritto di accedere
            </Alert>
          </Col>
        </Row>
        : []}
    </Container>
  );
}

const mapStateToProps = state => {
  return {
    info: state.info,
    authentication: state.authentication,
  };
};

const mapDispatchToProps = {
  login,
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(Login);
