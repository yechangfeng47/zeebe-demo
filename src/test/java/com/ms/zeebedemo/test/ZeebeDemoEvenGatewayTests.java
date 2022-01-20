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
 *  1. deployEvenGatewayTest()
 *  2. createPayOrderWorkerTest()
 *  3. createShipWorkerTest()
 *  4. createCancelOrderWorkerTest()
 *  5. createInstanceTest()
 *  6. createPayReceivedMessageTest()
 *  可以在ship中看到任务执行了
 *  再次执行第5步，不执行第6步，等待30s，可以在cancel_order看到任务执行了
 *
 */
@SpringBootTest
class ZeebeDemoEvenGatewayTests extends ZeebeDemoBaseTests{

	/**
	 * 部署bpmn模型到borker
	 */
	@Test
	public void deployEvenGatewayTest() {
		DeploymentEvent deployment = client.newDeployCommand()
				.addResourceFromClasspath("even-gateway.bpmn").send().join(3, TimeUnit.SECONDS);
		String bpmnProcessId = deployment.getWorkflows().get(0).getBpmnProcessId();
		System.out.println(bpmnProcessId);
		assert bpmnProcessId.equals("even-gateway");
	}


	CountDownLatch countDownLatch = new CountDownLatch(1);
	@Test
	public void createPayOrderWorkerTest() throws InterruptedException {
		client.newWorker().jobType("pay_order").handler((jobClient, activatedJob) -> {
			Map<String, Object> params = activatedJob.getVariablesAsMap();
			System.out.println("params: " + params);
			params.put("pay_order", true);
			jobClient.newCompleteCommand(activatedJob.getKey()).variables(params).send().join();
			System.out.println("pay_order handler job: " + activatedJob.getKey());
		}).open();

		System.out.println("wait pay_order job");
		countDownLatch.await();
	}

	@Test
	public void createShipWorkerTest() throws InterruptedException {
		client.newWorker().jobType("ship").handler((jobClient, activatedJob) -> {
			Map<String, Object> params = activatedJob.getVariablesAsMap();
			System.out.println("params: " + params);
			params.put("ship", true);
			jobClient.newCompleteCommand(activatedJob.getKey()).variables(params).send().join();
			System.out.println("ship handler job: " + activatedJob.getKey());
		}).open();

		System.out.println("wait ship job");
		countDownLatch.await();
	}

	@Test
	public void createCancelOrderWorkerTest() throws InterruptedException {
		client.newWorker().jobType("cancel_order").handler((jobClient, activatedJob) -> {
			Map<String, Object> params = activatedJob.getVariablesAsMap();
			System.out.println("params: " + params);
			params.put("cancel_order", true);
			jobClient.newCompleteCommand(activatedJob.getKey()).variables(params).send().join();
			System.out.println("cancel_order handler job: " + activatedJob.getKey());
		}).open();

		System.out.println("wait cancel_order job");
		countDownLatch.await();
	}

	@Test
	public void createInstanceTest() {
		Map<String, Object> params = Maps.newHashMap();
		params.put("orderId", "123456");
		WorkflowInstanceEvent workflowInstance = client.newCreateInstanceCommand().bpmnProcessId("even-gateway").latestVersion().variables(params)
				.send().join();
		System.out.println("workflowInstanceKey: " + workflowInstance.getWorkflowInstanceKey());
	}

	@Test
	public void createPayReceivedMessageTest(){
		client.newPublishMessageCommand().messageName("pay_success").correlationKey("123456").send().join();
		System.out.println("create pay_success message");
	}

}
