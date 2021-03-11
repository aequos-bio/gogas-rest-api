/****** Object:  Table [anno]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [anno](
	[anno] [int] NOT NULL,
	[chiuso] [bit] NOT NULL,
PRIMARY KEY CLUSTERED
(
	[anno] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
/****** Object:  Table [categoriaProdotti]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [categoriaProdotti](
	[idCategoriaProdotto] [uniqueidentifier] NOT NULL,
	[idTipologiaOrdine] [uniqueidentifier] NOT NULL,
	[descrizione] [varchar](100) NULL,
	[ordine_listino] [int] NOT NULL,
	[colore_listino] [varchar](10) NULL,
 CONSTRAINT [PK_categoriaProdotti] PRIMARY KEY CLUSTERED
(
	[idCategoriaProdotto] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO
/****** Object:  Table [causale]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [causale](
	[codiceCausale] [varchar](10) NOT NULL,
	[segno] [varchar](1) NULL,
	[descrizione] [varchar](100) NULL,
	[codiceContabile] [nvarchar](255) NULL,
 CONSTRAINT [PK_causale] PRIMARY KEY CLUSTERED
(
	[codiceCausale] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO
/****** Object:  Table [comunicazioni]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING OFF
GO
CREATE TABLE [comunicazioni](
	[id] [uniqueidentifier] NOT NULL,
	[titolo] [varchar](150) NOT NULL,
	[messaggio] [varchar](max) NOT NULL,
	[dataPubblicazione] [datetime] NOT NULL,
	[dataFineValidita] [datetime] NOT NULL,
	[ruolo] [varchar](1) NULL,
	[Attachment] [nvarchar](255) NULL,
	[CreatoDa] [uniqueidentifier] NOT NULL,
 CONSTRAINT [PK_comunicazioni] PRIMARY KEY CLUSTERED
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO
/****** Object:  Table [configurazione]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [configurazione](
	[chiave] [varchar](50) NOT NULL,
	[valore] [varchar](50) NOT NULL,
	[descrizione] [varchar](1024) NULL,
	[visibile] [bit] NOT NULL CONSTRAINT [DF_configurazione_visibile]  DEFAULT ((0)),
 CONSTRAINT [PK_configurazione] PRIMARY KEY CLUSTERED
(
	[chiave] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO
/****** Object:  Table [dateOrdini]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dateOrdini](
	[idDateOrdini] [uniqueidentifier] NOT NULL,
	[idTipologiaOrdine] [uniqueidentifier] NULL,
	[dataConsegna] [datetime] NULL,
	[dataChiusura] [datetime] NULL,
	[oraChiusura] [int] NULL,
	[valutazioneInviata] [bit] NULL,
	[testoMailListino] [varchar](2000) NULL,
	[stato] [int] NOT NULL,
	[dataApertura] [datetime] NULL,
	[idOrdineEsterno] [varchar](50) NULL,
	[inviato] [bit] NOT NULL,
	[speseTrasporto] [decimal](5, 2) NOT NULL
) ON [PRIMARY]
SET ANSI_PADDING OFF
ALTER TABLE [dateOrdini] ADD [externalLink] [varchar](255) NULL
ALTER TABLE [dateOrdini] ADD [invoiceAmount] [decimal](9, 2) NULL
ALTER TABLE [dateOrdini] ADD [invoiceNumber] [varchar](50) NULL
ALTER TABLE [dateOrdini] ADD [attachmentType] [varchar](100) NULL
ALTER TABLE [dateOrdini] ADD [invoiceDate] [datetime] NULL
ALTER TABLE [dateOrdini] ADD [paid] [bit] NOT NULL
ALTER TABLE [dateOrdini] ADD [paymentDate] [datetime] NULL
ALTER TABLE [dateOrdini] ADD [lastSynchro] [datetime] NULL
ALTER TABLE [dateOrdini] ADD [lastWeightUpdate] [datetime] NULL
 CONSTRAINT [PK_dateOrdini] PRIMARY KEY CLUSTERED
(
	[idDateOrdini] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO
/****** Object:  Table [menu]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [menu](
	[idMenu] [uniqueidentifier] NOT NULL,
	[label] [varchar](50) NOT NULL,
	[url] [varchar](255) NULL,
	[parentMenu] [uniqueidentifier] NULL,
	[external] [bit] NOT NULL CONSTRAINT [DF_menu_external]  DEFAULT ((0)),
 CONSTRAINT [PK_menu] PRIMARY KEY CLUSTERED
(
	[idMenu] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO
/****** Object:  Table [menuRuolo]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [menuRuolo](
	[idMenu] [uniqueidentifier] NOT NULL,
	[ruolo] [varchar](10) NOT NULL,
	[ordine] [int] NOT NULL,
 CONSTRAINT [PK_menuRuolo] PRIMARY KEY CLUSTERED
(
	[idMenu] ASC,
	[ruolo] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO
/****** Object:  Table [movimenti]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [movimenti](
	[idMovimento] [uniqueidentifier] NOT NULL,
	[idUtente] [uniqueidentifier] NULL,
	[dataMovimento] [datetime] NULL,
	[causale] [varchar](10) NULL,
	[idReferente] [uniqueidentifier] NULL,
	[descrizione] [varchar](100) NULL,
	[importo] [numeric](18, 2) NULL,
	[confermato] [bit] NOT NULL,
	[idDateOrdini] [uniqueidentifier] NULL,
 CONSTRAINT [PK_movimenti] PRIMARY KEY CLUSTERED
(
	[idMovimento] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO
/****** Object:  Table [movimentiGas]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING OFF
GO
CREATE TABLE [movimentiGas](
	[id] [uniqueidentifier] NOT NULL,
	[data] [datetime] NOT NULL,
	[causale] [varchar](10) NOT NULL,
	[descrizione] [varchar](100) NOT NULL,
	[importo] [decimal](18, 2) NOT NULL,
 CONSTRAINT [PK_movimentiGas] PRIMARY KEY CLUSTERED
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO
/****** Object:  Table [notificationPrefs]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [notificationPrefs](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[idUtente] [uniqueidentifier] NOT NULL,
	[idTipologiaOrdine] [uniqueidentifier] NULL,
	[apertura] [bit] NOT NULL,
	[scadenza] [bit] NOT NULL,
	[minutiScadenza] [int] NOT NULL,
	[consegna] [bit] NOT NULL,
	[minutiConsegna] [int] NOT NULL,
	[aggiornamentoQta] [bit] NOT NULL,
	[contabilizzazione] [bit] NOT NULL,
 CONSTRAINT [PK_notificationPrefs] PRIMARY KEY CLUSTERED
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
/****** Object:  Table [ordini]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [ordini](
	[idRigaOrdine] [uniqueidentifier] NOT NULL,
	[idDateOrdine] [uniqueidentifier] NULL,
	[idUtente] [uniqueidentifier] NULL,
	[idProdotto] [uniqueidentifier] NULL,
	[idReferenteAmico] [uniqueidentifier] NULL,
	[qtaOrdinata] [numeric](18, 2) NULL,
	[um] [varchar](15) NULL,
	[qtaRitirataKg] [decimal](19, 3) NULL,
	[prezzoKg] [numeric](18, 2) NULL,
	[inviato] [bit] NULL,
	[annullato] [bit] NULL,
	[riepilogoUtente] [bit] NULL,
	[inserimentoPostChiusura] [bit] NULL,
	[aspettoGenerale] [int] NULL,
	[sapore] [int] NULL,
	[contabilizzato] [bit] NULL,
	[idProdottoSostituito] [uniqueidentifier] NULL,
 CONSTRAINT [PK_ordini] PRIMARY KEY CLUSTERED
(
	[idRigaOrdine] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO

CREATE INDEX [IX_ordini_1] ON [ordini] ([idDateOrdine], [idUtente], [idProdotto])

GO
/****** Object:  Table [ordiniFornitore]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [ordiniFornitore](
	[idRigaOrdine] [uniqueidentifier] NOT NULL,
	[idDateOrdine] [uniqueidentifier] NOT NULL,
	[idProdotto] [uniqueidentifier] NOT NULL,
	[numeroColli] [numeric](18, 2) NOT NULL,
	[qtaCollo] [numeric](18, 2) NOT NULL,
	[prezzoKg] [numeric](18, 2) NOT NULL,
	[qtaOrdinata] [decimal](18, 2) NOT NULL,
	[weightUpdated] [bit] NOT NULL,
 CONSTRAINT [PK_ordiniFornitore] PRIMARY KEY CLUSTERED
(
	[idRigaOrdine] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
/****** Object:  Table [prodotti]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [prodotti](
	[idProdotto] [uniqueidentifier] NOT NULL,
	[Prodotto] [varchar](255) NULL,
	[um] [varchar](15) NULL,
	[pesoCassa] [numeric](18, 2) NULL,
	[PrezzoKg] [numeric](18, 2) NULL,
	[idTipoProd] [uniqueidentifier] NULL,
	[acquistabile] [bit] NULL,
	[umCollo] [varchar](15) NULL,
	[idProduttore] [uniqueidentifier] NULL,
	[annullato] [bit] NULL,
	[note] [varchar](2048) NULL,
	[idCategoria] [uniqueidentifier] NULL,
	[cadenza] [varchar](50) NULL,
	[idEsterno] [varchar](50) NULL,
	[soloColloIntero] [bit] NOT NULL,
	[multiplo] [numeric](18, 2) NULL,
 CONSTRAINT [PK_prodotti] PRIMARY KEY CLUSTERED
(
	[idProdotto] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO
/****** Object:  Table [produttori]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [produttori](
	[idProduttore] [uniqueidentifier] NOT NULL,
	[ragioneSociale] [varchar](255) NULL,
	[provincia] [varchar](50) NULL,
	[idEsterno] [varchar](50) NULL,
 CONSTRAINT [PK_produttori] PRIMARY KEY CLUSTERED
(
	[idProduttore] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO
/****** Object:  Table [pushToken]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING OFF
GO
CREATE TABLE [pushToken](
	[token] [varchar](1024) NOT NULL,
	[idUtente] [uniqueidentifier] NOT NULL,
	[deviceId] [varchar](1024) NULL,
 CONSTRAINT [PK_pushToken] PRIMARY KEY CLUSTERED
(
	[token] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO
/****** Object:  Table [responsabili]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [responsabili](
	[idResponsabili] [uniqueidentifier] NOT NULL,
	[idTipologiaOrdine] [uniqueidentifier] NULL,
	[idUtente] [uniqueidentifier] NULL,
 CONSTRAINT [PK_responsabili] PRIMARY KEY CLUSTERED
(
	[idResponsabili] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
/****** Object:  Table [speseTrasporto]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [speseTrasporto](
	[idDateOrdini] [uniqueidentifier] NOT NULL,
	[idUtente] [uniqueidentifier] NOT NULL,
	[importo] [decimal](5, 2) NOT NULL,
 CONSTRAINT [PK_speseTrasporto] PRIMARY KEY CLUSTERED
(
	[idDateOrdini] ASC,
	[idUtente] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
/****** Object:  Table [tipologiaOrdine]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [tipologiaOrdine](
	[idTipologiaOrdine] [uniqueidentifier] NOT NULL,
	[tipoOrdine] [varchar](255) NULL,
	[riepilogo] [bit] NULL,
	[totaleCalcolato] [bit] NULL,
	[prevedeTurni] [bit] NULL,
	[idOrdineAequos] [int] NULL,
	[mostraPreventivo] [bit] NOT NULL,
	[mostraCompletamentoColli] [bit] NOT NULL,
	[external] [bit] NOT NULL
) ON [PRIMARY]
SET ANSI_PADDING OFF
ALTER TABLE [tipologiaOrdine] ADD [externalLink] [varchar](255) NULL
ALTER TABLE [tipologiaOrdine] ADD [lastSynchro] [datetime] NULL
ALTER TABLE [tipologiaOrdine] ADD [excelAllUsers] [bit] NOT NULL
ALTER TABLE [tipologiaOrdine] ADD [excelAllProducts] [bit] NOT NULL
 CONSTRAINT [PK_tipologiaOrdine] PRIMARY KEY CLUSTERED
(
	[idTipologiaOrdine] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO
/****** Object:  Table [tipologiaTurno]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [tipologiaTurno](
	[idTipologiaTurno] [uniqueidentifier] NOT NULL,
	[descrizione] [nchar](255) NULL,
PRIMARY KEY CLUSTERED
(
	[idTipologiaTurno] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
/****** Object:  Table [turni]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [turni](
	[idTurni] [uniqueidentifier] NOT NULL,
	[idDateOrdini] [uniqueidentifier] NULL,
	[idUtente] [uniqueidentifier] NULL,
	[idTipologiaTurno] [uniqueidentifier] NULL,
	[notes] [varchar](255) NULL,
 CONSTRAINT [PK_turni] PRIMARY KEY CLUSTERED
(
	[idTurni] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO
/****** Object:  Table [utenti]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [utenti](
	[idUtente] [uniqueidentifier] NOT NULL,
	[utente] [varchar](30) NOT NULL,
	[pwd] [varchar](100) NOT NULL,
	[ruolo] [varchar](1) NULL,
	[nome] [varchar](50) NULL,
	[cognome] [varchar](50) NULL,
	[email] [varchar](255) NULL,
	[attivo] [bit] NULL,
	[idReferente] [uniqueidentifier] NULL,
	[telefono] [varchar](20) NULL,
	[position] [numeric] NOT NULL,
 CONSTRAINT [PK_utenti] PRIMARY KEY CLUSTERED
(
	[idUtente] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO
/****** Object:  Table [VersionInfo]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [VersionInfo](
	[Version] [bigint] NOT NULL,
	[AppliedOn] [datetime] NULL,
	[Description] [nvarchar](1024) NULL
) ON [PRIMARY]

GO
/****** Object:  Index [UC_Version]    Script Date: 11/12/2019 22:43:56 ******/
CREATE UNIQUE CLUSTERED INDEX [UC_Version] ON [VersionInfo]
(
	[Version] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
/****** Object:  View [riepilogoMovimenti]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE VIEW [riepilogoMovimenti]
AS
SELECT     'S' AS TipoMovimento, dbo.movimenti.idUtente, dbo.movimenti.dataMovimento AS data, dbo.causale.descrizione + ' - ' + dbo.movimenti.descrizione AS descrizione,
                      dbo.causale.segno, dbo.movimenti.importo + COALESCE(s.importo, 0) as importo,
                      dbo.movimenti.idMovimento AS idRiga, dbo.movimenti.idDateOrdini AS idOrdine
FROM         dbo.causale INNER JOIN dbo.movimenti ON dbo.causale.codiceCausale = dbo.movimenti.causale
             LEFT OUTER JOIN speseTrasporto s ON dbo.movimenti.idUtente = s.idUtente AND dbo.movimenti.idDateOrdini = s.idDateOrdini
WHERE     confermato = 1

GO
/****** Object:  View [riepilogoMovimentiAmici]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE VIEW [riepilogoMovimentiAmici]
AS
SELECT 'A' AS TipoMovimento, movimenti.idReferente, movimenti.dataMovimento AS data, causale.descrizione + ' - (' + utenti.nome + ' ' + utenti.cognome + ') ' + movimenti.descrizione AS descrizione, causale.segno, movimenti.importo + COALESCE(s.importo, 0) as importo, movimenti.idMovimento AS idRiga, movimenti.idDateOrdini AS idOrdine
FROM causale INNER JOIN movimenti ON causale.codiceCausale = movimenti.causale
             INNER JOIN utenti ON movimenti.idUtente = utenti.idUtente
             LEFT OUTER JOIN speseTrasporto s ON dbo.movimenti.idUtente = s.idUtente AND dbo.movimenti.idDateOrdini = s.idDateOrdini
WHERE movimenti.idReferente IS NOT NULL AND confermato = 1

GO
/****** Object:  View [riepilogoOrdini]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE VIEW [riepilogoOrdini]
AS
SELECT 'S' AS TipoMovimento,
	   CASE WHEN o.idReferenteAmico IS NOT NULL THEN o.idReferenteAmico ELSE o.idUtente END AS idUtente,
	    d.dataConsegna AS data,
       'Totale ordine ' + t.tipoOrdine + ' in consegna ' + CONVERT(VARCHAR, d.dataConsegna, 3) AS descrizione, '-' AS segno,
       ROUND(SUM(CASE WHEN [totaleCalcolato] = 1 THEN [qtaRitirataKg] * [prezzoKg] ELSE o.[prezzoKg] END), 2) + COALESCE(s.importo, 0) AS importo,
       d.idDateOrdini AS idRiga, o.contabilizzato
FROM dateOrdini d INNER JOIN tipologiaOrdine t ON d.idTipologiaOrdine = t.idTipologiaOrdine
     INNER JOIN ordini o ON d.idDateOrdini = o.idDateOrdine
     INNER JOIN utenti u ON o.idUtente = u.idUtente
     LEFT OUTER JOIN speseTrasporto s ON u.idUtente = s.idUtente AND d.idDateOrdini = s.idDateOrdini
WHERE o.riepilogoUtente = 1
GROUP BY CASE WHEN o.idReferenteAmico IS NOT NULL THEN o.idReferenteAmico ELSE o.idUtente END,
         d.dataConsegna, d.idDateOrdini, s.importo,
         'Totale ordine ' + t.tipoOrdine + ' in consegna ' + CONVERT(VARCHAR, d.dataConsegna, 3), o.contabilizzato

GO
/****** Object:  View [riepilogoOrdiniAmici]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE VIEW [riepilogoOrdiniAmici]
AS
SELECT     'A' AS TipoMovimento, dbo.ordini.idUtente, dbo.dateOrdini.dataConsegna AS data,
                      'Totale ordine ' + dbo.tipologiaOrdine.tipoOrdine + ' in consegna ' + CONVERT(VARCHAR, dbo.dateOrdini.dataConsegna, 3) AS descrizione, '-' AS segno,
                      ROUND(SUM(CASE WHEN [totaleCalcolato] = 1 THEN [qtaRitirataKg] * [prezzoKg] ELSE [ordini].[prezzoKg] END), 2) + COALESCE(s.importo, 0) AS importo,
                      dbo.ordini.idDateOrdine AS idRiga,
                      dbo.ordini.contabilizzato
FROM         dbo.ordini INNER JOIN
                      dbo.utenti ON dbo.ordini.idUtente = dbo.utenti.idUtente INNER JOIN
                      dbo.dateOrdini ON dbo.ordini.idDateOrdine = dbo.dateOrdini.idDateOrdini INNER JOIN
                      dbo.tipologiaOrdine ON dbo.dateOrdini.idTipologiaOrdine = dbo.tipologiaOrdine.idTipologiaOrdine
                      LEFT OUTER JOIN speseTrasporto s ON dbo.utenti.idUtente = s.idUtente AND dbo.dateOrdini.idDateOrdini = s.idDateOrdini
WHERE     dbo.ordini.idReferenteAmico IS NOT NULL AND dbo.ordini.riepilogoUtente <> dbo.tipologiaOrdine.riepilogo
GROUP BY dbo.ordini.idUtente, dbo.dateOrdini.dataConsegna, 'Totale ordine ' + dbo.tipologiaOrdine.tipoOrdine + ' in consegna ' + CONVERT(VARCHAR,
                      dbo.dateOrdini.dataConsegna, 3), dbo.ordini.idDateOrdine, s.importo,
                      dbo.ordini.contabilizzato

GO
/****** Object:  View [SchedaContabile]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE VIEW [SchedaContabile]
AS
SELECT     TipoMovimento, idUtente, data,
                      descrizione, segno,
                      importo,
                      idRiga, idRiga as idDateOrdini
FROM         riepilogoOrdini
WHERE     importo IS NOT NULL and contabilizzato=1
UNION ALL
SELECT     *
FROM         riepilogoMovimenti
UNION ALL
SELECT     TipoMovimento, idUtente, data,
                      descrizione, segno,
                      importo,
                      idRiga, idRiga as idDateOrdini
FROM         riepilogoOrdiniAmici
WHERE     importo IS NOT NULL and contabilizzato=1
UNION ALL
SELECT     *
FROM         riepilogoMovimentiAmici

GO
/****** Object:  View [SchedaContabileMobile]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE VIEW [SchedaContabileMobile]
AS
SELECT TipoMovimento, idUtente, data, descrizione, segno, importo, idRiga, s.idDateOrdini, tipoOrdine, t."external", d.externalLink
FROM SchedaContabile s
LEFT OUTER JOIN dateOrdini d ON s.idDateOrdini = d.idDateOrdini
LEFT OUTER JOIN tipologiaOrdine t ON  d.idTipologiaOrdine = t.idTipologiaOrdine
GO
/****** Object:  View [estrattoContoBanca_det]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE VIEW [estrattoContoBanca_det]
AS
SELECT     'S' AS TipoMovimento, dbo.movimenti.idUtente, dbo.movimenti.dataMovimento AS data, dbo.causale.descrizione + ' ' + dbo.movimenti.descrizione AS descrizione,
                      dbo.causale.segno, dbo.movimenti.importo, dbo.utenti.nome + ' ' + dbo.utenti.cognome AS NomeCognome, dbo.utenti.attivo, dbo.movimenti.idMovimento AS idRiga,
                      dbo.causale.codiceCausale, dbo.utenti.ruolo
FROM         dbo.causale INNER JOIN
                      dbo.movimenti ON dbo.causale.codiceCausale = dbo.movimenti.causale INNER JOIN
                      dbo.utenti ON dbo.movimenti.idUtente = dbo.utenti.idUtente
WHERE     (dbo.utenti.ruolo = 'U')

GO
/****** Object:  View [estrattoContoBanca]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
create view [estrattoContoBanca] as
select data, descrizione + ' '+ NomeCognome descrizione, case segno when '+' then importo when '-' then importo*-1 end importo, NomeCognome, idRiga from estrattoContoBanca_det where codiceCausale='vers'
union all
select data, descrizione, case segno when '+' then importo when '-' then importo*-1 end importo, NomeCognome, idRiga from estrattoContoBanca_det where idUtente='21D666E1-B444-4D17-837F-3984DFE9887E'

GO
/****** Object:  View [QtaDaOrdinare]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE VIEW [QtaDaOrdinare]
AS
SELECT ordini.idDateOrdine, tipologiaOrdine.tipoOrdine, dateOrdini.dataConsegna, ordini.idProdotto, prodotti.prodotto, Sum(CASE WHEN [ordini].[um]=[umCollo] THEN [qtaOrdinata]*[pesoCassa] ELSE [qtaOrdinata] END)  AS QtaDaOrdinare, prodotti.um, prodotti.pesoCassa, ordini.riepilogoUtente
FROM tipologiaOrdine INNER JOIN (dateOrdini INNER JOIN (prodotti INNER JOIN ordini ON prodotti.idProdotto = ordini.idProdotto) ON dateOrdini.idDateOrdini = ordini.idDateOrdine) ON tipologiaOrdine.idTipologiaOrdine = dateOrdini.idTipologiaOrdine
GROUP BY ordini.idDateOrdine, tipologiaOrdine.tipoOrdine, dateOrdini.dataConsegna, ordini.idProdotto, prodotti.prodotto, prodotti.um, prodotti.pesoCassa, ordini.riepilogoUtente, ordini.inviato, ordini.annullato
HAVING (((ordini.annullato)=0));

GO
/****** Object:  View [QtaDaOrdinareStato]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE VIEW [QtaDaOrdinareStato]
AS
SELECT     idDateOrdine, tipoOrdine, dataConsegna, idProdotto, prodotto, QtaDaOrdinare, um, pesoCassa,
                      CASE WHEN [pesoCassa] = 0 THEN 0 ELSE CAST([QtaDaOrdinare] / [pesoCassa] AS int) END AS NrCasse,
                      CASE WHEN [pesoCassa] = 0 THEN 0 ELSE ([qtaDaOrdinare] - (CAST([QtaDaOrdinare] / [pesoCassa] AS int) * [pesoCassa])) END AS NrKg,
                      CASE WHEN [pesoCassa] = 0 THEN '0' ELSE CASE WHEN [QtaDaOrdinare] >= [pesoCassa] THEN 'ORDINABILE' ELSE CAST(([QtaDaOrdinare] - [pesoCassa])
                      AS NVARCHAR) END END AS Stato, riepilogoUtente
FROM         dbo.QtaDaOrdinare

GO
/****** Object:  View [riepilogoOrdinexProdottoUtente]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE VIEW [riepilogoOrdinexProdottoUtente]
AS
SELECT ordini.idProdotto, prodotti.prodotto, ordini.idDateOrdine, tipologiaOrdine.tipoOrdine, dateOrdini.dataConsegna, Round(Sum(CASE WHEN [totaleCalcolato]=1 THEN [qtaRitirataKg]*[ordini].[prezzoKg] ELSE [ordini].[prezzoKg] END),2) AS importo, Sum(ordini.qtaRitirataKg) AS qta, ordini.riepilogoUtente, ordini.inviato, ordini.idUtente
FROM (dateOrdini INNER JOIN (ordini INNER JOIN prodotti ON ordini.idProdotto = prodotti.idProdotto) ON dateOrdini.idDateOrdini = ordini.idDateOrdine) INNER JOIN tipologiaOrdine ON dateOrdini.idTipologiaOrdine = tipologiaOrdine.idTipologiaOrdine
GROUP BY ordini.idProdotto, prodotti.prodotto, ordini.idDateOrdine, tipologiaOrdine.tipoOrdine, dateOrdini.dataConsegna, ordini.riepilogoUtente, ordini.inviato, ordini.idUtente;

GO
/****** Object:  View [gestioneOrdineAmiciConScostamento_1]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE VIEW [gestioneOrdineAmiciConScostamento_1]
AS
SELECT     idProdotto, idUtente, idDateOrdine, Prodotto, dataConsegna, tipoOrdine
FROM         riepilogoordinexProdottoUtente
WHERE     riepilogoUtente = 0
UNION ALL
SELECT     idProdotto, idUtente, idDateOrdine, Prodotto, dataConsegna, tipoOrdine
FROM         riepilogoordinexProdottoUtente
WHERE     riepilogoUtente = 1

GO
/****** Object:  View [gestioneOrdineAmiciConScostamento_Utenti]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE VIEW [gestioneOrdineAmiciConScostamento_Utenti]
AS
SELECT     idProdotto, idDateOrdine, idUtente, riepilogoUtente, importo, qta, inviato
FROM         dbo.riepilogoOrdinexProdottoUtente
WHERE     (riepilogoUtente = 1)

GO
/****** Object:  View [gestioneOrdineAmiciConScostamento_Amici]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE VIEW [gestioneOrdineAmiciConScostamento_Amici]
AS
SELECT     idProdotto, idDateOrdine, idUtente, riepilogoUtente, importo, qta, inviato
FROM         dbo.riepilogoOrdinexProdottoUtente
WHERE     (riepilogoUtente = 0)

GO
/****** Object:  View [SchedaContabile_internet]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
create view [SchedaContabile_internet]
as
SELECT SchedaContabile.*
FROM SchedaContabile;

GO
/****** Object:  View [gestioneOrdineAmiciConScostamento_2]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE VIEW [gestioneOrdineAmiciConScostamento_2]
AS
SELECT     idProdotto, idUtente, idDateOrdine, Prodotto, tipoOrdine, dataConsegna
FROM         dbo.gestioneOrdineAmiciConScostamento_1
GROUP BY idProdotto, idUtente, idDateOrdine, Prodotto, tipoOrdine, dataConsegna

GO
/****** Object:  View [SaldoContabile]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE VIEW [SaldoContabile] AS SELECT SchedaContabile.idUtente, SUM(CASE WHEN segno = '-' THEN importo * - 1 ELSE importo END) AS Saldo, utenti.cognome, utenti.nome, utenti.cognome + ' ' + dbo.utenti.nome as NomeCognome,  dbo.utenti.attivo, dbo.utenti.idReferente, dbo.utenti.ruolo FROM dbo.SchedaContabile INNER JOIN dbo.utenti ON dbo.SchedaContabile.idUtente = dbo.utenti.idUtente GROUP BY dbo.SchedaContabile.idUtente, utenti.cognome, utenti.nome, utenti.cognome + ' ' + dbo.utenti.nome, dbo.utenti.attivo, dbo.utenti.idReferente, dbo.utenti.ruolo
GO
/****** Object:  View [gestioneOrdineAmiciConScostamento]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE VIEW [gestioneOrdineAmiciConScostamento]
AS
SELECT     dbo.gestioneOrdineAmiciConScostamento_2.idProdotto, dbo.gestioneOrdineAmiciConScostamento_2.Prodotto,
                      dbo.gestioneOrdineAmiciConScostamento_2.idDateOrdine, dbo.gestioneOrdineAmiciConScostamento_2.tipoOrdine,
                      dbo.gestioneOrdineAmiciConScostamento_2.dataConsegna, ISNULL(dbo.gestioneOrdineAmiciConScostamento_Amici.importo, 0) AS importo,
                      ISNULL(dbo.gestioneOrdineAmiciConScostamento_Amici.qta, 0) AS qta, ISNULL(dbo.gestioneOrdineAmiciConScostamento_Utenti.importo, 0) AS importoUtente,
                      ISNULL(dbo.gestioneOrdineAmiciConScostamento_Utenti.qta, 0) AS qtaUtente, ISNULL(dbo.gestioneOrdineAmiciConScostamento_Amici.inviato, 0) AS inviato,
                      dbo.gestioneOrdineAmiciConScostamento_2.idUtente, ISNULL(dbo.gestioneOrdineAmiciConScostamento_Amici.qta, 0)
                      - ISNULL(dbo.gestioneOrdineAmiciConScostamento_Utenti.qta, 0) AS diffQta, ISNULL(dbo.gestioneOrdineAmiciConScostamento_Amici.importo, 0)
                      - ISNULL(dbo.gestioneOrdineAmiciConScostamento_Utenti.importo, 0) AS diffImporto
FROM         dbo.gestioneOrdineAmiciConScostamento_2 LEFT OUTER JOIN
                      dbo.gestioneOrdineAmiciConScostamento_Utenti ON
                      dbo.gestioneOrdineAmiciConScostamento_2.idProdotto = dbo.gestioneOrdineAmiciConScostamento_Utenti.idProdotto AND
                      dbo.gestioneOrdineAmiciConScostamento_2.idDateOrdine = dbo.gestioneOrdineAmiciConScostamento_Utenti.idDateOrdine AND
                      dbo.gestioneOrdineAmiciConScostamento_2.idUtente = dbo.gestioneOrdineAmiciConScostamento_Utenti.idUtente LEFT OUTER JOIN
                      dbo.gestioneOrdineAmiciConScostamento_Amici ON
                      dbo.gestioneOrdineAmiciConScostamento_2.idProdotto = dbo.gestioneOrdineAmiciConScostamento_Amici.idProdotto AND
                      dbo.gestioneOrdineAmiciConScostamento_2.idDateOrdine = dbo.gestioneOrdineAmiciConScostamento_Amici.idDateOrdine AND
                      dbo.gestioneOrdineAmiciConScostamento_2.idUtente = dbo.gestioneOrdineAmiciConScostamento_Amici.idUtente

GO
/****** Object:  View [aequos_prodotti_non_ordinati]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE VIEW [aequos_prodotti_non_ordinati] AS
  select o.idDateOrdine, p.Prodotto, SUM(o.qtaOrdinata) as qtaOrdinata, p.pesoCassa, COALESCE(f.numeroColli, 0) as colliOrdinati
  from ordini o
  INNER JOIN prodotti p ON o.idProdotto = p.idProdotto
  INNER JOIN dateOrdini d ON o.idDateOrdine = d.idDateOrdini
  INNER JOIN tipologiaOrdine t ON t.idTipologiaOrdine = d.idTipologiaOrdine AND idOrdineAequos = 0
  LEFT JOIN ordiniFornitore f ON f.idDateOrdine = o.idDateOrdine and f.idProdotto = o.idProdotto
  where o.riepilogoUtente = 1 AND p.um = 'kg'
  group by o.idDateOrdine, o.idProdotto, p.Prodotto, p.pesoCassa, f.numeroColli

GO
/****** Object:  View [consultazione_ordini]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE VIEW [consultazione_ordini] AS
SELECT [idRigaOrdine], t.tipoOrdine, d.dataConsegna, p.Prodotto, u.cognome + ' ' + u.nome as utente,
      [idReferenteAmico],[qtaOrdinata],o.[um],[qtaRitirataKg],o.[prezzoKg],[riepilogoUtente],[contabilizzato]
      ,o.[inviato],o.[annullato],[inserimentoPostChiusura],[aspettoGenerale],[sapore]
  FROM [dbo].[ordini] o,
	   [dbo].[dateOrdini] d,
	   [dbo].[tipologiaOrdine] t,
	   [dbo].[prodotti] p,
	   [dbo].[utenti] u
where d.idDateOrdini = o.idDateOrdine
  and d.idTipologiaOrdine = t.idTipologiaOrdine
  and o.idProdotto = p.idProdotto
  and o.idUtente = u.idUtente

GO
/****** Object:  View [controlloConsolidato]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE VIEW [controlloConsolidato]
AS
SELECT ordini.idDateOrdine, tipologiaOrdine.tipoOrdine, dateOrdini.dataConsegna, Round(Sum(CASE
WHEN [totaleCalcolato]=1 THEN [qtaRitirataKg]*[prezzoKg] ELSE [prezzoKg]
END),2) AS importo, ordini.riepilogoUtente
FROM tipologiaOrdine INNER JOIN ((ordini INNER JOIN utenti ON ordini.idUtente = utenti.idUtente) INNER JOIN dateOrdini ON ordini.idDateOrdine = dateOrdini.idDateOrdini) ON tipologiaOrdine.idTipologiaOrdine = dateOrdini.idTipologiaOrdine
GROUP BY ordini.idDateOrdine, tipologiaOrdine.tipoOrdine, dateOrdini.dataConsegna, ordini.riepilogoUtente;

GO
/****** Object:  View [ElencoDaConsegnareXUtente]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE VIEW [ElencoDaConsegnareXUtente]
as
SELECT tipologiaOrdine.idTipologiaOrdine, tipologiaOrdine.tipoOrdine, dateOrdini.idDateOrdini, dateOrdini.dataConsegna, dateOrdini.dataChiusura, dateOrdini.oraChiusura, ordini.idUtente, utenti.nome, utenti.cognome, ordini.idProdotto, prodotti.Prodotto, prodotti.idCategoria, categoriaProdotti.descrizione AS Categoria, prodotti.PrezzoKg, Sum(ordini.qtaordinata) AS qtaDaConsegnare, ordini.inviato, ordini.annullato, Sum(CASE WHEN [pesoCassa]=0 THEN 0 ELSE cast(qtaordinata/[pesoCassa] as int) END) AS NrCasseIntere, Sum(CASE WHEN [pesoCassa]=0 THEN 0 ELSE[qtaordinata]-(cast([qtaordinata]/[pesoCassa] as int)*[pesoCassa]) end) AS NrKgSfusi, prodotti.pesoCassa, prodotti.um
FROM (dateOrdini INNER JOIN tipologiaOrdine ON dateOrdini.idTipologiaOrdine = tipologiaOrdine.idTipologiaOrdine)
INNER JOIN ((prodotti INNER JOIN categoriaProdotti ON prodotti.idCategoria = categoriaProdotti.idCategoriaProdotto) INNER JOIN (utenti INNER JOIN ordini ON (utenti.idUtente = ordini.idUtente) AND (utenti.idUtente = ordini.idUtente)) ON prodotti.idProdotto = ordini.idProdotto) ON dateOrdini.idDateOrdini = ordini.idDateOrdine
GROUP BY tipologiaOrdine.idTipologiaOrdine, tipologiaOrdine.tipoOrdine, dateOrdini.idDateOrdini, dateOrdini.dataConsegna, dateOrdini.dataChiusura, dateOrdini.oraChiusura, ordini.idUtente, utenti.nome, utenti.cognome, ordini.idProdotto, prodotti.Prodotto, prodotti.idCategoria, categoriaProdotti.descrizione, prodotti.PrezzoKg, ordini.inviato, ordini.annullato, prodotti.pesoCassa, prodotti.um;

GO
/****** Object:  View [notificationPrefsView]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE VIEW [notificationPrefsView] AS
SELECT COALESCE(pt.id, pg.id) AS id
	  ,pg.idUtente
	  ,pg.idTipologiaOrdine
	  ,CASE WHEN pt.id IS NULL THEN 'G' ELSE 'T' END AS tipo
	  ,COALESCE(pt.apertura, pg.apertura) AS apertura
	  ,COALESCE(pt.scadenza, pg.scadenza) AS scadenza
	  ,COALESCE(pt.minutiScadenza, pg.minutiScadenza) AS minutiScadenza
	  ,COALESCE(pt.consegna, pg.consegna) AS consegna
	  ,COALESCE(pt.minutiConsegna, pg.minutiConsegna) AS minutiConsegna
	  ,COALESCE(pt.aggiornamentoQta, pg.aggiornamentoQta) AS aggiornamentoQta
	  ,COALESCE(pt.contabilizzazione, pg.contabilizzazione) AS contabilizzazione
 FROM (SELECT t.idTipologiaOrdine, id, idUtente, apertura, scadenza, minutiScadenza, consegna, minutiConsegna, aggiornamentoQta, contabilizzazione FROM tipologiaOrdine t, notificationPrefs p WHERE p.idTipologiaOrdine IS NULL) pg
  LEFT OUTER JOIN notificationPrefs pt ON pg.idUtente = pt.idUtente AND pg.idTipologiaOrdine = pt.idTipologiaOrdine
GO
/****** Object:  View [qtaDaRitirare]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE VIEW [qtaDaRitirare]
AS
SELECT     dbo.ordini.idDateOrdine, dbo.tipologiaOrdine.tipoOrdine, dbo.dateOrdini.dataConsegna, dbo.ordini.idProdotto, dbo.prodotti.Prodotto, SUM(dbo.ordini.qtaRitirataKg)
                      AS TotQtaDaRitirare, dbo.prodotti.um, dbo.prodotti.pesoCassa, dbo.ordini.riepilogoUtente, dbo.prodotti.umCollo
FROM         dbo.tipologiaOrdine INNER JOIN
                      dbo.dateOrdini INNER JOIN
                      dbo.prodotti INNER JOIN
                      dbo.ordini ON dbo.prodotti.idProdotto = dbo.ordini.idProdotto ON dbo.dateOrdini.idDateOrdini = dbo.ordini.idDateOrdine ON
                      dbo.tipologiaOrdine.idTipologiaOrdine = dbo.dateOrdini.idTipologiaOrdine
GROUP BY dbo.ordini.idDateOrdine, dbo.tipologiaOrdine.tipoOrdine, dbo.dateOrdini.dataConsegna, dbo.ordini.idProdotto, dbo.prodotti.Prodotto, dbo.prodotti.um,
                      dbo.prodotti.pesoCassa, dbo.ordini.riepilogoUtente, dbo.ordini.inviato, dbo.ordini.annullato, dbo.prodotti.umCollo
HAVING      (dbo.ordini.inviato = 1) AND (dbo.ordini.riepilogoUtente = 1)

GO
/****** Object:  View [responsabilixTipoOrdine]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE VIEW [responsabilixTipoOrdine]
AS
SELECT tipologiaOrdine.idTipologiaOrdine, tipologiaOrdine.tipoOrdine, responsabili.idUtente
FROM tipologiaOrdine INNER JOIN responsabili ON tipologiaOrdine.idTipologiaOrdine = responsabili.idTipologiaOrdine;

GO
/****** Object:  View [riepilogoOrdinexProdotto]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE VIEW [riepilogoOrdinexProdotto]
AS
SELECT ordini.idProdotto, prodotti.prodotto, ordini.idDateOrdine, tipologiaOrdine.tipoOrdine, dateOrdini.dataConsegna, Round(Sum(CASE WHEN [totaleCalcolato]=1 THEN [qtaRitirataKg]*[ordini].[prezzoKg] ELSE[ordini].[prezzoKg] END),2) AS importo, Sum(ordini.qtaRitirataKg) AS qta, ordini.riepilogoUtente, ordini.inviato
FROM (dateOrdini INNER JOIN (ordini INNER JOIN prodotti ON ordini.idProdotto = prodotti.idProdotto) ON dateOrdini.idDateOrdini = ordini.idDateOrdine) INNER JOIN tipologiaOrdine ON dateOrdini.idTipologiaOrdine = tipologiaOrdine.idTipologiaOrdine
GROUP BY ordini.idProdotto, prodotti.prodotto, ordini.idDateOrdine, tipologiaOrdine.tipoOrdine, dateOrdini.dataConsegna, ordini.riepilogoUtente, ordini.inviato;

GO
/****** Object:  View [riepilogoValutazioneProdotti]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE VIEW [riepilogoValutazioneProdotti]
AS
SELECT     dbo.ordini.idDateOrdine, dbo.ordini.idProdotto, AVG(dbo.ordini.aspettoGenerale) AS aspettoGenerale, AVG(dbo.ordini.sapore) AS sapore, dbo.ordini.riepilogoUtente,
                      dbo.ordini.idUtente, dbo.prodotti.Prodotto
FROM         dbo.ordini INNER JOIN
                      dbo.prodotti ON dbo.ordini.idProdotto = dbo.prodotti.idProdotto
WHERE     (dbo.ordini.inviato = 1) AND (dbo.ordini.aspettoGenerale <> 0)
GROUP BY dbo.ordini.idDateOrdine, dbo.ordini.idProdotto, dbo.ordini.riepilogoUtente, dbo.ordini.idUtente, dbo.prodotti.Prodotto
HAVING      (dbo.ordini.riepilogoUtente = 1)

GO
/****** Object:  View [riepilogoValutazioneProdottiAmici]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE VIEW [riepilogoValutazioneProdottiAmici]
AS
SELECT     dbo.ordini.idDateOrdine, dbo.ordini.idProdotto, AVG(dbo.ordini.aspettoGenerale) AS aspettoGenerale, AVG(dbo.ordini.sapore) AS sapore, dbo.ordini.riepilogoUtente,
                      dbo.prodotti.Prodotto, dbo.ordini.idUtente
FROM         dbo.ordini INNER JOIN
                      dbo.prodotti ON dbo.ordini.idProdotto = dbo.prodotti.idProdotto
WHERE     (dbo.ordini.inviato = 1) AND (dbo.ordini.aspettoGenerale <> 0)
GROUP BY dbo.ordini.idDateOrdine, dbo.ordini.idProdotto, dbo.ordini.riepilogoUtente, dbo.prodotti.Prodotto, dbo.ordini.idUtente
HAVING      (dbo.ordini.riepilogoUtente = 0)

GO
/****** Object:  View [riepilogoValutazioneProdottiUtente]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE VIEW [riepilogoValutazioneProdottiUtente]
AS
SELECT     dbo.ordini.idDateOrdine, dbo.ordini.idProdotto, AVG(dbo.ordini.aspettoGenerale) AS aspettoGenerale, AVG(dbo.ordini.sapore) AS sapore, dbo.ordini.riepilogoUtente,
                      dbo.ordini.idUtente, dbo.prodotti.Prodotto
FROM         dbo.ordini INNER JOIN
                      dbo.prodotti ON dbo.ordini.idProdotto = dbo.prodotti.idProdotto
WHERE     (dbo.ordini.inviato = 1) AND (dbo.ordini.aspettoGenerale <> 0)
GROUP BY dbo.ordini.idDateOrdine, dbo.ordini.idProdotto, dbo.ordini.riepilogoUtente, dbo.ordini.idUtente, dbo.prodotti.Prodotto

GO
/****** Object:  View [SchedaUtenteOrdine]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE VIEW [SchedaUtenteOrdine] AS SELECT tipologiaOrdine.idTipologiaOrdine, tipologiaOrdine.tipoOrdine, dateOrdini.idDateOrdini,  dateOrdini.dataConsegna, dateOrdini.dataChiusura, dateOrdini.oraChiusura, ordini.idUtente,  utenti.nome, utenti.cognome, ordini.idProdotto, prodotti.Prodotto, prodotti.idCategoria,  categoriaProdotti.descrizione AS Categoria, categoriaProdotti.ordine_listino, ordini.PrezzoKg, ordini.qtaRitirataKg AS qtaDaConsegnare,  ordini.inviato, ordini.annullato, CASE WHEN [pesoCassa]=0 THEN 0 ELSE cast(qtaRitirataKg/[pesoCassa] as int) END AS NrCasseIntere,  CASE WHEN [pesoCassa]=0 THEN 0 ELSE qtaRitirataKg -(cast([qtaRitirataKg]/[pesoCassa] as int)*[pesoCassa]) end AS NrKgSfusi,  prodotti.pesoCassa, prodotti.um FROM (dateOrdini INNER JOIN tipologiaOrdine ON dateOrdini.idTipologiaOrdine = tipologiaOrdine.idTipologiaOrdine)  INNER JOIN ((prodotti INNER JOIN categoriaProdotti ON prodotti.idCategoria = categoriaProdotti.idCategoriaProdotto) INNER JOIN  (utenti INNER JOIN ordini ON (utenti.idUtente = ordini.idUtente) AND (utenti.idUtente = ordini.idUtente)) ON prodotti.idProdotto = ordini.idProdotto) ON dateOrdini.idDateOrdini = ordini.idDateOrdine  WHERE ordini.riepilogoUtente = 1

GO
ALTER TABLE [anno] ADD  DEFAULT ((0)) FOR [chiuso]
GO
ALTER TABLE [categoriaProdotti] ADD  DEFAULT ((1)) FOR [ordine_listino]
GO
ALTER TABLE [categoriaProdotti] ADD  CONSTRAINT [DF_categoriaProdotti_colore_listino]  DEFAULT ('FFFFFF') FOR [colore_listino]
GO
ALTER TABLE [dateOrdini] ADD  DEFAULT ((0)) FOR [stato]
GO
ALTER TABLE [dateOrdini] ADD  DEFAULT ((0)) FOR [inviato]
GO
ALTER TABLE [dateOrdini] ADD  DEFAULT ((0)) FOR [speseTrasporto]
GO
ALTER TABLE [dateOrdini] ADD  CONSTRAINT [DF_dateOrdini_paid]  DEFAULT ((0)) FOR [paid]
GO
ALTER TABLE [movimenti] ADD  DEFAULT ((1)) FOR [confermato]
GO
ALTER TABLE [ordiniFornitore] ADD  CONSTRAINT [DF_ordiniFornitore_weightUpdated]  DEFAULT ((0)) FOR [weightUpdated]
GO
ALTER TABLE [tipologiaOrdine] ADD  DEFAULT ((1)) FOR [mostraPreventivo]
GO
ALTER TABLE [tipologiaOrdine] ADD  DEFAULT ((1)) FOR [mostraCompletamentoColli]
GO
ALTER TABLE [tipologiaOrdine] ADD  CONSTRAINT [DF_tipologiaOrdine_external]  DEFAULT ((0)) FOR [external]
GO
ALTER TABLE [tipologiaOrdine] ADD  CONSTRAINT [DF_tipologiaOrdine_excelAllUsers]  DEFAULT ((0)) FOR [excelAllUsers]
GO
ALTER TABLE [tipologiaOrdine] ADD  CONSTRAINT [DF_tipologiaOrdine_excelAllProducts]  DEFAULT ((0)) FOR [excelAllProducts]
GO
ALTER TABLE [categoriaProdotti]  WITH CHECK ADD FOREIGN KEY([idTipologiaOrdine])
REFERENCES [tipologiaOrdine] ([idTipologiaOrdine])
GO
ALTER TABLE [comunicazioni]  WITH CHECK ADD  CONSTRAINT [FK_comunicazioni_CreatoDa_utenti_idUtente] FOREIGN KEY([CreatoDa])
REFERENCES [utenti] ([idUtente])
GO
ALTER TABLE [comunicazioni] CHECK CONSTRAINT [FK_comunicazioni_CreatoDa_utenti_idUtente]
GO
ALTER TABLE [dateOrdini]  WITH CHECK ADD FOREIGN KEY([idTipologiaOrdine])
REFERENCES [tipologiaOrdine] ([idTipologiaOrdine])
GO
ALTER TABLE [menu]  WITH CHECK ADD FOREIGN KEY([parentMenu])
REFERENCES [menu] ([idMenu])
GO
ALTER TABLE [menuRuolo]  WITH CHECK ADD FOREIGN KEY([idMenu])
REFERENCES [menu] ([idMenu])
GO
ALTER TABLE [movimenti]  WITH CHECK ADD FOREIGN KEY([causale])
REFERENCES [causale] ([codiceCausale])
GO
ALTER TABLE [movimenti]  WITH CHECK ADD FOREIGN KEY([idDateOrdini])
REFERENCES [dateOrdini] ([idDateOrdini])
GO
ALTER TABLE [movimenti]  WITH CHECK ADD FOREIGN KEY([idUtente])
REFERENCES [utenti] ([idUtente])
GO
ALTER TABLE [movimentiGas]  WITH CHECK ADD  CONSTRAINT [fk_movimentiGas_causale] FOREIGN KEY([causale])
REFERENCES [causale] ([codiceCausale])
GO
ALTER TABLE [movimentiGas] CHECK CONSTRAINT [fk_movimentiGas_causale]
GO
ALTER TABLE [notificationPrefs]  WITH CHECK ADD  CONSTRAINT [fk_notifTipoOrdine] FOREIGN KEY([idTipologiaOrdine])
REFERENCES [tipologiaOrdine] ([idTipologiaOrdine])
GO
ALTER TABLE [notificationPrefs] CHECK CONSTRAINT [fk_notifTipoOrdine]
GO
ALTER TABLE [notificationPrefs]  WITH CHECK ADD  CONSTRAINT [fk_notifUtente] FOREIGN KEY([idUtente])
REFERENCES [utenti] ([idUtente])
GO
ALTER TABLE [notificationPrefs] CHECK CONSTRAINT [fk_notifUtente]
GO
ALTER TABLE [ordini]  WITH CHECK ADD FOREIGN KEY([idDateOrdine])
REFERENCES [dateOrdini] ([idDateOrdini])
GO
ALTER TABLE [ordini]  WITH CHECK ADD FOREIGN KEY([idProdottoSostituito])
REFERENCES [prodotti] ([idProdotto])
GO
ALTER TABLE [ordini]  WITH CHECK ADD FOREIGN KEY([idProdotto])
REFERENCES [prodotti] ([idProdotto])
GO
ALTER TABLE [ordini]  WITH CHECK ADD FOREIGN KEY([idReferenteAmico])
REFERENCES [utenti] ([idUtente])
GO
ALTER TABLE [ordini]  WITH CHECK ADD FOREIGN KEY([idUtente])
REFERENCES [utenti] ([idUtente])
GO
ALTER TABLE [ordiniFornitore]  WITH CHECK ADD FOREIGN KEY([idDateOrdine])
REFERENCES [dateOrdini] ([idDateOrdini])
GO
ALTER TABLE [ordiniFornitore]  WITH CHECK ADD FOREIGN KEY([idProdotto])
REFERENCES [prodotti] ([idProdotto])
GO
ALTER TABLE [prodotti]  WITH CHECK ADD FOREIGN KEY([idCategoria])
REFERENCES [categoriaProdotti] ([idCategoriaProdotto])
GO
ALTER TABLE [prodotti]  WITH CHECK ADD FOREIGN KEY([idProduttore])
REFERENCES [produttori] ([idProduttore])
GO
ALTER TABLE [prodotti]  WITH CHECK ADD FOREIGN KEY([idTipoProd])
REFERENCES [tipologiaOrdine] ([idTipologiaOrdine])
GO
ALTER TABLE [pushToken]  WITH CHECK ADD  CONSTRAINT [fk_pushUtente] FOREIGN KEY([idUtente])
REFERENCES [utenti] ([idUtente])
GO
ALTER TABLE [pushToken] CHECK CONSTRAINT [fk_pushUtente]
GO
ALTER TABLE [responsabili]  WITH CHECK ADD FOREIGN KEY([idTipologiaOrdine])
REFERENCES [tipologiaOrdine] ([idTipologiaOrdine])
GO
ALTER TABLE [responsabili]  WITH CHECK ADD FOREIGN KEY([idUtente])
REFERENCES [utenti] ([idUtente])
GO
ALTER TABLE [speseTrasporto]  WITH CHECK ADD  CONSTRAINT [FK_speseTrasporto_dateOrdini] FOREIGN KEY([idDateOrdini])
REFERENCES [dateOrdini] ([idDateOrdini])
GO
ALTER TABLE [speseTrasporto] CHECK CONSTRAINT [FK_speseTrasporto_dateOrdini]
GO
ALTER TABLE [speseTrasporto]  WITH CHECK ADD  CONSTRAINT [FK_speseTrasporto_utenti] FOREIGN KEY([idUtente])
REFERENCES [utenti] ([idUtente])
GO
ALTER TABLE [speseTrasporto] CHECK CONSTRAINT [FK_speseTrasporto_utenti]
GO
ALTER TABLE [turni]  WITH CHECK ADD FOREIGN KEY([idDateOrdini])
REFERENCES [dateOrdini] ([idDateOrdini])
GO
ALTER TABLE [turni]  WITH CHECK ADD FOREIGN KEY([idTipologiaTurno])
REFERENCES [tipologiaTurno] ([idTipologiaTurno])
GO
ALTER TABLE [turni]  WITH CHECK ADD FOREIGN KEY([idUtente])
REFERENCES [utenti] ([idUtente])
GO
ALTER TABLE [utenti]  WITH CHECK ADD FOREIGN KEY([idReferente])
REFERENCES [utenti] ([idUtente])
GO
CREATE UNIQUE CLUSTERED INDEX IDX_utenti_position
ON utenti ([position] ASC)
GO
/****** Object:  StoredProcedure [CalendarioInternet_Get]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
-- =============================================
-- Author:		<Author,,Name>
-- Create date: <Create Date,,>
-- Description:	<Description,,>
-- =============================================
CREATE PROCEDURE [CalendarioInternet_Get]
	-- Add the parameters for the stored procedure here
	@data date
AS
BEGIN
	-- SET NOCOUNT ON added to prevent extra result sets from
	-- interfering with SELECT statements.
	SET NOCOUNT ON;
    -- Insert statements for procedure here
	SELECT Data, Tipo, Campo1, Campo2 FROM calendarioInternet WHERE (Data >= @data) ORDER BY Data
END

GO
/****** Object:  StoredProcedure [CategoriaProdotti_Get_ByTipoOrdine]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
-- =============================================
-- Author:		<Author,,Name>
-- Create date: <Create Date,,>
-- Description:	<Description,,>
-- =============================================
CREATE PROCEDURE [CategoriaProdotti_Get_ByTipoOrdine]
	@idTipo uniqueidentifier
AS
BEGIN
	-- SET NOCOUNT ON added to prevent extra result sets from
	-- interfering with SELECT statements.
	SET NOCOUNT ON;
    -- Insert statements for procedure here
	SELECT idCategoriaProdotto, descrizione FROM categoriaProdotti WHERE idTipologiaOrdine = @idTipo ORDER BY descrizione
END

GO
/****** Object:  StoredProcedure [GetriepilogoOrdinexProdottoByDataOrdine]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
-- =============================================
-- Author:		<Author,,Name>
-- Create date: <Create Date,,>
-- Description:	<Description,,>
-- =============================================
CREATE PROCEDURE [GetriepilogoOrdinexProdottoByDataOrdine]
	-- Add the parameters for the stored procedure here
	@idDateOrdine uniqueidentifier,
	@riepilogoUtente bit
AS
BEGIN
	-- SET NOCOUNT ON added to prevent extra result sets from
	-- interfering with SELECT statements.
	SET NOCOUNT ON;
	SELECT [idProdotto], [prodotto], [qta], [importo], [inviato] FROM [riepilogoOrdinexProdotto] WHERE ([idDateOrdine] = @idDateOrdine) AND ([riepilogoUtente] = @riepilogoUtente) ORDER BY [prodotto]

END

GO
/****** Object:  StoredProcedure [Prodotti_Get_ByTipoCategoriaStatoAnnullato]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
-- =============================================
-- Author:		<Author,,Name>
-- Create date: <Create Date,,>
-- Description:	<Description,,>
-- =============================================
CREATE PROCEDURE [Prodotti_Get_ByTipoCategoriaStatoAnnullato]
	-- Add the parameters for the stored procedure here
	@idTipologia varchar(36),
	@idCategoria varchar(36),
	@idOrdinabile varchar(5),
	@idAnnullato bit
AS
BEGIN
	-- SET NOCOUNT ON added to prevent extra result sets from
	-- interfering with SELECT statements.
	SET NOCOUNT ON;
    -- Insert statements for procedure here
	SELECT * from prodotti WHERE
	idTipoProd = COALESCE(NULLIF(@idTipologia, ''), idTipoProd) AND
	idCategoria=CASE WHEN @idCategoria IS NULL THEN idCategoria
					WHEN LEN(@idCategoria)=36 THEN CAST(@idCategoria AS UNIQUEIDENTIFIER)
					 ELSE idCategoria END AND
	acquistabile=CASE WHEN @idOrdinabile='true' THEN 1
					WHEN @idOrdinabile='false' THEN 0
					ELSE acquistabile END AND
	annullato=COALESCE(NULLIF(@idAnnullato, ''), annullato)
END

GO
/****** Object:  StoredProcedure [Produttori_Get]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
-- =============================================
-- Author:		<Author,,Name>
-- Create date: <Create Date,,>
-- Description:	<Description,,>
-- =============================================
CREATE PROCEDURE [Produttori_Get]
AS
BEGIN
	-- SET NOCOUNT ON added to prevent extra result sets from
	-- interfering with SELECT statements.
	SET NOCOUNT ON;
    -- Insert statements for procedure here
	SELECT idProduttore, ragioneSociale FROM produttori ORDER BY ragioneSociale
END

GO
/****** Object:  StoredProcedure [QtaDaOrdinareStato_Get_ByIdDateOrdine]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
-- =============================================
-- Author:		<Author,,Name>
-- Create date: <Create Date,,>
-- Description:	<Description,,>
-- =============================================
CREATE PROCEDURE [QtaDaOrdinareStato_Get_ByIdDateOrdine]
	-- Add the parameters for the stored procedure here
	@idDateOrdine uniqueidentifier,
	@riepilogoUtente bit
AS
BEGIN
	-- SET NOCOUNT ON added to prevent extra result sets from
	-- interfering with SELECT statements.
	SET NOCOUNT ON;
    -- Insert statements for procedure here
	SELECT [idProdotto], [prodotto], [QtaDaOrdinare], [um], [pesoCassa], [NrCasse], [NrKg] FROM [QtaDaOrdinareStato] WHERE (([idDateOrdine] = @idDateOrdine) AND ([riepilogoUtente] = @riepilogoUtente)) ORDER BY [prodotto]
END

GO
/****** Object:  StoredProcedure [QtaDaRitirare_Get]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
-- =============================================
-- Author:		<Author,,Name>
-- Create date: <Create Date,,>
-- Description:	<Description,,>
-- =============================================
CREATE PROCEDURE [QtaDaRitirare_Get]
	-- Add the parameters for the stored procedure here
	@idDataOrdine uniqueidentifier
AS
BEGIN
	-- SET NOCOUNT ON added to prevent extra result sets from
	-- interfering with SELECT statements.
	SET NOCOUNT ON;
    -- Insert statements for procedure here
	SELECT idDateOrdine, tipoOrdine, dataConsegna, idProdotto, prodotto, TotQtaDaRitirare, um, pesoCassa, riepilogoUtente, umCollo FROM qtaDaRitirare WHERE idDateOrdine=@idDataOrdine order by prodotto asc;
END

GO
/****** Object:  StoredProcedure [ResponsabiliPerTipoOrdine_Get_ByIdResponsabile]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
-- =============================================
-- Author:		<Author,,Name>
-- Create date: <Create Date,,>
-- Description:	<Description,,>
-- =============================================
CREATE PROCEDURE [ResponsabiliPerTipoOrdine_Get_ByIdResponsabile]
	-- Add the parameters for the stored procedure here
	@idResponsabile uniqueidentifier
AS
BEGIN
	-- SET NOCOUNT ON added to prevent extra result sets from
	-- interfering with SELECT statements.
	SET NOCOUNT ON;
    -- Insert statements for procedure here
	SELECT idTipologiaOrdine, tipoOrdine, idUtente FROM responsabilixTipoOrdine
	WHERE idUtente=@idResponsabile;
END

GO
/****** Object:  StoredProcedure [SaldoContabile_Get_ByIdUtente]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
-- =============================================
-- Author:		<Author,,Name>
-- Create date: <Create Date,,>
-- Description:	<Description,,>
-- =============================================
CREATE PROCEDURE [SaldoContabile_Get_ByIdUtente]
	-- Add the parameters for the stored procedure here
	@idUtente uniqueidentifier,
	@saldo float OUT
AS
BEGIN
	-- SET NOCOUNT ON added to prevent extra result sets from
	-- interfering with SELECT statements.
	SET NOCOUNT ON;
    -- Insert statements for procedure here
	SELECT @saldo=Round([Saldo],2) FROM [SaldoContabile] WHERE idUtente=@idUtente;
END

GO
/****** Object:  StoredProcedure [TipologiaOrdine_Get]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
-- =============================================
-- Author:		<Author,,Name>
-- Create date: <Create Date,,>
-- Description:	<Description,,>
-- =============================================
CREATE PROCEDURE [TipologiaOrdine_Get]
AS
BEGIN
	-- SET NOCOUNT ON added to prevent extra result sets from
	-- interfering with SELECT statements.
	SET NOCOUNT ON;
    -- Insert statements for procedure here
	SELECT idTipologiaOrdine, tipoOrdine FROM tipologiaOrdine ORDER BY tipoOrdine
END

GO
/****** Object:  StoredProcedure [Utenti_Get_ByRuolo]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
-- =============================================
-- Author:		<Author,,Name>
-- Create date: <Create Date,,>
-- Description:	<Description,,>
-- =============================================
CREATE PROCEDURE [Utenti_Get_ByRuolo]
	-- Add the parameters for the stored procedure here
	@ruolo varchar(1)

AS
BEGIN
	-- SET NOCOUNT ON added to prevent extra result sets from
	-- interfering with SELECT statements.
	SET NOCOUNT ON;
    -- Insert statements for procedure here
	SELECT [idUtente], [nome] + ' ' + [cognome] as NomeCognome FROM [utenti] WHERE ([ruolo] = @ruolo) ORDER BY [nome]
END

GO
/****** Object:  StoredProcedure [Utenti_GetAll]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
-- =============================================
-- Author:		<Author,,Name>
-- Create date: <Create Date,,>
-- Description:	<Description,,>
-- =============================================
CREATE PROCEDURE [Utenti_GetAll]
	-- Add the parameters for the stored procedure here

AS
BEGIN
	-- SET NOCOUNT ON added to prevent extra result sets from
	-- interfering with SELECT statements.
	SET NOCOUNT ON;
    -- Insert statements for procedure here
	SELECT [idUtente], [nome] + ' ' + [cognome] as NomeCognome FROM [utenti] ORDER BY [nome];
END

GO
/****** Object:  StoredProcedure [ValidateUser]    Script Date: 11/12/2019 22:43:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE PROCEDURE [ValidateUser]
	-- Add the parameters for the stored procedure here
	@user varchar(30),
	@pwd varchar(100)
AS
BEGIN
	-- SET NOCOUNT ON added to prevent extra result sets from
	-- interfering with SELECT statements.
	SET NOCOUNT ON;

    -- Insert statements for procedure here
	SELECT * FROM utenti where utente=@user and pwd=CONVERT(VARCHAR(100), HASHBYTES('SHA1', @pwd), 2) and attivo=1

	RETURN 0;
END
GO

/**************** ADDITIONAL INDEXES **********************/

CREATE INDEX IDX_dataOrdine_utente ON ordini (idDateOrdine ASC, idUtente ASC
INCLUDE (riepilogoUtente, qtaRitirataKg, prezzoKg, idReferenteAmico, contabilizzato)

CREATE INDEX IDX_spese_trasporto ON speseTrasporto (idDateOrdini ASC, idUtente ASC)
INCLUDE (importo)

CREATE UNIQUE INDEX IDX_dateOrdine_tipologia
ON dateOrdini (idTipologiaOrdine ASC, dataConsegna DESC, idDateOrdini ASC)

CREATE INDEX IDX_movimento_utente ON movimenti (confermato ASC, idUtente ASC, causale DESC)
INCLUDE (idMovimento, dataMovimento, idReferente, descrizione, importo, idDateOrdini)

/******************** INITIAL DATA ********************************************/

--- MENU
INSERT [dbo].[menu] ([idMenu], [label], [url], [parentMenu], [external]) VALUES (N'76a5aa4d-1b92-4663-bc03-2bf11e4a97a3', N'Manuali', N'#MANUALI#', NULL, 0)
INSERT [dbo].[menu] ([idMenu], [label], [url], [parentMenu], [external]) VALUES (N'93c66a2f-faf9-46ce-a347-58c25bf0bfbb', N'Referente', N'#REFERENTE#', NULL, 0)
INSERT [dbo].[menu] ([idMenu], [label], [url], [parentMenu], [external]) VALUES (N'6739dc1a-3d88-49d0-b89c-c569def3b1fe', N'Utente', N'#UTENTE#', NULL, 0)
INSERT [dbo].[menu] ([idMenu], [label], [url], [parentMenu], [external]) VALUES (N'330c7ebf-39a9-48cf-8172-a9ba97a3297c', N'Report', N'#REPORT#', NULL, 0)
INSERT [dbo].[menu] ([idMenu], [label], [url], [parentMenu], [external]) VALUES (N'24514070-cb62-40e1-8e77-ed91d8e7227d', N'Contabilità', N'#CONTABILITA#', NULL, 0)
INSERT [dbo].[menu] ([idMenu], [label], [url], [parentMenu], [external]) VALUES (N'218a28b7-262a-46aa-a44a-fae95ff45078', N'Amministratore', N'#AMMINISTRATORE#', NULL, 0)
INSERT [dbo].[menu] ([idMenu], [label], [url], [parentMenu], [external]) VALUES (N'0084223e-31f7-431f-b259-031868f42cad', N'Tool per smistamento', N'/tools/smistamento.exe', N'93c66a2f-faf9-46ce-a347-58c25bf0bfbb', 1)
INSERT [dbo].[menu] ([idMenu], [label], [url], [parentMenu], [external]) VALUES (N'd64ec25c-0502-4f42-9081-0dbba4946685', N'Gestione movimenti GAS', N'/Contabilita/ListMovimentiGas', N'24514070-cb62-40e1-8e77-ed91d8e7227d', 0)
INSERT [dbo].[menu] ([idMenu], [label], [url], [parentMenu], [external]) VALUES (N'0af46cc7-30cf-46d2-9b0e-2ea13753904f', N'Utenti', N'/Utenti/List', N'218a28b7-262a-46aa-a44a-fae95ff45078', 0)
INSERT [dbo].[menu] ([idMenu], [label], [url], [parentMenu], [external]) VALUES (N'afed90c9-d0dd-42f2-914b-413b44927820', N'Situazione utenti', N'/Contabilita/SituazioneConti', N'24514070-cb62-40e1-8e77-ed91d8e7227d', 0)
INSERT [dbo].[menu] ([idMenu], [label], [url], [parentMenu], [external]) VALUES (N'66e814a4-9a78-4eb9-9f63-531aba192277', N'Tipologie ordine', N'/TipoOrdine/List', N'218a28b7-262a-46aa-a44a-fae95ff45078', 0)
INSERT [dbo].[menu] ([idMenu], [label], [url], [parentMenu], [external]) VALUES (N'a379eccc-2a45-4b63-aa75-94efedccbeb9', N'Inserimento ordini', N'/Ordini/Aperti', N'6739dc1a-3d88-49d0-b89c-c569def3b1fe', 0)
INSERT [dbo].[menu] ([idMenu], [label], [url], [parentMenu], [external]) VALUES (N'bfdd7de4-259d-44e3-9fce-96bdabebc450', N'Produttori', N'/Produttori/List', N'218a28b7-262a-46aa-a44a-fae95ff45078', 0)
INSERT [dbo].[menu] ([idMenu], [label], [url], [parentMenu], [external]) VALUES (N'a629e00c-e65a-4e2a-bf38-9a65a3b1a7ac', N'Ordinanti', N'/Report/Ordinanti', N'330c7ebf-39a9-48cf-8172-a9ba97a3297c', 0)
INSERT [dbo].[menu] ([idMenu], [label], [url], [parentMenu], [external]) VALUES (N'657ce8ae-4604-4721-ab8e-9f40eff79812', N'Storico ordini', N'/Ordini/Storico', N'6739dc1a-3d88-49d0-b89c-c569def3b1fe', 0)
INSERT [dbo].[menu] ([idMenu], [label], [url], [parentMenu], [external]) VALUES (N'17f1062a-ed92-4492-b564-9f72bb16a698', N'Configurazione generale', N'/Configurazione/List', N'218a28b7-262a-46aa-a44a-fae95ff45078', 0)
INSERT [dbo].[menu] ([idMenu], [label], [url], [parentMenu], [external]) VALUES (N'1a407c8b-fe4b-4952-9752-b0dc575c0df5', N'Manuale referente', N'/Manuali/GoGas - Manuale Referente.pdf', N'76a5aa4d-1b92-4663-bc03-2bf11e4a97a3', 1)
INSERT [dbo].[menu] ([idMenu], [label], [url], [parentMenu], [external]) VALUES (N'098d116a-b0f3-466c-9ad6-c0c3524c299c', N'Manuale utente', N'/Manuali/GoGas - Manuale Utente.pdf', N'76a5aa4d-1b92-4663-bc03-2bf11e4a97a3', 1)
INSERT [dbo].[menu] ([idMenu], [label], [url], [parentMenu], [external]) VALUES (N'3ac0204f-3def-4571-a01b-c2cff9ed83e7', N'Prodotti', N'/Prodotti/List', N'93c66a2f-faf9-46ce-a347-58c25bf0bfbb', 0)
INSERT [dbo].[menu] ([idMenu], [label], [url], [parentMenu], [external]) VALUES (N'2e61877e-c5d4-4f0e-9743-c699c75bc580', N'Prodotti', N'/Prodotti/List', N'218a28b7-262a-46aa-a44a-fae95ff45078', 0)
INSERT [dbo].[menu] ([idMenu], [label], [url], [parentMenu], [external]) VALUES (N'3a9ae6bb-1459-4a37-b4a0-cb1c57cf9c46', N'Causali', N'/Causali/List', N'218a28b7-262a-46aa-a44a-fae95ff45078', 0)
INSERT [dbo].[menu] ([idMenu], [label], [url], [parentMenu], [external]) VALUES (N'4b82686b-fb47-4b7a-a4af-e913b54f7473', N'Gestione ordini', N'/GestioneOrdini/List', N'93c66a2f-faf9-46ce-a347-58c25bf0bfbb', 0)
INSERT [dbo].[menu] ([idMenu], [label], [url], [parentMenu], [external]) VALUES (N'693a5b25-0d6b-4993-9ac6-f9b2345c6551', N'Gestione movimenti', N'/Contabilita/List', N'24514070-cb62-40e1-8e77-ed91d8e7227d', 0)
INSERT [dbo].[menu] ([idMenu], [label], [url], [parentMenu], [external]) VALUES (N'9477c97c-42a1-41b4-92a4-fb300ef95312', N'Manuale amministratore', N'/Manuali/GoGas - Manuale Amministratore.pdf', N'76a5aa4d-1b92-4663-bc03-2bf11e4a97a3', 1)
INSERT [dbo].[menu] ([idMenu], [label], [url], [parentMenu], [external]) VALUES (N'61067f4e-4c85-41fb-aca3-fcbd82515a99', N'Tool per smistamento (JAR)', N'/tools/smistamento.jar', N'93c66a2f-faf9-46ce-a347-58c25bf0bfbb', 1)


--MENU AMICI
--INSERT [dbo].[menu] ([idMenu], [label], [url], [parentMenu]) VALUES (N'7d33e9b0-07a0-4daa-b85c-b02f4d0ebe6f', N'Amici', NULL, NULL)
--INSERT [dbo].[menu] ([idMenu], [label], [url], [parentMenu]) VALUES (N'42f271b2-43e0-4f02-8b23-6ecc5823ec4e', N'Situazione conti', N'/Contabilita/SituazioneConti?amici=true', N'7d33e9b0-07a0-4daa-b85c-b02f4d0ebe6f')
--INSERT [dbo].[menu] ([idMenu], [label], [url], [parentMenu]) VALUES (N'7bdf4bc7-50bf-411e-9966-39933d729828', N'Gestione movimenti', N'/Contabilita/List?amici=true', N'7d33e9b0-07a0-4daa-b85c-b02f4d0ebe6f')
--INSERT [dbo].[menu] ([idMenu], [label], [url], [parentMenu]) VALUES (N'111d6698-59d4-46d7-8887-4944a1012e3b', N'Gestione amici', N'/Utenti/AmiciList', N'7d33e9b0-07a0-4daa-b85c-b02f4d0ebe6f')

--- MENU_RUOLO
INSERT [dbo].[menuRuolo] ([idMenu], [ruolo], [ordine]) VALUES (N'0084223e-31f7-431f-b259-031868f42cad', N'R', 3)
INSERT [dbo].[menuRuolo] ([idMenu], [ruolo], [ordine]) VALUES (N'd64ec25c-0502-4f42-9081-0dbba4946685', N'A', 3)
INSERT [dbo].[menuRuolo] ([idMenu], [ruolo], [ordine]) VALUES (N'76a5aa4d-1b92-4663-bc03-2bf11e4a97a3', N'A', 5)
INSERT [dbo].[menuRuolo] ([idMenu], [ruolo], [ordine]) VALUES (N'76a5aa4d-1b92-4663-bc03-2bf11e4a97a3', N'R', 5)
INSERT [dbo].[menuRuolo] ([idMenu], [ruolo], [ordine]) VALUES (N'76a5aa4d-1b92-4663-bc03-2bf11e4a97a3', N'U', 5)
INSERT [dbo].[menuRuolo] ([idMenu], [ruolo], [ordine]) VALUES (N'0af46cc7-30cf-46d2-9b0e-2ea13753904f', N'A', 1)
INSERT [dbo].[menuRuolo] ([idMenu], [ruolo], [ordine]) VALUES (N'afed90c9-d0dd-42f2-914b-413b44927820', N'A', 1)
INSERT [dbo].[menuRuolo] ([idMenu], [ruolo], [ordine]) VALUES (N'66e814a4-9a78-4eb9-9f63-531aba192277', N'A', 1)
INSERT [dbo].[menuRuolo] ([idMenu], [ruolo], [ordine]) VALUES (N'93c66a2f-faf9-46ce-a347-58c25bf0bfbb', N'A', 2)
INSERT [dbo].[menuRuolo] ([idMenu], [ruolo], [ordine]) VALUES (N'93c66a2f-faf9-46ce-a347-58c25bf0bfbb', N'R', 2)
INSERT [dbo].[menuRuolo] ([idMenu], [ruolo], [ordine]) VALUES (N'a379eccc-2a45-4b63-aa75-94efedccbeb9', N'R', 1)
INSERT [dbo].[menuRuolo] ([idMenu], [ruolo], [ordine]) VALUES (N'a379eccc-2a45-4b63-aa75-94efedccbeb9', N'S', 1)
INSERT [dbo].[menuRuolo] ([idMenu], [ruolo], [ordine]) VALUES (N'a379eccc-2a45-4b63-aa75-94efedccbeb9', N'U', 1)
INSERT [dbo].[menuRuolo] ([idMenu], [ruolo], [ordine]) VALUES (N'bfdd7de4-259d-44e3-9fce-96bdabebc450', N'A', 1)
INSERT [dbo].[menuRuolo] ([idMenu], [ruolo], [ordine]) VALUES (N'a629e00c-e65a-4e2a-bf38-9a65a3b1a7ac', N'R', 1)
INSERT [dbo].[menuRuolo] ([idMenu], [ruolo], [ordine]) VALUES (N'657ce8ae-4604-4721-ab8e-9f40eff79812', N'R', 1)
INSERT [dbo].[menuRuolo] ([idMenu], [ruolo], [ordine]) VALUES (N'657ce8ae-4604-4721-ab8e-9f40eff79812', N'S', 1)
INSERT [dbo].[menuRuolo] ([idMenu], [ruolo], [ordine]) VALUES (N'657ce8ae-4604-4721-ab8e-9f40eff79812', N'U', 1)
INSERT [dbo].[menuRuolo] ([idMenu], [ruolo], [ordine]) VALUES (N'17f1062a-ed92-4492-b564-9f72bb16a698', N'A', 4)
INSERT [dbo].[menuRuolo] ([idMenu], [ruolo], [ordine]) VALUES (N'330c7ebf-39a9-48cf-8172-a9ba97a3297c', N'R', 4)
INSERT [dbo].[menuRuolo] ([idMenu], [ruolo], [ordine]) VALUES (N'1a407c8b-fe4b-4952-9752-b0dc575c0df5', N'A', 2)
INSERT [dbo].[menuRuolo] ([idMenu], [ruolo], [ordine]) VALUES (N'1a407c8b-fe4b-4952-9752-b0dc575c0df5', N'R', 2)
INSERT [dbo].[menuRuolo] ([idMenu], [ruolo], [ordine]) VALUES (N'098d116a-b0f3-466c-9ad6-c0c3524c299c', N'A', 1)
INSERT [dbo].[menuRuolo] ([idMenu], [ruolo], [ordine]) VALUES (N'098d116a-b0f3-466c-9ad6-c0c3524c299c', N'R', 1)
INSERT [dbo].[menuRuolo] ([idMenu], [ruolo], [ordine]) VALUES (N'098d116a-b0f3-466c-9ad6-c0c3524c299c', N'U', 1)
INSERT [dbo].[menuRuolo] ([idMenu], [ruolo], [ordine]) VALUES (N'3ac0204f-3def-4571-a01b-c2cff9ed83e7', N'R', 2)
INSERT [dbo].[menuRuolo] ([idMenu], [ruolo], [ordine]) VALUES (N'6739dc1a-3d88-49d0-b89c-c569def3b1fe', N'R', 1)
INSERT [dbo].[menuRuolo] ([idMenu], [ruolo], [ordine]) VALUES (N'6739dc1a-3d88-49d0-b89c-c569def3b1fe', N'S', 1)
INSERT [dbo].[menuRuolo] ([idMenu], [ruolo], [ordine]) VALUES (N'6739dc1a-3d88-49d0-b89c-c569def3b1fe', N'U', 1)
INSERT [dbo].[menuRuolo] ([idMenu], [ruolo], [ordine]) VALUES (N'2e61877e-c5d4-4f0e-9743-c699c75bc580', N'A', 1)
INSERT [dbo].[menuRuolo] ([idMenu], [ruolo], [ordine]) VALUES (N'3a9ae6bb-1459-4a37-b4a0-cb1c57cf9c46', N'A', 1)
INSERT [dbo].[menuRuolo] ([idMenu], [ruolo], [ordine]) VALUES (N'4b82686b-fb47-4b7a-a4af-e913b54f7473', N'A', 1)
INSERT [dbo].[menuRuolo] ([idMenu], [ruolo], [ordine]) VALUES (N'4b82686b-fb47-4b7a-a4af-e913b54f7473', N'R', 1)
INSERT [dbo].[menuRuolo] ([idMenu], [ruolo], [ordine]) VALUES (N'24514070-cb62-40e1-8e77-ed91d8e7227d', N'A', 1)
INSERT [dbo].[menuRuolo] ([idMenu], [ruolo], [ordine]) VALUES (N'693a5b25-0d6b-4993-9ac6-f9b2345c6551', N'A', 1)
INSERT [dbo].[menuRuolo] ([idMenu], [ruolo], [ordine]) VALUES (N'218a28b7-262a-46aa-a44a-fae95ff45078', N'A', 1)
INSERT [dbo].[menuRuolo] ([idMenu], [ruolo], [ordine]) VALUES (N'9477c97c-42a1-41b4-92a4-fb300ef95312', N'A', 3)
INSERT [dbo].[menuRuolo] ([idMenu], [ruolo], [ordine]) VALUES (N'61067f4e-4c85-41fb-aca3-fcbd82515a99', N'R', 4)

--MENU_RUOLO AMICI
--INSERT [dbo].[menuRuolo] ([idMenu], [ruolo], [ordine]) VALUES (N'7d33e9b0-07a0-4daa-b85c-b02f4d0ebe6f', N'R', 3)
--INSERT [dbo].[menuRuolo] ([idMenu], [ruolo], [ordine]) VALUES (N'7d33e9b0-07a0-4daa-b85c-b02f4d0ebe6f', N'U', 2)
--INSERT [dbo].[menuRuolo] ([idMenu], [ruolo], [ordine]) VALUES (N'7bdf4bc7-50bf-411e-9966-39933d729828', N'R', 2)
--INSERT [dbo].[menuRuolo] ([idMenu], [ruolo], [ordine]) VALUES (N'7bdf4bc7-50bf-411e-9966-39933d729828', N'U', 2)
--INSERT [dbo].[menuRuolo] ([idMenu], [ruolo], [ordine]) VALUES (N'111d6698-59d4-46d7-8887-4944a1012e3b', N'A', 1)
--INSERT [dbo].[menuRuolo] ([idMenu], [ruolo], [ordine]) VALUES (N'111d6698-59d4-46d7-8887-4944a1012e3b', N'R', 1)
--INSERT [dbo].[menuRuolo] ([idMenu], [ruolo], [ordine]) VALUES (N'111d6698-59d4-46d7-8887-4944a1012e3b', N'U', 1)
--INSERT [dbo].[menuRuolo] ([idMenu], [ruolo], [ordine]) VALUES (N'42f271b2-43e0-4f02-8b23-6ecc5823ec4e', N'R', 3)
--INSERT [dbo].[menuRuolo] ([idMenu], [ruolo], [ordine]) VALUES (N'42f271b2-43e0-4f02-8b23-6ecc5823ec4e', N'U', 3)

INSERT [dbo].[configurazione] ([chiave], [descrizione], [valore], [visibile]) VALUES (N'aequos.password', N'Username per l''invio ordini ad Aequos', N'', 1)
INSERT [dbo].[configurazione] ([chiave], [descrizione], [valore], [visibile]) VALUES (N'aequos.username', N'Password per l''invio ordini ad Aequos', N'', 1)
INSERT [dbo].[configurazione] ([chiave], [descrizione], [valore], [visibile]) VALUES (N'colli.soglia_arrotondamento', N'Soglia per determinare l''arrotondamento dei colli al momento della chiusura dell''ordine. Inserire un valore compreso tra 0 e 1 utilizzando il punto come separatore dei decimali (ad esempio 0.6)', N'0.5', 1)
INSERT [dbo].[configurazione] ([chiave], [descrizione], [valore], [visibile]) VALUES (N'gas.nome', N'Nome del GAS visualizzato nell''intestazione di GoGas', N'$$$-bogus-$$$tenant_id}', 1)
INSERT [dbo].[configurazione] ([chiave], [descrizione], [valore], [visibile]) VALUES (N'visualizzazione.utenti', N'Modalità di visualizzazione degli utenti (NC=nome cognome, CN=cognome nome)', N'NC', 1)
INSERT [dbo].[configurazione] ([chiave], [descrizione], [valore], [visibile]) VALUES (N'gas.logo.img', N'', N'', 0)
INSERT [dbo].[configurazione] ([chiave], [descrizione], [valore], [visibile]) VALUES (N'notifications.scheduler.active', N'', N'false', 0)

INSERT INTO [dbo].[causale] ([codiceCausale], [segno], [descrizione]) VALUES ('ORDINE', '-', 'Addebito ordine')

INSERT INTO [dbo].[utenti] ([idUtente], [utente], [pwd], [ruolo], [nome], [cognome], [attivo], [position]) VALUES (N'00000000-0000-0000-0000-000000000000', N'admin', CONVERT(VARCHAR(100), HASHBYTES('SHA1', '$$$-bogus-$$$tenant_id}'), 2), N'A', N'Admin', N'', 1, 0)

SET IDENTITY_INSERT [dbo].[notificationPrefs] ON
INSERT [dbo].[notificationPrefs] ([id], [idUtente], [idTipologiaOrdine], [apertura], [scadenza], [minutiScadenza], [consegna], [minutiConsegna], [aggiornamentoQta], [contabilizzazione]) VALUES (1, N'00000000-0000-0000-0000-000000000000', NULL, 1, 1, 60, 1, 60, 1, 1)
SET IDENTITY_INSERT [dbo].[notificationPrefs] OFF