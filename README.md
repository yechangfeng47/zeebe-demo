找到test包下的ZeebeDemoOrderProcess2Tests

修改initZeebeClient方法中关于zeebe borker的连接信息
然后 依次运行：
1. deployOrderProcess()
2. createOrderPayWorkerTest()
3. createNoInsuranceWorkerTest()
4. createHaveInsuranceWorkerTest()
5. createInstance1Test()

此时可以在operate上查看进度，order-pay任务已经执行完成，等待pay-received类型的Message

6. createPayReceivedMessageTest()

可以修改第5步方法中的参数price为150，再次执行5、6步骤的方法，尝试运行工作流的另一条流程。
