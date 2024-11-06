
/* TABLAS EN ORACLE */
DROP TABLE  RESERVAS cascade constraints;
DROP TABLE VIAJES cascade constraints ;
DROP TABLE  CIUDADES cascade constraints; 
DROP TABLE CLIENTES cascade constraints;
DROP TABLE PAISES cascade constraints;

/* Create Tables */

CREATE TABLE ciudades
(
	CIUDAD varchar2(20 char) NOT NULL,
	CODPAIS char(3 char),
	PRIMARY KEY (CIUDAD)
);


CREATE TABLE clientes
(
	CODCLIEN number(10,0) NOT NULL,
	NOMBRE varchar2(25 char),
	DIRECCION varchar2(25 char),
	TLF varchar2(10 char),
	POBLACION varchar2(25 char),
	PRIMARY KEY (CODCLIEN)
);


CREATE TABLE paises
(
	CODPAIS char(3 char) NOT NULL,
	NOMBRE varchar2(25 char),
	IDIOMA varchar2(15 char),
	TASA float,
	PRIMARY KEY (CODPAIS)
);


CREATE TABLE reservas
(
	CODRESERVA number(10,0) NOT NULL,
	NUMPLAZA number(10,0),
	FECHARESERVA date,
	CODCLIEN number(10,0),
	CODVIAJE number(10,0),
	PRIMARY KEY (CODRESERVA)
);


CREATE TABLE viajes
(
	CODVIAJE number(10,0) NOT NULL,
	FECHAINICIO date,
	FECHAFIN date,
	PLAZASOFERTADAS number(10,0),
	PLAZASOCUPADAS number(10,0),
	DESCRIPCION varchar2(50 char),
	CIUDADORIGEN varchar2(20 char),
	CIUDADDESTINO varchar2(20 char),
	PRECIO float,
	PRIMARY KEY (CODVIAJE)
);



/* Create Foreign Keys */

ALTER TABLE viajes
	ADD CONSTRAINT ORIGENviajes16 FOREIGN KEY (CIUDADORIGEN)
	REFERENCES ciudades (CIUDAD)
;


ALTER TABLE viajes
	ADD CONSTRAINT DESTINOviajes16 FOREIGN KEY (CIUDADDESTINO)
	REFERENCES ciudades (CIUDAD)
;


ALTER TABLE reservas
	ADD CONSTRAINT REALIZANRESERV16 FOREIGN KEY (CODCLIEN)
	REFERENCES clientes (CODCLIEN)
;


ALTER TABLE ciudades
	ADD CONSTRAINT PAISCIUDAD16 FOREIGN KEY (CODPAIS)
	REFERENCES paises (CODPAIS)
;


ALTER TABLE reservas
	ADD CONSTRAINT VIAJERESERVA16 FOREIGN KEY (CODVIAJE)
	REFERENCES viajes (CODVIAJE)
;


/*  insert PAISES */

 INSERT INTO PAISES VALUES ('DEU', 'Alemania', 'Alem n', 170);
 INSERT INTO PAISES VALUES ('AUS', 'Australia', 'Ingl s', 200);
 INSERT INTO PAISES VALUES ('ARG', 'Argentina', 'Espa ol', 150);
 INSERT INTO PAISES VALUES ('FRA', 'Francia', 'Franc s', 200);
INSERT INTO PAISES VALUES ('ITA', 'Italia', 'Italiano', 130);
 INSERT INTO PAISES VALUES ('ESP', 'Espa a', 'Espa ol', 30);
 INSERT INTO PAISES VALUES ('NLD', 'Paises Bajos', 'Neerland s', 350);
 INSERT INTO PAISES VALUES ('GBR', 'Reino Unido', 'Ingl s', 210);
	 
/*  insert CIUDADES   */
	
Insert into ciudades values( 'MUNICH','DEU');
Insert into ciudades values( 'BERLIN','DEU');
Insert into ciudades values( 'FRANKFURT','DEU');
Insert into ciudades values( 'HAMBURGO','DEU');
	
Insert into ciudades values( 'ADELAIDA','AUS');
Insert into ciudades values( 'SIDNEY','AUS');
Insert into ciudades values( 'BRISBANE','AUS');

Insert into ciudades values( 'PARIS','FRA');
Insert into ciudades values( 'RENNES','FRA');
Insert into ciudades values( 'LYON','FRA');
Insert into ciudades values( 'MARSELLA','FRA');
	
Insert into ciudades values('ROMA','ITA');
Insert into ciudades values('FLORENCIA','ITA');
Insert into ciudades values( 'VENECIA','ITA');
Insert into ciudades values( 'N POLES','ITA');
	
Insert into ciudades values( 'MADRID','ESP');
Insert into ciudades values( 'BARCELONA','ESP');
Insert into ciudades values('BILBAO','ESP');
	  
Insert into ciudades values( 'AMSTERDAM','NLD');
Insert into ciudades values( 'ROTTERDAM','NLD');
Insert into ciudades values( 'GRONINGEN','NLD');
	
Insert into ciudades values( 'LONDRES','GBR');
Insert into ciudades values( 'LIVERPOOL','GBR');
Insert into ciudades values( 'GLASGOW','GBR');
Insert into ciudades values('OXFORD','GBR');
	 
/*  insert CLIENTES  */

 insert into clientes values (1, 'AG. Alicia Gil', 'C/Prado 25', '925453535', 'Talavera de la Reina');
 insert into clientes values (2, 'Transportes G mez', 'Pol gono la Floresta', '925009988', 'Talavera de la Reina');
 insert into clientes values (3, 'Limpiezas brillante', 'C/La Alameda 22', '915009900', 'Madrid');
 insert into clientes values (4, 'Pedro P rez', 'C/Martillo 10', '919191919', 'Madrid');
  insert into clientes values(5, 'Ana Mary n', 'C/Alameda 3', '925450035', 'Talavera de la Reina');
 insert into clientes values (6, 'Viajes Mar a Rato Melz', 'C/Chicharr n 22', '923456588', 'Alcal  de Henares');
 insert into clientes values (7, 'Escuela Padel', 'Pol gono ceniceros', '912332900', 'Madrid');
 insert into clientes values (8, 'Rosaura Memez', 'C/Televisiones 5', '9100998877', 'Madrid');


 
/*  insert VIAJES  */
/* sumar fechas  ADDDATE(ADDDATE(sysdate,, 31);
 restar  ADDDATE(sysdate, -31) */
 /*
	CREATE TABLE VIAJES
(
	CODVIAJE int NOT NULL,	FECHAINICIO date,	FECHAFIN date,
	PLAZASOFERTADAS int,	PLAZASOCUPADAS int,
	DESCRIPCION varchar(30),	CIUDADORIGEN varchar(20),
	CIUDADDESTINO varchar(20),	PRECIO float,
	CONSTRAINT VIAJESPK PRIMARY KEY (CODVIAJE)
);
*/
Insert into VIAJES values(1,sysdate+10,sysdate+20,30,3, 
'La selva Negra', 'MADRID', 'MUNICH', 70.5 );


Insert into VIAJES values(2,sysdate+15,sysdate+25,40,4, 
'Valles del Rhin', 'BARCELONA', 'FRANKFURT', 150.5 );

Insert into VIAJES values(3,sysdate+5,sysdate+13,50,5, 
'Visitas Rom nticas', 'MADRID', 'FLORENCIA', 80 );

Insert into VIAJES values(4,sysdate+15,sysdate+20,55,0, 
'Por los canales rom nticos', 'BILBAO', 'VENECIA', 80.5 );

Insert into VIAJES values(5,sysdate+10,sysdate+18,20,0, 
'Universidad 1', 'BILBAO', 'OXFORD', 55.5 );

Insert into VIAJES values(6,sysdate+25,sysdate+30,30,0, 
'Ciudad de la m sica', 'BARCELONA', 'LIVERPOOL', 85.5 );

Insert into VIAJES values(7,sysdate+23,sysdate+30,60,0, 
'De Baviera a la ciudad de la m sica', 'MUNICH', 'LIVERPOOL', 95.5 );

Insert into VIAJES values(8,sysdate+12,sysdate+17,52,3, 
'Ciudad Imperial-Tulipanes', 'ROMA', 'AMSTERDAM', 55.5 );

Insert into VIAJES values(9,sysdate+12,sysdate+22,45,6, 
'Castillos imperiales', 'MUNICH', 'PARIS', 155.5 );

Insert into VIAJES values(10,sysdate+11,sysdate+32,35,4, 
'A las ant podas', 'MADRID', 'SIDNEY', 200 );


/*  insert RESERVAS  */
/*

	 CODRESERVA int NOT NULL,
	NUMPLAZA int ,
	FECHARESERVA date,
	CODCLIEN int, 1 a 8
	CODVIAJE int, 1 a 10
*/

/*viaje 1 */
Insert into RESERVAS values(100,1,  sysdate-10, 1, 1);
Insert into RESERVAS values(101,2,  sysdate-10, 2, 1);
Insert into RESERVAS values(102,10,  sysdate-8, 3, 1);

/*viaje 2 */
Insert into RESERVAS values(200,  1,sysdate-12, 5, 2);
Insert into RESERVAS values(201,  2,sysdate-11, 6, 2);
Insert into RESERVAS values(202,  11,sysdate-9, 7, 2);
Insert into RESERVAS values(203,  12,sysdate-8, 8, 2);


/*viaje 3 */
Insert into RESERVAS values(300,  1,sysdate-30, 1, 3);
Insert into RESERVAS values(301,  2,sysdate-27, 2, 3);
Insert into RESERVAS values(302,  11,sysdate-20, 5, 3);
Insert into RESERVAS values(303,  12,sysdate-17, 7, 3);
Insert into RESERVAS values(304,  15,sysdate-7, 8, 3);

/*viaje 8 */
Insert into RESERVAS values(800,  1,sysdate-12, 3, 8);
Insert into RESERVAS values(801,  2,sysdate-10, 4, 8);
Insert into RESERVAS values(802,  4,sysdate-5, 8,  8);


/*viaje 9 */
Insert into RESERVAS values(900,  1,sysdate-32, 4, 9);
Insert into RESERVAS values(901,  2,sysdate-30, 4, 9);
Insert into RESERVAS values(902,  4,sysdate-25, 6, 9);
Insert into RESERVAS values(903,  5,sysdate-24, 7, 9);
Insert into RESERVAS values(904,  15,sysdate-22, 8, 9);
Insert into RESERVAS values(905,  16,sysdate-22, 8, 9);

/*viaje 10 */
Insert into RESERVAS values(1000,  1,sysdate-22, 5, 10);
Insert into RESERVAS values(1001,  2,sysdate-20, 5, 10);
Insert into RESERVAS values(1002,  12,sysdate-15, 6, 10);
Insert into RESERVAS values(1003,  13,sysdate-14, 7, 10);

commit;





create table estadisticasciudades(
    ciudad varchar(20) primary key,
	nombrepais varchar(25),
	numviajesdestino number(5),
	numviajesprocedencia number(5)
    );
    
    
    select ciudad, nombre from ciudades join paises using (codpais) order by ciudad; 
    
    insert into estadisticasciudades select ciudad,nombre,0,0 from ciudades join paises using(codpais) order by ciudad;
    
    select count(*) from viajes where ciudadorigen='MADRID';
    
 
 
 update estadisticasciudades es
 set NUMVIAJESDESTINO=(select count(*) from viajes where ciudaddestino=es.ciudad),
 numviajesprocedencia=(   select count(*) from viajes where ciudadorigen=es.ciudad);
 
 
 