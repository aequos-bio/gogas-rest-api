/* eslint-disable no-nested-ternary */
/* eslint-disable jsx-a11y/control-has-associated-label */
import React, { useState, useEffect, useMemo, useCallback } from "react";
import { connect } from "react-redux";
import {
  Container,
  Button,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableCell,
  TableBody
} from "@material-ui/core";
import { withSnackbar } from "notistack";
import _ from "lodash";
import { getJson } from "../../utils/axios_utils";
import PageTitle from "../../components/PageTitle";

const Years = ({ enqueueSnackbar }) => {
  const [years, setYears] = useState([]);

  const reload = useCallback(() => {
    getJson("/api/year/all", {}).then(yy => {
      if (yy.error) {
        enqueueSnackbar(yy.errorMessage, { variant: "error" });
      } else {
        setYears(_.orderBy(yy.data, "year", "desc"));
      }
    });
  }, [enqueueSnackbar]);

  useEffect(() => {
    reload();
  }, [reload]);

  const closeYear = useCallback(
    y => {
      console.warn("closing year", y);
      enqueueSnackbar("Funzione non implementata!", { variant: "error" });
    },
    [enqueueSnackbar]
  );

  const rows = useMemo(() => {
    return years.map((y, i) => (
      <TableRow key={`year-${y.year}`} hover>
        <TableCell>{y.year}</TableCell>
        <TableCell style={{ color: y.closed ? "red" : "black" }}>
          {y.closed ? "Chiuso" : `Aperto${i === 0 ? ", in corso" : ""}`}
        </TableCell>
        <TableCell align="right">
          {y.closed || i === 0 ? null : (
            <Button
              variant="outlined"
              size="small"
              color="secondary"
              onClick={() => closeYear(y)}
            >
              Chiudi
            </Button>
          )}
        </TableCell>
      </TableRow>
    ));
  }, [years, closeYear]);

  return (
    <Container maxWidth={false}>
      <PageTitle title="Anni contabili" />

      <TableContainer>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Anno</TableCell>
              <TableCell>Stato</TableCell>
              <TableCell style={{ width: "30%" }} />
            </TableRow>
          </TableHead>

          <TableBody>{rows}</TableBody>
        </Table>
      </TableContainer>
    </Container>
  );
};

const mapStateToProps = state => {
  return {
    authentication: state.authentication
  };
};

const mapDispatchToProps = {};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(withSnackbar(Years));
