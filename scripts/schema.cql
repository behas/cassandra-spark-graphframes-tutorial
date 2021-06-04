DROP KEYSPACE IF EXISTS hep_citations;

CREATE KEYSPACE hep_citations WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

USE hep_citations;

CREATE TABLE edge (
	source int,
	target int,
	PRIMARY KEY (source, target)
);