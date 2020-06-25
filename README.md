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

* The CellPhone.csv and CellPhoneUsageByMonth.csv are found at https://github.com/WCF-Insurance/java-developer-cell-phone-usage. 
* The fields in the CellPhoneUsageByMonth.csv did not match the bullet points in the README.md file. When creating the cellphoneusage table I decided to fields in the .csv file so the records would easily import.
* I used a HashMap to store Usage objects and used the usage date field as the key. When records had the same usage date I simply added the aded the totalMinutes and totalData to the same Usage object.
* Just realized as I am typing these notes that the report was suppose to have a minutes and data usage columns for each month. I handled the multiple records on a single date by simply adding the additional minutes and data to my same Usage object.  However, I did not handle multiple usage dates per month.  I should fix that!
