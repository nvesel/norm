package norm.dao;

import norm.dao.exceptions.DaoRuntimeException;
import org.apache.log4j.Logger;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PersistQuery {
    private static final Logger log = Logger.getLogger(PersistQuery.class);

    private static final Pattern patternResultColumns = Pattern.compile("[^\\w_]([\\w_]+?)\\/\\*RES:<(.+?)>\\*\\/");
    private static final Pattern patternConditionColumns = Pattern.compile("\\?\\/\\*COND:<(.+?)>\\*\\/");

    private Persist.Operation persistOperation;
    private String description;
    private String query;
    //a list of Column definitions that are used to determine a single record
    private List<Column> conditionColumns;
    //a list of Column definitions that are to be inserted, updated or returned as result
    private List<Column> resultColumns;

    private static List<Column> parseResultColumns(String queryDescription, String sqlStatement) {
        log.info("parseResultColumns for "+queryDescription);

        List<Column> resultColumns = new ArrayList<>();
        Matcher match = patternResultColumns.matcher(sqlStatement);
        while (match.find()) {
            String columnName = match.group(1);
            String argumentsString = match.group(2);
            String[] arguments = argumentsString.split("><");
            if (arguments.length != 3) {
                log.warn("Potentially malformed Result Columns:"
                        + queryDescription+", columnName="+columnName+", argumentsString="+argumentsString);
                continue;
            }

            String fieldName = null;
            if (arguments[0] != null && arguments[0].length() > 0)
                fieldName = arguments[0];

            Method method = null;
            if (arguments[1] != null && arguments[1].length() > 0) {
                try {
                    String[] methodDefinition = arguments[1].split(":");
                    String methodClassName = methodDefinition[0];
                    String methodName = methodDefinition[1];
                    method = Class.forName(methodClassName).getMethod(methodName);
                }
                catch (Throwable e) {
                    log.error(e.toString());
                    throw new DaoRuntimeException("Persist query '"+queryDescription+"' column '"
                            +columnName+"' is malformed.");
                }
            }

            Column.InOut columnInOut = null;
            switch (arguments[2]) {
                case "INPUT":
                    columnInOut = Column.InOut.INPUT;
                    break;
                case "OUTPUT":
                    columnInOut = Column.InOut.OUTPUT;
                    break;
            }

            resultColumns.add(new Column(columnName, fieldName, method, columnInOut));
        }

        return resultColumns;
    }

    private static List<Column> parseConditionColumns(String queryDescription, String sqlStatement) {
        log.info("parseConditionColumns for "+queryDescription+"="+sqlStatement);

        List<Column> conditionColumns = new ArrayList<>();
        Matcher match = patternConditionColumns.matcher(sqlStatement);
        int conditionIx = 0;
        while (match.find()) {
            String columnName = "Condition["+(++conditionIx)+"]";
            String argumentsString = match.group(1);
            String[] arguments = argumentsString.split("><");
            if (arguments.length < 1 || arguments.length > 2) {
                log.warn("Potentially malformed Condition Columns:" + queryDescription+", "
                        +columnName+", argumentsString="+argumentsString);
                continue;
            }

            String fieldName = null;
            if (arguments[0] != null && arguments[0].length() > 0)
                fieldName = arguments[0];

            Method method = null;
            if (arguments.length == 2 && arguments[1] != null && arguments[1].length() > 0) {
                try {
                    String[] methodDefinition = arguments[1].split(":");
                    String methodClassName = methodDefinition[0];
                    String methodName = methodDefinition[1];
                    method = Class.forName(methodClassName).getMethod(methodName);
                }
                catch (Throwable e) {
                    log.error(e.toString());
                    throw new DaoRuntimeException("Persist query '"+queryDescription+"' '"
                            +columnName+"' is malformed.");
                }
            }

            conditionColumns.add(new Column(columnName, fieldName, method, Column.InOut.INPUT));
        }

        return conditionColumns;
    }

    /**
     * <p>A persist query (not s select query) definition. The same is used when an Entity is being persisted.</p>
     * @param persistOperation: query that can be used to insert, update or both (merge)
     * @param description: short, human readable description
     * @param query: the SQL query string
     */
    public PersistQuery(Persist.Operation persistOperation, String description, String query) {
        this.persistOperation = persistOperation;
        this.description = description;
        this.query = query;
        this.conditionColumns = parseConditionColumns(description, query);
        this.resultColumns = parseResultColumns(description, query);
    }

    public Persist.Operation getPersistOperation() {
        return persistOperation;
    }

    public String getDescription() {
        return description;
    }

    public String getQuery() {
        return query;
    }

    public List<Column> getConditionColumns() {
        return conditionColumns;
    }

    public List<Column> getResultColumns() {
        return resultColumns;
    }

    public String toString() {
        return "{persistOperation:" + String.valueOf(persistOperation.getOperationName()) + ", " +
                "description:" + String.valueOf(description) + ", " +
                "query:" + String.valueOf(query) + ", " +
                "conditionColumns:" + String.valueOf(conditionColumns) + ", " +
                "resultColumns:" + String.valueOf(resultColumns) + "}\n";
    }

    public static class Column {
        private final Logger log = Logger.getLogger(this.getClass());

        public enum InOut{INPUT, OUTPUT}

        private String entityFieldName;
        private String columnName;
        private Method method = null;
        private InOut inOut = InOut.INPUT;

        public Column(String columnName, String entityFieldName) {
            this(columnName, entityFieldName, null, InOut.INPUT);
        }

        public Column(String columnName, String entityFieldName, Method method) {
            this(columnName, entityFieldName, method, InOut.INPUT);
        }

        /**
         * <p>Defines how a table column maps to an Entity field.</p>
         * @param columnName: The column name the way it is defined on the SQL server side
         * @param entityFieldName: Optionally, an EntityProperty name, if there is a mapping between column and field.
         * @param method: In case a column is not being set based on a simple Entity field,
         *              specify how to get the value.
         * @param inOut: declare whether the column is input or output one.
         *             Output columns are used to get auto-generated values from the SQL server.
         */
        public Column(String columnName, String entityFieldName, Method method, InOut inOut) {
            if (columnName == null)
                throw new NullPointerException("columnName cannot be null");

            if (entityFieldName == null && method != null && (method.getModifiers() & Modifier.STATIC) == 0)
                throw new IllegalArgumentException("When no filed is provided, the method must be static.");

            this.entityFieldName = entityFieldName;
            this.columnName = columnName;
            this.method = method;
            this.inOut = inOut;
        }

        public String getEntityField() {
            return entityFieldName;
        }

        public String getColumnName() {
            return columnName;
        }

        public Method getMethod() {
            return method;
        }

        public InOut getInOut() {
            return inOut;
        }

        public String toString() {
            return "{columnName:" + String.valueOf(columnName) + ", " +
                    "entityField:" + String.valueOf(entityFieldName) + ", " +
                    "method:" + String.valueOf(method) + ", " +
                    "inOut:" + String.valueOf(inOut) + "}";
        }
    }
}

