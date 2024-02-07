-- Creating new table to store summary of user orders (total, count)
CREATE TABLE userOrderSummary (
  userId uniqueidentifier NOT NULL,
  friendReferralId  uniqueidentifier,
  orderId uniqueidentifier NOT NULL,
  totalAmount decimal (5, 2),
  itemsCount int NOT NULL DEFAULT 0,
  accountedItemsCount int NOT NULL DEFAULT 0,
  aggregated bit NOT NULL DEFAULT 0,
  CONSTRAINT PK_userOrderSummary PRIMARY KEY CLUSTERED (userId, orderId),
  CONSTRAINT FK_userOrderSummary_dateOrdini FOREIGN KEY (orderId) REFERENCES dateOrdini(idDateOrdini),
  CONSTRAINT FK_userOrderSummary_utenti FOREIGN KEY (userId) REFERENCES utenti(idUtente)
)
GO

-- fixing any missing friend referral info in orders
UPDATE o
SET o.idReferenteAmico = u.idReferente
FROM ordini o
INNER JOIN utenti u ON o.idUtente = u.idUtente
WHERE u.ruolo = 'S' AND o.idReferenteAmico IS NULL;

-- adding new accounting reason for automatic charge of orders
INSERT INTO causale (codiceCausale, segno, descrizione, codiceContabile)
VALUES ('ORDINE_CAL', '-', 'Addebito ordine (automatico)', NULL);

-- insert order summary for not computed orders (from accounting entries)
INSERT INTO userOrderSummary (userId, friendReferralId, orderId, totalAmount, itemsCount, accountedItemsCount, aggregated)
SELECT tot.idUtente, tot.idReferente, d.idDateOrdini AS orderId, tot.importo, i.itemsCount, 0 as accountedItemsCount, 1 as aggregated
FROM dateOrdini d
         INNER JOIN tipologiaOrdine t ON d.idTipologiaOrdine = t.idTipologiaOrdine
         INNER JOIN (SELECT idDateOrdini, idUtente, idReferente, SUM(importo) as importo FROM movimenti m GROUP BY idDateOrdini, idUtente, idReferente) as tot ON d.idDateOrdini = tot.idDateOrdini
         INNER JOIN (SELECT idDateOrdine, idUtente, riepilogoUtente, COUNT(*) itemsCount FROM ordini o GROUP BY idDateOrdine, idUtente, riepilogoUtente) as i ON d.idDateOrdini = i.idDateOrdine AND tot.idUtente = i.idUtente AND i.riepilogoUtente = CAST(d.stato AS BIT)
WHERE t."external" = 1 OR t.totaleCalcolato = 0;

-- insert order summary for computed orders
INSERT INTO userOrderSummary (userId, friendReferralId, orderId, totalAmount, itemsCount, accountedItemsCount, aggregated)
SELECT tot.idUtente, tot.idReferenteAmico, d.idDateOrdini AS orderId, tot.importo, tot.itemsCount, tot.accountedUtemsCount, CAST(d.stato AS BIT) as aggregated
FROM dateOrdini d
 INNER JOIN tipologiaOrdine t ON d.idTipologiaOrdine = t.idTipologiaOrdine
 INNER JOIN (
    SELECT idDateOrdine, idUtente, idReferenteAmico, riepilogoUtente,
           ROUND(SUM(COALESCE(qtaRitirataKg, qtaOrdinata) * o.prezzoKg * CASE WHEN o.um = p.umCollo THEN p.pesoCassa ELSE 1 END), 2) importo,
           COUNT(*) itemsCount,
           SUM(CAST(contabilizzato AS int)) accountedUtemsCount
    FROM ordini o
      INNER JOIN prodotti p ON o.idProdotto = p.idProdotto
    GROUP BY idDateOrdine, idUtente, idReferenteAmico, riepilogoUtente
) as tot ON d.idDateOrdini = tot.idDateOrdine AND tot.riepilogoUtente = CAST(d.stato AS BIT)
WHERE t.totaleCalcolato = 1;

-- insert order summary for friends when computed orders require summary and are already closed or accounted
INSERT INTO userOrderSummary (userId, friendReferralId, orderId, totalAmount, itemsCount, accountedItemsCount, aggregated)
SELECT tot.idUtente, tot.idReferenteAmico, d.idDateOrdini AS orderId, tot.importo, tot.itemsCount, tot.accountedUtemsCount, 0 as aggregated
FROM dateOrdini d
 INNER JOIN tipologiaOrdine t ON d.idTipologiaOrdine = t.idTipologiaOrdine
 INNER JOIN (
    SELECT idDateOrdine, idUtente, idReferenteAmico,
           ROUND(SUM(COALESCE(qtaRitirataKg, qtaOrdinata) * o.prezzoKg * CASE WHEN o.um = p.umCollo THEN p.pesoCassa ELSE 1 END), 2) importo,
           COUNT(*) itemsCount,
           SUM(CAST(contabilizzato AS int)) accountedUtemsCount
    FROM ordini o
      INNER JOIN prodotti p ON o.idProdotto = p.idProdotto
    WHERE riepilogoUtente = 0 and idReferenteAmico IS NOT NULL
    GROUP BY idDateOrdine, idUtente, idReferenteAmico
) as tot ON d.idDateOrdini = tot.idDateOrdine
WHERE t.totaleCalcolato = 1 AND t.riepilogo = 1 AND d.stato IN (1, 2);

-- Removing not confirmed accounting entries, replaced by userOrderSummary
DELETE FROM movimenti WHERE confermato = 0;

-- Adding shipping costs to accounting entries (new way of manage charges for users)
UPDATE m
SET m.importo = m.importo + t.importo, causale = 'ORDINE_CAL'
FROM movimenti m
INNER JOIN speseTrasporto t ON m.idDateOrdini = t.idDateOrdini AND m.idUtente = t.idUtente
WHERE m.idDateOrdini IS NOT NULL AND t.importo IS NOT NULL;

-- Insert accounting entries for USERS (with confermato=false) for computed orders to replace "schedacontabile" view
INSERT INTO movimenti (idMovimento, idUtente, dataMovimento, causale, idReferente, descrizione, importo, confermato, idDateOrdini)
SELECT newid() as idMovimento,
       o.idUtente,
       d.dataConsegna AS dataMovimento,
       'ORDINE_CAL' as causale,
       o.idReferenteAmico AS idReferente,
       'Totale ordine ' + t.tipoOrdine + ' in consegna ' + CONVERT(VARCHAR, d.dataConsegna, 3) AS descrizione,
       ROUND(SUM(CASE WHEN [totaleCalcolato] = 1 THEN [qtaRitirataKg] * [prezzoKg] ELSE o.[prezzoKg] END), 2) + COALESCE(s.importo, 0) AS importo,
       0 AS confermato,
       d.idDateOrdini AS idDateOrdini
FROM dateOrdini d
    INNER JOIN tipologiaOrdine t ON d.idTipologiaOrdine = t.idTipologiaOrdine
    INNER JOIN ordini o ON d.idDateOrdini = o.idDateOrdine
    INNER JOIN utenti u ON o.idUtente = u.idUtente
    LEFT OUTER JOIN speseTrasporto s ON d.idDateOrdini = s.idDateOrdini AND s.idUtente = o.idUtente
WHERE o.riepilogoUtente = 1 and o.contabilizzato = 1
GROUP BY o.idUtente,
         o.idReferenteAmico,
         d.dataConsegna,
         d.idDateOrdini,
         s.importo,
         'Totale ordine ' + t.tipoOrdine + ' in consegna ' + CONVERT(VARCHAR, d.dataConsegna, 3)

-- Insert accounting entries for FRIENDS (with confermato=false) for computed orders to replace "schedacontabile" view
INSERT INTO movimenti (idMovimento, idUtente, dataMovimento, causale, idReferente, descrizione, importo, confermato, idDateOrdini)
SELECT newid() as idMovimento,
       o.idUtente AS idUtente,
       d.dataConsegna AS dataMovimento,
       'ORDINE_CAL' as causale,
       o.idReferenteAmico AS idReferente,
       'Totale ordine ' + t.tipoOrdine + ' in consegna ' + CONVERT(VARCHAR, d.dataConsegna, 3) AS descrizione,
       ROUND(SUM(CASE WHEN [totaleCalcolato] = 1 THEN [qtaRitirataKg] * [prezzoKg] ELSE o.[prezzoKg] END), 2) + COALESCE(s.importo, 0) AS importo,
       0 AS confermato,
       d.idDateOrdini AS idDateOrdini
FROM dateOrdini d
     INNER JOIN tipologiaOrdine t ON d.idTipologiaOrdine = t.idTipologiaOrdine
     INNER JOIN ordini o ON d.idDateOrdini = o.idDateOrdine
     INNER JOIN utenti u ON o.idUtente = u.idUtente
     LEFT OUTER JOIN speseTrasporto s ON d.idDateOrdini = s.idDateOrdini AND s.idUtente = o.idUtente
WHERE o.idReferenteAmico IS NOT NULL AND o.riepilogoUtente = 0 AND o.contabilizzato = 1
GROUP BY o.idUtente,
         o.idReferenteAmico,
         d.dataConsegna,
         d.idDateOrdini,
         s.importo,
         'Totale ordine ' + t.tipoOrdine + ' in consegna ' + CONVERT(VARCHAR, d.dataConsegna, 3)
GO

-- View used to check if list of accounting entries matches with old "SchedaContabile"
CREATE VIEW checkSchedaContabile AS
SELECT idUtente, idReferente, dataMovimento as data,
CASE WHEN c.codiceCausale = 'ORDINE_CAL' AND t.totaleCalcolato = 1 THEN m.descrizione ELSE c.descrizione + ' - ' + m.descrizione END as descrizione, c.segno,
CAST(m.importo AS DECIMAL(18, 5)) as importo,
CASE WHEN c.codiceCausale  = 'ORDINE_CAL' AND t.totaleCalcolato = 1 THEN COALESCE(m.idDateOrdini, idMovimento) ELSE idMovimento END as idRiga,
m.idDateOrdini,
riepilogo
FROM movimenti m
LEFT OUTER JOIN (SELECT d.idDateOrdini, t.totaleCalcolato, t.riepilogo FROM dateOrdini d INNER JOIN tipologiaOrdine t ON d.idTipologiaOrdine = t.idTipologiaOrdine) t ON t.idDateOrdini = m.idDateOrdini
INNER JOIN causale c ON m.causale = c.codiceCausale
GO

-- Adjusting SchedaContabile nested views to exclude shipping costs (already included now in accounting entries)
ALTER VIEW riepilogoMovimenti AS
SELECT 'S' AS TipoMovimento, dbo.movimenti.idUtente, dbo.movimenti.dataMovimento AS data,
dbo.causale.descrizione + ' - ' + dbo.movimenti.descrizione AS descrizione,
dbo.causale.segno, dbo.movimenti.importo,
dbo.movimenti.idMovimento AS idRiga, dbo.movimenti.idDateOrdini AS idOrdine
FROM  dbo.movimenti
INNER JOIN dbo.causale ON dbo.causale.codiceCausale = dbo.movimenti.causale
WHERE confermato = 1
GO

ALTER VIEW riepilogoMovimentiAmici
AS
SELECT 'A' AS TipoMovimento, movimenti.idReferente, movimenti.dataMovimento AS data, causale.descrizione + ' - (' + utenti.nome + ' ' + utenti.cognome + ') ' + movimenti.descrizione AS descrizione, causale.segno, movimenti.importo, movimenti.idMovimento AS idRiga, movimenti.idDateOrdini AS idOrdine
FROM causale INNER JOIN movimenti ON causale.codiceCausale = movimenti.causale
             INNER JOIN utenti ON movimenti.idUtente = utenti.idUtente
WHERE movimenti.idReferente IS NOT NULL AND confermato = 1
GO

-- QUERIES TO VERIFY ALIGNMENT OF SCHEDA CONTABILE
--
--select idUtente, FORMAT(data, 'yyyy-MM-dd') as data, descrizione, segno, FORMAT(SUM(importo), 'N2', 'it-it') as importo, idRiga, idDateOrdini FROM
--(
--  SELECT
--  CASE WHEN riepilogo = 0 THEN COALESCE(idReferente, idUtente) ELSE idUtente END as idUtente,
--  data, descrizione, segno, importo as importo, idRiga, idDateOrdini
--  from checkSchedaContabile
--  UNION ALL
--  select idUtente, data, descrizione, segno, importo, idRiga, idDateOrdini
--  from checkSchedaContabile
--  where idReferente IS NOT NULL
--) x
--group by idUtente, data, descrizione, segno, idRiga, idDateOrdini
--order by idUtente, data desc, idRiga, descrizione
--
--select idUtente, FORMAT(data, 'yyyy-MM-dd') as data, descrizione, segno, FORMAT(SUM(importo), 'N2', 'it-it') as importo, idRiga, idDateOrdini
--from schedacontabile
--group by idUtente, data, descrizione, segno, idRiga, idDateOrdini
--order by idUtente, data desc, idRiga, descrizione