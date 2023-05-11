# Users schema

# --- !Ups

ALTER TABLE projects ADD IF NOT EXISTS assertions JSONB DEFAULT jsonb_build_array() NOT NULL;

# --- !Downs

ALTER TABLE projects DROP IF EXISTS assertions;
