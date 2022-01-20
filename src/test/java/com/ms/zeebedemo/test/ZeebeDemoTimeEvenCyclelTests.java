package com.ms.zeebedemo.test;

import io.zeebe.client.api.response.DeploymentEvent;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 依次运行：
 *  1. createCheckDataWorkerTest()
 *  2. deployTimeEvenCycleTest()
 *  每隔30秒，工作人员会收到一次任务
 */
@SpringBootTest
class ZeebeDemoTimeEvenCyclelTests extends ZeebeDemoBaseTests{

	/**
	 * 部署bpmn模型到borker
	 */
	@Test
	public void deployTimeEvenCycleTest() {
		DeploymentEvent deployment = client.newDeployCommand()
				.addResourceFromClasspath("time-even-cycle.bpmn").send().join(3, TimeUnit.SECONDS);
		String bpmnProcessId = deployment.getWorkflows().get(0).getBpmnProcessId();
		System.out.println(bpmnProcessId);
		assert bpmnProcessId.equals("time-even-cycle");
	}


	CountDownLatch countDownLatch = new CountDownLatch(1);

	@Test
	public void createCheckDataWorkerTest() throws InterruptedException {
		client.newWorker().jobType("check_data").handler((jobClient, activatedJob) -> {
			Map<String, Object> params = activatedJob.getVariablesAsMap();
			System.out.println("params: " + params);
			params.put("check_data", true);
			jobClient.newCompleteCommand(activatedJob.getKey()).variables(params).send().join();
			System.out.println("check_data handler job: " + activatedJob.getKey());
		}).open();

		System.out.println("wait check_data job");
		countDownLatch.await();
	}

}
