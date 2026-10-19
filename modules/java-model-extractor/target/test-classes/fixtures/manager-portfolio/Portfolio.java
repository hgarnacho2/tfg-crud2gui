package fixtures.portfolio;

import java.util.ArrayList;
import java.util.List;

class Portfolio {
    private final List<Project> projects = new ArrayList<>();
    private Team team;

    void addProject(Project project) { projects.add(project); }
}

class Team {
    private Portfolio portfolio;
}

class Project {
    private final List<Task> tasks = new ArrayList<>();
    void addTask(Task task) { tasks.add(task); }
}

class Task { }