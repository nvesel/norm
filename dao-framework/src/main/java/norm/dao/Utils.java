package norm.dao;

import norm.dao.annotations.EntityIdentifier;
import norm.dao.annotations.ForeignFactory;
import norm.dao.exceptions.DaoRuntimeException;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    private static final Logger log = Logger.getLogger(Utils.class);
    private static final Pattern PATTERN_PROP_NAME = Pattern.compile("\\$\\{(.+?)\\}");

    public static Properties loadXmlProperties(String propFile) throws IOException {
        Properties props = new Properties();

        InputStream in = Utils.class.getClassLoader().getResourceAsStream(propFile);
        props.loadFromXML(in);
        in.close();

        //replace all foreign properties references
        int found = 1;
        while(found > 0) {
            found = 0;
            for (Object propKey : props.keySet()) {
                String prop = props.getProperty((String)propKey);

                Matcher match = PATTERN_PROP_NAME.matcher(prop);

                if (match.find()) {
                    String foreignProp = match.group(1);

                    if (props.getProperty(foreignProp) == null)
                        throw new DaoRuntimeException("Unknown property "+foreignProp);

                    String replaceWith = Matcher.quoteReplacement(props.getProperty(foreignProp));
                    props.setProperty((String)propKey, prop.replaceAll("\\$\\{"+foreignProp+"\\}",replaceWith));
                    found++;
                }
            }
        }

        return props;
    }

    public static Map<String, SelectQuery> getSelectQueriesFromProperties(Properties sqlStatements) {
        Map<String, SelectQuery> selectQueries = new HashMap<>();
        for (String queryName : sqlStatements.stringPropertyNames())  {
            SelectQuery selectQuery = new SelectQuery(queryName, sqlStatements.getProperty(queryName));
            if (!selectQuery.getColumnEntityFieldMap().isEmpty())
                selectQueries.put(queryName, selectQuery);
        }
        return selectQueries;
    }

    public static Map<String, EntityProperty> getEntityFields(Class clazz) throws ClassNotFoundException {
        Map<String, EntityProperty> entityFields = new HashMap<>();
        for (Field field : clazz.getFields()) {

            boolean isArray = field.getType().isInstance(new ArrayList<>());
            Class arrayItemType = null;

            try {
                if (isArray)
                    arrayItemType = Class.forName(((ParameterizedType)field
                            .getGenericType()).getActualTypeArguments()[0].getTypeName());
            } catch (ClassNotFoundException e) {
                log.error("Could not determine array item type while parsing class:"+clazz+" field:"+field.getName());
                throw e;
            }

            EntityProperty entityProperty = new EntityProperty(
                    field.getName(),
                    field.getType(),
                    field.isAnnotationPresent(EntityIdentifier.class),
                    ((field.isAnnotationPresent(EntityIdentifier.class))?
                            field.getAnnotation(EntityIdentifier.class).position():null),
                    isArray,
                    arrayItemType,
                    ((field.isAnnotationPresent(ForeignFactory.class))?
                            field.getAnnotation(ForeignFactory.class).value():null),
                    (field.isAnnotationPresent(ForeignFactory.class)
                            && field.getAnnotation(ForeignFactory.class).immutable())
            );
            entityFields.put(field.getName(), entityProperty);
        }
        return entityFields;
    }

    public static java.util.Date getCurrentTime() {
        return new java.util.Date();
    }

    public static <T> T[] toArray(List<T> list, Class<T> entityClass) {
        @SuppressWarnings("unchecked")
        T[] a = (T[]) java.lang.reflect.Array.newInstance(entityClass, list.size());
        return list.toArray(a);
    }
}

