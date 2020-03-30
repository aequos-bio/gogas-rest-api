CREATE TABLE userOrderSummary (
    userId uniqueidentifier NOT NULL,
    orderId uniqueidentifier NOT NULL,
    totalAmount decimal (5, 2),
    itemsCount int NOT NULL DEFAULT 0,
    friendItemsCount int NOT NULL DEFAULT 0,
    friendItemsAccounted int NOT NULL DEFAULT 0,
    shippingCost decimal (5, 2) NOT NULL DEFAULT 0,
    CONSTRAINT PK_userOrderSummary PRIMARY KEY CLUSTERED (userId, orderId),
    CONSTRAINT FK_userOrderSummary_dateOrdini FOREIGN KEY (orderId) REFERENCES dateOrdini(idDateOrdini),
    CONSTRAINT FK_userOrderSummary_utenti FOREIGN KEY (userId) REFERENCES utenti(idUtente)
)
GO

INSERT INTO userOrderSummary
SELECT tot.idUtente, d.idDateOrdini AS orderId, tot.importo, i.itemsCount,
COALESCE(f.friendCount, 0) as friendCount,
COALESCE(f.friendAccounted, 0) as friendAccounted,
COALESCE(s.importo, 0) AS shippingCost
FROM dateOrdini d
INNER JOIN tipologiaOrdine t ON d.idTipologiaOrdine = t.idTipologiaOrdine
INNER JOIN (SELECT idDateOrdini, idUtente, SUM(importo) as importo FROM movimenti m GROUP BY idDateOrdini, idUtente) as tot ON d.idDateOrdini = tot.idDateOrdini
INNER JOIN (SELECT idDateOrdine, idUtente, riepilogoUtente, COUNT(*) itemsCount FROM ordini o GROUP BY idDateOrdine, idUtente, riepilogoUtente) as i ON d.idDateOrdini = i.idDateOrdine AND tot.idUtente = i.idUtente AND i.riepilogoUtente = CAST(d.stato AS BIT)
LEFT OUTER JOIN speseTrasporto s ON  d.idDateOrdini = s.idDateOrdini AND s.idUtente = tot.idUtente
LEFT OUTER JOIN (
  SELECT o.idDateOrdine, o.idReferenteAmico, COUNT(*) AS friendCount,
  CASE WHEN SUM(1 - o.contabilizzato) = 0 THEN 1 ELSE 0 END AS friendAccounted
  FROM ordini o
  WHERE o.riepilogoUtente = 0 AND o.idReferenteAmico IS NOT NULL
  GROUP BY o.idDateOrdine, o.idReferenteAmico
) f ON d.idDateOrdini = f.idDateOrdine AND f.idReferenteAmico = tot.idUtente
WHERE t."external" = 1 OR t.totaleCalcolato = 0;

INSERT INTO userOrderSummary
SELECT tot.idUtente, d.idDateOrdini AS orderId, tot.importo, tot.itemsCount,
COALESCE(f.friendCount, 0) as friendCount,
COALESCE(f.friendAccounted, 0) as friendAccounted,
COALESCE(s.importo, 0) AS shippingCost
FROM dateOrdini d
INNER JOIN tipologiaOrdine t ON d.idTipologiaOrdine = t.idTipologiaOrdine
INNER JOIN (
    SELECT idDateOrdine, idUtente, riepilogoUtente,
            SUM(COALESCE(qtaRitirataKg, qtaOrdinata) * o.prezzoKg * CASE WHEN o.um = p.umCollo THEN p.pesoCassa ELSE 1 END) importo,
            COUNT(*) itemsCount
    FROM ordini o
    INNER JOIN prodotti p ON o.idProdotto = p.idProdotto
    GROUP BY idDateOrdine, idUtente, riepilogoUtente
) as tot ON d.idDateOrdini = tot.idDateOrdine AND tot.riepilogoUtente = CAST(d.stato AS BIT)
LEFT OUTER JOIN speseTrasporto s ON  d.idDateOrdini = s.idDateOrdini AND s.idUtente = tot.idUtente
LEFT OUTER JOIN (
  SELECT o.idDateOrdine, o.idReferenteAmico, COUNT(*) AS friendCount,
  CASE WHEN SUM(1 - o.contabilizzato) = 0 THEN 1 ELSE 0 END AS friendAccounted
  FROM ordini o
  WHERE o.riepilogoUtente = 0 AND o.idReferenteAmico IS NOT NULL
  GROUP BY o.idDateOrdine, o.idReferenteAmico
) f ON d.idDateOrdini = f.idDateOrdine AND f.idReferenteAmico = tot.idUtente
WHERE t.totaleCalcolato = 1;