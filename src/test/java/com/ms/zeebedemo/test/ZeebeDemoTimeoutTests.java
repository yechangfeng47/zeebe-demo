package com.ms.zeebedemo.test;

import com.google.common.collect.Maps;
import io.zeebe.client.api.response.DeploymentEvent;
import io.zeebe.client.api.response.WorkflowInstanceEvent;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 依次运行：
 *  1. deployTimeoutTest()
 *  2. createParOrderWorkerTest()
 *  3. createSuccessWorkerTest()
 *  4. createTimeoutHandlerWorkerTest()
 *  多次调用createInstanceTest，观察par_order的输出，睡眠超出3秒的任务会被取消，进入timeout_handler
 */
@SpringBootTest
class ZeebeDemoTimeoutTests extends ZeebeDemoBaseTests{

	/**
	 * 部署bpmn模型到borker
	 */
	@Test
	public void deployTimeoutTest() {
		DeploymentEvent deployment = client.newDeployCommand()
				.addResourceFromClasspath("timeout.bpmn").send().join(3, TimeUnit.SECONDS);
		String bpmnProcessId = deployment.getWorkflows().get(0).getBpmnProcessId();
		System.out.println(bpmnProcessId);
		assert bpmnProcessId.equals("timeout");
	}


	CountDownLatch countDownLatch = new CountDownLatch(1);
	Random random = new Random();
	@Test
	public void createParOrderWorkerTest() throws InterruptedException {
		client.newWorker().jobType("par_order").handler((jobClient, activatedJob) -> {
			int i = random.nextInt(8);
			System.out.println("sleep: " + i);
			Thread.sleep(i * 1000);
			Map<String, Object> params = activatedJob.getVariablesAsMap();
			System.out.println("params: " + params);
			params.put("par_order", true);
			try{
				jobClient.newCompleteCommand(activatedJob.getKey()).variables(params).send().join();
			}catch (Exception e) {
				e.printStackTrace();
				System.out.println("rollback");
			}
			System.out.println("par_order handler job: " + activatedJob.getKey());
		}).open();

		System.out.println("wait par_order job");
		countDownLatch.await();
	}
	@Test
	public void createParOrder2WorkerTest() throws InterruptedException {
		client.newWorker().jobType("par_order").handler((jobClient, activatedJob) -> {
			int i = random.nextInt(8);
			System.out.println("sleep: " + i);
//			Thread.sleep(i * 1000);
			Map<String, Object> params = activatedJob.getVariablesAsMap();
			System.out.println("params: " + params);
			params.put("par_order", true);
			try{
				jobClient.newCompleteCommand(activatedJob.getKey()).variables(params).send().join();
			}catch (Exception e) {
				e.printStackTrace();
				System.out.println("rollback");
			}
			System.out.println("par_order handler job: " + activatedJob.getKey());
		}).open();

		System.out.println("wait par_order job");
		countDownLatch.await();
	}

	@Test
	public void createSuccessWorkerTest() throws InterruptedException {
		client.newWorker().jobType("success").handler((jobClient, activatedJob) -> {
			Map<String, Object> params = activatedJob.getVariablesAsMap();
			System.out.println("params: " + params);
			params.put("success", true);
			jobClient.newCompleteCommand(activatedJob.getKey()).variables(params).send().join();
			System.out.println("success handler job: " + activatedJob.getKey());
		}).open();

		System.out.println("wait success job");
		countDownLatch.await();
	}

	@Test
	public void createTimeoutHandlerWorkerTest() throws InterruptedException {
		client.newWorker().jobType("timeout_handler").handler((jobClient, activatedJob) -> {
			Map<String, Object> params = activatedJob.getVariablesAsMap();
			System.out.println("params: " + params);
			params.put("timeout_handler", true);
			jobClient.newCompleteCommand(activatedJob.getKey()).variables(params).send().join();
			System.out.println("timeout_handler handler job: " + activatedJob.getKey());
		}).open();

		System.out.println("wait timeout_handler job");
		countDownLatch.await();
	}

	@Test
	public void createInstanceTest() {
		Map<String, Object> params = Maps.newHashMap();
		params.put("orderId", "123456");
		WorkflowInstanceEvent workflowInstance = client.newCreateInstanceCommand().bpmnProcessId("timeout").latestVersion().variables(params)
				.send().join();
		System.out.println("workflowInstanceKey: " + workflowInstance.getWorkflowInstanceKey());
	}

}
