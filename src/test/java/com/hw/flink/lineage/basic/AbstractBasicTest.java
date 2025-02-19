package com.hw.flink.lineage.basic;

import com.hw.flink.lineage.LineageContext;
import com.hw.flink.lineage.Result;
import org.apache.flink.table.catalog.hive.HiveCatalog;
import org.apache.flink.table.catalog.hive.HiveTestUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

/**
 * @description: AbstractBasicTest
 * @author: HamaWhite
 * @version: 1.0.0
 * @date: 2022/11/23 11:04 AM
 */
public abstract class AbstractBasicTest {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractBasicTest.class);

    private static String catalogName = "hive";

    private static String hiveVersion = "3.1.2";

    private static String defaultDatabase = "default";

    private static HiveCatalog hiveCatalog;

    protected static LineageContext context;

    @BeforeClass
    public static void setup() throws Exception {
        hiveCatalog = HiveTestUtils.createHiveCatalog(catalogName, defaultDatabase, hiveVersion);
        hiveCatalog.open();

        context = new LineageContext(hiveCatalog);
    }


    @AfterClass
    public static void closeCatalog() {
        if (hiveCatalog != null) {
            hiveCatalog.close();
        }
        HiveTestUtils.deleteTemporaryFolder();
    }

    protected void parseFieldLineage(String sql, String[][] expectedArray) {
        List<Result> actualList = context.parseFieldLineage(sql);
        LOG.info("Linage Result: ");
        actualList.forEach(e -> LOG.info(e.toString()));

        List<Result> expectedList = Stream.of(expectedArray)
                .map(e -> {
                    Result result = buildResult(e[0], e[1], e[2], e[3]);
                    // transform field is optional
                    if (e.length == 5) {
                        result.setTransform(e[4]);
                    }
                    return result;
                }).collect(Collectors.toList());


        assertEquals(expectedList, actualList);
    }

    protected Result buildResult(String sourceTable, String sourceColumn, String targetTable, String targetColumn) {
        return Result.builder()
                .sourceCatalog(catalogName)
                .sourceDatabase(defaultDatabase)
                .sourceTable(sourceTable)
                .sourceColumn(sourceColumn)
                .targetCatalog(catalogName)
                .targetDatabase(defaultDatabase)
                .targetTable(targetTable)
                .targetColumn(targetColumn)
                .build();
    }


    /**
     * Create mysql cdc table ods_mysql_users
     */
    protected void createTableOfOdsMysqlUsers() {
        context.execute("DROP TABLE IF EXISTS ods_mysql_users ");

        context.execute("CREATE TABLE IF NOT EXISTS ods_mysql_users (" +
                "       id                  BIGINT PRIMARY KEY NOT ENFORCED ," +
                "       name                STRING                          ," +
                "       birthday            TIMESTAMP(3)                    ," +
                "       ts                  TIMESTAMP(3)                    ," +
                "       proc_time as proctime()                              " +
                ") WITH ( " +
                "       'connector' = 'mysql-cdc'            ," +
                "       'hostname'  = '127.0.0.1'       ," +
                "       'port'      = '3306'                 ," +
                "       'username'  = 'root'                 ," +
                "       'password'  = 'xxx'          ," +
                "       'server-time-zone' = 'Asia/Shanghai' ," +
                "       'database-name' = 'demo'             ," +
                "       'table-name'    = 'users' " +
                ")"
        );
    }

    /**
     * Create mysql cdc table ods_mysql_users_watermark
     */
    protected void createTableOfOdsMysqlUsersWatermark() {
        context.execute("DROP TABLE IF EXISTS ods_mysql_users_watermark ");

        context.execute("CREATE TABLE IF NOT EXISTS ods_mysql_users_watermark (" +
                "       id                  BIGINT PRIMARY KEY NOT ENFORCED ," +
                "       name                STRING                          ," +
                "       birthday            TIMESTAMP(3)                    ," +
                "       ts                  TIMESTAMP(3)                    ," +
                "       proc_time as proctime()                             ," +
                "       WATERMARK FOR ts AS ts - INTERVAL '5' SECOND         " +
                ") WITH ( " +
                "       'connector' = 'mysql-cdc'            ," +
                "       'hostname'  = '127.0.0.1'       ," +
                "       'port'      = '3306'                 ," +
                "       'username'  = 'root'                 ," +
                "       'password'  = 'xxx'          ," +
                "       'server-time-zone' = 'Asia/Shanghai' ," +
                "       'database-name' = 'demo'             ," +
                "       'table-name'    = 'users' " +
                ")"
        );
    }


    /**
     * Create mysql dim table dim_mysql_company
     */
    protected void createTableOfDimMysqlCompany() {
        context.execute("DROP TABLE IF EXISTS dim_mysql_company ");

        context.execute("CREATE TABLE IF NOT EXISTS dim_mysql_company (" +
                "       user_id                  BIGINT     ," +
                "       company_name              STRING     " +
                ") WITH ( " +
                "       'connector' = 'jdbc'                 ," +
                "       'url'       = 'jdbc:mysql://127.0.0.1:3306/demo?useSSL=false&characterEncoding=UTF-8'," +
                "       'username'  = 'root'                 ," +
                "       'password'  = 'xxx'          ," +
                "       'table-name'= 'company' " +
                ")"
        );
    }

    /**
     * Create Hudi sink table dwd_hudi_users
     */
    protected void createTableOfDwdHudiUsers() {
        context.execute("DROP TABLE IF EXISTS dwd_hudi_users");

        context.execute("CREATE TABLE IF NOT EXISTS  dwd_hudi_users ( " +
                "       id                  BIGINT PRIMARY KEY NOT ENFORCED ," +
                "       name                STRING                          ," +
                "       company_name        STRING                          ," +
                "       birthday            TIMESTAMP(3)                    ," +
                "       ts                  TIMESTAMP(3)                    ," +
                "        `partition`        VARCHAR(20)                      " +
                ") PARTITIONED BY (`partition`) WITH ( " +
                "       'connector' = 'hudi'                                    ," +
                "       'table.type' = 'COPY_ON_WRITE'                          ," +
                "       'read.streaming.enabled' = 'true'                       ," +
                "       'read.streaming.check-interval' = '1'                    " +
                ")"
        );
    }

}
