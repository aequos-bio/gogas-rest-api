ALTER TABLE utenti ADD balance NUMERIC(18, 5) NOT NULL DEFAULT 0
GO

CREATE TABLE auditUserBalance (
  userId uniqueidentifier NOT NULL,
  ts datetime NOT NULL,
  entryType varchar(100) NOT NULL, -- MIGRATION, ORDER or ACCOUNTING
  operationType varchar(100) NOT NULL, -- ADD, UPDATE, REMOVE
  referenceId uniqueidentifier NOT NULL, -- ID of order or accounting entry
  amount decimal (18, 5) NOT NULL,
  currentBalance decimal (18, 5) NOT NULL,
  CONSTRAINT PK_auditUserBalance PRIMARY KEY CLUSTERED (userId, ts),
  CONSTRAINT FK_auditUserBalance_utenti FOREIGN KEY (userId) REFERENCES utenti(idUtente)
)
GO

UPDATE u
SET u.balance = s.saldo
FROM utenti u
INNER JOIN SaldoContabile s ON u.idUtente = s.idUtente

INSERT INTO auditUserBalance (ts, userId, entryType, operationType, referenceId, amount, currentBalance)
SELECT CURRENT_TIMESTAMP, u.idUtente, 'MIGRATION', 'ADD', newid(), u.balance, 0 FROM utenti u
WHERE u.balance > 0