# Apache Cassandra & Spark Graph Analysis Tutorial

The aim of this tutorial is to demonstrate how [Apache Cassandra](https://cassandra.apache.org/) and [Apache Spark](https://spark.apache.org/) can be used in combination for analyzing graphs.

## Background

Apache Cassandra is an open source NOSQL database designed for horizontal scalability. That means large structured datasets can be stored by adding more machines to a cluster.

Apache Spark is an analytics engine for processing an analyzing large-scale datasets on-top of a distributed machine cluster. 

With the [GraphFrames](https://graphframes.github.io/graphframes) package, Spark also provides an API for graph computation.

Spark can be connected with Cassandra using the [DataStax Spark Cassandra Connector](https://github.com/datastax/spark-cassandra-connector).


## Prerequisites

Make sure [Java 11](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html) and the latest version of [Python 3.6+](https://www.python.org/) is installed on your system.

	java -version
	python --version

Download, unpack and link Apache Cassandra (tested with 4.0-beta4)

	curl -O https://downloads.apache.org/cassandra/4.0-beta4/apache-cassandra-4.0-beta4-bin.tar.gz

	tar xvfz apache-cassandra-4.0-beta4-bin.tar.gz

	ln -s apache-cassandra-4.0-beta4 apache-cassandra

Download, unpack and link Apache Spark (tested with release 3.1.2)

	curl -O https://mirror.klaus-uwe.me/apache/spark/spark-3.1.2/spark-3.1.2-bin-hadoop3.2.tgz

	tar xvfz spark-3.1.2-bin-hadoop3.2.tgz

	ln -s spark-3.1.2-bin-hadoop3.2 apache-spark

Set environment variables

	export CASSANDRA_HOME="YOUR_LOCAL_PATH/apache-cassandra"
	export SPARK_HOME="YOUR_LOCAL_PATH/apache-spark"
	export PATH=$PATH:"$CASSANDRA_HOME/bin"
	export PATH=$PATH:"$SPARK_HOME/bin"

## Launch Cassandra and Spark Shell

Start Apache Cassandra and cqlsh shell

	cassandra -f
	cqlsh

Start Apache Spark shell with Spark Cassandra connector and GraphFrames package

	spark-shell \
		--conf spark.cassandra.connection.host=127.0.0.1 \
        --packages com.datastax.spark:spark-cassandra-connector_2.12:3.0.1,graphframes:graphframes:0.8.1-spark3.0-s_2.12 \ 
        --conf spark.sql.extensions=com.datastax.spark.connector.CassandraSparkExtensions


You should now see a Spark shell and the Spark UI running on [localhost:4040](localhost:4040)


## Check Connection

Now, within the spark shell, we configure the connection to Cassandra and check whether we can see the default tables and keyspaces.

	spark.conf.set(s"spark.sql.catalog.mycatalog", "com.datastax.spark.connector.datasource.CassandraCatalog")

	spark.sql("SHOW NAMESPACES FROM mycatalog").show


## Ingest Data into Cassandra

For the sake of simplicity, this tutorial will use a single-node setup and the rather small High-energy physics citation network retrieved from the [Stanford Large Network Dataset Collection](https://snap.stanford.edu/data/cit-HepPh.html).

First, we create a keyspace and some table in Cassandra and ingest some network data.

	cqlsh -f scripts/schema.cql

Now, we should see the created keyspace and edge table in Spark.

	spark.sql("SHOW NAMESPACES FROM mycatalog").show
	spark.sql("SHOW TABLES FROM mycatalog.hep_citations").show

Before loading the dataset, we must remove the comments and convert the delimiter (Cassandra does not support tabs)
	
	tail -n +5 data/cit-HepPh.txt | sed 's/\t/;/g' > data/cit-HepPh.csv

Next, we ingest the data into Cassandra

	cqlsh -f scripts/ingest_data.cql

## Load data as Spark Data Frame

Now we load the edge list into an Apache Spark Dataframe

	val df = spark.read.table("mycatalog.hep_citations.edge")
	println(df.count)
	df.show

Alternatively we can use SQL to count the number of edges (421578)

	spark.sql("SELECT count(*) FROM mycatalog.hep_citations.edge").show()

...and the number of nodes (34546)

	spark.sql("SELECT source FROM mycatalog.hep_citations.edge UNION SELECT target FROM mycatalog.hep_citations.edge").count

## Using the GraphFrames API

**Disclaimer**: the GraphFrames package is still under development and might not be ready for production use. 

First, we import required GraphFrames

	import org.graphframes._
	import org.graphframes.GraphFrame

Next, we must rename our dataframe columns because GraphFrames expects *src* and *dst* columns when loading a Graph from an edge list.

	val norm_edge = df.select($"source" as "src", $"target" as "dst")

Now we can instantiate a GraphFrame instance

	val g = GraphFrame.fromEdges(norm_edge)

...and compute some simple graph statistics

	g.vertices.count()

	g.edges.count()

...and also run some Graph algorithms like connected component detection.

	sc.setCheckpointDir(".")
	val result = g.connectedComponents.run()

	result.show()

	result.select("component").distinct.count

For more GraphFrame examples, check the [GraphFrames User Guide](https://graphframes.github.io/graphframes/docs/_site/user-guide.html).

## Notes and Design Considerations

* Apache Cassandra scales horizontally (more data -> more machines) and can store huge amounts of data; the challenge lies in choosing a partitioning scheme that follows roughly a uniform distribution. This is challenging for edge lists of highly-skewed graphs, but can be solved flattening the distribution via a secondary-lookup table.

* Apache Spark works perfectly for filtering and joining huge data volumes; for certain graph algorithms it might, however, be more efficient to collect relevant data point at the driver, perform the computation, and then re-distribute the data across machines.


















