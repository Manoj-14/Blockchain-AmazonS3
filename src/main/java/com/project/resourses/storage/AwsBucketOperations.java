package com.project.resourses.storage;

import java.util.List;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;

public class AwsBucketOperations {
	public static boolean createBucket(String bucketName, AmazonS3 s3Client) {
		try {
			s3Client.createBucket(bucketName);
			return true;
		} catch (AmazonServiceException ase) {
			System.out.println(ase.getMessage());
			return false;
		}
	}

	public static List<Bucket> listBucket(AmazonS3 s3Client) {

		List<Bucket> buckets = s3Client.listBuckets();
		return buckets;
	}
}
