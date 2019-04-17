package com.IBS.serviceA;

public class TestServiceFallback implements TestService {

	@Override
	public String greeting() {
		System.out.println("From Fallback");
		return "from Fallback";
	}

}
