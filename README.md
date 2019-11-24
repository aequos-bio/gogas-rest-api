# gogas-rest-api
GoGas REST APIs

### Configurazione del database
Le definizioni delle connessioni ai tenant sono salvate in un database master.
Per il primo avviamento dell'applicazione é necessario creare questo database ed inserire le properties url, username, password e driverClassName nel file **application.properties** sotto la chiave **spring.datasource**

Esempio:
```
spring.datasource.url=jdbc:sqlserver://localhost\\SQLEXPRESS:1433;database=masterdb
spring.datasource.username=master
spring.datasource.password=master
spring.datasource.driverClassName=com.microsoft.sqlserver.jdbc.SQLServerDriver
```

Nel database deve essere presente una tablella denominata **tenants** con la struttura definita da questo DDL:


```
CREATE TABLE tenants (
	tenant_id varchar(30) NOT NULL PRIMARY KEY,
	username varchar(30) NOT NULL,
	password varchar(30) NOT NULL,
	url varchar(256) NOT NULL
);

```




