CREATE TABLE master_tenant (
	id numeric(19, 0) IDENTITY(1,1) NOT NULL PRIMARY KEY,
	[password] varchar(30) NOT NULL,
	tenant_id varchar(30) NOT NULL,
	url varchar(256) NOT NULL,
	username varchar(30) NOT NULL,
	[version] int NOT NULL
)