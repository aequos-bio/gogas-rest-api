import React, { useCallback } from 'react';
import { Container, Button, TextField } from '@material-ui/core';
import Alert from '@material-ui/lab/Alert';
import { makeStyles } from '@material-ui/core/styles';
import { Redirect } from 'react-router-dom';
import { useSelector } from 'react-redux';
import queryString from 'query-string';
import Logo from '../../assets/logo_aequos.png';
import useJwt from '../../hooks/JwtHooks';
import { login } from '../../store/features/authentication.slice';
import { RootState, useAppDispatch } from '../../store/store';
import { History, Location } from 'history';

const useStyles = makeStyles((theme) => ({
  container: {
    marginTop: '100px',
  },
  title: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: '30px',
  },
  buttons: {
    margin: theme.spacing(2, 0),
  },
  copyright: {
    textAlign: 'center',
    color: theme.palette.grey[500],
  },
}));

interface Props {
  location: Location<any>;
  history: History;
}

const Login: React.FC<Props> = ({ location, history }) => {
  const classes = useStyles();
  const search = queryString.parse(location.search);
  const info = useSelector((state: RootState) => state.info);
  const authentication = useSelector((state: RootState) => state.authentication);
  const dispatch = useAppDispatch();
  const validJwt = useJwt();

  const dologin = useCallback(
    (e) => {
      e.preventDefault();
      const username = e.target.username.value;
      const password = e.target.password.value;
      dispatch(login({ username, password }));
      history.push('/');
      return false;
    },
    [dispatch, history],
  );

  return validJwt ? (
    <Redirect to={location.state ? location.state.from : '/'} />
  ) : (
    <Container className={classes.container} maxWidth='xs'>
      {authentication.running ? (
        'loading ...'
      ) : (
        <form className='' noValidate autoComplete='off' onSubmit={dologin}>
          <div className={classes.title}>
            <img src={Logo} width='50px' height='50px' alt='' />
            <h1 style={{ margin: '0px 15px' }}>
              {info['gas.nome'] || 'GoGas'}
            </h1>
          </div>

          <TextField
            name='username'
            label='Nome utente'
            margin='normal'
            fullWidth
          />
          <TextField
            name='password'
            label='Password'
            type='password'
            margin='normal'
            fullWidth
          />

          <div className={classes.buttons}>
            <Button
              variant='contained'
              size='medium'
              color='primary'
              type='submit'
              style={{ color: 'white' }}
              fullWidth
            >
              Login
            </Button>
          </div>
        </form>
      )}

      {authentication.running ? null : (
        <div className={classes.copyright}>
          Copyright 2019-2020 Cooperativa Aequos
          <br />
          Tutti i diritti riservati
        </div>
      )}
      {authentication.error_message ? (
        <Alert severity='error'>
          Username/password non validi o nessun diritto di accedere
        </Alert>
      ) : null}
      {'disconnect' in search ? (
        <Alert severity='success'>Ti sei disconnesso</Alert>
      ) : null}
    </Container>
  );
};

export default Login;
