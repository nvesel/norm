package norm.dao;

import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SelectQuery {
    private final Logger log = Logger.getLogger(this.getClass());

    private static final Pattern pattern = Pattern.compile("[^\\w_]([\\w_$]+?)/\\*<!(.+?)!>\\*/");

    private String queryName;
    private String query;
    private Map<String, String[]> columnEntityFieldMap = new HashMap<>(); //i.e. {employee_id:id}
    private Map<String, String> columnDataTypeMap = new HashMap<>(); //Will be set when a query is ran for first time

    public SelectQuery(String queryName, String query) {
        log.info("Parsing query:"+queryName);

        this.queryName = queryName;
        this.query = query;

        Matcher match = pattern.matcher(query);
        while (match.find()) {
            String column = match.group(1);
            String field = match.group(2);
            if ((column == null && field != null) || (column != null && field == null)) {
                log.warn("Potentially malformed select query:" + queryName);
                continue;
            }

            columnEntityFieldMap.put(column.toLowerCase(), field != null ? field.split("\\.") : new String[0]);
        }

        if (!columnEntityFieldMap.isEmpty())
            log.info("Query " + queryName + " is a select query with mapped columns:" + columnEntityFieldMap);
    }

    public String getQueryName() {
        return queryName;
    }

    public String getQuery() {
        return query;
    }

    public Map<String, String[]> getColumnEntityFieldMap() {
        return columnEntityFieldMap;
    }

    public Map<String, String> getColumnDataTypeMap() {
        return columnDataTypeMap;
    }

}

