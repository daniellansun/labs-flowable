package me.sunlan.labs.flowable.service;

import org.flowable.engine.RepositoryService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
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
        long count = repositoryService.createProcessDefinitionQuery().processDefinitionKey("helloworldProcess").count();
        assertEquals(1, count);

        Map<String, Object> variables = new HashMap<>();
        variables.put("hello", "world");

        // 启动流程
        ProcessInstance processInstance =
                flowableService.startProcessInstanceByKey("helloworldProcess", variables, "daniel");

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
        assertEquals("daniel", flowableService.findVariable(helloworldTask.getId(), "applyUserId"));
        assertEquals("world", flowableService.findVariable(helloworldTask.getId(), "hello"));

        // 完成问好任务
        flowableService.completeTask(helloworldTask.getId());

        // 流程应已结束
        assertTrue(flowableService.isProcessEnded(processInstance.getProcessInstanceId()));
    }
}
