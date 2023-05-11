# Users schema

# --- !Ups

ALTER TABLE builds ADD IF NOT EXISTS links JSONB DEFAULT jsonb_build_array() NOT NULL;

# --- !Downs

ALTER TABLE builds DROP IF EXISTS links;
