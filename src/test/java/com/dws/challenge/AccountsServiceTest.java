package com.dws.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.service.AccountsService;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class AccountsServiceTest {

	@Autowired
	private AccountsService accountsService;
	
	 @BeforeEach
	  void setUp() {
	    accountsService.getAccountsRepository().clearAccounts();
	  }

	 @Test
	void addAccount() {
		Account account = new Account("Id-123");
		account.setBalance(new BigDecimal(1000));
		this.accountsService.createAccount(account);

		assertThat(this.accountsService.getAccount("Id-123")).isEqualTo(account);
	}

	 @Test
	void addAccount_failsOnDuplicateId() {
		String uniqueId = "Id-" + System.currentTimeMillis();
		Account account = new Account(uniqueId);
		this.accountsService.createAccount(account);

		try {
			this.accountsService.createAccount(account);
			fail("Should have failed when adding duplicate account");
		} catch (DuplicateAccountIdException ex) {
			assertThat(ex.getMessage()).isEqualTo("Account id " + uniqueId + " already exists!");
		}
	}

	 @Test
	void transferMoney_sucessfullOneTimeTransfer() {
		Account account1 = new Account("Id-1001");
		account1.setBalance(new BigDecimal(1000));
		this.accountsService.createAccount(account1);

		Account account2 = new Account("Id-1022");
		account2.setBalance(new BigDecimal(1000));
		this.accountsService.createAccount(account2);

		this.accountsService.transfer("Id-1001", "Id-1022", BigDecimal.valueOf(100));

		BigDecimal balAccount1 = BigDecimal.valueOf(900);
		BigDecimal balAccount2 = BigDecimal.valueOf(1100);

		assertEquals(accountsService.getAccount("Id-1001").getBalance().equals(balAccount1), true);
		assertEquals(accountsService.getAccount("Id-1022").getBalance().equals(balAccount2), true);
	}

	 @Test
	void transferMoney_sucessfullMultipleTransfer() {
		Account account1 = new Account("Id-101");
		account1.setBalance(new BigDecimal(1000));
		this.accountsService.createAccount(account1);

		Account account2 = new Account("Id-102");
		account2.setBalance(new BigDecimal(1000));
		this.accountsService.createAccount(account2);

		for (int i = 0; i < 5; i++) {
			this.accountsService.transfer("Id-101", "Id-102", BigDecimal.valueOf(100));
		}
		BigDecimal balAccount1 = BigDecimal.valueOf(500);
		BigDecimal balAccount2 = BigDecimal.valueOf(1500);

		assertEquals(accountsService.getAccount("Id-101").getBalance().equals(balAccount1), true);
		assertEquals(accountsService.getAccount("Id-102").getBalance().equals(balAccount2), true);
	}

	 @Test
	void transferMoney_MultipleTransfer_leadingInsufficentBalance() {
		Account account1 = new Account("Id-3001");
		account1.setBalance(new BigDecimal(1000));
		this.accountsService.createAccount(account1);

		Account account2 = new Account("Id-3002");
		account2.setBalance(new BigDecimal(1000));
		this.accountsService.createAccount(account2);

		for (int i = 0; i < 6; i++) {
			try {
				this.accountsService.transfer("Id-3001", "Id-3002", BigDecimal.valueOf(300));
			} catch (Exception e) {
				// some transaction will fail due to insufficient balance
			}
		}
		BigDecimal balAccount1 = BigDecimal.valueOf(100);
		BigDecimal balAccount2 = BigDecimal.valueOf(1900);

		assertEquals(accountsService.getAccount("Id-3001").getBalance().equals(balAccount1), true);
		assertEquals(accountsService.getAccount("Id-3002").getBalance().equals(balAccount2), true);
	}

	@Test
	void transferMoney_MultipleConcurrentTransfer() {
		Account account1 = new Account("Id-4001");
		account1.setBalance(new BigDecimal(1000));
		this.accountsService.createAccount(account1);

		Account account2 = new Account("Id-4002");
		account2.setBalance(new BigDecimal(1000));
		this.accountsService.createAccount(account2);
		
		/*
		 * Created 2 accounts and doing 10 transaction from acc1 -> acc2 with amount 10
		 * and another 10 transaction from acc2 -> acc1 with same amount 10 in parallel. So
		 * at the end the balance of both the account remains unchanged
		 */

		int threadCount = 20;
		BigDecimal amount = new BigDecimal("10.00");

		List<Future<?>> futures = new ArrayList<>();
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		for (int i = 0; i < threadCount; i++) {
			futures.add(executor.submit(() -> {
				try {
					accountsService.transfer("Id-4001", "Id-4002", amount);
				} catch (Exception e) {
				}
			}));
		}

		for (int i = 0; i < threadCount; i++) {
			futures.add(executor.submit(() -> {
				try {
					accountsService.transfer("Id-4002", "Id-4001", amount);
				} catch (Exception e) {
				}
			}));
		}
		executor.shutdown();

		for (Future<?> f : futures) {
		    try {
		        f.get();  
		    } catch (InterruptedException | ExecutionException e) {
		    }
		}
		assertEquals(accountsService.getAccount("Id-4001").getBalance().equals(account1.getBalance()), true);
		assertEquals(accountsService.getAccount("Id-4002").getBalance().equals(account2.getBalance()), true);
	}

	 @Test
	void transferMoney_NonPositiveTransfer() {
		Account account1 = new Account("Id-201");
		account1.setBalance(new BigDecimal(1000));
		this.accountsService.createAccount(account1);

		Account account2 = new Account("Id-202");
		account2.setBalance(new BigDecimal(1000));
		this.accountsService.createAccount(account2);

		try {
			this.accountsService.transfer("Id-201", "Id-202", BigDecimal.valueOf(-2));
		} catch (IllegalArgumentException ex) {
			assertThat(ex.getMessage()).isEqualTo("Transfer amount must be positive");
		}

	}
}
