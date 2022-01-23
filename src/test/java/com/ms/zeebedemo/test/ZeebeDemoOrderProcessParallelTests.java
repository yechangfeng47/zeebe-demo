package com.ms.zeebedemo.test;

import com.google.common.collect.Maps;
import io.zeebe.client.api.response.DeploymentEvent;
import io.zeebe.client.api.response.ProcessInstanceEvent;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 依次运行：
 *  1. deployOrderProcessParallelTest()
 *  2. createOrderPayWorkerTest()
 *  3. createShipWorkerTest()
 *  4. createSmsWorkerTest()
 *  5. createInstance1Test()
 *  此时可在operate上查看进度，order-pay任务已经执行完成，阻塞在already paid的received task处，等待pay-received类型的Message
 *  6. createPayReceivedMessageTest()
 */
@SpringBootTest
class ZeebeDemoOrderProcessParallelTests extends ZeebeDemoBaseTests{

	/**
	 * 部署bpmn模型到borker
	 */
	@Test
	public void deployOrderProcessParallelTest() {
		DeploymentEvent deployment = client.newDeployCommand()
				.addResourceFromClasspath("processes/order-process-parallel.bpmn").send().join(3, TimeUnit.SECONDS);
		String bpmnProcessId = deployment.getProcesses().get(0).getBpmnProcessId();
		System.out.println(bpmnProcessId);
		assert bpmnProcessId.equals("order-process-parallel");
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
	public void createSmsWorkerTest() throws InterruptedException {
		client.newWorker().jobType("sms").handler((jobClient, activatedJob) -> {
			Map<String, Object> params = activatedJob.getVariablesAsMap();
			System.out.println("params: " + params);
			params.put("sms", true);
			jobClient.newCompleteCommand(activatedJob.getKey()).variables(params).send().join();
			System.out.println("sms handler job: " + activatedJob.getKey());
		}).open();

		System.out.println("wait sms job");
		countDownLatch.await();
	}

	@Test
	public void createInstance1Test() {
		Map<String, Object> params = Maps.newHashMap();
		params.put("orderId", "123456");
		params.put("price", 50);
		ProcessInstanceEvent workflowInstance = client.newCreateInstanceCommand().bpmnProcessId("order-process-parallel").latestVersion().variables(params)
				.send().join();
		System.out.println("workflowInstanceKey: " + workflowInstance.getProcessInstanceKey());
	}

	@Test
	public void createPayReceivedMessageTest(){
		client.newPublishMessageCommand().messageName("pay-received").correlationKey("123456").send().join();
	}

}
