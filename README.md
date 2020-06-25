## Install the PostgreSQL

## Create and connect to the database 
create database cellphoneusage;

\c cellphoneusage;

## Create tables and import records 
create table cellphone
(
	employeeId integer not null,
	employeeName text,
	purchaseDate date,
	model text
);

\copy cellphone FROM \cellphoneusage\java-developer-cell-phone-usage\cellphone.csv WITH CSV HEADER;

create table cellphoneusage
(
	employeeId integer not null,
	date date,
	totalMinutes integer,
	totalData numeric(5,2)
);

\copy cellphoneusage FROM \cellphoneusage\java-developer-cell-phone-usage\CellPhoneUsageByMonth.csv WITH CSV HEADER;
