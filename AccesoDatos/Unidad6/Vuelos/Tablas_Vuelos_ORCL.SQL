/*  ORACLE */

DROP TABLE pasaje cascade constraints;
DROP TABLE vuelo cascade constraints; 
DROP TABLE pasajero cascade constraints;
DROP TABLE aeropuerto cascade constraints;

/* Create Tables */
CREATE TABLE aeropuerto
(
	codaeropuerto varchar2(10) NOT NULL,
	nombre varchar2(20),
	ciudad varchar2(20),
	pais varchar2(20),
	tasa int,
	CONSTRAINT PKaeropuerto PRIMARY KEY (codaeropuerto)
) ;

CREATE TABLE pasaje
(
	idpasaje int NOT NULL PRIMARY KEY, 
	pasajerocod int NOT NULL,
	identificador varchar2(10) NOT NULL,
	numasiento int,
	clase varchar2(10) ,
	pvp float,
	CONSTRAINT pasajeidentificador UNIQUE (pasajerocod, identificador)
) ;


CREATE TABLE pasajero
(
	pasajerocod int NOT NULL,
	nombre varchar2(30),
	tlf varchar2(10),
	direccion varchar2(40),
	pais varchar2(15),
	CONSTRAINT pkpasajero PRIMARY KEY (pasajerocod)
) ;


CREATE TABLE vuelo
(
	identificador varchar2(10) NOT NULL,
	aeropuertoorigen varchar2(10),
	aeropuertodestino varchar2(10),
	tipovuelo varchar2(15),
	fechavuelo date,
	descuento int,
	CONSTRAINT pkvuelo PRIMARY KEY (identificador)
)  ;



/* Create Foreign Keys */

ALTER TABLE vuelo
	ADD CONSTRAINT esorigen FOREIGN KEY (aeropuertoorigen)
	REFERENCES aeropuerto (codaeropuerto)
;


ALTER TABLE vuelo
	ADD CONSTRAINT esdestino FOREIGN KEY (aeropuertodestino)
	REFERENCES aeropuerto (codaeropuerto)
;



ALTER TABLE pasaje
	ADD CONSTRAINT fkpasajepasajero FOREIGN KEY (pasajerocod)
	REFERENCES pasajero(pasajerocod)
;


ALTER TABLE pasaje
	ADD CONSTRAINT fkpasajevuelo FOREIGN KEY (identificador)
	REFERENCES vuelo (identificador)

;


/* -- Inserta en tablas */
/* -- INSERTAR EN aeropuerto
-- codaeropuerto varchar22 (10),  nombre varchar22 (20) ciudad varchar22 (20) , pais varchar22 (20) */
insert into aeropuerto values ('BRU EBBR', 'Bruselas-Zaventem', 'Bruselas', 'Bélgica', 20);
insert into aeropuerto values ('RL  EBCI', 'Bruselas-Charleroi', 'Bruselas', 'Bélgica', 20);
insert into aeropuerto values ('PRG LKPR', 'aeropuerto de Praga', 'Praga', 'República Checa',50);
insert into aeropuerto values ('CPH EKCH', 'Copenhague-Kastrup', 'Copenhague', 'Dinamarca',150);

insert into aeropuerto values ('MAD LEMD', 'Madrid-Barajas', 'Madrid', 'España', 30);
insert into aeropuerto values ('PMI LEPA', 'Son San Juan', 'Palma de Mallorca', 'España',30);
insert into aeropuerto values ('HEL EFHK', 'Helsinki-Vantaa', 'Helsinki', 'Finlandia',170);
insert into aeropuerto values ('GWY EICM', 'aeropuerto de Galway', 'Galway', 'Irlanda',30);

insert into aeropuerto values ('OSL ENGM', 'Oslo-Gardermoen', 'Olso', 'Noruega',170);

	


/*----pasajero,  cod NUMBER (5)     nombre varchar22 (30) ,   tlf varchar22 (10) ,  direccion varchar22 (40)  */
INSERT INTO pasajero VALUES (1,'ATONIO MARTÍNEZ', '123456','AVDA MADRID 2, 2ºA, TALAVERA', 'ESPAÑA');
INSERT INTO pasajero VALUES (2,'MARIA DUEÑAS', '444456','AVDA ALFARES 4, 8ºA, MADRID','ESPAÑA');
INSERT INTO pasajero VALUES (3,'CARLOS GIL', '568956','C/ALAMEDA 20, MADRID','ESPAÑA');
INSERT INTO pasajero VALUES (4,'MARIA JIMENEZ', '990456','C/LA MONTERA 50, TALAVERA','ESPAÑA');
INSERT INTO pasajero VALUES (5,'ROSARIO FORES', '123888','C/LA RIADA 3, ALCORCÓN','ESPAÑA');
INSERT INTO pasajero VALUES (6,'ALICIA RAMOS', '66688','C/LOS PINTORES 10, TALAVERA','ESPAÑA');
INSERT INTO pasajero VALUES (7,'ANA FUENTES', '8903456','C/LA REINA 34, MADRID','FRANCIA');

INSERT INTO pasajero VALUES (8,'PETRA ALONSO', '222456','AVDA MADRID 32, 2ºA, TALAVERA','FRANCIA');
INSERT INTO pasajero VALUES (9,'ANGEL GIL', '444026','AVDA ALFARES 14, 8ºA, MADRID','FRANCIA');
INSERT INTO pasajero VALUES (10,'MARTIN PESACADOR', '238956','C/ALAMEDA 21, MADRID','FRANCIA');
INSERT INTO pasajero VALUES (11,'LUIS CABRERO', '130456','C/LA CAPA 50, TALAVERA','FRANCIA');
INSERT INTO pasajero VALUES (12,'JESÚS GINER FORES', '23888','C/LA REJA 4, ALCORCÓN','FRANCIA');
INSERT INTO pasajero VALUES (13,'CARMEN RAMI', '6668809','C/LOS SAUCES 10, TALAVERA','PORTUGAL');
INSERT INTO pasajero VALUES (14,'FELIME PRIMERO', '89803456','C/LA ROSA 3, MADRID','PORTUGAL');


INSERT INTO pasajero VALUES (15,'JUANJO PÉREZ', '92522245','AVDA CACERES 32, 2ºA, TALAVERA','PORTUGAL');
INSERT INTO pasajero VALUES (16,'PEDRO ALÍA', '949944026','AVDA DANZANTES 14, 8ºA, MADRID','PORTUGAL');
INSERT INTO pasajero VALUES (17,'SERGIO GRACÍA', '912238956','C/GINER DE LOS RIOS 21, LEGANÉS','PORTUGAL');
INSERT INTO pasajero VALUES (18,'MRÍA CABRERO', '925130456','C/LA HERRADURA 30, TORRIJOS','PORTUGAL');
INSERT INTO pasajero VALUES (19,'OSCAR ALONSO', '918823888','C/LOS MOLINOS 3, ALCALÁ DE HENARES','ALEMANIA');
INSERT INTO pasajero VALUES (20,'PILAR MARTÍN', '925666880','C/ALVARADO 30, TOLEDO','ESPAÑA');
INSERT INTO pasajero VALUES (21,'ANGÉLICA SEGUNDO', '918980345','C/LA ROSA 3, ALCALÁ DE HENARES','ESPAÑA');


INSERT INTO pasajero VALUES (22,'MIRIAM CAMACHO', '92544245','AVDA MADRID 32, 2ºA, TALAVERA','ESPAÑA');
INSERT INTO pasajero VALUES (23,'ROSA ALÍA', '949870026','AVDA SANZ VAZQUEZ 2, 8ºA, MADRID','ESPAÑA');
INSERT INTO pasajero VALUES (24,'ALICIA GARCÍA', '916738956','C/LIBREROS 23, LEGANÉS','ESPAÑA');
INSERT INTO pasajero VALUES (25,'MARY CASTUERA', '925220400','C/LA MINA 5, TORRIJOS','ALEMANIA');
INSERT INTO pasajero VALUES (26,'SERGIO ALONSO', '91673888','C/LOS ALCORES 41, ALCALÁ DE HENARES','ALEMANIA');
INSERT INTO pasajero VALUES (27,'RAQUEL RINCÓN', '925316882','C/ÁVILA 13, TOLEDO','ALEMANIA');
INSERT INTO pasajero VALUES (28,'TITO VALVERDE', '916675544','C/LA FRUTA 33, ALCALÁ DE HENARES','ALEMANIA');

/* - TABLA vuelo
--  identificador varchar22 (10) ,    aeropuerto_origen ,  aeropuerto_destino ,  tipo_vuelo varchar22 (15) 
-- identificador IBE- Iberia, BRU- bruselas airlines, RYA -Ryanair, 
-- VUE vueling, QAT qatar, DEL delta, KLM KLM, AVI Avianca

-- tipo_vuelo IN ('CHARTER', 'DIRECTO', 'DOMÉSTICO', 'LARGO RECORRIDO', 'LOW COST', 'REGULAR')

-- origen 'BRU	EBBR', 'Bruselas-Zaventem'
-- destinoS ('CPH	EKCH', 'Copenhague-Kastrup', 'Copenhague', 'Dinamarca');
-- ('MAD LEMD', 'Madrid-Barajas', 'Madrid', 'España')
-- ('PMI	LEPA', 'Son San Juan', 'Palma de Mallorca', 'España'); */

INSERT INTO vuelo VALUES ('BRU-1234','BRU EBBR','CPH EKCH','CHARTER',sysdate+30, 5);
INSERT INTO vuelo VALUES ('BRU-2222','BRU EBBR','MAD LEMD','DIRECTO',sysdate+6, 5);
INSERT INTO vuelo VALUES ('KLM-2343','BRU EBBR','PMI LEPA','DIRECTO',sysdate+5, 10);


/*  origen ('MAD	LEMD', 'Madrid-Barajas', 'Madrid', 'España');
-- destinoS ('HEL EFHK', 'Helsinki-Vantaa', 'Helsinki', 'Finlandia');
-- ('GWY EICM', 'aeropuerto de Galway', 'Galway', 'Irlanda');
-- ('OSL ENGM', 'Oslo-Gardermoen', 'Olso', 'Noruega'); 'PRG	LKPR' PRAGA   */
INSERT INTO vuelo VALUES ('IBE-762','MAD LEMD','HEL EFHK','DOMÉSTICO',sysdate+30,5);
INSERT INTO vuelo VALUES ('IBE-789','MAD LEMD','GWY EICM','DIRECTO',sysdate+63, 10);
INSERT INTO vuelo VALUES ('AVI-ASD','MAD LEMD','OSL ENGM','LARGO RECORRIDO',sysdate+31, 10);
INSERT INTO vuelo VALUES ('AVI-345','MAD LEMD','BRU EBBR','LARGO RECORRIDO',sysdate+15, 5);
INSERT INTO vuelo VALUES ('DEL-456','MAD LEMD','PRG LKPR','LARGO RECORRIDO',sysdate+70, 20);

/*  origen ('CPH	EKCH', 'Copenhague-Kastrup', 'Copenhague', 'Dinamarca');
-- destinoS  ('BRU	EBBR', 'Bruselas-Zaventem', 'Bruselas', 'Bélgica');
-- ('RL	EBCI', 'Bruselas-Charleroi', 'Bruselas', 'Bélgica');
-- ('PRG	LKPR', 'aeropuerto de Praga', 'Praga', 'República Checa');
-- ('MAD	LEMD', 'Madrid-Barajas', 'Madrid', 'España');   */

INSERT INTO vuelo VALUES ('DEL-34', 'CPH EKCH', 'BRU EBBR','DOMÉSTICO',sysdate+80, 10);
INSERT INTO vuelo VALUES ('KLM-332','CPH EKCH', 'HEL EFHK','DIRECTO',sysdate+77, 5);
INSERT INTO vuelo VALUES ('IBE-212','CPH EKCH', 'RL  EBCI','CHARTER',sysdate+32, 10);
INSERT INTO vuelo VALUES ('QAT-224','CPH EKCH', 'PRG LKPR','CHARTER',sysdate+20, 20);
INSERT INTO vuelo VALUES ('KLM-78', 'CPH EKCH', 'MAD LEMD','DOMÉSTICO',sysdate+66, 10);

/*  origen ('PMI	LEPA', 'Son San Juan', 'Palma de Mallorca', 'España');
-- destinoS  ('HEL	EFHK', 'Helsinki-Vantaa', 'Helsinki', 'Finlandia');
-- ('GWY	EICM', 'aeropuerto de Galway', 'Galway', 'Irlanda');
-- ('OSL	ENGM', 'Oslo-Gardermoen', 'Olso', 'Noruega');
--  ('MAD	LEMD', 'Madrid-Barajas', 'Madrid', 'España');   */
INSERT INTO vuelo VALUES ('IBE-234', 'PMI LEPA','HEL EFHK','DOMÉSTICO',sysdate+40,5);
INSERT INTO vuelo VALUES ('RYA-22',  'PMI LEPA','GWY EICM','CHARTER',sysdate+8, 5);
INSERT INTO vuelo VALUES ('RYA-230', 'PMI LEPA','OSL ENGM','CHARTER',sysdate+25, 8);
INSERT INTO vuelo VALUES ('QAT-900', 'PMI LEPA','MAD LEMD','REGULAR',sysdate+67,10);




/*  TABLA pasaje   pasajero_cod,    identificador varchar22 (10) , numasiento NUMBER (5) ,
--  clase varchar22 (10) ,  pvp NUMBER (6,2) , clase IN ('BUSINESS', 'PRIMERA', 'TURISTA')) 
-- HASTA 28 VIAJEROS
-- vuelo 'IBE-762'   */
	
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (101, 1, 'IBE-762', 10, 'PRIMERA', 200);
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (102, 2, 'IBE-762', 11, 'PRIMERA', 200);
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (103, 3, 'IBE-762', 13, 'PRIMERA', 220);
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (104, 4, 'IBE-762', 14, 'TURISTA', 230);
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (105, 5, 'IBE-762', 16, 'TURISTA', 250);
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (106, 6, 'IBE-762', 25, 'TURISTA', 260);
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (107, 7, 'IBE-762', 27, 'BUSINESS', 280);

/*  vuelo 'BRU-1234' */
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (108, 1, 'BRU-1234', 20, 'TURISTA', 180);
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (109, 2, 'BRU-1234', 21, 'PRIMERA', 190);
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (110, 3, 'BRU-1234', 23, 'PRIMERA', 220);
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (111, 4, 'BRU-1234', 24, 'TURISTA', 230);
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (112, 5, 'BRU-1234', 16, 'TURISTA', 250);
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (113, 6, 'BRU-1234', 25, 'TURISTA', 260);

/*  vuelo 'KLM-2343' */

INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (114, 8, 'KLM-2343', 12, 'TURISTA', 180);
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (115, 9, 'KLM-2343', 13, 'PRIMERA', 190);
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (116, 10, 'KLM-2343', 14, 'PRIMERA', 220);
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (117, 11, 'KLM-2343', 15,'TURISTA', 230);
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (118, 12, 'KLM-2343', 36, 'TURISTA', 250);
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (119, 13, 'KLM-2343', 37, 'TURISTA', 260);
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (120, 14, 'KLM-2343', 38, 'BUSINESS', 250);
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (121, 15, 'KLM-2343', 39, 'BUSINESS', 260);
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (122, 16, 'KLM-2343', 40, 'BUSINESS', 270);
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (123, 17, 'KLM-2343', 41, 'BUSINESS', 260);

/*  vuelo 'IBE-789' */
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (124, 18, 'IBE-789', 10, 'TURISTA', 170);
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (125, 19, 'IBE-789', 13, 'PRIMERA', 180);
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (126, 20, 'IBE-789', 14, 'PRIMERA', 220);
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (127, 21, 'IBE-789', 15,'TURISTA', 240);
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (128, 22, 'IBE-789', 46, 'TURISTA', 230);
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (129, 23, 'IBE-789', 47, 'TURISTA', 270);
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (130, 24, 'IBE-789', 48, 'BUSINESS', 220);
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (131, 25, 'IBE-789', 49, 'BUSINESS', 230);
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (132, 26, 'IBE-789', 50, 'TURISTA', 220);

/*  vuelo 'AVI-ASD', */
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (133, 15, 'AVI-ASD', 30, 'TURISTA', 170);
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (134, 16, 'AVI-ASD', 33, 'PRIMERA', 180);
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (135, 17, 'AVI-ASD', 34, 'PRIMERA', 220);
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (136, 18, 'AVI-ASD', 35,'TURISTA', 160);
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (137, 19, 'AVI-ASD', 36, 'TURISTA', 150);
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (138, 20, 'AVI-ASD', 37, 'TURISTA', 180);
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (139, 21, 'AVI-ASD', 58, 'BUSINESS', 170);
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (140, 22, 'AVI-ASD', 59, 'BUSINESS', 170);
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (141, 23, 'AVI-ASD', 40, 'TURISTA', 120);
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (142, 24, 'AVI-ASD', 68, 'BUSINESS', 220);
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (143, 25, 'AVI-ASD', 69, 'BUSINESS', 230);
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (144, 26, 'AVI-ASD', 41, 'TURISTA', 120);

/*  vuelo 'KLM-332', */
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (145, 1, 'KLM-332', 20, 'PRIMERA', 180);
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (146, 2, 'KLM-332', 30, 'TURISTA', 170);
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (147, 3, 'KLM-332', 33, 'PRIMERA', 180);
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (148, 10, 'KLM-332', 34, 'PRIMERA', 220);
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (149, 11, 'KLM-332', 35,'TURISTA', 160);
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (151, 12, 'KLM-332', 36, 'TURISTA', 150);
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (152, 20, 'KLM-332', 37, 'TURISTA', 180);
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (153, 21, 'KLM-332', 58, 'BUSINESS', 170);
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (154, 22, 'KLM-332', 59, 'BUSINESS', 170);
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (155, 23, 'KLM-332', 40, 'TURISTA', 120);
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (156, 24, 'KLM-332', 68, 'BUSINESS', 220);
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (157, 25, 'KLM-332', 69, 'BUSINESS', 230);
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (158, 26, 'KLM-332', 41, 'TURISTA', 120);

/*  vuelo 'QAT-900' */
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )   VALUES (159, 20, 'QAT-900', 10, 'TURISTA', 100);
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (160, 21, 'QAT-900', 20, 'BUSINESS', 170);
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (161, 22,'QAT-900', 25, 'BUSINESS', 170);
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (162, 23, 'QAT-900', 40, 'TURISTA', 100);
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (163, 24, 'QAT-900', 30, 'BUSINESS', 220);
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (164, 25, 'QAT-900', 35, 'BUSINESS', 230);
INSERT INTO pasaje (idpasaje, pasajerocod, identificador, numasiento,clase , pvp )  VALUES (165, 26, 'QAT-900', 41, 'PRIMERA', 120);

commit;
