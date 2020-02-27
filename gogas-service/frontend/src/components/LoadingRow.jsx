import React from "react";
import { TableRow, TableCell, CircularProgress } from "@material-ui/core";

const LoadingRow = ({ colSpan }) => {
  return (
    <TableRow>
      <TableCell
        colSpan={colSpan}
        style={{
          textAlign: "center",
          height: "100px",
          verticalAlign: "middle"
        }}
      >
        <CircularProgress />
      </TableCell>
    </TableRow>
  );
};

export default LoadingRow;
