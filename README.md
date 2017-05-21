Scatter
=======

If you're going to yell into a void, it might as well be your own.


## About
_Scatter_ is a one-person social network.


## Getting Set Up
Here's a couple tips if you want to get your own version up and running.

### Database tables
Here is the sql to create the requisite tables.

```postgresql
CREATE TABLE IF NOT EXISTS posts(
  id BIGSERIAL PRIMARY KEY ,
  date BIGINT,
  content TEXT,
  media_type INT,
  media_link TEXT,
  tags TEXT[] DEFAULT '{}',
  n_likes BIGINT
);
CREATE INDEX on posts ("tags");


CREATE TABLE IF NOT EXISTS api_keys (
  public TEXT PRIMARY KEY ,
  private TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS users (
  id TEXT PRIMARY KEY ,
  email TEXT NOT NULL ,
  phone TEXT,
  password TEXT NOT NULL
);

```