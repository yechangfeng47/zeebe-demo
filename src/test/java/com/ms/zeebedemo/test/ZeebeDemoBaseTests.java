package com.ms.zeebedemo.test;

import io.zeebe.client.ZeebeClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

class ZeebeDemoBaseTests {
	ZeebeClient client;

	@BeforeEach
	public void initZeebeClient(){
		client = ZeebeClient.newClientBuilder()
//				.brokerContactPoint("47.113.206.200:26500")
				.gatewayAddress("47.113.206.200:26500")
				.usePlaintext()
				.build();
		System.out.println("connected...");
	}

	@AfterEach
	public void closeZeebeClient() {
		client.close();
		System.out.println("closed...");
	}

}
