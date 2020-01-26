package main.org.vikingsoftware.dropshipper.tools;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.ebay.sdk.ApiContext;
import com.ebay.sdk.ApiException;
import com.ebay.sdk.TimeFilter;
import com.ebay.sdk.call.GetSellerTransactionsCall;
import com.ebay.soap.eBLBaseComponents.AccountDetailEntryCodeType;
import com.ebay.soap.eBLBaseComponents.AccountEntryType;
import com.ebay.soap.eBLBaseComponents.DetailLevelCodeType;
import com.ebay.soap.eBLBaseComponents.ItemType;
import com.ebay.soap.eBLBaseComponents.PaginationType;
import com.ebay.soap.eBLBaseComponents.PaymentTransactionType;
import com.ebay.soap.eBLBaseComponents.PaymentsInformationType;
import com.ebay.soap.eBLBaseComponents.TransactionType;

import main.org.vikingsoftware.dropshipper.core.data.customer.order.CustomerOrder;
import main.org.vikingsoftware.dropshipper.core.data.customer.order.CustomerOrderManager;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.MarketplaceLoader;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.listing.MarketplaceListing;
import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrder;
import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrderManager;
import main.org.vikingsoftware.dropshipper.core.data.transaction.Transaction;
import main.org.vikingsoftware.dropshipper.core.ebay.EbayApiContextManager;
import main.org.vikingsoftware.dropshipper.core.ebay.EbayCalls;
import main.org.vikingsoftware.dropshipper.core.utils.TransactionUtils;

public class FillTransactionTable {

	public static void main(final String[] args) {
		final List<TransactionType> orderTransactions = getAllOrderTransactions();
		System.out.println("Loaded " + orderTransactions.size() + " eBay transactions.");
		insertOrderTransactions(orderTransactions);
		
		//insertAllAccountTransactions(120);
	}
	
	public static List<TransactionType> getAllOrderTransactions() {
		final List<TransactionType> allTransactions = new ArrayList<>();
		try {
			
			int pageNumber = 1;
			int numDaysInPast = -365;
			TransactionType[] transactions = null;
			do {
				final ApiContext apiContext = EbayApiContextManager.getLiveContext();
				final GetSellerTransactionsCall call = new GetSellerTransactionsCall(apiContext);
				
				call.setIncludeFinalValueFee(true);
				call.setDetailLevel(new DetailLevelCodeType[]{DetailLevelCodeType.RETURN_ALL});
				
				final PaginationType pagination = new PaginationType();
				pagination.setEntriesPerPage(200);
				pagination.setPageNumber(pageNumber);
				call.setPagination(pagination);
				
				final Calendar from = Calendar.getInstance();
				final Calendar to = Calendar.getInstance();
				from.add(Calendar.DAY_OF_YEAR, numDaysInPast);
				to.add(Calendar.DAY_OF_YEAR, numDaysInPast + 29);
				call.setTimeFilter(new TimeFilter(from, to)); //will use number of days filter
				
				try {
					transactions = call.getSellerTransactions();
				} catch(final ApiException e) {
					transactions = null;
				}
				
				if(transactions != null && transactions.length > 0 ) {
					System.out.println("Num eBay transactions on page " + pageNumber + ": " + transactions.length);
					for(final TransactionType transaction : transactions) {
						if(transaction.getAmountPaid().getValue() <= 0) {
							continue;
						}
						allTransactions.add(transaction);
					}
					pageNumber++;
				} else {
					numDaysInPast += 29;
					pageNumber = 1;
				}
			} while((transactions != null && transactions.length > 0) || numDaysInPast < 0);

		} catch(final Exception e) {
			e.printStackTrace();
		}

		return allTransactions;
	}
	
	public static void insertAllAccountTransactions(final int daysInPast) {		
		try {
			//last 4 months is max time frame for eBay account activity
			final List<AccountEntryType> accountEntries = EbayCalls.getAccountActivityLastXDays(daysInPast).stream()
					.filter(entry -> entry.getAccountDetailsEntryType() == AccountDetailEntryCodeType.FEE_INSERTION
					 		|| (entry.getAccountDetailsEntryType() == AccountDetailEntryCodeType.SUBSCRIPTIONE_BAY_STORES
					 		       && entry.getTitle().contains("Store") && entry.getTitle().contains("Subscription Fee")))
					.collect(Collectors.toList());
			
			System.out.println("Found " + accountEntries.size() + " eBay account transaction entries.");
			
			for(final AccountEntryType entry : accountEntries) {
				System.out.println("Attempting to insert Account Transaction: " + entry.getItemID() + ", " + entry.getTitle() + ", "
						+ entry.getDate().getTime() + ", " + entry.getNetDetailAmount().getValue() + ", "
				        + entry.getAccountDetailsEntryType());
				
				final MarketplaceListing marketplaceListing = entry.getItemID() == null ? null : MarketplaceLoader.loadMarketplaceListingByListingId(entry.getItemID());
				final Integer marketplaceListingId = marketplaceListing == null ? null : marketplaceListing.id;
				
				final Transaction transaction = new Transaction.Builder()
						.type(main.org.vikingsoftware.dropshipper.core.data.transaction.TransactionType.MARKETPLACE_OPERATING_COST)
						.amount(0 - (float)entry.getNetDetailAmount().getValue())
						.date(entry.getDate().getTimeInMillis())
						.notes(entry.getAccountDetailsEntryType().name())
						.marketplace_listing_id(marketplaceListingId)
						.build();
				
				TransactionUtils.insertTransaction(transaction);
			}
			
		} catch(final Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void insertOrderTransactions(final List<TransactionType> orderTransactions) {
		for(final TransactionType transaction : orderTransactions) {
			Integer customerOrderId = null;
			Integer processedOrderId = null;
			Integer marketplaceListingId = null;
			
			final Optional<CustomerOrder> mappedCustomerOrder = CustomerOrderManager.loadCustomerOrderByMarketplaceOrderId(transaction.getTransactionID());
			if(mappedCustomerOrder.isPresent()) {
				final CustomerOrder customerOrder = mappedCustomerOrder.get();
				customerOrderId = customerOrder.id;
				marketplaceListingId = customerOrder.marketplace_listing_id;
				
				final Optional<ProcessedOrder> mappedProcessedOrder = ProcessedOrderManager.getProcessedOrderForCustomerOrder(customerOrder.id);
				if(mappedProcessedOrder.isPresent()) {
					final ProcessedOrder processedOrder = mappedProcessedOrder.get();
					processedOrderId = processedOrder.id;
					
					final Transaction fulfillmentCostTransaction = new Transaction.Builder()
							.type(main.org.vikingsoftware.dropshipper.core.data.transaction.TransactionType.FULFILLMENT_COST)
							.amount((float)processedOrder.buy_total * -1)
							.marketplace_listing_id(marketplaceListingId)
							.customer_order_id(customerOrderId)
							.processed_order_id(processedOrderId)
							.date(processedOrder.date_processed)
							.build();
					
					TransactionUtils.insertTransaction(fulfillmentCostTransaction);
				}
			}
			final ItemType item = transaction.getItem();
			System.out.println("Total eBay income: "  + transaction.getTransactionPrice().getValue());
			System.out.println("Transaction Paid Time: " + transaction.getPaidTime().toInstant().toEpochMilli());
			System.out.println("Order ID: " + transaction.getTransactionID());
			System.out.println("Item ID: " + item.getItemID());
			System.out.println("Final Value Fees: " + transaction.getFinalValueFee().getValue());
			
			final PaymentsInformationType monetaryDetails = transaction.getMonetaryDetails();
			System.out.println("monetaryDetails: " + monetaryDetails);
			final PaymentTransactionType initialBuyerPayment = monetaryDetails.getPayments().getPayment(0);
			System.out.println("Paypal Transaction ID: " + initialBuyerPayment.getReferenceID().getValue());
			System.out.println("Paypal Fee: " + initialBuyerPayment.getFeeOrCreditAmount().getValue());
			System.out.println("Paypal Payment Time: " + initialBuyerPayment.getPaymentTime().toInstant().toEpochMilli());
			
			final Transaction marketplaceIncomeTransaction = new Transaction.Builder()
					.type(main.org.vikingsoftware.dropshipper.core.data.transaction.TransactionType.MARKETPLACE_INCOME)
					.amount((float)transaction.getTransactionPrice().getValue())
					.marketplace_listing_id(marketplaceListingId)
					.customer_order_id(customerOrderId)
					.processed_order_id(processedOrderId)
					.date(transaction.getPaidTime().toInstant().toEpochMilli())
					.build();
			
			final Transaction paymentProcessorFeeTransaction = new Transaction.Builder()
					.type(main.org.vikingsoftware.dropshipper.core.data.transaction.TransactionType.PAYMENT_PROCESSOR_FEE)
					.amount((float)initialBuyerPayment.getFeeOrCreditAmount().getValue() * -1)
					.marketplace_listing_id(marketplaceListingId)
					.customer_order_id(customerOrderId)
					.processed_order_id(processedOrderId)
					.date(initialBuyerPayment.getPaymentTime().toInstant().toEpochMilli())
					.build();
			
			final Transaction finalValueFeeTransaction = new Transaction.Builder()
					.type(main.org.vikingsoftware.dropshipper.core.data.transaction.TransactionType.MARKETPLACE_SELL_FEE)
					.amount((float)transaction.getFinalValueFee().getValue() * -1)
					.marketplace_listing_id(marketplaceListingId)
					.customer_order_id(customerOrderId)
					.processed_order_id(processedOrderId)
					.date(transaction.getPaidTime().toInstant().toEpochMilli())
					.build();
			
			TransactionUtils.insertTransaction(marketplaceIncomeTransaction);
			TransactionUtils.insertTransaction(paymentProcessorFeeTransaction);
			TransactionUtils.insertTransaction(finalValueFeeTransaction);
		}
	}
}
