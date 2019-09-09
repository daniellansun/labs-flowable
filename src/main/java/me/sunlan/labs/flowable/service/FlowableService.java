package me.sunlan.labs.flowable.service;


import org.flowable.engine.*;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional
public class FlowableService {
    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private FormService formService;

    @Autowired
    private IdentityService identityService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private ManagementService managementService;

    public ProcessInstance startProcessInstanceByKey(String processDefinitionKey, Map<String, Object> variables, String initiator) {
        identityService.setAuthenticatedUserId(initiator);
        return runtimeService.startProcessInstanceByKey(processDefinitionKey, variables);
    }

    public ProcessInstance findProcessInstanceById(String processInstanceId) {
        return runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
    }

    public boolean isProcessEnded(String processInstanceId) {
        return null == findProcessInstanceById(processInstanceId);
    }

    public List<Task> findTasksByAssignee(String assignee) {
        return taskService.createTaskQuery().taskCandidateUser(assignee).list();
    }

    public List<Task> findTasksByAssigneeGroup(String assigneeGroup) {
        return taskService.createTaskQuery().taskCandidateGroup(assigneeGroup).list();
    }

    public List<Task> findTasksByProcessInstanceId(String processInstanceId) {
        return taskService.createTaskQuery().processInstanceId(processInstanceId).list();
    }

    public void completeTask(String taskId) {
        taskService.complete(taskId);
    }

    public void completeTask(String taskId, Map<String, Object> taskVariables) {
        taskService.complete(taskId, taskVariables);
    }

    public Map<String, Object> findVariablesByTaskId(String taskId) {
        return taskService.getVariables(taskId);
    }

    public Object findVariable(String taskId, String variableName) {
        return taskService.getVariable(taskId, variableName);
    }

    public RepositoryService getRepositoryService() {
        return repositoryService;
    }

    public RuntimeService getRuntimeService() {
        return runtimeService;
    }

    public FormService getFormService() {
        return formService;
    }

    public IdentityService getIdentityService() {
        return identityService;
    }

    public TaskService getTaskService() {
        return taskService;
    }

    public HistoryService getHistoryService() {
        return historyService;
    }

    public ManagementService getManagementService() {
        return managementService;
    }
}