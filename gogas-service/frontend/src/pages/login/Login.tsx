import React, { useCallback, useState } from 'react';
import { Container, Button, TextField } from '@material-ui/core';
import Alert from '@material-ui/lab/Alert';
import { makeStyles } from '@material-ui/core/styles';
import { Redirect } from 'react-router-dom';
import queryString from 'query-string';
import Logo from '../../components/Logo';
import useJwt from '../../hooks/JwtHooks';
import { login } from '../../store/features/authentication.slice';
import { useAppDispatch, useAppSelector } from '../../store/store';
import { History, Location } from 'history';
import ResetPasswordDialog from './ResetPasswordDialog'

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
    fontSize: 'small',
    marginBottom: '20px',
  },
  reset: {
    textAlign: 'center',
    color: theme.palette.grey[500],
    margin: '30px 0px',
  },
  resetLink: {
    color: 'blue',
    cursor: 'pointer'
  },
}));

interface Props {
  location: Location<any>;
  history: History;
}

const Login: React.FC<Props> = ({ location, history }) => {
  const classes = useStyles();
  const search = queryString.parse(location.search);
  const info = useAppSelector((state) => state.info);
  const authentication = useAppSelector((state) => state.authentication);
  const [showDlg, setShowDlg] = useState(false);
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

  const resetPassword = useCallback(() => {
    setShowDlg(true);
  }, []);

  const closeResetPassword = useCallback(() => {
    setShowDlg(false);
  }, []);

  return validJwt ? (
    <Redirect to={location.state ? location.state.from : '/'} />
  ) : (
    <Container className={classes.container} maxWidth='xs'>
      {authentication.running ? (
        'loading ...'
      ) : (
        <form className='' noValidate autoComplete='off' onSubmit={dologin}>
          <div className={classes.title}>
            <Logo height='50px' />
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

      <div className={classes.reset}>
        Hai dimenticato la password? <a onClick={resetPassword} className={classes.resetLink}> Clicca qui per il reset.</a>
      </div>

      {authentication.running ? null : (
        <div className={classes.copyright}>
          Copyright 2019-2020 Cooperativa Aequos
          <br />
          Tutti i diritti riservati
        </div>
      )}
      {authentication.error_message ? (
        <Alert severity='error'>
          {authentication.error_message}
        </Alert>
      ) : null}
      {'disconnect' in search ? (
        <Alert severity='success'>Ti sei disconnesso</Alert>
      ) : null}
      <ResetPasswordDialog open={showDlg} handleClose={closeResetPassword} />
    </Container>
  );
};

export default Login;
