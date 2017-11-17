package com.isuwang.plugins

import java.io.{FileWriter, PrintWriter}
import java.sql.{Connection, DriverManager}
import java.text.ParseException

import sbt.Keys.{baseDirectory, logLevel}
import sbt._

import scala.collection.mutable

object DbGeneratorPlugin extends AutoPlugin {

  val driver = "com.mysql.jdbc.Driver"
  val user = "root"
  val passwd = "root"

  val enumRegx = """(.*\s*),(\s*\d:\s*.*\(\s*[a-zA-Z]+\)\s*;?)+""".r
  val singleEnumRegx = """\s*([\d]+):\s*([\u4e00-\u9fa5]+|[\w]+)\(([a-zA-Z]+)\)""".r

  val generatedDbEntity = inputKey[Unit]("A demo input task.")

  import complete.DefaultParsers._


  override lazy val projectSettings = Seq(
    generatedDbEntity := {
      val help =
        """
      Please specific your ip, dbname, tableName for generated the entity..
      For Example:
        generatedDbEntity 127.0.0.1 crm crm_companies

      note: enum COMMENT FORMAT should be:
      Comment,EnumIndex:ChineseChars(EnglishChars);enumIndex:ChineseChars(EnglishChars);

      like:
      账户类型,1:资金账户(CAPITAL);2:贷款账号(CREDIT);3:预付账户(PREPAY);
    """.stripMargin
      // get the result of parsing
      val args: Seq[String] = spaceDelimited("").parsed
      if (args.isEmpty) {
        println(help)
      } else {
        println(" start to generated db entity....args: ")
        args foreach println
        val ipAddress = args(0)
        val db = args(1)
        val tableName = args(2)
        val targetPath = (baseDirectory in Compile).value.getAbsolutePath

        val connection = connectJdbc(ipAddress, db, user, passwd)

        val columns = getTableColumnInfos(tableName.toLowerCase, db, connection)
        println(s" oriTableName: ${tableName} -> tarTableName: ${toFirstUpperCamel(tableName)}")
        println(s"columns: ${columns}")
        toCaseClassEntity(toFirstUpperCamel(tableName), columns, targetPath)
      }
    },
    logLevel in generatedDbEntity := Level.Debug
  )


  def toCaseClassEntity(tableName: String, columns: List[(String, String, String)], targetPath: String) = {
    val sb = new StringBuilder(256)
    sb.append(" package com.isuwang.soa.scala.entity \r\n")
    sb.append("\r\n import java.sql.Timestamp \r\n")
    if (columns.exists(c => List("DATETIME", "DATE", "TIMESTAMP").contains(c._2))) {
      sb.append(" import java.sql.Timestamp \r\n")
    }
    sb.append(s" case class ${tableName} ( \r\n")
    columns.foreach(column => {
      sb.append(s" /** ${column._3} */ \r\n")
      sb.append(toCamel(column._1)).append(": ").append(toScalaFieldType(column._2)).append(",\r\n")

      generateEnumFile(column._1, column._3, targetPath)
    })
    sb.delete(sb.lastIndexOf(","), sb.lastIndexOf(",") + 1)
    sb.append(")")


    val path = targetPath + "/src/main/scala/com/isuwang/soa/" + s"${tableName}.scala"
    generateEntityFile(sb.toString(), path)

  }

  def generateEntityFile(fileContent: String, path: String) = {
    println(s"generated file path: ${path}")
    val printWriter = new PrintWriter(new FileWriter(path))
    printWriter.println(fileContent)
    printWriter.flush()
    printWriter.close()
  }

  def generateEnumFile(columnName: String, columnComment: String, targetPath: String) = {

    columnComment match {
      case enumRegx(a, b) => println(s" comment: ${a}, enumValue: ${b}")
        val enums: Array[(String, String)] = b.split(";").map(item => {
          item match {
            case singleEnumRegx(index, cnChars, enChars) =>
              println(s"foundEnumValue index: ${index}  cnChars:${cnChars}  enChars: ${enChars}")
              (index, enChars)
            case _ => throw new ParseException(s"invalid enum format: ${item} should looks like Int:xxx(englishWord)", 0)
          }
        })

        val path = targetPath + "/src/main/scala/com/isuwang/soa/" + s"${toFirstUpperCamel(columnName)}.scala"
        generateEntityFile(toEnumFileContent(enums), path)

      case _ => println(s" Not match enum comment, skipped....${columnComment}")
    }

    def toEnumFileContent(enums: Array[(String, String)]): String = {
      val sb = new StringBuilder(256)
      val enumClassName = toFirstUpperCamel(columnName)
      sb.append(" package com.isuwang.soa.scala.enum \r\n")
      sb.append(s" object ${enumClassName} extends Enumeration { \r\n")
      sb.append(s" \t type ${enumClassName} = Value \r\n")

      enums.foreach(enum => {
        sb.append(s"""\t val ${enum._2} = Value(${enum._1},"${enum._2}") \r\n""")
      })

      sb.append("}")

      sb.toString()
    }
  }

  def toScalaFieldType(tableFieldType: String): String = {
    tableFieldType.toUpperCase() match {
      case "INT" | "SMALLINT" | "TINYINT" => "Int"
      case "CHAR" | "VARCHAR" => "String"
      case "DECIMAL" => "BigDecimal"
      case "DATETIME" | "DATE" | "TIMESTAMP" => "Timestamp"
      case "ENUM" => "String"
      case _ => throw new ParseException(s"tableFieldType = ${tableFieldType} 无法识别", 1023)
    }
  }

  def connectJdbc(ip: String, db: String, user: String = "root", passwd: String = "root"): Connection = {

    val url = s"jdbc:mysql://${ip}/${db}?useUnicode=true&characterEncoding=utf8"

    try {
      Class.forName(driver)
    } catch {
      case e: Exception => println(s" failed to instance jdbc driver: ${e.getStackTrace}")
    }

    DriverManager.getConnection(url, user, passwd)
  }


  def getTableColumnInfos(tableName: String, db: String, connection: Connection): List[(String, String, String)] = {
    val sql = s"select column_name,data_type,column_comment from information_schema.Columns where table_name='${tableName}' and table_schema='${db}'"

    val sqlStatement = connection.prepareStatement(sql)

    val resultSet = sqlStatement.executeQuery()
    val columnInfos = mutable.MutableList[(String, String, String)]()
    while (resultSet.next()) {
      val columnInfo = (
        keywordConvert(resultSet.getString("column_name")),
        resultSet.getString("data_type"),
        resultSet.getString("column_comment")
      )
      columnInfos += columnInfo
    }

    columnInfos.toList
  }

  def keywordConvert(word: String) = {
    if (List("abstract",
      "case",
      "catch",
      "class",
      "def",
      "do",
      "else",
      "extends",
      "false",
      "final",
      "finally",
      "for",
      "forSome",
      "if",
      "implicit",
      "import",
      "lazy",
      "macro",
      "match",
      "new",
      "null",
      "object",
      "override",
      "package",
      "private",
      "protected",
      "return",
      "sealed",
      "super",
      "this",
      "throw",
      "trait",
      "try",
      "true",
      "type",
      "val",
      "var",
      "while",
      "with",
      "yield").exists(_.equals(word))) {
      s"`${word}`"
    } else word
  }

  /**
    * sss_xxx => sssXxx
    *
    * @param name
    * @return
    */
  def toCamel(name: String): String = {
    val camel = name.split("_").map(item => {
      val result = item.toLowerCase
      result.charAt(0).toUpper + result.substring(1)
    }).mkString("")
    camel.replaceFirst(s"${camel.charAt(0)}", s"${camel.charAt(0).toLower}")
  }

  /**
    * aaa_bbb => AaaBbb
    *
    * @param name
    * @return
    */
  def toFirstUpperCamel(name: String): String = {
    name.split("_").map(item => {
      val result = item.toLowerCase
      result.charAt(0).toUpper + result.substring(1)
    }).mkString("")
  }


}



