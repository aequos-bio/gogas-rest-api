import React, { useState, useEffect } from 'react';
import { connect } from 'react-redux';
import { Container, Typography } from '@material-ui/core';
import { apiGetJson } from '../utils/axios_utils';

function Home() {
  const [year, setYear] = useState();

  useEffect(() => {
    apiGetJson('/api/year/current', {}).then(y => setYear(y));
  }, []);

  return (
    <Container maxWidth={false}>
      <Typography variant="h4" component="h4">
        Home {year ? year.year : null}
      </Typography>
    </Container>
  );
}

const mapStateToProps = state => {
  return {
    authentication: state.authentication,
  };
};

const mapDispatchToProps = {};

export default connect(mapStateToProps, mapDispatchToProps)(Home);
