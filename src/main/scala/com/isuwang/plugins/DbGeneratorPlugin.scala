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
      Please specific your ip, dbname, package, tableName for generated the entity..
      Default package is:
        com.isuwang.soa.scala.dbName.entity
      TableName is Optional, will generated all tableEntity if tableName not set
      For Example:
       1. generatedDbEntity 127.0.0.1 crm  com.isuwang.soa.scala.crm  crm_companies
          will generate entity like:  com.isuwang.soa.scala.crm.entity.XXX
          will generate enum like: com.isuwang.soa.scala.crm.enum.XXX

       2. generatedDbEntity 127.0.0.1 crm
          will generate entity like:  com.isuwang.soa.scala.crm.entity.XXX
          will generate enum like: com.isuwang.soa.scala.crm.enum.XXX

       3. generatedDbEntity 127.0.0.1 crm com.isuwang.soa.scala.crm.entity
          will generate entity like: com.isuwang.soa.scala.crm.entity.entity.XXX
          will generate enum like: com.isuwang.soa.scala.crm.entity.enum.XXX

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

        val (ipAddress,db,packageName,tableName) = args.size match {
          case 0 =>
            println(help)
            ("","","","")
          case 1 => println(help)
            ("","","","")
          case 2 => (args(0),args(1),s"com.isuwang.soa.scala.${args(1)}","")
          case 3 => (args(0),args(1),args(2),"")
          case 4 => (args(0), args(1),args(2), args(3))
          case _ => println(help)
            ("","","","")
        }

        val baseTargetPath = (baseDirectory in Compile).value.getAbsolutePath
        val connection = connectJdbc(ipAddress, db, user, passwd)

        if (!tableName.isEmpty) {
          println(s" Found Specific tableName: ${tableName}, start to generateDbEntity..")
          generateDbClass(tableName,db,connection,packageName,baseTargetPath)
        } else {
          println(s" No specific tableName found. will generate ${db} all tables..")

          getTableNamesByDb(db,connection).foreach(item => {
            println(s" start to generated ${db}.${item} entity file...")
            generateDbClass(item,db,connection,packageName,baseTargetPath)
          })
        }
      }
    },
    logLevel in generatedDbEntity := Level.Debug
  )



  def generateDbClass(tableName: String, db: String, connection: Connection, packageName: String, baseTargetPath: String): Unit = {
    val columns = getTableColumnInfos(tableName.toLowerCase, db, connection)
    val targetPath = baseTargetPath + "/src/main/scala/" + packageName.split("\\.").mkString("/") + "/"

    toCaseClassEntity(toFirstUpperCamel(tableName),packageName, columns, targetPath)
  }


  def toCaseClassEntity(tableName: String, packageName: String,columns: List[(String, String, String)], targetPath: String) = {
    val sb = new StringBuilder(256)
    sb.append(s" package ${packageName}.entity \r\n")
    sb.append("\r\n import java.sql.Timestamp \r\n")
    if (columns.exists(c => List("DATETIME", "DATE", "TIMESTAMP").contains(c._2))) {
      sb.append(" import java.sql.Timestamp \r\n")
    }

    sb.append(" import wangzx.scala_commons.sql.ResultSetMapper \r\n\r\n ")

    sb.append(s" case class ${tableName} ( \r\n")
    columns.foreach(column => {
      val hasValidEnum: Boolean = generateEnumFile(column._1, column._3, targetPath,packageName)
      sb.append(s" /** ${column._3} */ \r\n")
      sb.append(toCamel(column._1)).append(": ").append(
        if (hasValidEnum) toFirstUpperCamel(column._1) else toScalaFieldType(column._2)
      ).append(",\r\n")
    })
    sb.delete(sb.lastIndexOf(","), sb.lastIndexOf(",") + 1)
    sb.append(") \t\n \t\n")

    //添加数据库的隐式转换
    /*
    *  object TableName {
    *     implicit val resultSetMapper: ResultSetMapper[TableName] = ResultSetMapper.meterial[TableName]
    *  }
    *
    * */
    sb.append(s" object ${tableName} { \r\n")
    sb.append(s" \timplicit val resultSetMapper: ResultSetMapper[${tableName}] = ResultSetMapper.meterial[${tableName}] \r\n")
    sb.append(" }")


    val path = targetPath + s"${toFirstUpperCamel(tableName)}.scala"
    generateEntityFile(sb.toString(), path)

  }

  def generateEntityFile(fileContent: String, path: String) = {
    println(s"generated file path: ${path}")
    val printWriter = new PrintWriter(new FileWriter(path))
    printWriter.println(fileContent)
    printWriter.flush()
    printWriter.close()
  }


  def generateEnumFile(columnName: String, columnComment: String, targetPath: String,packageName: String) = {
    /**
      * EnumClass content:
      *   class ${enumClassName} private(val id:Int, val name:String) extends DbEnum
      *   object ${enumClassName} {
      *      val NEW = new ${enumClassName}(0, "NEW")
      *      def unknowne(id: Int) = new OrderStatus(id, s"<$id>")
      *      def valueOf(id: Int): OrderStatus = id match {
      *        case 0 => NEW
      *        case _ => unknowne(id)
      *      }
      *      implicit object Accessor extends DbEnumJdbcValueAccessor[OrderStatus](valueOf)
      *    }
      *
      * @param enums
      * @return
      */
    def toEnumFileContent(enums: Array[(String, String)],packageName: String): String = {
      val sb = new StringBuilder(256)
      val enumClassName = toFirstUpperCamel(columnName)
      sb.append(s" package ${packageName}.enum \r\n")
      sb.append(" import wangzx.scala_commons.sql.DbEnum \r\n")
      sb.append(" import wangzx.scala_commons.sql._ \r\n")

      sb.append(s" class ${enumClassName} private(val id:Int, val name:String) extends DbEnum \r\n")
      sb.append(s" object ${enumClassName} { \r\n")
      enums.foreach(enum => {
        sb.append(s"""\t val ${enum._2} = new ${enumClassName}(${enum._1},"${enum._2}") \r\n""")
      })
      sb.append(s"""\t def unknown(id: Int) = new ${enumClassName}(id, id+"") \r\n""")

      sb.append(s"""\t def valueOf(id: Int): ${enumClassName} = id match { \r\n""")
      enums.foreach(enum => {
        sb.append(s" \t\t case ${enum._1} => ${enum._2} \r\n")
      })
      sb.append(" \t\t case _ => unknown(id) \r\n")
      sb.append(" } \r\n")

      sb.append(s" implicit object Accessor extends DbEnumJdbcValueAccessor[${enumClassName}](valueOf) \r\n")

      sb.append("}")

      sb.toString()
    }

    columnComment match {
      case enumRegx(a, b) =>
        val enums: Array[(String, String)] = b.split(";").map(item => {
          item match {
            case singleEnumRegx(index, cnChars, enChars) =>
              println(s"foundEnumValue index: ${index}  cnChars:${cnChars}  enChars: ${enChars}")
              (index, enChars)
            case _ => throw new ParseException(s"invalid enum format: ${item} should looks like Int:xxx(englishWord)", 0)
          }
        })

        val path = targetPath + s"${toFirstUpperCamel(columnName)}.scala"
        generateEntityFile(toEnumFileContent(enums,packageName), path)
        true
      case _ =>
        //println(s" Not match enum comment, skipped....${columnComment}")
        false
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


  def getTableNamesByDb(db: String, connection: Connection)= {
    val sql=s"select table_name from information_schema.tables where table_schema='${db}' and table_type='base table'";

    val sqlStatement = connection.prepareStatement(sql)
    val resultSet = sqlStatement.executeQuery()
    val tableNames = mutable.MutableList[String]()
    while (resultSet.next()) {
      val tableName = resultSet.getString("table_name")
      tableNames += tableName
    }

    tableNames
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



