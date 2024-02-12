CREATE TABLE orderUserBlacklist (
  orderTypeId uniqueidentifier NOT NULL,
  userId uniqueidentifier NOT NULL,
  FOREIGN KEY (orderTypeId) REFERENCES tipologiaOrdine(idTipologiaOrdine),
  FOREIGN KEY (userId) REFERENCES utenti(idUtente)
)
GO