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
 *  2. createCollectMoneyWorkerTest()
 *  3. createFetchItemsWorkerTest()
 *  4. createShipWorkerTest()
 *  5. createRollbackWorkerTest()
 *  6. createInstance1Test()
 *  再次执行6，同时执行7. createPayReceivedMessageTest()，process order被取消，进入rollback
 */
@SpringBootTest
class ZeebeDemoSubProcessTests extends ZeebeDemoBaseTests{

	/**
	 * 部署bpmn模型到borker
	 */
	@Test
	public void deployOrderProcessParallelTest() {
		DeploymentEvent deployment = client.newDeployCommand()
				.addResourceFromClasspath("processes/sub-process.bpmn").send().join(3, TimeUnit.SECONDS);
		String bpmnProcessId = deployment.getProcesses().get(0).getBpmnProcessId();
		System.out.println(bpmnProcessId);
		assert bpmnProcessId.equals("sub-process");
	}


	CountDownLatch countDownLatch = new CountDownLatch(1);

	@Test
	public void createCollectMoneyWorkerTest() throws InterruptedException {
		client.newWorker().jobType("collect_money").handler((jobClient, activatedJob) -> {
			Map<String, Object> params = activatedJob.getVariablesAsMap();
			System.out.println("params: " + params);
			params.put("collect_money", true);
			Thread.sleep(3000);
			jobClient.newCompleteCommand(activatedJob.getKey()).variables(params).send().join();
			System.out.println("collect_money handler job: " + activatedJob.getKey());
		}).open();

		System.out.println("wait collect_money job");
		countDownLatch.await();
	}

	@Test
	public void createFetchItemsWorkerTest() throws InterruptedException {
		client.newWorker().jobType("fetch_items").handler((jobClient, activatedJob) -> {
			Map<String, Object> params = activatedJob.getVariablesAsMap();
			System.out.println("params: " + params);
			params.put("fetch_items", true);
			Thread.sleep(3000);
			jobClient.newCompleteCommand(activatedJob.getKey()).variables(params).send().join();
			System.out.println("fetch_items handler job: " + activatedJob.getKey());
		}).open();

		System.out.println("wait fetch_items job");
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
	public void createRollbackWorkerTest() throws InterruptedException {
		client.newWorker().jobType("rollback").handler((jobClient, activatedJob) -> {
			Map<String, Object> params = activatedJob.getVariablesAsMap();
			System.out.println("params: " + params);
			params.put("rollback", true);
			jobClient.newCompleteCommand(activatedJob.getKey()).variables(params).send().join();
			System.out.println("rollback handler job: " + activatedJob.getKey());
		}).open();

		System.out.println("wait rollback job");
		countDownLatch.await();
	}

	@Test
	public void createInstance1Test() {
		Map<String, Object> params = Maps.newHashMap();
		params.put("orderId", "123456");
		ProcessInstanceEvent workflowInstance = client.newCreateInstanceCommand().bpmnProcessId("sub-process").latestVersion().variables(params)
				.send().join();
		System.out.println("workflowInstanceKey: " + workflowInstance.getProcessInstanceKey());
	}

	@Test
	public void createPayReceivedMessageTest(){
		client.newPublishMessageCommand().messageName("order_cancel").correlationKey("123456").send().join();
	}

}
