ALTER TABLE utenti ADD balance NUMERIC(18, 2) NOT NULL DEFAULT 0
GO

UPDATE u
SET u.balance = s.saldo
FROM utenti u
INNER JOIN SaldoContabile s ON u.idUtente = s.idUtente