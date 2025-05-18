package com.dws.challenge.service;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.dws.challenge.domain.Account;
import com.dws.challenge.repository.AccountsRepository;
import com.dws.challenge.web.AccountsController;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AccountsService {

	@Getter
	private final AccountsRepository accountsRepository;

	@Getter
	private final NotificationService notificationService;

	@Autowired
	public AccountsService(AccountsRepository accountsRepository, NotificationService notificationService) {
		this.accountsRepository = accountsRepository;
		this.notificationService = notificationService;
	}

	public void createAccount(Account account) {
		this.accountsRepository.createAccount(account);
	}

	public Account getAccount(String accountId) {
		return this.accountsRepository.getAccount(accountId);
	}

	/**
	 * Method to transfer funds from one account to another account
	 * 
	 * @param fromAccountId
	 * @param toAccountId
	 * @param amount
	 */
	public void transfer(String fromAccountId, String toAccountId, BigDecimal amount) {
		
		log.info("Start Tranfer money - {} from account- {} to account - {}" ,amount ,fromAccountId,toAccountId );

		if (amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Transfer amount must be positive");
		}

		if (fromAccountId.trim().equalsIgnoreCase(toAccountId.trim())) {
			throw new IllegalArgumentException("Transfer can happen between 2 different accounts");
		}

		Account fromAccount = accountsRepository.getAccount(fromAccountId);
		Account toAccount = accountsRepository.getAccount(toAccountId);

		if (fromAccount == null || toAccount == null) {
			throw new IllegalArgumentException("One or both accounts not found");
		}

		/*
		 * we need to take locks in Same order .Else it will go in deadlock situation,for
		 * example if 2 transfer request is coming in parallel - Request#1: transfer x
		 * dollar from A/C1 to A/C2 
		 * Request#2 - transfer y dollar from A/C2 to A/C1
		 */
		Object lock1 = fromAccountId.compareTo(toAccountId) < 0 ? fromAccount : toAccount;
		Object lock2 = fromAccountId.compareTo(toAccountId) < 0 ? toAccount : fromAccount;
		

		synchronized (lock1) {
			synchronized (lock2) {
				if (fromAccount.getBalance().compareTo(amount) < 0) {
					throw new IllegalArgumentException("Insufficient funds in source account");
				}
				fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
				toAccount.setBalance(toAccount.getBalance().add(amount));

			}
		}

		try {
			notificationService.notifyAboutTransfer(fromAccount,
					String.format("Debited %s from account %s", amount, fromAccountId));
		} catch (Exception e) {
			log.error("Failed to notify debit transaction", e);
		}

		try {
			notificationService.notifyAboutTransfer(toAccount,
					String.format("Credited %s to account %s", amount, toAccountId));
		} catch (Exception e) {
			log.error("Failed to notify Credit transaction", e);
		}
		log.info("End Tranfer money - {} from account- {} to account - {}" ,amount ,fromAccountId,toAccountId );

	}

}
