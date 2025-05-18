package com.dws.challenge.domain;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequest {

	@NotNull
	@NotEmpty
	private  String accountFromId;

	@NotNull
	@NotEmpty
	private  String accountToId;

	@NotNull
	private BigDecimal amount;

}
