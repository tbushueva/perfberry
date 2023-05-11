# Users schema

# --- !Ups

CREATE TABLE projects (
	id SERIAL NOT NULL PRIMARY KEY,
	alias varchar(96) NOT NULL UNIQUE,
	name varchar(96) NOT NULL,
	overview JSONB NOT NULL DEFAULT jsonb_build_object(),
	searches JSONB NOT NULL DEFAULT jsonb_build_array(),
	graphs JSONB NOT NULL DEFAULT jsonb_build_array()
);

CREATE TABLE reports (
	id BIGSERIAL NOT NULL PRIMARY KEY,
	project_id SERIAL NOT NULL REFERENCES projects ON DELETE CASCADE ON UPDATE CASCADE,
	label varchar(128) NULL,
	links JSONB NOT NULL DEFAULT jsonb_build_array(),
	passed BOOLEAN NULL,
	created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()
);

CREATE TABLE builds (
	id BIGSERIAL NOT NULL PRIMARY KEY,
	report_id BIGSERIAL NOT NULL REFERENCES reports ON DELETE CASCADE ON UPDATE CASCADE,
	env varchar(96) NOT NULL,
	description varchar(255) NULL,
	statistics JSONB NOT NULL DEFAULT jsonb_build_object(),
	assertions JSONB NOT NULL DEFAULT jsonb_build_array(),
	passed BOOLEAN NULL,
	transactions JSONB NOT NULL DEFAULT jsonb_build_array(),
	created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()
);

# --- !Downs

DROP TABLE builds;
DROP TABLE reports;
DROP TABLE projects;
