DROP TABLE pushToken;
GO

ALTER VIEW notificationPrefsView AS
SELECT COALESCE(pt.id, pg.id) AS id
	  ,pg.idUtente
	  ,pg.idTipologiaOrdine
	  ,CASE WHEN pt.id IS NULL THEN 'G' ELSE 'T' END AS tipo
	  ,COALESCE(pt.apertura, pg.apertura) AS apertura
	  ,COALESCE(pt.scadenza, pg.scadenza) AS scadenza
	  ,COALESCE(pt.consegna, pg.consegna) AS consegna
	  ,COALESCE(pt.aggiornamentoQta, pg.aggiornamentoQta) AS aggiornamentoQta
	  ,COALESCE(pt.contabilizzazione, pg.contabilizzazione) AS contabilizzazione
 FROM (SELECT t.idTipologiaOrdine, id, idUtente, apertura, scadenza, consegna, aggiornamentoQta, contabilizzazione FROM tipologiaOrdine t, notificationPrefs p WHERE p.idTipologiaOrdine IS NULL) pg
  LEFT OUTER JOIN notificationPrefs pt ON pg.idUtente = pt.idUtente AND pg.idTipologiaOrdine = pt.idTipologiaOrdine;
GO

ALTER TABLE notificationPrefs DROP COLUMN minutiScadenza;
ALTER TABLE notificationPrefs DROP COLUMN minutiConsegna;
GO

