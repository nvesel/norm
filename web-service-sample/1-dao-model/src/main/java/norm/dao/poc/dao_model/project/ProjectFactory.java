package norm.dao.poc.dao_model.project;

import norm.dao.EntityFactory;

import java.util.List;

public interface ProjectFactory extends EntityFactory<ProjectDAO> {
    List<ProjectDAO> getProjectsByEmplId(final int emplId);
    ProjectDAO getProjectById(int projId);
}
