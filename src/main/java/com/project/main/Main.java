package com.project.main;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.project.resourses.operations.Block;
import com.project.resourses.storage.AwsAccountAuthentication;
import com.project.resourses.storage.BucketFileOperations;

public class Main {
	public static void main(String[] args) {
		String bucketName = "iot-blocks";
		try {
			AmazonS3 s3Clinet = AwsAccountAuthentication.getAccess();
			BucketFileOperations bfo = new BucketFileOperations(s3Clinet, bucketName);
//			bfo.readFile("device1.xml");
			Block block = new Block();
			bfo.addBlockToS3(block, "devices/device-fan.xml");

		} catch (AmazonServiceException e) {
			System.out.println(e.getMessage());
		}
	}
}
