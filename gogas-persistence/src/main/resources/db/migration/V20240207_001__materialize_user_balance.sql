ALTER TABLE utenti ADD balance NUMERIC(18, 5) NOT NULL DEFAULT 0
GO

CREATE TABLE auditUserBalance (
  id uniqueidentifier NOT NULL PRIMARY KEY,
  userId uniqueidentifier NOT NULL,
  ts datetime NOT NULL,
  entryType varchar(100) NOT NULL, -- MIGRATION, ORDER or ACCOUNTING
  operationType varchar(100) NOT NULL, -- ADD, UPDATE, REMOVE
  referenceId uniqueidentifier NOT NULL, -- ID of order or accounting entry
  amount decimal (18, 5) NOT NULL,
  currentBalance decimal (18, 5) NOT NULL,
  CONSTRAINT FK_auditUserBalance_utenti FOREIGN KEY (userId) REFERENCES utenti(idUtente)
)
GO