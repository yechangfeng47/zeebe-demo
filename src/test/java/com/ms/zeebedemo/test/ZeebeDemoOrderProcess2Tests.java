package com.ms.zeebedemo.test;

import com.google.common.collect.Maps;
import io.zeebe.client.api.response.DeploymentEvent;
import io.zeebe.client.api.response.WorkflowInstanceEvent;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 依次运行：
 *  1. deployOrderProcess()
 *  2. createOrderPayWorkerTest()
 *  3. createNoInsuranceWorkerTest()
 *  4. createHaveInsuranceWorkerTest()
 *  5. createInstance1Test()
 *  此时可在operate上查看进度，order-pay任务已经执行完成，阻塞在already paid的message even处，等待pay-received类型的Message
 *  6. createPayReceivedMessageTest()
 */
@SpringBootTest
class ZeebeDemoOrderProcess2Tests extends ZeebeDemoBaseTests{

	/**
	 * 部署bpmn模型到borker
	 */
	@Test
	public void deployOrderProcessTest() {
		DeploymentEvent deployment = client.newDeployCommand()
				.addResourceFromClasspath("order-process2.bpmn").send().join(3, TimeUnit.SECONDS);
		String bpmnProcessId = deployment.getWorkflows().get(0).getBpmnProcessId();
		System.out.println(bpmnProcessId);
		assert bpmnProcessId.equals("order-process");
	}


	CountDownLatch countDownLatch = new CountDownLatch(1);

	@Test
	public void createOrderPayWorkerTest() throws InterruptedException {
		client.newWorker().jobType("order-pay").handler((jobClient, activatedJob) -> {
			Map<String, Object> params = activatedJob.getVariablesAsMap();
			System.out.println("params: " + params);
			params.put("order-pay", true);
			jobClient.newCompleteCommand(activatedJob.getKey()).variables(params).send().join();
			System.out.println("order-pay handler job: " + activatedJob.getKey());
		}).open();

		System.out.println("wait order-pay job");
		countDownLatch.await();
	}

	@Test
	public void createNoInsuranceWorkerTest() throws InterruptedException {
		client.newWorker().jobType("no-insurance").handler((jobClient, activatedJob) -> {
			Map<String, Object> params = activatedJob.getVariablesAsMap();
			System.out.println("params: " + params);
			params.put("no-insurance", true);
			jobClient.newCompleteCommand(activatedJob.getKey()).variables(params).send().join();
			System.out.println("no-insurance handler job: " + activatedJob.getKey());
		}).open();

		System.out.println("wait no-insurance job");
		countDownLatch.await();
	}

	@Test
	public void createHaveInsuranceWorkerTest() throws InterruptedException {
		client.newWorker().jobType("have-insurance").handler((jobClient, activatedJob) -> {
			Map<String, Object> params = activatedJob.getVariablesAsMap();
			System.out.println("params: " + params);
			params.put("have-insurance", true);
			jobClient.newCompleteCommand(activatedJob.getKey()).variables(params).variables(params).send().join();
			System.out.println("have-insurance handler job: " + activatedJob.getKey());
		}).open();

		System.out.println("wait have-insurance job");
		countDownLatch.await();
	}

	@Test
	public void createInstance1Test() {
		Map<String, Object> params = Maps.newHashMap();
		params.put("orderId", "123456");
		params.put("price", 50);
		WorkflowInstanceEvent workflowInstance = client.newCreateInstanceCommand().bpmnProcessId("order-process").latestVersion().variables(params)
				.send().join();
		System.out.println("workflowInstanceKey: " + workflowInstance.getWorkflowInstanceKey());
	}

	@Test
	public void createPayReceivedMessageTest(){
		client.newPublishMessageCommand().messageName("pay-received").correlationKey("123456").send().join();
		System.out.println("create pay-received message");
	}

}
