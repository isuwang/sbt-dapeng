package com.isuwang.plugins.utils

import java.io.{File, FileWriter, PrintWriter}
import java.sql.{Connection, DriverManager}
import java.text.ParseException

import com.isuwang.plugins.DbGeneratorPlugin

import scala.collection.mutable

object DbGeneratorUtil {

  val driver = "com.mysql.jdbc.Driver"
  val enumRegx = """(.*\s*),(\s*\d:\s*.*\(\s*[a-zA-Z]+\)\s*;?)+""".r
  val singleEnumRegx = """\s*([\d]+):\s*([\u4e00-\u9fa5]+|[\w]+)\(([a-zA-Z]+)\)""".r

  def generateEntityFile(fileContent: String, targetPath: String, fileName: String) = {
    val file = new File(targetPath + fileName)
    val created = if (!file.getParentFile.exists()) file.getParentFile.mkdirs() else true
    println(s"generating file: ${targetPath} / ${file.getName}: ${created}")
    val printWriter = new PrintWriter(new FileWriter(file))
    printWriter.println(fileContent)
    printWriter.flush()
    printWriter.close()
  }

  def getEnumFields(columnComment: String) = {
    val result = columnComment match {
      case enumRegx(a, b) =>
        val enums: Array[(String, String)] = b.split(";").map(item => {
          item match {
            case singleEnumRegx(index, cnChars, enChars) =>
              println(s"foundEnumValue index: ${index}  cnChars:${cnChars}  enChars: ${enChars}")
              (index, enChars)
            case _ => throw new ParseException(s"invalid enum format: ${item} should looks like Int:xxx(englishWord)", 0)
          }
        })
        enums.toList
      case _ =>
        //println(s" Not match enum comment, skipped....${columnComment}")
        List[(String,String)]()
    }
    result
  }

  def generateEnumFile(columnName: String, columnComment: String, targetPath: String,packageName: String,fileName: String) = {
    val enumFields = getEnumFields(columnComment)
    if (enumFields.size > 0) {
      generateEntityFile(toEnumFileTemplate(enumFields,packageName,columnName), targetPath,fileName)
    }
  }


  def toDbClassTemplate(tableName: String, packageName: String,columns: List[(String, String, String)]) = {
    val sb = new StringBuilder(256)
    sb.append(s" package ${packageName}.entity \r\n")

    sb.append("\r\n import java.sql.Timestamp \r\n")
    sb.append(s" import ${packageName}.enum._ \r\n")
    if (columns.exists(c => List("DATETIME", "DATE", "TIMESTAMP").contains(c._2))) {
      sb.append(" import java.sql.Timestamp \r\n")
    }

    sb.append(" import wangzx.scala_commons.sql.ResultSetMapper \r\n\r\n ")

    sb.append(s" case class ${tableName} ( \r\n")
    columns.foreach(column => {
      val hasValidEnum: Boolean = !getEnumFields(column._3).isEmpty
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

    sb.toString()

  }


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
  def toEnumFileTemplate(enums: List[(String, String)],packageName: String, columnName: String): String = {
    val sb = new StringBuilder(256)
    val enumClassName = toFirstUpperCamel(columnName)
    sb.append(s" package ${packageName}.enum \r\n")
    sb.append(" import wangzx.scala_commons.sql.DbEnum \r\n")
    sb.append(" import wangzx.scala_commons.sql._ \r\n")

    /*
    override def toString(): String = s"(${id},${name})"

	 override def equals(obj: Any): Boolean =
		 if (obj == null) false
		 else if (obj == this) true
		 else if (obj.isInstanceOf[AccountType]) obj.asInstanceOf[AccountType].id == this.id
		 else false

	 override def hashCode(): Int = this.id
     */
    sb.append(s" class ${enumClassName} private(val id:Int, val name:String) extends DbEnum { \r\n")
    sb.append(s""" \t override def toString(): String = "(" + id + "," + name + ")"  \r\n\r\n """)
    sb.append(s"\t override def equals(obj: Any): Boolean = { \r\n")
    sb.append(s" \t\t\t if (obj == null) false \r\n")
    sb.append(s" \t\t\t else if (obj.isInstanceOf[${enumClassName}]) obj.asInstanceOf[${enumClassName}].id == this.id \r\n")
    sb.append(s" \t\t\t else false \r\n")
    sb.append(s" \t } \r\n\r\n")
    sb.append(" \t override def hashCode(): Int = this.id \r\n")
    sb.append(" } \r\n\r\n")

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
}
