package norm.dao;

import norm.dao.exceptions.DaoRuntimeException;
import org.apache.log4j.Logger;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Initialize the Data Base
 * * Create Missing Objects
 * * Update Existing Objects
 * * Insert New Records
 *
 * Make sure initDb is Idempotent!!!
 */
public class InitDB {
    private static final Logger log = Logger.getLogger(InitDB.class);

    private NamedParameterJdbcTemplate jdbcTemplate;
    private int startVersionMajor = 0;
    private int startVersionMinor = 0;
    private Properties sqlStatements = new Properties();
    private static final Pattern batchKeyPattern = Pattern.compile("^batch\\.([0-9]+)\\.([0-9]+)\\.([0-9]+)$",
            Pattern.CASE_INSENSITIVE);//Batch keys should match /batch.MAJOR_V.MINOR_V.SORT_ORDER_ID/i

    public InitDB(NamedParameterJdbcTemplate jdbcTemplate, String sqlStatementsXmlPropertyFile,
                  Integer startVersionMajor, Integer startVersionMinor) {
        this.jdbcTemplate = jdbcTemplate;
        this.startVersionMajor = startVersionMajor;
        this.startVersionMinor = startVersionMinor;

        if (startVersionMajor < 0 || startVersionMinor < 0)
            return;

        try
        {
            this.sqlStatements = Utils.loadXmlProperties(sqlStatementsXmlPropertyFile);

            initDb();
        }
        catch (IOException e) {
            throw new DaoRuntimeException("DB Initialization failed:" + e.toString());
        }
    }

    private void initDb() {
        log.info("DB Initialization Started");

        List<String> batchNames = new ArrayList<>(sqlStatements.stringPropertyNames());
        Collections.sort(batchNames, new Comparator<String>() {
            public int compare(String key1, String key2) {

                int major1 = 0;
                int minor1 = 0;
                int batchId1 = 0;
                Matcher match1 = batchKeyPattern.matcher(key1);
                if (match1.find()) {
                    major1 = Integer.parseInt(match1.group(1));
                    minor1 = Integer.parseInt(match1.group(2));
                    batchId1 = Integer.parseInt(match1.group(3));
                }
                else {
                    throw new DaoRuntimeException("Malformed SQL Batch Property Name:"+key1);
                }

                int major2 = 0;
                int minor2 = 0;
                int batchId2 = 0;
                Matcher match2 = batchKeyPattern.matcher(key2);
                if (match2.find()) {
                    major2 = Integer.parseInt(match2.group(1));
                    minor2 = Integer.parseInt(match2.group(2));
                    batchId2 = Integer.parseInt(match2.group(3));
                }
                else {
                    throw new DaoRuntimeException("Malformed SQL Batch Property Name:"+key1);
                }

                if (major1 > major2)
                    return 1;
                else if (major1 < major2)
                    return -1;
                else if (minor1 > minor2)
                    return 1;
                else if (minor1 < minor2)
                    return -1;
                else if (batchId1 > batchId2)
                    return 1;
                else if (batchId1 < batchId2)
                    return -1;

                throw new DaoRuntimeException("Duplicated SQL Batch Property Name:"+key1);
            }
        });

        for (String batchName : batchNames) {

            int major = 0;
            int minor = 0;
            Matcher match = batchKeyPattern.matcher(batchName);
            if (match.find()) {
                major = Integer.parseInt(match.group(1));
                minor = Integer.parseInt(match.group(2));
            }

            if (major < startVersionMajor)
                continue;

            if (minor < startVersionMinor)
                continue;

            //execute batch
            log.info("Db Init "+batchName);
            jdbcTemplate.getJdbcOperations().execute(sqlStatements.getProperty(batchName));
        }

        log.info("DB Initialization Finished");
    }
}

