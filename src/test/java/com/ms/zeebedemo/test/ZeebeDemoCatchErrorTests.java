package com.ms.zeebedemo.test;

import com.google.common.collect.Maps;
import io.zeebe.client.api.response.DeploymentEvent;
import io.zeebe.client.api.response.ProcessInstanceEvent;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 依次运行：
 *  1. deployCatchErrorTest()
 *  2. createPayOrderWorkerTest()
 *  3. createPaySuccessWorkerTest()
 *  4. createRollbackWorkerTest()
 *  5. createInstanceTest()
 *  多次执行第5步，会随机执行pay_success或rollback的任务
 *
 */
@SpringBootTest
class ZeebeDemoCatchErrorTests extends ZeebeDemoBaseTests{

	/**
	 * 部署bpmn模型到borker
	 */
	@Test
	public void deployCatchErrorTest() {
		DeploymentEvent deployment = client.newDeployCommand()
				.addResourceFromClasspath("processes/catch-error.bpmn").send().join(3, TimeUnit.SECONDS);
		String bpmnProcessId = deployment.getProcesses().get(0).getBpmnProcessId();
		System.out.println(bpmnProcessId);
		assert bpmnProcessId.equals("catch-error");
	}


	CountDownLatch countDownLatch = new CountDownLatch(1);
	Random random = new Random();
	@Test
	public void createPayOrderWorkerTest() throws InterruptedException {
		client.newWorker().jobType("pay_order").handler((jobClient, activatedJob) -> {
			Map<String, Object> params = activatedJob.getVariablesAsMap();
			System.out.println("params: " + params);
			params.put("pay_order", true);
			if (random.nextBoolean()) {
				jobClient.newThrowErrorCommand(activatedJob.getKey()).errorCode("pay_failed").send().join();
				System.out.println("pay_order handler job failed: " + activatedJob.getKey());
			}else{
				jobClient.newCompleteCommand(activatedJob.getKey()).variables(params).send().join();
				System.out.println("pay_order handler job: " + activatedJob.getKey());
			}
		}).open();

		System.out.println("wait pay_order job");
		countDownLatch.await();
	}

	@Test
	public void createPaySuccessWorkerTest() throws InterruptedException {
		client.newWorker().jobType("pay_success").handler((jobClient, activatedJob) -> {
			Map<String, Object> params = activatedJob.getVariablesAsMap();
			System.out.println("params: " + params);
			params.put("pay_success", true);
			jobClient.newCompleteCommand(activatedJob.getKey()).variables(params).send().join();
			System.out.println("pay_success handler job: " + activatedJob.getKey());
		}).open();

		System.out.println("wait pay success job");
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
	public void createInstanceTest() {
		Map<String, Object> params = Maps.newHashMap();
		params.put("orderId", "123456");
		ProcessInstanceEvent workflowInstance = client.newCreateInstanceCommand().bpmnProcessId("catch-error").latestVersion().variables(params)
				.send().join();
		System.out.println("workflowInstanceKey: " + workflowInstance.getProcessInstanceKey());
	}

}
