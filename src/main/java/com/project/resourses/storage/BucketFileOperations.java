package com.project.resourses.storage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.project.resourses.operations.Block;

public class BucketFileOperations {
	private AmazonS3 s3Client;
	private String bucketName;

	public AmazonS3 getS3Client() {
		return s3Client;
	}

	public BucketFileOperations(AmazonS3 s3Client, String bucketName) {
		this.s3Client = s3Client;
		this.bucketName = bucketName;
	}

	public void setS3Client(AmazonS3 s3Client) {
		this.s3Client = s3Client;
	}

	public String getBucketName() {
		return bucketName;
	}

	public void setBucketName(String bucketName) {
		this.bucketName = bucketName;
	}

	public boolean createFile(String fileName) {
		try {
			this.s3Client.putObject(this.bucketName, fileName, "");
			return true;
		} catch (AmazonServiceException ase) {
			System.out.println(ase.getMessage());
		}
		return false;
	}

	public boolean uploadFile(String fileName, String path) {
		File file = new File(path);
		try {
			this.s3Client.putObject(this.bucketName, fileName, file);
			return true;
		} catch (AmazonServiceException ase) {
			System.out.println(ase.getMessage());
		}
		return false;
	}

	public void readFile(String fileName) {

		S3Object object = this.s3Client.getObject(this.bucketName, fileName);

		DocumentBuilderFactory dbFactory = null;
		DocumentBuilder dBuilder = null;
		try (InputStream file = object.getObjectContent();) {
			dbFactory = DocumentBuilderFactory.newInstance();
			dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(file);
			doc.getDocumentElement().normalize();
			System.out.println("Root element :" + doc.getDocumentElement().getNodeName());
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void addBlockToS3(Block block, String fileName) {

		S3Object object = this.s3Client.getObject(this.bucketName, fileName);

		DocumentBuilderFactory dbFactory = null;
		DocumentBuilder dBuilder = null;
		try (InputStream file = object.getObjectContent();) {
			dbFactory = DocumentBuilderFactory.newInstance();
			dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(file);

			NodeList nodeList = doc.getElementsByTagName("transactions");

			Element transactions = (Element) nodeList.item(0);
			NodeList blocks = transactions.getElementsByTagName("block");
			int blockLen = blocks.getLength();
			block.setPrev("0");
			block.setId(blockLen);

			if (blockLen > 0) {
				blockVerificationAndUpdate();
				Element lastBlock = (Element) blocks.item(blockLen - 1);
				NodeList blockNodes = lastBlock.getChildNodes();
				Node prevHash = blockNodes.item(blockNodes.getLength() - 1);
				System.out.println(prevHash.getTextContent());
				block.setPrev(prevHash.getTextContent());
			}
			System.out.println("Adding block");

			Element blockTag = this.addBlock(block, doc);

			transactions.appendChild(blockTag);

			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new java.io.StringWriter());
			transformer.transform(source, result);
			String modifiedXml = result.getWriter().toString();

			// upload the modified XML file to S3
			byte[] content = modifiedXml.getBytes(StandardCharsets.UTF_8);
			InputStream stream = new ByteArrayInputStream(content);
			ObjectMetadata metadata = new ObjectMetadata();
			metadata.setContentLength(content.length);
			List<String> filesInBucket = this.listObjects();
			for (String fileInBucket : filesInBucket) {
//			s3Client.putObject(this.bucketName, fileName, stream, metadata);
				s3Client.putObject(this.bucketName, fileInBucket, stream, metadata);
				stream.reset();
			}
			stream.close();
			System.out.println("Block added");

		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private Element addBlock(Block block, Document doc) {
		Element blockTag = doc.createElement("block");
		Element timeStamp = doc.createElement("timestamp");
		timeStamp.setTextContent(String.valueOf(block.getTimestamp()));
		Element id = doc.createElement("id");
		id.setTextContent(String.valueOf(block.getId()));
		Element prevHash = doc.createElement("previous-hash");
		prevHash.setTextContent(String.valueOf(block.getPrev()));
		Element curHash = doc.createElement("current-hash");
		curHash.setTextContent(String.valueOf(block.getHash()));
		blockTag.appendChild(timeStamp);
		blockTag.appendChild(id);
		blockTag.appendChild(prevHash);
		blockTag.appendChild(curHash);

		return blockTag;
	}

	private List<String> listObjects() {
		ObjectListing objectListing = this.s3Client.listObjects(this.bucketName, "devices/");
		List<S3ObjectSummary> objectSummaries = objectListing.getObjectSummaries();
		List<String> fileNames = new ArrayList<String>();
		for (S3ObjectSummary objectSummary : objectSummaries) {
			String key = objectSummary.getKey();
			fileNames.add(key);
		}
		fileNames.remove(0);
		return fileNames;
	}

	public void blockVerificationAndUpdate() {
		List<String> fileNames = this.listObjects();
		Map<String, String> prevHashList = new HashMap<>();
		Map<String, String> curHashList = new HashMap<>();
		System.out.println("Verifing the blocks");
		for (String file : fileNames) {
			Element lastBlock = this.getLastBlock(file);
			NodeList blockNodes = lastBlock.getChildNodes();
			Node prevHash = blockNodes.item(blockNodes.getLength() - 2);
			Node curHash = blockNodes.item(blockNodes.getLength() - 1);
			prevHashList.put(file, prevHash.getTextContent());
			curHashList.put(file, curHash.getTextContent());
		}
		if (BucketFileOperations.allKeysHaveSameValue(prevHashList, prevHashList.get(fileNames.get(0)))
				&& BucketFileOperations.allKeysHaveSameValue(curHashList, curHashList.get(fileNames.get(0)))) {
			System.out.println("Verification success all chain have same blocks");
		} else {
			String filesCurrpted = BucketFileOperations.getCurrupedKeys(prevHashList);
			S3Object object = this.s3Client.getObject(this.bucketName, filesCurrpted);
			System.out.println(filesCurrpted+" attemdt to tampred");
			System.out.println("Resolving blocks in file.......");
			DocumentBuilderFactory dbFactory = null;
			DocumentBuilder dBuilder = null;
			try (InputStream file = object.getObjectContent();) {
				dbFactory = DocumentBuilderFactory.newInstance();
				dBuilder = dbFactory.newDocumentBuilder();
				Document doc = dBuilder.parse(file);
				TransformerFactory transformerFactory = TransformerFactory.newInstance();
				Transformer transformer = transformerFactory.newTransformer();
				DOMSource source = new DOMSource(doc);
				StreamResult result = new StreamResult(new java.io.StringWriter());
				transformer.transform(source, result);
				String modifiedXml = result.getWriter().toString();

				byte[] content = modifiedXml.getBytes(StandardCharsets.UTF_8);
				InputStream stream = new ByteArrayInputStream(content);
				ObjectMetadata metadata = new ObjectMetadata();
				metadata.setContentLength(content.length);
				List<String> filesInBucket = this.listObjects();
				for (String fileInBucket : filesInBucket) {
					if (fileInBucket != filesCurrpted)
						s3Client.putObject(this.bucketName, fileInBucket, stream, metadata);
					stream.reset();
				}
				stream.close();
				System.out.println("Resolved");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ParserConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SAXException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (TransformerConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (TransformerException s) {
				// TODO Auto-generated catch block
				s.printStackTrace();
			}
		}
	}

	private static boolean allKeysHaveSameValue(Map<?, ?> map, Object value) {
		for (Map.Entry<?, ?> entry : map.entrySet()) {
			if (!Objects.equals(entry.getValue(), value)) {
				return false;
			}
		}
		return true;
	}

	private Element getLastBlock(String fileName) throws NullPointerException {
		S3Object object = this.s3Client.getObject(this.bucketName, fileName);

		DocumentBuilderFactory dbFactory = null;
		DocumentBuilder dBuilder = null;
		Element lastBlock = null;
		try (InputStream file = object.getObjectContent();) {
			dbFactory = DocumentBuilderFactory.newInstance();
			dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(file);

			NodeList nodeList = doc.getElementsByTagName("transactions");

			Element transactions = (Element) nodeList.item(0);
			NodeList blocks = transactions.getElementsByTagName("block");
			int blockLen = blocks.getLength();

			lastBlock = (Element) blocks.item(blockLen - 1);
			return lastBlock;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return lastBlock;
	}

	public static String getCurrupedKeys(Map<String, String> map) {
		String key = null;
		Map<String, Integer> countMap = new HashMap<>();

		// loop through each entry in the map and count the occurrences of each value
		for (Map.Entry<String, String> entry : map.entrySet()) {
			String value = entry.getValue();
			if (countMap.containsKey(value)) {
				countMap.put(value, countMap.get(value) + 1);
			} else {
				countMap.put(value, 1);
			}
		}

		// loop through the countMap and return the key of the value that occurs only
		// once
		for (Map.Entry<String, Integer> entry : countMap.entrySet()) {
			if (entry.getValue() == 1) {
				key = getKeyFromValue(map, entry.getKey());
			}
		}

		return key;
	}

	// helper function to retrieve the key given a value in a HashMap
	public static String getKeyFromValue(Map<String, String> map, String value) {
		for (String key : map.keySet()) {
			if (map.get(key).equals(value)) {
				return key;
			}
		}
		return null;
	}

}
