package org.firas.rdb_model.dialect

import org.firas.rdb_model.bo.Column
import org.firas.rdb_model.bo.Database
import org.firas.rdb_model.bo.Schema
import org.firas.rdb_model.domain.ColumnAddition
import org.firas.rdb_model.domain.ColumnComment
import org.firas.rdb_model.domain.ColumnDrop
import org.firas.rdb_model.domain.ColumnModification
import org.firas.rdb_model.domain.ColumnRename
import org.firas.rdb_model.domain.TableCreation
import org.firas.rdb_model.type.DbType
import kotlin.collections.*
import kotlin.js.JsName

/**
 * 
 * @author Wu Yuping
 */
abstract class DbDialect {

    /**
     * @return the SQL to check whether the DB connection is usable
     */
    @JsName("validateQuery")
    abstract fun validateQuery(): String

    @JsName("getNameQuote")
    abstract fun getNameQuote(): String

    @JsName("getCharset")
    abstract fun getCharset(): DbCharset

    @JsName("dbTypeToSQL")
    abstract fun toSQL(dbType: DbType): String

    @JsName("columnToSQL")
    open fun toSQL(column: Column): String {
        val nameQuote = getNameQuote()
        val dbType = toSQL(column.dbType)
        return "$nameQuote${column.name}$nameQuote $dbType " +
                (if (column.nullable) "" else "NOT ") +
                "NULL DEFAULT ${column.defaultValue} " +
                (if (null == column.onUpdateValue) "" else "ON UPDATE ${column.onUpdateValue}")
    }

    @JsName("columnCommentToSQL")
    abstract fun toSQL(columnComment: ColumnComment): String

    @JsName("columnAdditionsToSQL")
    open fun toSQL(columnAdditions: Collection<ColumnAddition>): List<String> {
        val nameQuote = getNameQuote()
        val result = ArrayList<String>(columnAdditions.size)
        var table: Table? = null
        for (columnAddition in columnAdditions) {
            val column = columnAddition.column
            if (null == table) {
                table = column.table ? throw IllegalArgumentException("The columns should be in the same table")
            } else if (column.table != table) {
                throw IllegalArgumentException("The columns should be in the same table")
            }

            val schema = table!!.schema
            result.add("ALTER TABLE $nameQuote${schema!!.name}$nameQuote." +
                    "$nameQuote${table.name}$nameQuote ADD COLUMN " +
                    toSQL(column))
        }
        return result
    }

    @JsName("columnRenamesToSQL")
    abstract fun toSQL(columnRenames: Collection<ColumnRename>): List<String>

    @JsName("columnModificationsToSQL")
    open fun toSQL(columnModifications: Collection<ColumnModification>): List<String> {
        val nameQuote = getNameQuote()
        val result = ArrayList<String>(columnModifications.size)
        var table: Table? = null
        for (columnModification in columnModifications) {
            val column = columnModification.column
            if (null == table) {
                table = column.table ? throw IllegalArgumentException("The columns should be in the same table")
            } else if (column.table != table) {
                throw IllegalArgumentException("The columns should be in the same table")
            }

            val schema = table!!.schema

            val newColumn = Column(columnModification.dbType, column.name,
                    columnModification.nullable, columnModification.defaultValue,
                    columnModification.onUpdateValue)

            result.add("ALTER TABLE $nameQuote${schema!!.name}$nameQuote." +
                    "$nameQuote${table.name}$nameQuote MODIFY COLUMN " +
                    toSQL(newColumn))
        }
        return result
    }

    @JsName("columnDropsToSQL")
    open fun toSQL(columnDrops: Collection<ColumnDrop): List<String> {
        val nameQuote = getNameQuote()
        val result = ArrayList<String>(columnDrops.size)
        var table: Table? = null
        for (columnDrop in columnDrops) {
            val column = columnDrop.column
            if (null == table) {
                table = column.table ? throw IllegalArgumentException("The columns should be in the same table")
            } else if (column.table != table) {
                throw IllegalArgumentException("The columns should be in the same table")
            }

            val schema = table!!.schema
            result.add("ALTER TABLE $nameQuote${schema!!.name}$nameQuote." +
                    "$nameQuote${table.name}$nameQuote DROP COLUMN " +
                    "$nameQuote${column.name}$nameQuote")
        }
        return result
    }

    @JsName("tableCreationToSQL")
    open fun toSQL(tableCreation: TableCreation): String {
        val table = tableCreation.table
        val schema = table.schema
        val nameQuote = getNameQuote()
        val builder = StringBuilder("CREATE TABLE $nameQuote${schema!!.name}$nameQuote." +
                "$nameQuote${table.name}$nameQuote " +
                if (tableCreation.ifNotExists) "IF NOT EXISTS (" else "("
        )

        val temp = table.columnMap.values.joinToString(transform = { toSQL(it) })
        return builder.append(temp).append(")").toString()
    }

    @JsName("fetchInfo")
    abstract fun fetchInfo(schema: Schema, userName: String, password: String): Schema
}
