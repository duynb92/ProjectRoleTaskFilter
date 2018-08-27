package com.kietnh.jira.jira.jql;

import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.UserHistoryItem;
import com.atlassian.jira.user.UserProjectHistoryManager;
import com.atlassian.jira.security.roles.ProjectRoleManager;
import com.atlassian.jira.security.roles.ProjectRole;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.issue.Issue;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.atlassian.jira.JiraDataType;
import com.atlassian.jira.JiraDataTypes;
import com.atlassian.jira.jql.operand.QueryLiteral;
import com.atlassian.jira.jql.query.QueryCreationContext;
import com.atlassian.jira.plugin.jql.function.AbstractJqlFunction;
import com.atlassian.jira.util.MessageSet;
import com.atlassian.query.clause.TerminalClause;
import com.atlassian.query.operand.FunctionOperand;

import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Arrays;
import java.util.stream.Collectors;

@Scanned
public class ProjectRoleTaskFilterFunction extends AbstractJqlFunction {
    private static final Logger log = LoggerFactory.getLogger(ProjectRoleTaskFilterFunction.class);

    private HashMap<String, HashMap<String, List<String>>> projectRoleMaps;

    @ComponentImport
    private final ProjectRoleManager projectRoleManager;
    @ComponentImport
    private final ProjectManager projectManager;

    public ProjectRoleTaskFilterFunction(ProjectRoleManager projectRoleManager, ProjectManager projectManager) {
        this.projectRoleManager = projectRoleManager;
        this.projectManager = projectManager;
    }

    public MessageSet validate(ApplicationUser searcher, FunctionOperand operand, TerminalClause terminalClause) {
        return validateNumberOfArgs(operand, 1);
    }

    public List<QueryLiteral> getValues(QueryCreationContext queryCreationContext, FunctionOperand operand, TerminalClause terminalClause) {
        final List<QueryLiteral> literals = new LinkedList<>();
        final Project project = projectManager.getProjectByCurrentKey(operand.getArgs().get(0));

        ApplicationUser currentUser = queryCreationContext.getApplicationUser();
//        ArrayList<ProjectRole> userProjectRoles = new ArrayList<ProjectRole>(projectRoleManager.getProjectRoles(currentUser, project));
        List<String> userProjectRoles = projectRoleManager.getProjectRoles(currentUser, project).stream().map(x->x.getName()).collect(Collectors.toList());
        if (isProjectLead(currentUser, project)) {
            userProjectRoles.add("Project Lead");
        }
        for (final String userProjectRole : userProjectRoles) {
            for (final String projectRoleFilter : getProjectRoleFilters(userProjectRole)) {
                log.warn("ADD FILTER " + projectRoleFilter);
                literals.add(new QueryLiteral(operand, projectRoleFilter));
            }
        }

//        if (isProjectLead(currentUser, project)) {
//            for (final String projectRoleFilter : getProjectRoleFilters("Project Lead")) {
//                log.warn("ADD FILTER " + projectRoleFilter);
//                literals.add(new QueryLiteral(operand, projectRoleFilter));
//            }
//        }
        return literals;
    }

    public int getMinimumNumberOfExpectedArguments() {
        return 1;
    }

    public JiraDataType getDataType() {
        return JiraDataTypes.getFieldType("environment");
    }

    private List<String> getProjectRoleFilters(String projectRole) {
        if (projectRoleMaps == null) {
            projectRoleMaps = createProjectMaps();
        }

        if (projectRoleMaps.containsKey(projectRole)) {
            HashMap<String, List<String>> projectRoleMap = projectRoleMaps.get(projectRole);
            return getProjectRoleFilters(projectRoleMap);
        } else {
            return new LinkedList<>();
        }
    }

    private List<String> getProjectRoleFilters(HashMap<String, List<String>> projectRoleMap) {
        List<String> filters = new LinkedList<>();
        for (String status: projectRoleMap.keySet()) {
            for (String step: projectRoleMap.get(status)) {
                filters.add(String.format("'%s':'%s'", status, step));
            }
        }
        return filters;
    }

    private HashMap<String, HashMap<String, List<String>>> createProjectMaps() {
        //based on Product Launch Project_v8
        return new HashMap<String, HashMap<String, List<String>>>() {
            {
                put("Merchandiser", new HashMap<String, List<String>>() {{
                    put("Market-end Feedback", Arrays.asList("Information Gathering", "Information Analysis", "Documentation"));
                    put("Sample Purchase", Arrays.asList("Information Gathering", "Information Analysis", "Documentation"));
                    put("Competitor Sample Purchase", Arrays.asList("Information Gathering"));
                    put("Market-end Feedback vs. Factory-end Sourcing Evaluation", Arrays.asList("Information Analysis", "Documentation"));
                    put("Initiation Stage Wrap-up", Arrays.asList("Check List Verification"));
                    put("Prototype Internal Communication", Arrays.asList("Information Gathering", "Meeting Arrangement", "Actual Meeting", "Meeting Follow-up", "Documentation"));
                    put("Product Prototype Evaluation", Arrays.asList("Information Analysis", "Documentation"));
                    put("MRD", Arrays.asList("Information Gathering", "Task Processing", "Task Review", "Documentation"));
                    put("Pre-Launch Stage Wrap-up", Arrays.asList("Check List Verification"));
                    put("Pilot-Run Sample Review",Arrays.asList("Information Analysis", "Documentation"));
                    put("Initial PO Budget", Arrays.asList("Information Analysis"));
                    put("Digital Creation Stage Wrap-Up", Arrays.asList("Check List Verification"));
                }});
                put("Buyer", new HashMap<String, List<String>>() {{
                    put("Sample Purchase", Arrays.asList("Information Gathering", "Acquisition", "Documentation"));
                    put("Competitor Sample Purchase", Arrays.asList("Acquisition", "Documentation"));
                    put("Factory-end Sourcing", Arrays.asList("Information Gathering", "Information Analysis", "Documentation"));
                    put("Factory Sample Purchase", Arrays.asList("Information Gathering", "Acquisition", "Documentation"));
                    put("Market-end Feedback vs. Factory-end Sourcing Evaluation", Arrays.asList("Information Gathering"));
                    put("Initiation Stage Wrap-up", Arrays.asList("Check List Verification"));
                    put("Prototype External Communication", Arrays.asList("Information Gathering", "Meeting Arrangement", "Actual Meeting", "Meeting Follow-up", "Documentation"));
                    put("Product Prototype Evaluation", Arrays.asList("Information Gathering"));
                    put("Prototype Stage Wrap-up", Arrays.asList("Check List Verification"));
                    put("SKU-UP Initiation", Arrays.asList("Task Processing", "Task Review", "Documentation"));
                    put("Product Specs", Arrays.asList("Information Gathering", "Task Processing", "Task Review", "Documentation"));
                    put("Pre-Launch Stage Wrap-up", Arrays.asList("Check List Verification"));
                    put("PRD", Arrays.asList("Information Gathering", "Task Processing", "Task Review", "Documentation"));
                    put("Pilot-Run External Communication", Arrays.asList("Information Gathering", "Meeting Arrangement", "Actual Meeting", "Meeting Follow-up", "Documentation"));
                    put("Pilot-Run Sample Review",Arrays.asList("Information Gathering"));
                    put("QC Test Procedure", Arrays.asList("Information Gathering", "Task Processing", "Task Review", "Documentation"));
                    put("Pilot-Run Stage Wrap-up", Arrays.asList("Check List Verification"));
                    put("Initial PO Budget", Arrays.asList("Information Gathering", "Documentation"));
                    put("PO Release", Arrays.asList("Information Gathering", "Task Processing", "Task Review", "Documentation"));
                    put("Initial PO Stage Wrap-Up", Arrays.asList("Check List Verification"));
                }});
                put("Project Lead", new HashMap<String, List<String>>() {{
                    put("Market-end Feedback", Arrays.asList("Approval Review", "Closed"));
                    put("Sample Purchase", Arrays.asList("Approval Review", "Closed"));
                    put("Competitor Sample Purchase", Arrays.asList("Approval Review", "Closed"));
                    put("Factory-end Sourcing", Arrays.asList("Approval Review", "Closed"));
                    put("Factory Sample Purchase", Arrays.asList("Approval Review", "Closed"));
                    put("Market-end Feedback vs. Factory-end Sourcing Evaluation", Arrays.asList("Approval Review", "Closed"));
                    put("Initiation Stage Wrap-up", Arrays.asList("Approval Review", "Closed"));
                    put("Prototype Internal Communication", Arrays.asList("Information Gathering","Approval Review", "Closed"));
                    put("Prototype External Communication", Arrays.asList("Information Gathering","Approval Review", "Closed"));
                    put("Product Prototype Evaluation", Arrays.asList("Approval Review", "Closed"));
                    put("Prototype Stage Wrap-up", Arrays.asList("Approval Review", "Closed"));
                    put("SKU-UP Initiation", Arrays.asList("Information Gathering", "Task Processing", "Approval Review", "Documentation", "Closed"));
                    put("Product Specs", Arrays.asList("Approval Review", "Closed"));
                    put("MRD", Arrays.asList("Approval Review", "Closed"));
                    put("Label Artworks", Arrays.asList("Approval Review", "Closed"));
                    put("Product Instruction Manual", Arrays.asList("Closed"));
                    put("Product Copy", Arrays.asList("Closed"));
                    put("Picture Shots Planning", Arrays.asList("Closed"));
                    put("Product Packaging Design", Arrays.asList("Approval Review", "Closed"));
                    put("Pre-Launch Stage Wrap-up", Arrays.asList("Approval Review", "Closed"));
                    put("PRD", Arrays.asList("Approval Review", "Closed"));
                    put("Pilot-Run External Communication", Arrays.asList("Information Gathering", "Approval Review", "Closed"));
                    put("Pilot-Run Sample Review", Arrays.asList("Approval Review", "Closed"));
                    put("QC Test Procedure", Arrays.asList("Approval Review", "Closed"));
                    put("Pilot-Run Stage Wrap-up", Arrays.asList("Approval Review", "Closed"));
                    put("Initial PO Budget", Arrays.asList("Approval Review", "Closed"));
                    put("PO Release", Arrays.asList("Approval Review", "Closed"));
                    put("Initial PO Stage Wrap-Up", Arrays.asList("Approval Review", "Closed"));
                    put("Digital Creation Stage Wrap-Up", Arrays.asList("Approval Review", "Closed"));
                }});
                put("Packaging Designer", new HashMap<String, List<String>>() {{
                    put("Label Artworks", Arrays.asList("Information Gathering", "Task Processing", "Approval Review", "Documentation"));
                    put("Product Packaging Design", Arrays.asList("Information Gathering", "Task Processing", "Task Review", "Documentation"));
                }});
                put("Editor", new HashMap<String, List<String>>() {{
                    put("Product Instruction Manual", Arrays.asList("Task Processing", "Approval Review"));
                    put("Product Copy", Arrays.asList("Approval Review"));
                }});
                put("Instruction Writer", new HashMap<String, List<String>>() {{
                    put("Product Instruction Manual", Arrays.asList("Information Gathering", "Task Processing"));
                }});
                put("Graphic Designer", new HashMap<String, List<String>>() {{
                    put("Product Instruction Manual", Arrays.asList("Task Processing", "Task Review", "Documentation"));
                    put("Picture Shots Planning", Arrays.asList("Information Gathering", "Task Processing", "Task Review", "Documentation"));
                    put("Studio Shot Shooting", Arrays.asList("Approval Review"));
                    put("Additional Sketching", Arrays.asList("Approval Review"));
                    put("Outdoor Shot Shooting", Arrays.asList("Approval Review", "Closed", "Task Review"));
                    put("Image Post-Editing", Arrays.asList("Information Gathering", "Task Processing", "Approval Review", "Documentation"));
                }});
                put("Copy Writer", new HashMap<String, List<String>>() {{
                    put("Product Copy", Arrays.asList("Information Gathering", "Task Processing", "Task Review", "Documentation"));
                }});
                put("Graphic Master", new HashMap<String, List<String>>() {{
                    put("Picture Shots Planning", Arrays.asList("Approval Review"));
                    put("Studio Shot Shooting", Arrays.asList("Closed"));
                    put("Additional Sketching", Arrays.asList("Closed"));
                    put("Image Post-Editing", Arrays.asList("Approval Review", "Closed"));
                    put("Listing Feed Creation", Arrays.asList("Information Gathering"));
                }});
                put("Photographer", new HashMap<String, List<String>>() {{
                    put("Studio Shot Shooting", Arrays.asList("Information Gathering", "Task Processing", "Approval Review", "Documentation"));
                    put("Outdoor Shot Shooting", Arrays.asList("Information Gathering", "Task Processing", "Approval Review", "Documentation"));
                }});
                put("Sketching Designer", new HashMap<String, List<String>>() {{
                    put("Additional Sketching", Arrays.asList("Information Gathering", "Task Processing", "Approval Review", "Documentation"));
                }});

            }
        };
    }

    public boolean isProjectLead(ApplicationUser user, Project project) {
        return user.getName().equals(project.getProjectLead().getName());
    }
}