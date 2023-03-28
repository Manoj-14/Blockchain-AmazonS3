package com.project.resourses.storage;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;

public class AwsAccountAuthentication {
	private static String accessKey = "AKIA5YSYRHPNUSIZ2ZNE";
	private static String secretKey = "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";

	public static AmazonS3Client getAccess() {
		BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKey, secretKey);
		AmazonS3Client s3client = new AmazonS3Client(awsCreds);
		return s3client;
	}
}
