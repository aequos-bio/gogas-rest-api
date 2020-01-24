import React, { useState, useEffect, useMemo, useCallback } from "react";
import { connect } from "react-redux";
import { Modal, Button, Form, InputGroup } from "react-bootstrap";
import NumPad from "react-numpad";
import _ from "lodash";
import moment from "moment-timezone";
import Select from "react-select";
import { getJson, apiPost } from "../../../utils/axios_utils";

const AddTransactionDialog = ({ title, user, show, onClose, info }) => {
  const sort = info["visualizzazione.utenti"]
    ? info["visualizzazione.utenti"].value
    : "NC";
  const userLabel = useCallback(
    u => {
      const name =
        sort === "NC" ? `${u.nome} ${u.cognome}` : `${u.cognome} ${u.nome}`;
      const disa = u.attivo ? null : (
        <span className="fa fa-ban" style={{ color: "red" }} />
      );

      return (
        <span>
          {disa} {name}
        </span>
      );
    },
    [sort]
  );

  const [_user, setUser] = useState(
    user ? { value: user, label: userLabel(user) } : undefined
  );
  const [date, setDate] = useState();
  const [reason, setReason] = useState();
  const [description, setDescription] = useState("");
  const [amount, setAmount] = useState(0);

  const [users, setUsers] = useState([]);
  const [reasons, setReasons] = useState([]);
  const [error, setError] = useState();
  const [refreshNeeded, setRefreshNeeded] = useState(false);

  useEffect(() => {
    if (!show) return;

    setUser(user ? { value: user, label: userLabel(user) } : null);
    setReason(null);
    setDate();
    setDescription("");
    setAmount(0);

    getJson("/api/user/list", {}).then(uu => {
      if (uu.error) {
        setError(uu.errorMessage);
      } else {
        setUsers(
          _.orderBy(
            uu,
            [
              "attivo",
              sort === "NC" ? "nome" : "cognome",
              sort === "NC" ? "cognome" : "nome"
            ],
            ["desc", "asc", "asc"]
          )
        );
      }
    });
    getJson("/api/accounting/reason/list", {}).then(rr => {
      if (rr.error) {
        setError(rr.errorMessage);
      } else {
        setReasons(rr);
      }
    });
  }, [info, show, sort, user, userLabel]);

  const canSave = useMemo(() => {
    return _user && reason && date && description.length && amount;
  }, [_user, reason, date, description, amount]);

  const close = useCallback(
    forcerefresh => {
      onClose(refreshNeeded || forcerefresh);
    },
    [refreshNeeded, onClose]
  );

  const save = useCallback(
    contnue => {
      apiPost("/api/accounting/user/entry", {
        data: moment(date, "YYYY-MM-DD").format("DD/MM/YYYY"),
        idutente: _user.value.idUtente,
        nomeutente: `${_user.value.nome} ${_user.value.cognome}`,
        codicecausale: reason.value.reasonCode,
        nomecausale: reason.value.description,
        descrizione: description,
        importo: amount
      }).then(() => {
        if (contnue) {
          setRefreshNeeded(true);
          setUser(user ? { value: user, label: userLabel(user) } : undefined);
          setAmount(0);
        } else {
          close(true);
        }
      });
    },
    [
      _user,
      reason,
      date,
      description,
      amount,
      setRefreshNeeded,
      close,
      user,
      userLabel
    ]
  );

  return (
    <Modal show={show} onHide={close}>
      <Modal.Header>
        <Modal.Title>{title}</Modal.Title>
      </Modal.Header>

      <Modal.Body>
        <Form.Group>
          <Select
            options={users.map(u => ({ value: u, label: userLabel(u) }))}
            placeholder="Selezionare un utente"
            onChange={u => setUser(u)}
            value={_user}
            isClearable
            isDisabled={user !== undefined}
          />
        </Form.Group>

        <Form.Group style={{ width: "50%" }}>
          <NumPad.Calendar
            position="startBottomLeft"
            locale="it"
            dateFormat="DD/MM/YYYY"
            onClick={e => {
              e.preventDefault();
              e.stopPropagation();
            }}
            onChange={value =>
              setDate(moment(value, "DD/MM/YYYY").format("YYYY-MM-DD"))
            }
            confirm={() => {}}
            value={date ? moment(date, "YYYY-MM-DD").format("DD/MM/YYYY") : ""}
          >
            <InputGroup>
              <Form.Control
                type="text"
                placeholder="Inserire una data"
                value={
                  date ? moment(date, "YYYY-MM-DD").format("DD/MM/YYYY") : ""
                }
                readOnly
              />
              <InputGroup.Append>
                <InputGroup.Text id="basic-addon2">
                  <span className="fa fa-calendar" />
                </InputGroup.Text>
              </InputGroup.Append>
            </InputGroup>
          </NumPad.Calendar>
        </Form.Group>

        <Form.Group>
          <Select
            options={reasons.map(r => ({ value: r, label: r.description }))}
            placeholder="Selezionare una causale"
            onChange={r => setReason(r)}
            value={reason}
            isClearable
          />
        </Form.Group>

        <Form.Group>
          <Form.Control
            type="text"
            placeholder="Inserire una descrizione"
            value={description}
            onChange={evt => setDescription(evt.target.value)}
          />
        </Form.Group>

        <Form.Group style={{ width: "50%" }}>
          <InputGroup>
            <Form.Control
              type="number"
              placeholder="Inserire un importo"
              value={amount}
              onChange={evt => setAmount(evt.target.value)}
            />
            <InputGroup.Append>
              <InputGroup.Text id="basic-addon2">
                <span className="fa fa-euro" />
              </InputGroup.Text>
            </InputGroup.Append>
          </InputGroup>
        </Form.Group>
      </Modal.Body>

      <Modal.Footer>
        {error ? error.errorMessage : ""}
        <Button variant="outline-secondary" onClick={close}>
          Annulla
        </Button>
        <Button
          variant="outline-primary"
          disabled={!canSave}
          onClick={() => save(true)}
        >
          Salva e continua
        </Button>
        <Button
          variant="outline-primary"
          disabled={!canSave}
          onClick={() => save()}
        >
          Salva
        </Button>
      </Modal.Footer>
    </Modal>
  );
};

const mapStateToProps = state => {
  return {
    info: state.info
  };
};

const mapDispatchToProps = {};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(AddTransactionDialog);
