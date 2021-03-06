package com.example.demo.entity;

import javax.persistence.*;

@Entity
@Table(name="transfer_transaction", indexes = @Index(columnList = "trans_id", unique = true, name = "transfer_index"))
public class P2PTransaction {
	@Id
	@Column(name="trans_id")
	private String id;

	@Column(name="sender_id")
	private String senderId;
	
	@Column(name="receiver_id")
	private String receiverId;
	
	@Column(name="timestamp")
	private Long timestamp;
	
	@Column(name="amount")
	private Long amount;
	
	
	@Column(name="description")
	private String description;

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}

	public String getSenderId() {
		return senderId;
	}
	public void setSenderId(String senderId) {
		this.senderId = senderId;
	}

	public String getReceiverId() {
		return receiverId;
	}
	public void setReceiverId(String receiverId) {
		this.receiverId = receiverId;
	}

	public Long getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
	}

	public Long getAmount() {
		return amount;
	}
	public void setAmount(Long amount) {
		this.amount = amount;
	}

	
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
}
