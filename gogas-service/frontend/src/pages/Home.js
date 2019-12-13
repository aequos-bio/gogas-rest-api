import React, { useState, useEffect } from "react";
import { connect } from "react-redux";
import { Container, Row, Col } from 'react-bootstrap';
import { getJson } from '../utils/axios_utils';

function Home({authentication}) {
	const [year, setYear ] = useState();

	useEffect(() => {
		getJson('/api/year/current', {}, authentication.jwtToken).then(y => setYear(y));
	}, []);

	return (
		<Container fluid style={{backgroundColor: 'white'}}>
			<Row>
				<Col>
					<h2>Home {year ? year.year : null}</h2>
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
)(Home);
