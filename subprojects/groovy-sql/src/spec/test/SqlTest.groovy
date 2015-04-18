/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

/**
* Tests for groovy.sql.Sql.
*/
class SqlTest extends GroovyTestCase {

    void testConnectingToHsqlDB() {
        assertScript '''
// tag::sql_connecting[]
        import groovy.sql.Sql

        def url = 'jdbc:hsqldb:mem:yourDB'
        def user = 'sa'
        def password = ''
        def driver = 'org.hsqldb.jdbcDriver'
        def sql = Sql.newInstance(url, user, password, driver)

        // use 'sql' instance ...
// end::sql_connecting[]

        // test of a system table within HSQLDB
        assert sql.firstRow('SELECT 1 FROM INFORMATION_SCHEMA.SYSTEM_USERS')[0] == 1

// tag::sql_connecting_close[]

        sql.close()
// end::sql_connecting_close[]
'''
    }

    void testConnectingToHsqlDBWithInstance() {
        assertScript '''
        import groovy.sql.Sql

        def url = 'jdbc:hsqldb:mem:yourDB'
        def user = 'sa'
        def password = ''
        def driver = 'org.hsqldb.jdbcDriver'
        // tag::sql_withInstance_p1[]
        Sql.withInstance(url, user, password, driver) { sql ->
          // use 'sql' instance ...
        // end::sql_withInstance_p1[]
          assert sql.firstRow('SELECT 1 FROM INFORMATION_SCHEMA.SYSTEM_USERS')[0] == 1
        // tag::sql_withInstance_p2[]
        }
        // end::sql_withInstance_p2[]
'''
    }

    void testConnectingUsingDataSource() {
        assertScript '''
        /*
        commented out as already on classpath
// tag::sql_connecting_grab[]
        @Grab('org.hsqldb:hsqldb:2.3.2')
        @GrabConfig(systemClassLoader=true)
        // create, use, and then close sql instance ...
// end::sql_connecting_grab[]
*/

// tag::sql_connecting_datasource[]
        import groovy.sql.Sql
        import org.hsqldb.jdbc.JDBCDataSource

        def dataSource = new JDBCDataSource(
            database: 'jdbc:hsqldb:mem:yourDB', user: 'sa', password: '')
        def sql = new Sql(dataSource)

        // use then close 'sql' instance ...
// end::sql_connecting_datasource[]

        // test of a system table within HSQLDB
        assert sql.firstRow('SELECT 1 FROM INFORMATION_SCHEMA.SYSTEM_USERS')[0] == 1
        sql.close()
'''
    }

    void testConnectingUsingApacheDataSource() {
        assertScript '''
        // tag::sql_connecting_datasource_dbcp[]
        @Grab('commons-dbcp:commons-dbcp:1.4')
        import groovy.sql.Sql
        import org.apache.commons.dbcp.BasicDataSource

        def ds = new BasicDataSource(driverClassName: "org.hsqldb.jdbcDriver",
            url: 'jdbc:hsqldb:mem:yourDB', username: 'sa', password: '')
        def sql = new Sql(ds)
        // use then close 'sql' instance ...
        // end::sql_connecting_datasource_dbcp[]
        assert sql.firstRow('SELECT 1 FROM INFORMATION_SCHEMA.SYSTEM_USERS')[0] == 1
        sql.close()
'''
    }

    void testCreatingTableByExecutingSql() {
        assertScript """
        import groovy.sql.Sql

        def url = 'jdbc:hsqldb:mem:yourDB'
        def user = 'sa'
        def password = ''
        def driver = 'org.hsqldb.jdbcDriver'
        def sql = Sql.newInstance(url, user, password, driver)

        sql.execute '''
        DROP TABLE Author IF EXISTS
        '''
        // tag::sql_creating_table[]
        // ... create 'sql' instance
        sql.execute '''
          CREATE TABLE Author (
            id          INTEGER GENERATED BY DEFAULT AS IDENTITY,
            firstname   VARCHAR(64),
            lastname    VARCHAR(64)
          );
        '''
        // close 'sql' instance ...
        // end::sql_creating_table[]
        assert sql.firstRow('SELECT COUNT(*) as num FROM Author').num == 0
        sql.close()
"""
    }

    void testInsertingRows() {
        assertScript '''
        import groovy.sql.Sql

        def url = 'jdbc:hsqldb:mem:yourDB'
        def user = 'sa'
        def password = ''
        def driver = 'org.hsqldb.jdbcDriver'
        Sql.withInstance(url, user, password, driver) { sql ->
          sql.execute """
          DROP TABLE Author IF EXISTS
          """
          sql.execute """
          CREATE TABLE Author (
            id          INTEGER GENERATED BY DEFAULT AS IDENTITY,
            firstname   VARCHAR(64),
            lastname    VARCHAR(64)
          )
          """

            // tag::sql_inserting_row[]
            sql.execute "INSERT INTO Author (firstname, lastname) VALUES ('Dierk', 'Koenig')"
            // end::sql_inserting_row[]

            // tag::sql_inserting_row_executeInsert[]
            def insertSql = 'INSERT INTO Author (firstname, lastname) VALUES (?,?)'
            def params = ['Jon', 'Skeet']
            def keys = sql.executeInsert insertSql, params
            assert keys[0] == [1]
            // end::sql_inserting_row_executeInsert[]

            // tag::sql_inserting_row_executeInsert_keys[]
            def first = 'Guillaume'
            def last = 'Laforge'
            def myKeyNames = ['ID']
            def myKeys = sql.executeInsert """
              INSERT INTO Author (firstname, lastname)
              VALUES (${first}, ${last})
            """, myKeyNames
            assert myKeys[0] == [ID: 2]
            // end::sql_inserting_row_executeInsert_keys[]

            assert sql.firstRow('SELECT COUNT(*) as num FROM Author').num == 3
        }
'''
    }

    void testReadingRows() {
        assertScript '''
        import groovy.sql.Sql

        def url = 'jdbc:hsqldb:mem:yourDB'
        def user = 'sa'
        def password = ''
        def driver = 'org.hsqldb.jdbcDriver'
        Sql.withInstance(url, user, password, driver) { sql ->
          sql.execute """
          DROP TABLE Author IF EXISTS
          """
          sql.execute """
          CREATE TABLE Author (
            id          INTEGER GENERATED BY DEFAULT AS IDENTITY,
            firstname   VARCHAR(64),
            lastname    VARCHAR(64)
          )
          """
          sql.execute "INSERT INTO Author (firstname, lastname) VALUES ('Dierk', 'Koenig')"
          sql.execute "INSERT INTO Author (firstname, lastname) VALUES ('Jon', 'Skeet')"
          sql.execute "INSERT INTO Author (firstname, lastname) VALUES ('Guillaume', 'Laforge')"

          // tag::sql_reading_query[]
          def expected = ['Dierk Koenig', 'Jon Skeet', 'Guillaume Laforge']

          def rowNum = 0
          sql.query('SELECT firstname, lastname FROM Author') { resultSet ->
            while (resultSet.next()) {
              def first = resultSet.getString(1)
              def last = resultSet.getString('lastname')
              assert expected[rowNum++] == "$first $last"
            }
          }
          // end::sql_reading_query[]

          // tag::sql_reading_eachrow[]
          rowNum = 0
          sql.eachRow('SELECT firstname, lastname FROM Author') { row ->
            def first = row[0]
            def last = row.lastname
            assert expected[rowNum++] == "$first $last"
          }
          // end::sql_reading_eachrow[]

          // tag::sql_reading_firstrow[]
          def first = sql.firstRow('SELECT lastname, firstname FROM Author')
          assert first.values().sort().join(',') == 'Dierk,Koenig'
          // end::sql_reading_firstrow[]

          // tag::sql_reading_rows[]
          List authors = sql.rows('SELECT firstname, lastname FROM Author')
          assert authors.size() == 3
          assert authors.collect { "$it.FIRSTNAME ${it[-1]}" } == expected
          // end::sql_reading_rows[]

          // tag::sql_reading_scalar[]
          assert sql.firstRow('SELECT COUNT(*) AS num FROM Author').num == 3
          // end::sql_reading_scalar[]
        }
        '''
    }

    void testUpdatingRows() {
        assertScript '''
        import groovy.sql.Sql

        def url = 'jdbc:hsqldb:mem:yourDB'
        def user = 'sa'
        def password = ''
        def driver = 'org.hsqldb.jdbcDriver'
        Sql.withInstance(url, user, password, driver) { sql ->
          sql.execute """
          DROP TABLE Author IF EXISTS
          """
          sql.execute """
          CREATE TABLE Author (
            id          INTEGER GENERATED BY DEFAULT AS IDENTITY,
            firstname   VARCHAR(64),
            lastname    VARCHAR(64)
          )
          """
          sql.execute "INSERT INTO Author (firstname, lastname) VALUES ('Dierk', 'Koenig')"
          sql.execute "INSERT INTO Author (firstname, lastname) VALUES ('Jon', 'Skeet')"
          sql.execute "INSERT INTO Author (firstname, lastname) VALUES ('Guillaume', 'Laforge')"

          // tag::sql_updating_execute[]
          sql.execute "INSERT INTO Author (lastname) VALUES ('Thorvaldsson')"
          sql.execute "UPDATE Author SET firstname='Erik' where lastname='Thorvaldsson'"
          // end::sql_updating_execute[]

          // tag::sql_updating_execute_update[]
          def updateSql = "UPDATE Author SET lastname='Pragt' where lastname='Thorvaldsson'"
          def updateCount = sql.executeUpdate updateSql
          assert updateCount == 1

          def row = sql.firstRow "SELECT * FROM Author where firstname = 'Erik'"
          assert "${row.firstname} ${row.lastname}" == 'Erik Pragt'
          // end::sql_updating_execute_update[]
        }
        '''
    }

    void testMetadata() {
        assertScript '''
            import groovy.sql.Sql

            def url = 'jdbc:hsqldb:mem:yourDB'
            def user = 'sa'
            def password = ''
            def driver = 'org.hsqldb.jdbcDriver'
            Sql.withInstance(url, user, password, driver) { sql ->
              sql.execute """
              DROP TABLE Author IF EXISTS
              """
              sql.execute """
              CREATE TABLE Author (
                id          INTEGER GENERATED BY DEFAULT AS IDENTITY,
                firstname   VARCHAR(64),
                lastname    VARCHAR(64)
              )
              """
              sql.execute "INSERT INTO Author (firstname, lastname) VALUES ('Dierk', 'Koenig')"

              // tag::sql_basic_rs_metadata[]
              sql.eachRow("SELECT * FROM Author WHERE firstname = 'Dierk'") { row ->
                def md = row.getMetaData()
                assert md.getTableName(1) == 'AUTHOR'
                assert (1..md.columnCount).collect{ md.getColumnName(it) } == ['ID', 'FIRSTNAME', 'LASTNAME']
                assert (1..md.columnCount).collect{ md.getColumnTypeName(it) } == ['INTEGER', 'VARCHAR', 'VARCHAR']
              }
              // end::sql_basic_rs_metadata[]
              // tag::sql_basic_rs_metadata2[]
              sql.eachRow("SELECT firstname AS first FROM Author WHERE firstname = 'Dierk'") { row ->
                def md = row.getMetaData()
                assert md.getColumnName(1) == 'FIRSTNAME'
                assert md.getColumnLabel(1) == 'FIRST'
              }
              // end::sql_basic_rs_metadata2[]
              // tag::sql_basic_rs_metadata3[]
              def metaClosure = { meta -> assert meta.getColumnName(1) == 'FIRSTNAME' }
              def rowClosure = { row -> assert row.FIRSTNAME == 'Dierk' }
              sql.eachRow("SELECT firstname FROM Author WHERE firstname = 'Dierk'", metaClosure, rowClosure)
              // end::sql_basic_rs_metadata3[]
              // tag::sql_basic_table_metadata[]
              def md = sql.connection.metaData
              assert md.driverName == 'HSQL Database Engine Driver'
              assert md.databaseProductVersion == '2.3.2'
              assert ['JDBCMajorVersion', 'JDBCMinorVersion'].collect{ md[it] } == [4, 0]
              assert md.stringFunctions.tokenize(',').contains('CONCAT')
              def rs = md.getTables(null, null, 'AUTH%', null)
              assert rs.next()
              assert rs.getString('TABLE_NAME') == 'AUTHOR'
              // end::sql_basic_table_metadata[]
            }
        '''
    }

    void testNamedOrdinal() {
        assertScript '''
            import groovy.sql.Sql

            def url = 'jdbc:hsqldb:mem:yourDB'
            def user = 'sa'
            def password = ''
            def driver = 'org.hsqldb.jdbcDriver'
            // tag::sql_named_ordinal1[]
            class Rockstar { String first, last }
            // end::sql_named_ordinal1[]
            Sql.withInstance(url, user, password, driver) { sql ->
              sql.execute """
              DROP TABLE Author IF EXISTS
              """
              sql.execute """
              CREATE TABLE Author (
                id          INTEGER GENERATED BY DEFAULT AS IDENTITY,
                firstname   VARCHAR(64),
                lastname    VARCHAR(64)
              )
              """
              // tag::sql_named[]
              sql.execute "INSERT INTO Author (firstname, lastname) VALUES (:first, :last)", first: 'Dierk', last: 'Koenig'
              // end::sql_named[]
              // tag::sql_named2[]
              sql.execute "INSERT INTO Author (firstname, lastname) VALUES (?.first, ?.last)", first: 'Jon', last: 'Skeet'
              // end::sql_named2[]
              // tag::sql_named_ordinal2[]
              def pogo = new Rockstar(first: 'Paul', last: 'McCartney')
              def map = [lion: 'King']
              sql.execute "INSERT INTO Author (firstname, lastname) VALUES (?1.first, ?2.lion)", pogo, map
              // end::sql_named_ordinal2[]
              assert sql.firstRow('SELECT COUNT(*) as num FROM Author').num == 3
            }
        '''
    }

    void testDeletingRows() {
        assertScript '''
        import groovy.sql.Sql

        def url = 'jdbc:hsqldb:mem:yourDB'
        def user = 'sa'
        def password = ''
        def driver = 'org.hsqldb.jdbcDriver'
        Sql.withInstance(url, user, password, driver) { sql ->
          sql.execute """
          DROP TABLE Author IF EXISTS
          """
          sql.execute """
          CREATE TABLE Author (
            id          INTEGER GENERATED BY DEFAULT AS IDENTITY,
            firstname   VARCHAR(64),
            lastname    VARCHAR(64)
          )
          """
          sql.execute "INSERT INTO Author (firstname, lastname) VALUES ('Dierk', 'Koenig')"
          sql.execute "INSERT INTO Author (firstname, lastname) VALUES ('Jon', 'Skeet')"
          sql.execute "INSERT INTO Author (firstname, lastname) VALUES ('Guillaume', 'Laforge')"

          // tag::sql_deleting_data[]
          assert sql.firstRow('SELECT COUNT(*) as num FROM Author').num == 3
          sql.execute "DELETE FROM Author WHERE lastname = 'Skeet'"
          assert sql.firstRow('SELECT COUNT(*) as num FROM Author').num == 2
          // end::sql_deleting_data[]
        }
        '''
    }

}
