package com.example.demo.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;

import com.example.demo.entity.*;
import com.example.demo.repository.PresentTransactionRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.GetP2PsRequest;
import com.example.demo.model.GetPresentRequest;
import com.example.demo.model.GetTopupsRequest;
import com.example.demo.model.ReturnPresent;
import com.example.demo.model.SendP2PRequest;
import com.example.demo.model.SendPresentRequest;
import com.example.demo.model.TopupDirectRequest;
import com.example.demo.model.TopupRequest;
import com.example.demo.repository.PresentRepository;
import com.example.demo.util.GenerationUtil;

@Service
public class WalletService {
	@Autowired
	RedisTemplate<String, String> redisTemplate;

	@PersistenceContext
	EntityManager entityManager;

	@Autowired
	PresentRepository presentRepository;

	@Autowired
	PresentTransactionRepository presentTransactionRepository;

	@Transactional(propagation= Propagation.REQUIRED)
	public void addBalance(String userId, Long amount) {
		Wallet w = entityManager.find(Wallet.class, userId);
		w.setBalance(w.getBalance()  + amount);
	}
	
	@Transactional(propagation = Propagation.REQUIRED)
	public String topup(TopupRequest request) {
		String walletId = request.getWalletId(); 
		Long amount  = request.getAmount();
		
		Wallet wallet = entityManager.find(Wallet.class, walletId, LockModeType.PESSIMISTIC_WRITE);
		if(wallet ==null) {
			//System.out.println("wallet  is  null");
			return null;
		}
		
		wallet.setBalance(wallet.getBalance()+ amount);

		TopupTransaction tsx =new TopupTransaction();
		tsx.setId(GenerationUtil.generateId("trans"));
		tsx.setAmount(amount);
		tsx.setTimestamp(System.currentTimeMillis());
		tsx.setUserId(walletId);
		
		entityManager.persist(tsx);
		return tsx.getId();
		
	}
	
	@Transactional(propagation = Propagation.REQUIRED)
	public String topupDirect(TopupDirectRequest request) {
		String userId = request.getUserId(); 
		String pin = request.getPin();
		Wallet wallet = entityManager.find(Wallet.class, userId, LockModeType.PESSIMISTIC_WRITE);
		if(wallet ==null) {
			return "Wallet is not exist";
		}
		if(!pin.equals(wallet.getHashedPin())) return "PIN is not correct";
		
		Long amount  = request.getAmount();
		wallet.setBalance(wallet.getBalance()+ amount);

		TopupTransaction tsx =new TopupTransaction();
		tsx.setAmount(amount);
		tsx.setTimestamp(System.currentTimeMillis());
		tsx.setUserId(userId);
		entityManager.persist(tsx);
		return tsx.getId();
	}
	
	
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public String sendP2P(SendP2PRequest request) throws SQLException {
		String receiverId = request.getReceiverId();
		String senderId = request.getSenderId();
		Wallet senderWallet = entityManager.find(Wallet.class, senderId, LockModeType.PESSIMISTIC_WRITE);
		Wallet receiverWallet = entityManager.find(Wallet.class, receiverId, LockModeType.PESSIMISTIC_WRITE);

		Long amountSender = senderWallet.getBalance();
		Long amount = request.getAmount();
		if(amountSender < amount) {return null;}
		senderWallet.setBalance(amountSender-amount);
		receiverWallet.setBalance(receiverWallet.getBalance()+ amount);

		Long timestamp =  System.currentTimeMillis();
		
		P2PTransaction tsx =new P2PTransaction();
		tsx.setId(GenerationUtil.generateId("trans"));
		tsx.setAmount(amount);
		tsx.setDescription(request.getMesssage());
		tsx.setReceiverId(receiverId);
		tsx.setSenderId(senderId);
		tsx.setTimestamp(timestamp);
		
		entityManager.persist(tsx);
		return tsx.getId();
	}
	
	
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public boolean sendPresent(SendPresentRequest rq) {
		
		Wallet wallet = entityManager.find(Wallet.class, rq.getUserId());
		Long amount = rq.getAmount();
		if(wallet.getBalance() >= amount) {
			wallet.setBalance(wallet.getBalance() - amount);
			return true;
		}
		return false;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public Boolean createPresent(SendPresentRequest rq) {
		String userId  = rq.getUserId();
		
		Wallet wallet = entityManager.find(Wallet.class,userId, LockModeType.PESSIMISTIC_WRITE);
		Long amount = rq.getAmount();
		if(wallet.getBalance() < amount) return false;
		wallet.setBalance(wallet.getBalance() - amount);
		Present pr = new Present(rq.getPresentId(),rq.getUserId(),rq.getAmount(),rq.getAmount(),System.currentTimeMillis(),
				rq.getSessionId(), false, rq.getEnvelope(), rq.getEqual());
		presentRepository.save(pr);
		return true;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public Long getPresent(GetPresentRequest rq) {
		String presentId = rq.getPresentId();
		System.out.println("Find present id: "+ presentId);
		Present pr = entityManager.find(Present.class, presentId,LockModeType.PESSIMISTIC_WRITE);
		if(pr==null) return -1L;
		if(pr.getExpired()) return 0L;
		if(pr.getEnvelope()==0) return -2L;
		Boolean equal = pr.getEqual();
		Long am = pr.getTotalAmount()/pr.getEnvelope();
		System.out.println(am);
		if (!equal){
			Random rd = new Random();
			am = am - 100;
			am = 100 + Math.abs(rd.nextLong()) % (am*2);
			System.out.println(am);
			if (am > pr.getCurrentAmount())
				am = pr.getCurrentAmount();
		}
		pr.setCurrentAmount(pr.getCurrentAmount()-am);
		pr.setCurrentEnvelope(pr.getCurrentEnvelope()-1);
		Wallet wallet = entityManager.find(Wallet.class,rq.getUserId(), LockModeType.PESSIMISTIC_WRITE);
		wallet.setBalance(wallet.getBalance() + am);
		PresentTransaction pt = new PresentTransaction(GenerationUtil.generateId("trans"), rq.getUserId(), rq.getPresentId(), am, System.currentTimeMillis());
		presentTransactionRepository.save(pt);
		return am;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void checkExprired() {
		List<Present> presents = presentRepository.findExpiredPresent(System.currentTimeMillis()-24*60*60*1000);
		for (Present present:presents){
			present.setExpired(true);
			System.out.println("Expired present: " + present.getPresentId());
		}
	}
	
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void removePresent(String presentId) {
		Present pr = entityManager.find(Present.class, presentId, LockModeType.PESSIMISTIC_WRITE);
		if(pr==null) return;
		String userId = pr.getOwnerId();
		if(pr.getCurrentAmount()>0) {
			Wallet wallet = entityManager.find(Wallet.class, userId,LockModeType.PESSIMISTIC_WRITE);
			wallet.setBalance(wallet.getBalance() + pr.getCurrentAmount());
			redisTemplate.opsForHash().increment("wallet:" + userId, "balance", pr.getCurrentAmount());
		}
		presentRepository.delete(pr);
	}
	
	public void getTopupTransactions(GetTopupsRequest rq) {
		
	}

	public void getP2PTransactions(GetP2PsRequest rq) {
		String sql="select ts.id, ts.amount,ts.timestamp, s.username, r.username from P2PTransaction ts join Authenticate a";
		entityManager.createQuery("");
	}
}
