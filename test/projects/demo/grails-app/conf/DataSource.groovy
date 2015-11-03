dataSource {
    pooled = true
    jmxExport = true
    driverClassName = "org.h2.Driver"
    username = "sa"
    password = ""
    logSql = true
}
hibernate {
    singleSession = true // configure OSIV singleSession mode
    flush.mode = 'manual' // OSIV session flush mode outside of transactional context

}


def env = System.getenv()
def mysqlHost = env["DB_PORT_3306_TCP_ADDR"]?:"localhost"
def mysqlPort = env["DB_PORT_3306_TCP_PORT"]?:"3306"
def mysqlUser = env["DB_USER"]?:"root"
def mysqlPass = env["DB_PASS"]?:""
def mysqlDatabase = env["DATABASE"] ?: "nestedset_demo"

// environment specific settings
environments {
    development {
        dataSource {
            dbCreate = 'update'
            url = "jdbc:mysql://${mysqlHost}:${mysqlPort}/${mysqlDatabase}?characterEncoding=utf8&zeroDateTimeBehavior=convertToNull"
            driverClassName = 'com.mysql.jdbc.Driver'
            username = mysqlUser
            password = mysqlPass
        }
    }
    test {
        dataSource {
            dbCreate = "create-drop"
            url = "jdbc:h2:mem:testDb;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE"
        }
    }
    production {
        dataSource {
            dbCreate = "update"
            url = "jdbc:h2:prodDb;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE"
            properties {
               // See http://grails.org/doc/latest/guide/conf.html#dataSource for documentation
               jmxEnabled = true
               initialSize = 5
               maxActive = 50
               minIdle = 5
               maxIdle = 25
               maxWait = 10000
               maxAge = 10 * 60000
               timeBetweenEvictionRunsMillis = 5000
               minEvictableIdleTimeMillis = 60000
               validationQuery = "SELECT 1"
               validationQueryTimeout = 3
               validationInterval = 15000
               testOnBorrow = true
               testWhileIdle = true
               testOnReturn = false
               jdbcInterceptors = "ConnectionState"
               defaultTransactionIsolation = java.sql.Connection.TRANSACTION_READ_COMMITTED
            }
        }
    }
}
