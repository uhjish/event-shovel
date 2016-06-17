package com.rootedinsights.loggerhead

import java.sql.Timestamp
import java.util.UUID

import scala.math._

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.Row
import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.SaveMode
import org.apache.spark.sql.functions._
import org.apache.spark.sql.cassandra._

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext

import com.datastax.spark.connector._
import org.apache.log4j.{ LogManager, Level }

case class Event (
	event_day:Long,
	account_id:Long,
	occurred_millis:Long,
	actor_object_id:Long,
	object_id:Long,
	security_action_id:Long,
	event_data:String,
	event_type:String,
	object_group_oid:Long,
	id:java.util.UUID 
)

object EventShovel {

  LogManager.getRootLogger().setLevel(Level.WARN)
  
  def main(args:Array[String]) {

    val SparkMasterHost = "127.0.0.1"

    val CassandraHost = "127.0.0.1"

    // Tell Spark the address of one Cassandra node:
    val conf = new SparkConf(true)
      .set("spark.cassandra.connection.host", CassandraHost)
      .setAppName(getClass.getSimpleName)

    // Connect to the Spark cluster:
    lazy val sc = new SparkContext(conf)
    val minTs:Long = 1262322000000L
    val maxTs:Long = System.currentTimeMillis
    val offs:Long = maxTs % 473040000L

    val sqlContext = new org.apache.spark.sql.SQLContext(sc)

    import sqlContext.implicits._

    val df = sqlContext
      .read
      .format("org.apache.spark.sql.cassandra")
      .options(Map( "table" -> "security_event", "keyspace" -> "newton" ))
      .load()

    val rand_ts = df.map( t => {
      val ms = t.getAs[Long]("occurred_millis") - offs
      Event(
        t.getAs[Long]("event_day"),
        t.getAs[Long]("account_id"),
        ms,
        t.getAs[Long]("actor_object_id"),
        t.getAs[Long]("object_id"),
        t.getAs[Long]("security_action_id"),
        t.getAs[String]("event_data"),
        t.getAs[String]("event_type"),
        t.getAs[Long]("object_group_oid"),
        java.util.UUID.fromString(t.getAs[String]("id"))
      )
    })

    rand_ts.saveToCassandra("newton", "security_event")
    /*
      .format("org.apache.spark.sql.cassandra")
      .options(Map( "table" -> "security_event", "keyspace" -> "newton"))
      .save()
    */
  }
}


// vim: ft=scala tw=0 sw=2 ts=2 et
