package me.sunlan.labs.flowable.service;

import org.flowable.engine.RepositoryService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest
public class FlowableServiceTest {
    @Autowired
    private FlowableService flowableService;
    private String helloworldProcessDefinitionKey = "helloworldProcess";
    private String bondProcessDefinitionKey = "bondProcess";

    private static volatile boolean deployed = false;

    @Before
    public void setUp() {
        if (deployed) return;

        RepositoryService repositoryService = flowableService.getRepositoryService();
//        Deployment deployment = repositoryService.createDeployment().addClasspathResource("processes/VacationRequest.bpmn20.xml").deploy();
        assertEquals(1, repositoryService.createProcessDefinitionQuery().processDefinitionKey(helloworldProcessDefinitionKey).count());
        assertEquals(1, repositoryService.createProcessDefinitionQuery().processDefinitionKey(bondProcessDefinitionKey).count());

        deployed = true;
    }

    @Test
    public void testHelloWorldProcess() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("hello", "world");

        final String initiator = "daniel";

        // 启动流程
        ProcessInstance processInstance =
                flowableService.startProcessInstanceByKey(helloworldProcessDefinitionKey, variables, initiator);

        // 流程应未结束
        assertTrue(!flowableService.isProcessEnded(processInstance.getProcessInstanceId()));

        // 获取流程的当前任务
        List<Task> currentTasks = flowableService.findTasksByProcessInstanceId(processInstance.getProcessInstanceId());
        assertEquals(1, currentTasks.size());
        assertEquals("helloworld", currentTasks.get(0).getTaskDefinitionKey());

        // 获取问好任务
        Task helloworldTask = flowableService.findTasksByAssigneeGroup("deptLeader").get(0);
        assertEquals("helloworld", helloworldTask.getTaskDefinitionKey());

        // 获取变量
        assertEquals(2, flowableService.findVariables(helloworldTask.getId()).size());
        assertEquals(initiator, flowableService.findVariable(helloworldTask.getId(), "applyUserId"));
        assertEquals("world", flowableService.findVariable(helloworldTask.getId(), "hello"));

        // 完成问好任务
        flowableService.completeTask(helloworldTask.getId());

        // 流程应已结束
        assertTrue(flowableService.isProcessEnded(processInstance.getProcessInstanceId()));

        // 根据流程发起人，获取历史流程列表
        List<HistoricProcessInstance> historicProcessInstances = flowableService.findHistoricProcessInstancesByUserId(initiator);
        boolean helloworldHistoricProcessExists = historicProcessInstances.stream().anyMatch(e -> helloworldProcessDefinitionKey.equals(e.getProcessDefinitionKey()));
        assertTrue(helloworldHistoricProcessExists);

        // 历史变量列表
        List<HistoricVariableInstance> historicVariableInstances = flowableService.findHistoricVariableInstancesByProcessInstanceId(processInstance.getProcessInstanceId());
        assertTrue(!historicVariableInstances.isEmpty());
        assertEquals(initiator, flowableService.findHistoricVariableInstance(processInstance.getProcessInstanceId(), "applyUserId").getValue());
        assertEquals("world", flowableService.findHistoricVariableInstance(processInstance.getProcessInstanceId(), "hello").getValue());
    }

    @Test
    public void testBondProcess_allPass() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("demo", "true");

        final String initiator = "daniel.sun";

        ProcessInstance processInstance =
                flowableService.startProcessInstanceByKey(bondProcessDefinitionKey, variables, initiator);

        // 获取中台审批任务
        Task moAuditTask = flowableService.findTasks(tq -> tq.taskCandidateGroup("moUser").processInstanceId(processInstance.getProcessInstanceId())).get(0);
//        Task moAuditTask = flowableService.findTasksByProcessInstanceId(processInstance.getProcessInstanceId()).get(0);
        assertEquals("moAudit", moAuditTask.getTaskDefinitionKey());
        Map<String, Object> moAuditTaskVariables = new HashMap<>();
        moAuditTaskVariables.put("moUserPass", true);
        moAuditTaskVariables.put("moComments", "OK");
        flowableService.completeTask(moAuditTask.getId(), moAuditTaskVariables);

        // 获取后台清算任务
        Task boClearTask = flowableService.findTasks(tq -> tq.taskCandidateGroup("boUser").processInstanceId(processInstance.getProcessInstanceId())).get(0);
//        Task boClearTask = flowableService.findTasksByProcessInstanceId(processInstance.getProcessInstanceId()).get(0);
        assertEquals("boClear", boClearTask.getTaskDefinitionKey());
        // 获取中台审批时设置的变量
        assertEquals("OK", flowableService.findVariable(boClearTask.getId(), "moComments"));
        Map<String, Object> boClearTaskVariables = new HashMap<>();
        boClearTaskVariables.put("boUserPass", true);
        flowableService.completeTask(boClearTask.getId(), boClearTaskVariables);

        // 流程应已结束
        assertTrue(flowableService.isProcessEnded(processInstance.getProcessInstanceId()));
    }

    @Test
    public void testBondProcess_moNotPass_foNotReApply() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("demo", "true");

        final String initiator = "daniel.sun";

        ProcessInstance processInstance =
                flowableService.startProcessInstanceByKey(bondProcessDefinitionKey, variables, initiator);

        // 获取中台审批任务
        Task moAuditTask = flowableService.findTasks(tq -> tq.taskCandidateGroup("moUser").processInstanceId(processInstance.getProcessInstanceId())).get(0);
//        Task moAuditTask = flowableService.findTasksByProcessInstanceId(processInstance.getProcessInstanceId()).get(0);
        assertEquals("moAudit", moAuditTask.getTaskDefinitionKey());
        Map<String, Object> moAuditTaskVariables = new HashMap<>();
        moAuditTaskVariables.put("moUserPass", false);
        moAuditTaskVariables.put("moComments", "KO");
        flowableService.completeTask(moAuditTask.getId(), moAuditTaskVariables);

//        List<Task> currentTasks = flowableService.findTasksByProcessInstanceId(processInstance.getProcessInstanceId());

        // 获取前台修改任务
        Task foModifyTask = flowableService.findTasks(tq -> tq.taskAssignee(initiator).processInstanceId(processInstance.getProcessInstanceId())).get(0);
//        Task foModifyTask = flowableService.findTasksByProcessInstanceId(processInstance.getProcessInstanceId()).get(0);
        assertEquals("foModify", foModifyTask.getTaskDefinitionKey());
        // 获取中台审批时设置的变量
        assertEquals("KO", flowableService.findVariable(foModifyTask.getId(), "moComments"));
        Map<String, Object> foModifyTaskVariables = new HashMap<>();
        foModifyTaskVariables.put("reApply", false);
        flowableService.completeTask(foModifyTask.getId(), foModifyTaskVariables);

        // 流程应已结束
        assertTrue(flowableService.isProcessEnded(processInstance.getProcessInstanceId()));
    }

    @Test
    public void testBondProcess_moNotPass_foReApply() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("demo", "true");

        final String initiator = "daniel.sun";

        ProcessInstance processInstance =
                flowableService.startProcessInstanceByKey(bondProcessDefinitionKey, variables, initiator);

        // 获取中台审批任务
        Task moAuditTask = flowableService.findTasks(tq -> tq.taskCandidateGroup("moUser").processInstanceId(processInstance.getProcessInstanceId())).get(0);
//        Task moAuditTask = flowableService.findTasksByProcessInstanceId(processInstance.getProcessInstanceId()).get(0);
        assertEquals("moAudit", moAuditTask.getTaskDefinitionKey());
        Map<String, Object> moAuditTaskVariables = new HashMap<>();
        moAuditTaskVariables.put("moUserPass", false);
        moAuditTaskVariables.put("moComments", "KO");
        flowableService.completeTask(moAuditTask.getId(), moAuditTaskVariables);

//        List<Task> currentTasks = flowableService.findTasksByProcessInstanceId(processInstance.getProcessInstanceId());

        // 获取前台修改任务
        Task foModifyTask = flowableService.findTasks(tq -> tq.taskAssignee(initiator).processInstanceId(processInstance.getProcessInstanceId())).get(0);
//        Task foModifyTask = flowableService.findTasksByProcessInstanceId(processInstance.getProcessInstanceId()).get(0);
        assertEquals("foModify", foModifyTask.getTaskDefinitionKey());
        // 获取中台审批时设置的变量
        assertEquals("KO", flowableService.findVariable(foModifyTask.getId(), "moComments"));
        Map<String, Object> foModifyTaskVariables = new HashMap<>();
        foModifyTaskVariables.put("reApply", true);
        foModifyTaskVariables.put("foComments", "DONE");
        flowableService.completeTask(foModifyTask.getId(), foModifyTaskVariables);

        // 再次获取中台审批任务
        Task moAuditTask2 = flowableService.findTasks(tq -> tq.taskCandidateGroup("moUser").processInstanceId(processInstance.getProcessInstanceId())).get(0);
//        Task moAuditTask2 = flowableService.findTasksByProcessInstanceId(processInstance.getProcessInstanceId()).get(0);
        assertEquals("moAudit", moAuditTask2.getTaskDefinitionKey());
        assertEquals("DONE", flowableService.findVariable(moAuditTask2.getId(), "foComments"));
    }

    @Test
    public void testBondProcess_moPass_boNotPass() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("demo", "true");

        final String initiator = "daniel.sun";

        ProcessInstance processInstance =
                flowableService.startProcessInstanceByKey(bondProcessDefinitionKey, variables, initiator);

        // 获取中台审批任务
        Task moAuditTask = flowableService.findTasks(tq -> tq.taskCandidateGroup("moUser").processInstanceId(processInstance.getProcessInstanceId())).get(0);
//        Task moAuditTask = flowableService.findTasksByProcessInstanceId(processInstance.getProcessInstanceId()).get(0);
        assertEquals("moAudit", moAuditTask.getTaskDefinitionKey());
        Map<String, Object> moAuditTaskVariables = new HashMap<>();
        moAuditTaskVariables.put("moUserPass", true);
        moAuditTaskVariables.put("moComments", "OK");
        flowableService.completeTask(moAuditTask.getId(), moAuditTaskVariables);

        // 获取后台清算任务
        Task boClearTask = flowableService.findTasks(tq -> tq.taskCandidateGroup("boUser").processInstanceId(processInstance.getProcessInstanceId())).get(0);
//        Task boClearTask = flowableService.findTasksByProcessInstanceId(processInstance.getProcessInstanceId()).get(0);
        assertEquals("boClear", boClearTask.getTaskDefinitionKey());
        // 获取中台审批时设置的变量
        assertEquals("OK", flowableService.findVariable(boClearTask.getId(), "moComments"));
        Map<String, Object> boClearTaskVariables = new HashMap<>();
        boClearTaskVariables.put("boUserPass", false);
        boClearTaskVariables.put("boComments", "KO");
        flowableService.completeTask(boClearTask.getId(), boClearTaskVariables);

        // 获取前台修改任务
        Task foModifyTask = flowableService.findTasks(tq -> tq.taskAssignee(initiator).processInstanceId(processInstance.getProcessInstanceId())).get(0);
//        Task foModifyTask = flowableService.findTasksByProcessInstanceId(processInstance.getProcessInstanceId()).get(0);
        assertEquals("foModify", foModifyTask.getTaskDefinitionKey());
        // 获取中台审批时设置的变量
        assertEquals("OK", flowableService.findVariable(foModifyTask.getId(), "moComments"));
        // 获取后台清算时设置的变量
        assertEquals("KO", flowableService.findVariable(foModifyTask.getId(), "boComments"));
    }
}
