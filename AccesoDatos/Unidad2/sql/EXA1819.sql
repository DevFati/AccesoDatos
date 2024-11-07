/* Drop Tables */

DROP TABLE empleados CASCADE CONSTRAINTS;
DROP TABLE departamentos CASCADE CONSTRAINTS;

DROP TABLE empresas CASCADE CONSTRAINTS;
DROP TABLE oficios CASCADE CONSTRAINTS;

/* Create Tables */

CREATE TABLE departamentos
(
	coddepart number(10,0) NOT NULL,
	nombre varchar2(25) NOT NULL,
	direccion varchar2(35),
	localidad varchar2(35),
	codjefedepartamento number(10,0),
	codempre number(10,0) NOT NULL,
	PRIMARY KEY (coddepart)
);


CREATE TABLE empleados
(
	codemple number(10,0) NOT NULL,
	nombre varchar2(35 char) NOT NULL,
	direccion varchar2(35),
	poblacion varchar2(25),
	fechaalta date,
	codencargado number(10,0),
	coddepart number(10,0) NOT NULL,
	codoficio number(10,0) NOT NULL,
	PRIMARY KEY (codemple)
);



CREATE TABLE empresas
(
	codempre number(10,0) NOT NULL,
	nombre varchar2(35) NOT NULL,
	direccion varchar2(60) NOT NULL,
	tlf varchar2(15),
	presupuesto float,
	codsector number(10,0) NOT NULL,
	sede number(10,0) NOT NULL,
	PRIMARY KEY (codempre)
);


CREATE TABLE oficios
(
	codoficio number(10,0) NOT NULL,
	nombre varchar2(30) NOT NULL,
	salariomes float,
	preciotrienio float,
	PRIMARY KEY (codoficio)
);



/* 	codoficio int NOT NULL, 	nombre varchar(30)NOT NULL,	salariomes float,	preciotrienio float,*/

insert into oficios values (1, 'T CNICO ADMINISTRATIVO', 2000, 50);
insert into oficios values (2, 'ABOGADO', 2700, 100);
insert into oficios values (3, 'ECONOMISTA', 2700, 100);
insert into oficios values (4, 'INFORM TICO', 2900, 100);
insert into oficios values (5, 'T CNICO INFORM TICO', 2300, 50);

insert into oficios values (11, 'T CNICO GANADERO', 2000, 50);
insert into oficios values (12, 'PE N GANADERO', 1500, 50);
insert into oficios values (13, 'INGENIERO AGR COLA ', 2500, 60);

insert into oficios values (21, 'T CNICO INDUSTRIAL', 2000, 50);
insert into oficios values (22, 'T CNICO ENERG AS', 2100, 50);
insert into oficios values (23, 'INGENIERO INDUSTRIAL ', 2600, 60);

insert into oficios values (31, 'INGENIERO TELECO ', 2600, 60);
insert into oficios values (32, 'COMERCIAL ', 2500, 80);
insert into oficios values (35, 'LIMPIADOR ', 2500, 80);

/* 	codempre int NOT NULL,	nombre varchar(30) NOT NULL,	direccion varchar(30) NOT NULL,
	tlf varchar(15),	presupuesto float,	codsector int NOT NULL,	sede int NOT NULL,
	PRIMARY KEY (codempre) */

insert into empresas values (1,'Comercios Madrid','Pol gono 724. 28211. Madrid','910078909', 150000, 32, 724); /*sede espa*/
insert into empresas values (2,'GANADER AS S.A.','Finca Las Moreras S/N. Talavera. 45600','925889090', 120000, 10,724); /*sede espa*/

insert into empresas values (3,'Comunicaciones TALA S.A.','C/Francisco Aguirre. Talavera. 45600','925808990', 125000, 31,372); /*sede irlanda */

insert into empresas values (4,'Construcciones Indrustiales S.L.', 'Nave 112. Pol gono LAS ROSETAS. 28001','916808990', 225000, 20,784); /*sede emiratos*/
insert into empresas values (5,'Indrustias renovables. S.L.', 'Pol gono MIRALRIO 1. 19001','949808990', 245000, 21,32); /*sede argentina*/

insert into empresas values (10,'Toldos Sin Sol', 'Poligono 7. Urbanizacion Rosas. 45600','925889900', 185000, 20,784); /*sede emiratos*/
insert into empresas values (12,'Los Pochoclos ', 'Pol gono El pibe 17. 19006','949213456', 155000, 21,32); /*sede argentina*/


/* departamentos
 *	coddepart int NOT NULL,	nombre varchar(20) NOT NULL,	direccion varchar(30),
	localidad varchar(30),	codjefedepartamento int,	codempre int NOT NULL, */

/* empresa 1 */
insert into departamentos values(11, 'GESTI N', 'C/Mayor 17', 'Madrid', 101, 1);
insert into departamentos values(12, 'ALMAC N', 'Pol gono Torrehierro. 10', 'Talavera de la Reina', 102, 1);


/* empresa 2 */
insert into departamentos values(21, 'GESTI N', 'C/Mayor 17', 'Talavera de la Reina', 201, 2);


/* empresa 3 */
insert into departamentos values(31, 'GESTI N', 'C/Arboleda 10', 'Madrid', 301, 3);
insert into departamentos values(32, 'DISTRIBUCI N', 'Pol gono Alfares 22', 'Talavera de la Reina', 321, 3);



/* empresa 4 */
insert into departamentos values(41, 'GESTI N', 'Edificio 4 Torres 2', 'Abu DAbi', 401, 4);
insert into departamentos values(42, 'MARKETING', 'C/ Jovellanos 2 .28001', 'Madrid', 411, 4);


/* Trabajadores codemple int NOT NULL,	nombre varchar(30) NOT NULL,
	direccion varchar(30),	poblacion varchar(20),	fechaalta date,
	codencargado int NOT NULL,	coddepart int NOT NULL, 	codoficio int NOT NULL, 
 */

/* departamento 11 y 12 */
insert into empleados values(101, 'Pedro Mart nez', 'C/Los Sauces 5', 'Madrid', to_date('2000/01/01', 'yyyy/mm/dd'),     null, 11 , 13 );
insert into empleados values(107, 'Juana Salazar', 'C/Los Catedritos 3', 'Madrid', to_date('2005/01/12','yyyy/mm/dd'),  101, 11 , 1 );
   
insert into empleados values(102, 'Luisa Ros', 'C/Barricas 15', 'Talavera de la Reina',to_date('2001/11/02','yyyy/mm/dd'),         101, 12,  13  );
insert into empleados values(103, 'Juan Sales', 'C/Los Sauces 5', 'Madrid', to_date('2005/11/12', 'yyyy/mm/dd'),                   102, 12 , 11 );
insert into empleados values(104, 'Antonio Rios', 'C/Los Alfareros 34', 'Talavera de la Reina',to_date('2006/01/01','yyyy/mm/dd'), 102, 12 ,11 );
insert into empleados values(105, 'Alberto Mart n', 'C/Los Abetros 35', 'Oropesa', to_date('2008/01/01','yyyy/mm/dd'),             102, 12 ,12  );
insert into empleados values(106, 'Mar a Suarez', 'C/Los Molinos 10', 'Oropesa', to_date('2010/01/06', 'yyyy/mm/dd'),              102, 12 ,12 );


/* departamento 21 */
insert into empleados values(201, 'J Manuel Salas', 'C/Lapiceros 22', 'Madrid',  to_date('2003/01/02', 'yyyy/mm/dd'),            null, 21 , 3 );
insert into empleados values(202, 'Mariano Embid', 'C/Saucedilla 45', 'Talavera de la Reina',to_date('2004/11/11','yyyy/mm/dd'), 201, 21,  23  );
insert into empleados values(203, 'Casimiro Regatas', 'C/Los Lucillos 33', 'Oropesa',  to_date( '2004/01/01', 'yyyy/mm/dd'),    201, 21 , 1 );
insert into empleados values(204, 'Alicia Mart n', 'C/Las Minas 65', 'Talavera de la Reina',to_date('2006/10/10','yyyy/mm/dd'), 201, 21 ,5 );
insert into empleados values(205, 'Juana Orea', 'C/Las Minas 67', 'Talavera de la Reina', to_date( '2007/11/10', 'yyyy/mm/dd'),  201, 21 ,5 );


/* departamento 31 y 32 */  

insert into empleados values(301, 'J Antonio Rodriguez', 'Avda Mangantes 33', 'Madrid', to_date( '2005/12/02', 'yyyy/mm/dd'),             null, 31 , 3 );
insert into empleados values(302, 'Marcelino Pan y Vino', 'C/Los Serranos 23','Talavera de la Reina',to_date('2005/12/22','yyyy/mm/dd'),301, 31,  4  );
insert into empleados values(303, 'Juan Maria Cordel', 'C/Ucl s 4', 'Oropesa', to_date( '2006/08/02', 'yyyy/mm/dd'),            301, 31 , 1 );

insert into empleados values(321, 'Amalia Garc a', 'C/Puerto Rico 1', 'Madrid', to_date( '2006/09/02', 'yyyy/mm/dd'),             301, 32 , 31 );
insert into empleados values(322, 'Antonia G mez', 'C/La Perla 3', 'Talavera de la Reina',to_date( '2007/11/12', 'yyyy/mm/dd'),   321, 32,  22  );
insert into empleados values(323, 'Ainoa Yangh', 'C/Las Rosas 4', 'Oropesa',  to_date( '2007/12/02',    'yyyy/mm/dd'),            321, 32 , 22 );
insert into empleados values(324, 'Alberlo Melgares', 'C/Cavernas 55', 'Talavera de la Reina',to_date('2009/03/02', 'yyyy/mm/dd'),321, 32 ,21 );
insert into empleados values(325, 'Jorge L pez', 'Finca Carnaval', 'Talavera de la Reina', to_date( '2010/04/02', 'yyyy/mm/dd'),  321, 32 ,21 );


/* departamento 41 y 42 */

insert into empleados values(401, 'Juna Pedro Regaad ', 'Hotel Luz 7- Apt 20', 'Abu DAbi', to_date( '2008/01/02','yyyy/mm/dd'),  null, 41 , 3 );
insert into empleados values(402, 'Hassan Asaad', 'Hotel Luz 7- Apt 21','Abu DAbi',to_date('2008/09/02','yyyy/mm/dd'), 401, 41,  2  );

insert into empleados values(411, 'Zaira Amahs', 'C/Los Goya 53', 'Madrid', to_date(  '2008/11/02','yyyy/mm/dd'),               401, 42 , 4 );
insert into empleados values(412, 'Sonia  lvarez', 'C/Romer as 333', 'Talavera de la Reina', to_date('2008/11/11','yyyy/mm/dd'),411, 42,  5  );
insert into empleados values(413, 'Pedro Ramirez', 'Avda FCO AGG 467', 'Madrid',to_date( '2009/01/02', 'yyyy/mm/dd'),          411, 42 , 5 );
insert into empleados values(414, 'Silvia Garc s', 'C/Las Inviernas 4', 'Madrid',to_date('2009/01/02', 'yyyy/mm/dd'),          411, 42 ,32 );
insert into empleados values(415, 'Ant n P rez', 'Hotel Mariluz - apt 5', 'Madrid',to_date( '2009/10/05','yyyy/mm/dd'),        411, 42 ,32 );
insert into empleados values(416, 'Fernando Sagunto', 'Avad Pedro P rez 4', 'Madrid', to_date( '2010/01/02','yyyy/mm/dd'),     411, 42 ,1 );
insert into empleados values(417, 'Mohamed Brain', 'C/ Los inform ticos 5', 'Toledo', to_date(  '2010/06/02', 'yyyy/mm/dd'),   411, 42 ,1 );



/* Create Foreign Keys */

ALTER TABLE departamentos
	ADD CONSTRAINT departamentosjefefk FOREIGN KEY (codjefedepartamento)
	REFERENCES empleados (codemple)
;

ALTER TABLE empleados
	ADD CONSTRAINT empleadosdepartfk FOREIGN KEY (coddepart)
	REFERENCES departamentos (coddepart)
;


ALTER TABLE empleados
	ADD CONSTRAINT empleadosencargadofk FOREIGN KEY (codencargado)
	REFERENCES empleados (codemple)
;


ALTER TABLE departamentos
	ADD CONSTRAINT departamentosemprefk FOREIGN KEY (codempre)
	REFERENCES empresas (codempre)
;


ALTER TABLE empleados
	ADD CONSTRAINT empleadosofifk FOREIGN KEY (codoficio)
	REFERENCES oficios (codoficio)
;



select count(*) from departamentos where codempre=1;

select nombre from empleados where codemple=11;
select count(*) from empleados where coddepart=11;
select nombre from empleados where codemple=101;



 create table empresassindepar as select * from empresas where codempre not in ( select codempre  from departamentos ) order by codempre;
ALTER TABLE empresassindepar ADD CONSTRAINT CSP_PK PRIMARY KEY ( codempre );

delete from empresas where codempre not in ( select codempre from departamentos );

update oficios ofi set numemple = 
 ( select count(*) from empleados where codoficio = ofi.codoficio);
 
 select max(codemple+1) from empleados;
 
 select codjefedepartamento from departamentos where coddepart=11;
 
 select * from oficios where codoficio=110;
 
select codjefedepartamento from departamentos where coddepart=180;

select codjefedepartamento from departamentos where coddepart=43;

select codoficio, nombre, salariomes, preciotrienio from oficios;

select count(*) from empleados where codoficio=588;

select * from empleados where codemple=101;

select * from empresas where codempre=155;

select coddepart, nombre, direccion, localidad, codjefedepartamento, codempre from departamentos where coddepart= 255;

update empleados set nombre=?,  apellido1=?,
                			+ "  apellido2=?, extension=?,  email=?,   "
                			+ "codigooficina = ?,  codigojefe = ?, puesto = ?"
                			+ " where codigoempleado = ?";
                            
                            
select count(*) from empleados where coddepart in (select coddepart from departamentos where codempre=1);

select sum(salariomes) from empleados join oficios using (codoficio)
where coddepart in (select coddepart from departamentos where codempre=1);

update empresas set presupuesto=9500 where codempre=1;

