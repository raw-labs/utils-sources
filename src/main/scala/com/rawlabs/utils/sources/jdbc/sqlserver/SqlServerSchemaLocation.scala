/*
 * Copyright 2023 RAW Labs S.A.
 *
 * Use of this software is governed by the Business Source License
 * included in the file licenses/BSL.txt.
 *
 * As of the Change Date specified in that file, in accordance with
 * the Business Source License, use of this software will be governed
 * by the Apache License, Version 2.0, included in the file
 * licenses/APL.txt.
 */

package com.rawlabs.utils.sources.jdbc.sqlserver

import java.io.Closeable
import com.rawlabs.utils.sources.jdbc.api.{JdbcSchemaLocation, JdbcTableLocation}
import com.rawlabs.utils.core.RawSettings

class SqlServerSchemaLocation(
    cli: SqlServerClient,
    val schema: String
) extends JdbcSchemaLocation(cli, Some(schema)) {

  val host: String = cli.hostname

  val port: Int = cli.port

  val dbName: String = cli.maybeDatabase.get

  val username: String = cli.maybeUsername.get

  val password: String = cli.maybePassword.get

  def this(host: String, port: Int, dbName: String, username: String, password: String, schema: String)(
      implicit settings: RawSettings
  ) = {
    this(
      new SqlServerClient(host, port, dbName, username, password),
      schema
    )
  }

  override def listTables(): Iterator[JdbcTableLocation] with Closeable = {
    new Iterator[JdbcTableLocation] with Closeable {
      private val it = cli.listTables(schema)

      override def hasNext: Boolean = it.hasNext

      override def next(): JdbcTableLocation = {
        new SqlServerTableLocation(cli, schema, it.next())
      }

      override def close(): Unit = it.close()
    }
  }

  override def equals(obj: Any): Boolean = {
    obj match {
      case other: SqlServerSchemaLocation =>
        host == other.host && port == other.port && dbName == other.dbName && username == other.username && password == other.password && schema == other.schema
      case _ => false
    }
  }

  override def hashCode(): Int = {
    Seq(host, port, dbName, username, password, schema).hashCode()
  }

}
