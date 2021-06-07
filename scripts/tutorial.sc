// check connection and database schema

spark.conf.set(s"spark.sql.catalog.mycatalog", "com.datastax.spark.connector.datasource.CassandraCatalog")

spark.sql("SHOW NAMESPACES FROM mycatalog").show

// load edge list as dataframe
val df = spark.read.table("mycatalog.hep_citations.edge")


// create GraphFrame from loaded dataframe

import org.graphframes._
import org.graphframes.GraphFrame

// GraphFrames expects columns src and dst
val norm_edge = df.select($"source" as "src", $"target" as "dst")

val g = GraphFrame.fromEdges(norm_edge)

// compute some basic graph statistics

g.vertices.count()

g.edges.count()


sc.setCheckpointDir(".")
val result = g.connectedComponents.run()

result.show()

result.select("component").distinct.count