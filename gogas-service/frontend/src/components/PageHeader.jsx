import React, { useMemo } from 'react';
import { Navbar, Image, Nav, NavDropdown } from 'react-bootstrap';
import { Link } from 'react-router-dom';
import { connect } from 'react-redux';
import Jwt from 'jsonwebtoken';
import { LinkContainer } from 'react-router-bootstrap';
import { logout } from '../store/actions';
import Logo from '../logo_aequos.png';

const styles = {
  navbar: {
    zIndex: '100',
    position: 'sticky',
    top: '0',
    padding: '5px 16px',
    marginBottom: '20px',
    backgroundColor: 'white'
  },
  brand: {
    display: 'flex',
    alignItems: 'center',
    padding: '0px',
  },
  icon: {
    width: '40px',
    height: '40px',
    marginRight: '10px',
  },
  version: {
    fontSize: '50%',
  },
  title: {
    fontSize: '150%',
    marginRight: '10px',
  },
  pagetitle: {
    marginLeft: '30px',
    flexGrow: '1',
    fontSize: '150%',
  },
  username: {
    color: 'var(--gray)',
    padding: '0px',
    textAlign: 'right',
  },
  disconnect: {
    padding: '0px',
  },
};

function PageHeader({ authentication, info, history, ...props }) {
  const jwt = useMemo(() => {
    if (authentication.jwtToken) {
      const j = Jwt.decode(authentication.jwtToken);
      return j;
    }
    return null;
  }, [authentication]);

  return (
    <Navbar bg="primary" variant="dark" expand="lg" style={styles.navbar}>
      <Navbar.Brand href="/" as={Link} to="/" style={styles.brand}>
        <Image src={Logo} alt="" style={styles.icon} />
        <span style={styles.title}> {info['gas.nome'] ? info['gas.nome'].value : 'GoGas'}</span>
      </Navbar.Brand>
      <Navbar.Toggle aria-controls="basic-navbar-nav" />
      <Navbar.Collapse>
        <Nav className="mr-auto">
          
          <RestrictedMenu 
            title='Contabilità' 
            restrictedTo='A' 
            items={[
              { title: 'Anni contabili', href: '/years' },
              { title: 'Situazione utenti', href: '/useraccounting' }
            ]} 
            jwt={jwt} 
          />
          
          <RestrictedMenu 
            title='Gestione' 
            restrictedTo='A' 
            items={[
              { title: 'Utenti', href: '/users' }
            ]} 
            jwt={jwt} 
          />

        </Nav>

        <Navbar.Text>
          <NavDropdown alignRight title={<span style={{ marginLeft: '-16px' }}>{jwt ? `${jwt.firstname} ${jwt.lastname}` : ''}</span>}>
            {jwt ?
              <LinkContainer to={`/useraccountingdetails?userId=${jwt.id}`} style={{ color: 'var(--gray)' }} >
                <NavDropdown.Item>Saldo</NavDropdown.Item>
              </LinkContainer>
            : []}
            <NavDropdown.Item href='#' style={{ color: 'var(--gray)' }} onClick={() => {props.logout(); history.push('/login')}}>Disconnetti</NavDropdown.Item>
          </NavDropdown>
        </Navbar.Text>
      </Navbar.Collapse>
    </Navbar>
  );
}

function RestrictedMenu({ title, restrictedTo, items, jwt }) {
  let enable = false;

  if (jwt && jwt.sub && jwt.role) {
    if (Array.isArray(restrictedTo)) {
      restrictedTo.forEach(r => {
        if (jwt.role === r)
          enable = true;
      });
    } else if (jwt.role === restrictedTo) {
      enable = true;
    }
  }

  return (
    enable ?
      <NavDropdown title={title}>
        {items ? items.map((item, i) => (
          // eslint-disable-next-line react/no-array-index-key
          <LinkContainer to={item.href} key={`menuitem-${title}-${i}`} >
            <NavDropdown.Item >{item.title}</NavDropdown.Item>
          </LinkContainer>
        )) : null}
      </NavDropdown>
      : []
  );
}

const mapStateToProps = state => {
  return {
    info: state.info,
    authentication: state.authentication,
  };
};

const mapDispatchToProps = {
  logout,
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(PageHeader);
