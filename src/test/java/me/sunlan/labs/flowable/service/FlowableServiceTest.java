package me.sunlan.labs.flowable.service;

import org.flowable.engine.RepositoryService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.variable.api.history.HistoricVariableInstance;
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

    @Test
    public void testHelloWorld() {
        RepositoryService repositoryService = flowableService.getRepositoryService();
//        Deployment deployment = repositoryService.createDeployment().addClasspathResource("processes/VacationRequest.bpmn20.xml").deploy();
        final String helloworldProcessDefinitionKey = "helloworldProcess";
        long count = repositoryService.createProcessDefinitionQuery().processDefinitionKey(helloworldProcessDefinitionKey).count();
        assertEquals(1, count);

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
        assertEquals(2, flowableService.findVariablesByTaskId(helloworldTask.getId()).size());
        assertEquals(initiator, flowableService.findVariable(helloworldTask.getId(), "applyUserId"));
        assertEquals("world", flowableService.findVariable(helloworldTask.getId(), "hello"));

        // 完成问好任务
        flowableService.completeTask(helloworldTask.getId());

        // 流程应已结束
        assertTrue(flowableService.isProcessEnded(processInstance.getProcessInstanceId()));

        // 根据流程发起人，获取历史流程列表
        List<HistoricProcessInstance> historicProcessInstances = flowableService.findHistoricProcessInstanceByUserId(initiator);
        boolean helloworldHistoricProcessExists = historicProcessInstances.stream().anyMatch(e -> helloworldProcessDefinitionKey.equals(e.getProcessDefinitionKey()));
        assertTrue(helloworldHistoricProcessExists);

        // 历史变量列表
        List<HistoricVariableInstance> historicVariableInstances = flowableService.findHistoricVariableInstanceByProcessInstanceId(processInstance.getProcessInstanceId());
        assertTrue(!historicVariableInstances.isEmpty());
        assertEquals(initiator, flowableService.findHistoricVariableInstance(processInstance.getProcessInstanceId(), "applyUserId").get().getValue());
        assertEquals("world", flowableService.findHistoricVariableInstance(processInstance.getProcessInstanceId(), "hello").get().getValue());
    }
}
