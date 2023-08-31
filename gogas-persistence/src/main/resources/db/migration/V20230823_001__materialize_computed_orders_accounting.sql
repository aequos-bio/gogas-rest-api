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

UPDATE m
SET m.importo = m.importo + t.importo
FROM movimenti m
INNER JOIN speseTrasporto t ON m.idDateOrdini = t.idDateOrdini AND m.idUtente = t.idUtente
WHERE m.idDateOrdini IS NOT NULL AND t.importo IS NOT NULL

-- Insert accounting entries for USERS (with confermato=false) for computed orders to replace "schedacontabile" view
INSERT INTO movimenti (idMovimento, idUtente, dataMovimento, causale, idReferente, descrizione, importo, confermato, idDateOrdini)
SELECT newid() as idMovimento,
       CASE WHEN o.idReferenteAmico IS NOT NULL THEN o.idReferenteAmico ELSE o.idUtente END AS idUtente,
       d.dataConsegna AS dataMovimento,
       'ORDINE_CAL' as causale,
       null AS idReferente,
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
GROUP BY CASE WHEN o.idReferenteAmico IS NOT NULL THEN o.idReferenteAmico ELSE o.idUtente END,
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
WHERE o.idReferenteAmico IS NOT NULL AND o.contabilizzato = 1
GROUP BY o.idUtente,
         o.idReferenteAmico,
         d.dataConsegna,
         d.idDateOrdini,
         s.importo,
         'Totale ordine ' + t.tipoOrdine + ' in consegna ' + CONVERT(VARCHAR, d.dataConsegna, 3)

-- Query to check if accounting entries are the same as "schedacontabile"
-- SELECT m.idUtente, dataMovimento as data,
-- CASE c.codiceCausale WHEN 'ORDINE_CAL' THEN m.descrizione ELSE c.descrizione + ' - ' + m.descrizione END as descrizione, c.segno,
-- CAST(m.importo AS DECIMAL(18, 5)) as importo,
-- CASE c.codiceCausale WHEN 'ORDINE_CAL' THEN COALESCE(m.idDateOrdini, idMovimento) ELSE idMovimento END as idRiga,
-- m.idDateOrdini
-- FROM movimenti m
-- INNER JOIN causale c ON m.causale = c.codiceCausale
-- WHERE m.idUtente IN (SELECT idUtente FROM utenti WHERE utente = <username>)
-- order by dataMovimento desc, CASE c.codiceCausale WHEN 'ORDINE_CAL' THEN m.descrizione ELSE c.descrizione + ' - ' + m.descrizione END




