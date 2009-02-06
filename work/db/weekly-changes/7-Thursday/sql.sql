CREATE TABLE PUR_DFLT_PRNCPL_ADDR_T 
(PRNCPL_ID varchar2(40) NOT NULL,
OBJ_ID VARCHAR2(36) NOT NULL,
VER_NBR NUMBER(8,0) DEFAULT 1 not null,
CAMPUS_CD varchar2(2), 
BLDG_CD varchar2(10),
BLDG_ROOM_NBR varchar2(8))
/
ALTER TABLE PUR_DFLT_PRNCPL_ADDR_T
ADD CONSTRAINT PUR_DFLT_PRNCPL_ADDR_TP1
PRIMARY KEY (PRNCPL_ID)
/
ALTER TABLE PUR_DFLT_PRNCPL_ADDR_T
ADD CONSTRAINT PUR_DFLT_PRNCPL_ADDR_TR1
FOREIGN KEY (PRNCPL_ID) REFERENCES KRIM_PRNCPL_T
/
ALTER TABLE PUR_DFLT_PRNCPL_ADDR_T
ADD CONSTRAINT PUR_DFLT_PRNCPL_ADDR_TC0
UNIQUE (OBJ_ID)
/
ALTER TABLE PUR_DFLT_PRNCPL_ADDR_T
ADD CONSTRAINT PUR_DFLT_PRNCPL_ADDR_TR2
FOREIGN KEY (CAMPUS_CD, BLDG_CD) REFERENCES SH_BUILDING_T (CAMPUS_CD, BLDG_CD)
/

update krim_typ_t set nmspc_cd = 'KFS-CAM' where kim_typ_id = '50'
/
