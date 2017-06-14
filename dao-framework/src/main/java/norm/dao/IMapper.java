package norm.dao;

import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.Map;

public interface IMapper<MT> extends RowMapper<MT> {
    Map<String, Integer> getResultMap();
    List<MT> getResult();
    MT getSingleResult();
}
