# Users schema

# --- !Ups

ALTER TABLE projects ADD IF NOT EXISTS apdex JSONB DEFAULT jsonb_build_array() NOT NULL;

# --- !Downs

ALTER TABLE projects DROP IF EXISTS apdex;
