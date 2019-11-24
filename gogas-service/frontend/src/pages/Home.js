import React from "react";
import { connect } from "react-redux";
import { Container, Row, Col } from 'react-bootstrap';

function Home(props) {
	return (
		<Container fluid>
			<Row>
				<Col>
					<h2>Home</h2>
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
