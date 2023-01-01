CREATE DATABASE gogas_integration_test;

CREATE TABLE tenants (
	tenant_id varchar(30) NOT NULL,
	username varchar(30) NOT NULL,
	password varchar(30) NOT NULL,
	url varchar(256) NOT NULL,
    PRIMARY KEY CLUSTERED (tenant_id ASC)
);