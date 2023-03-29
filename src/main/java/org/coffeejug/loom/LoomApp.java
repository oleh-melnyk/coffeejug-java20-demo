package org.coffeejug.loom;

import static java.lang.System.out;

import com.github.javafaker.Faker;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.random.RandomGenerator;
import jdk.incubator.concurrent.StructuredTaskScope;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.With;

// --enable-preview --add-modules jdk.incubator.concurrent
public class LoomApp {

  @With
  @Builder
  record PaymentState(String id, String customerId, String accountNumber, int amount) {

    PaymentState masked() {
      return this.withAccountNumber(accountNumber.substring(0, 2) + "*****");
    }
  }

  final static ThreadLocal<PaymentState> PAYMENT_STATE = new ThreadLocal<>();

  public static void main(String[] args) {
    var state = PaymentState.builder()
        .accountNumber("1241241gds3")
        .customerId("jaks7241")
        .amount(10_000)
        .id("1111")
        .build();

    PAYMENT_STATE.set(state);
    PaymentClient.process();
    PAYMENT_STATE.remove();
  }
  
  static class PaymentClient {

    @SneakyThrows
    public static void process() {
      String status = PaymentService.doTransaction();
      String customerContact = CustomerCRM.fetchCustomerContact();

      notifyCustomer(status, customerContact);
    }

    @SneakyThrows
    public static void processAsync() {
      try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
        Future<String> status = scope.fork(PaymentService::doTransaction);
        Future<String> customerContact = scope.fork(CustomerCRM::fetchCustomerContact);

        scope.join();
        scope.throwIfFailed();

        notifyCustomer(status.resultNow(), customerContact.resultNow());
      }
    }

    private static void notifyCustomer(String status, String contact) {
      String paymentId = Objects.requireNonNull(PAYMENT_STATE.get()).id();
      out.println(PAYMENT_STATE.get().accountNumber());
      out.printf("Payment %s is finished with status %s%n", paymentId, status);
      out.printf("Sending SMS with a result to %s", contact);
    }
  }

  static class PaymentService {

    @SneakyThrows
    public static String doTransaction() {
      out.println("doTransaction -> " + Thread.currentThread());
      PaymentState paymentState = PAYMENT_STATE.get();
      validateAccount(paymentState.accountNumber());
      out.printf("Performing transaction with id %s and amount %d%n", paymentState.id(), paymentState.amount());
      TimeUnit.SECONDS.sleep(2);
      return RandomGenerator.getDefault().nextBoolean() ? "success" : "fail";
    }

    @SneakyThrows
    private static void validateAccount(String accountNumber) {
      out.printf("Validating account number %s...\n", accountNumber);
      TimeUnit.SECONDS.sleep(2);
    }
  }

  static class CustomerCRM {

    private static final Faker FAKER = Faker.instance(Locale.of("uk"));

    @SneakyThrows
    public static String fetchCustomerContact() {
      out.println("doTransaction -> " + Thread.currentThread());
      out.println(Thread.currentThread());

      TimeUnit.SECONDS.sleep(2);
      return FAKER.phoneNumber().phoneNumber();
    }
  }
}
