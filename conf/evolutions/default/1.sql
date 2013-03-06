# --- First database schema

# --- !Ups

create table "user" (
    "name" 		varchar(255) not null primary key,
    "password" 	varchar(255) not null
  );

create sequence "s_user_name";


# --- !Downs

drop table if exists  user		 CASCADE;

drop sequence if exists  s_user_name    ;
