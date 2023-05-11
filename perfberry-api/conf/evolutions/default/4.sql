# Users schema

# --- !Ups

ALTER TABLE reports ADD IF NOT EXISTS description VARCHAR(1024) NULL;
ALTER TABLE builds ALTER COLUMN description SET DATA TYPE VARCHAR(1024);
ALTER TABLE builds ADD IF NOT EXISTS label VARCHAR(128) NULL;
ALTER TABLE reports ADD IF NOT EXISTS scm JSONB DEFAULT jsonb_build_object('parameters', jsonb_build_object()) NOT NULL;
ALTER TABLE builds ADD IF NOT EXISTS scm JSONB DEFAULT jsonb_build_object('parameters', jsonb_build_object()) NOT NULL;

# --- !Downs

ALTER TABLE reports DROP IF EXISTS description;
ALTER TABLE builds ALTER COLUMN description SET DATA TYPE VARCHAR(255);
ALTER TABLE builds DROP IF EXISTS label;
ALTER TABLE reports DROP IF EXISTS scm;
ALTER TABLE builds DROP IF EXISTS scm;
