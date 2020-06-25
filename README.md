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


## Notes

* The CellPhone.csv and CellPhoneUsageByMonth.csv are found in the https://github.com/WCF-Insurance/java-developer-cell-phone-usage repo. 
* The fields in the CellPhoneUsageByMonth.csv did not match the bullet points in the README.md file. When creating the cellphoneusage table I decided to use the fields in the .csv file so the records would import.
* I used a HashMap to store a Usage object and used the year-month as the key. When records are in the same year-month I simply added the totalMinutes and totalData to the same Usage object.
* In addition to the minutes and data usage columns I added a year-month column.
