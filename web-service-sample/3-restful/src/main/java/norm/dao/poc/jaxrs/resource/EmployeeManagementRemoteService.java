package norm.dao.poc.jaxrs.resource;

import norm.dao.poc.jaxrs.error_handling.GeneralError;
import norm.dao.poc.jaxrs.error_handling.SuccessResponse;
import norm.dao.poc.jaxrs.error_handling.ValidationErrors;
import norm.dao.poc.service.EmployeeManagementLocalService;
import norm.dao.poc.service.dto_model.employee.EmployeeSmall;
import io.swagger.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import javax.validation.executable.ValidateOnExecution;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

@Path("empl")
@Api(value = "empl", description = "Employee Management API")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EmployeeManagementRemoteService {
    private final Logger log = LoggerFactory.getLogger(EmployeeManagementRemoteService.class);

    @Autowired
    private EmployeeManagementLocalService employeeManagementLocalService;

    @Context
    private Request request;

    @Context
    private UriInfo uriInfo;

    @GET
    @Path("{emplId}")
    @ApiOperation(value = "GetEmployeeById", notes = "Get Employee by ID")
    @NotNull
    @ApiResponses({
            @ApiResponse(code = 200, message = "HOORAY!", response = EmployeeSmall.class),
            @ApiResponse(code = 400, message = "AH YOU!", response = ValidationErrors.class),
            @ApiResponse(code = 500, message = "OH, SNAP!", response = GeneralError.class)
    })
    @ValidateOnExecution
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response getEmployeeById(
            @ApiParam(value = "Employee Id")
            @PathParam("emplId")
            @DecimalMin(value = "0", message = "{msg.err.400.emplId.negative}")
            int emplId)
    {
        log.info("getEmployeeById:"+emplId);

        EmployeeSmall employeeSmall = employeeManagementLocalService.getEmployeeSmall(emplId);

        employeeSmall.add(new Link(uriInfo.getAbsolutePathBuilder().toString()).withRel(Link.REL_SELF));

        return Response.ok().entity(employeeSmall).build();
    }

    @POST //{"name":"New Guy", "ssn":"123-123-1234", "title":"Intern", "department":{"pk": 2}}
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @ApiOperation(value = "AddEmployee", notes = "Add a New Employee")
    @ApiResponses({
            @ApiResponse(code = 200, message = "HOORAY!", response = SuccessResponse.class),
            @ApiResponse(code = 400, message = "AH YOU!", response = ValidationErrors.class),
            @ApiResponse(code = 500, message = "OH, SNAP!", response = GeneralError.class)
    })
    @ValidateOnExecution
    public Response addEmployee(@ApiParam(value = "Employee Small") @Valid EmployeeSmall employeeSmall)
    {
        if (employeeSmall == null) {
            log.info("EmployeeSmall NULL");
            return null;
        }

        log.info("Employee before insert:"+employeeSmall.toString());
        int emplId = employeeManagementLocalService.addEmployee(employeeSmall);
        log.info("Employee after insert:"+employeeSmall.toString());

        SuccessResponse successResponse = new SuccessResponse();
        successResponse.add(new Link(uriInfo.getAbsolutePathBuilder().path(EmployeeManagementRemoteService.class,"getEmployeeById").resolveTemplate("emplId", emplId).build().toString()).withRel(Link.REL_NEXT));

        return Response.ok().entity(successResponse).build();
    }

    @PUT //{"title": "Lead Engineer", "department": {"pk": 3}}
    @Path("{emplId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "PatchEmployee", notes = "Update an Employee")
    @ApiResponses({
            @ApiResponse(code = 200, message = "HOORAY!", response = EmployeeSmall.class),
            @ApiResponse(code = 400, message = "AH YOU!", response = ValidationErrors.class),
            @ApiResponse(code = 500, message = "OH, SNAP!", response = GeneralError.class)
    })
    @Transactional(readOnly=false)
    @ValidateOnExecution
    public Response patchEmployee(
            @ApiParam(value = "Employee Id") @PathParam("emplId") @DecimalMin(value = "0", message = "{msg.err.400.emplId.negative}") int emplId,
            @ApiParam(value = "Employee Small") @Valid EmployeeSmall employeeSmall)
    {
        if (employeeSmall == null) {
            log.info("EmployeeSmall NULL");
            return null;
        }

        log.info("Employee before update:"+employeeSmall.toString());
        employeeSmall = employeeManagementLocalService.updateEmployee(emplId, employeeSmall);
        log.info("Employee after update:"+employeeSmall.toString());

        employeeSmall = employeeManagementLocalService.getEmployeeSmall(emplId);
        log.info("Employee refreshed from the DB:"+employeeSmall.toString());

        employeeSmall.add(new Link(uriInfo.getAbsolutePathBuilder().toString()).withRel(Link.REL_SELF));

        return Response.ok().entity(employeeSmall).build();
    }
}
