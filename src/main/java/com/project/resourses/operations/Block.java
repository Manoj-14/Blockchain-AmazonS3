package com.project.resourses.operations;

import java.security.MessageDigest;
import java.util.Date;


public class Block {
	private int id;
	private long timestamp;
	private String hash, prev;

	public Block() {
		this.timestamp = new Date().getTime();
//		this.id = prevBlock == null ? 1 : prevBlock.getId() + 1;
//		this.prev = prevBlock == null ? "0" : prevBlock.getHash();
		this.hash = this.applySha256(String.format("%d%d%s", this.id, this.timestamp, this.hash));
		System.out.println(this);
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public String getHash() {
		return hash;
	}

	public void setHash(String hash) {
		this.hash = hash;
	}

	public String getPrev() {
		return prev;
	}

	public void setPrev(String prev) {
		this.prev = prev;
	}

	public String applySha256(String input) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			
			byte[] hash = digest.digest(input.getBytes("UTF-8"));
			StringBuilder hexString = new StringBuilder();
			for (byte elem : hash) {
				String hex = Integer.toHexString(0xff & elem);
				if (hex.length() == 1)
					hexString.append('0');
				hexString.append(hex);
			}
			return hexString.toString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public String toString() {
		return (String.format(
				"Block:%nId: %d%nTimestamp: %d%nHash of the previous block: %n%s%nHash of the block: %n%s%n", id,
				timestamp, prev, hash));
	}

}
