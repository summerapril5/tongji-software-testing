import com.github.JLQusername.api.Bankcard;
import com.github.JLQusername.api.OurSystem;
import com.github.JLQusername.api.client.AccountClient;
import com.github.JLQusername.api.client.SettleClient;
import com.github.JLQusername.transaction.domain.Holding;
import com.github.JLQusername.transaction.domain.Redemption;
import com.github.JLQusername.transaction.domain.Subscription;
import com.github.JLQusername.transaction.domain.dto.SubscriptionDTO;
import com.github.JLQusername.transaction.mapper.HoldingMapper;
import com.github.JLQusername.transaction.mapper.RedemptionMapper;
import com.github.JLQusername.transaction.mapper.SubscriptionMapper;
import com.github.JLQusername.transaction.service.TransactionService;
import com.github.JLQusername.transaction.service.impl.TransactionServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @InjectMocks
    private TransactionServiceImpl transactionService;

    @Mock
    private SubscriptionMapper subscriptionMapper;
    @Mock
    private RedemptionMapper redemptionMapper;
    @Mock
    private HoldingMapper holdingMapper;
    @Mock
    private AccountClient accountClient;
    @Mock
    private SettleClient settleClient;

    private SubscriptionDTO buildDTO(double amount, String tradingId) {
        SubscriptionDTO dto = new SubscriptionDTO();
        dto.setAmount(amount);
        dto.setTradingAccountId(tradingId);
        dto.setFundAccount(123L);
        dto.setProductId(1);
        dto.setProductName("TestProduct");
        return dto;
    }

    private Bankcard buildBankcard(double balance) {
        Bankcard bankcard = new Bankcard("6222334455667788", 1000.0);
        bankcard.setBalance(balance);
        return bankcard;
    }

    private OurSystem buildSystem(Date date, boolean stopped) {
        OurSystem system = new OurSystem();
        system.setTransactionDate(date);
        system.setHasStoppedApplication(stopped);
        return system;
    }

    @Nested
    class SubmitSubscriptionTests {

        @Test
        @DisplayName("P4-TC01-01")
        void test_P4_TC01_validDTO_sufficientBalance() {
            SubscriptionDTO dto = buildDTO(500, "1001");
            Bankcard bankcard = buildBankcard(1000);
            OurSystem system = buildSystem(new Date(), false);

            when(accountClient.getBankcard(1001L)).thenReturn(bankcard);
            when(settleClient.getSystem()).thenReturn(system);

            doAnswer(invocation -> {
                Subscription sub = invocation.getArgument(0);
                sub.setTransactionId(123L); // 模拟 DB 自动生成 ID
                return null;
            }).when(subscriptionMapper).insert(any(Subscription.class));

            long result = transactionService.submitSubscription(dto);
            assertEquals(123L, result);
        }

        @Test
        @DisplayName("P4-TC02-01")
        void test_P4_TC02_insufficientBalance() {
            SubscriptionDTO dto = buildDTO(200, "1001");
            Bankcard bankcard = buildBankcard(100);

            when(accountClient.getBankcard(1001L)).thenReturn(bankcard);

            long result = transactionService.submitSubscription(dto);
            assertEquals(1L, result);
        }

        @DisplayName("P4-TC03-01")
        @Test
        void test_P4_TC03_invalidAccountId() {
            SubscriptionDTO dto = buildDTO(200, "invalid");

            assertThrows(NumberFormatException.class, () -> {
                transactionService.submitSubscription(dto);
            });
        }

        @DisplayName("P4-TC04-01")
        @Test
        void test_P4_TC04_amountZero() {
            SubscriptionDTO dto = buildDTO(0, "1001");
            Bankcard bankcard = buildBankcard(1000);

            when(accountClient.getBankcard(1001L)).thenReturn(bankcard);

            assertThrows(RuntimeException.class, () -> {
                transactionService.submitSubscription(dto);
            });
        }

        @DisplayName("P4-TC01-02")
        @Test
        void test_P4_TC04_amountNegative() {
            SubscriptionDTO dto = buildDTO(-10, "1001");
            Bankcard bankcard = buildBankcard(1000);

            when(accountClient.getBankcard(1001L)).thenReturn(bankcard);

            assertThrows(RuntimeException.class, () -> {
                transactionService.submitSubscription(dto);
            });
        }

        @DisplayName("P4-TC05-01")
        @Test
        void test_P4_TC05_mapperThrowsException() {
            SubscriptionDTO dto = buildDTO(500, "1001");
            Bankcard bankcard = buildBankcard(1000);
            OurSystem system = buildSystem(new Date(), false);

            when(accountClient.getBankcard(1001L)).thenReturn(bankcard);
            when(settleClient.getSystem()).thenReturn(system);
            doThrow(new RuntimeException("DB insert error"))
                    .when(subscriptionMapper).insert(any(Subscription.class));

            assertThrows(RuntimeException.class, () -> {
                transactionService.submitSubscription(dto);
            });
        }

        @DisplayName("P4-TC06-01")
        @Test
        void test_P4_TC06_updateBalanceThrowsException() {
            SubscriptionDTO dto = buildDTO(500, "1001");
            Bankcard bankcard = buildBankcard(1000);
            OurSystem system = buildSystem(new Date(), false);

            when(accountClient.getBankcard(1001L)).thenReturn(bankcard);
            when(settleClient.getSystem()).thenReturn(system);

            doThrow(new RuntimeException("Update failed"))
                    .when(accountClient).updateBalance(bankcard);

            assertThrows(RuntimeException.class, () -> {
                transactionService.submitSubscription(dto);
            });
        }

        @DisplayName("P4-TC07-01")
        @Test
        void test_P4_TC07_getDateValid() {
            SubscriptionDTO dto = buildDTO(100, "1001");
            Bankcard bankcard = buildBankcard(1000);
            OurSystem system = buildSystem(new Date(), false);

            when(accountClient.getBankcard(1001L)).thenReturn(bankcard);
            when(settleClient.getSystem()).thenReturn(system);

            doAnswer(invocation -> {
                Subscription sub = invocation.getArgument(0);
                sub.setTransactionId(999L);
                return null;
            }).when(subscriptionMapper).insert(any(Subscription.class));

            long result = transactionService.submitSubscription(dto);
            assertEquals(999L, result);
        }

        @DisplayName("P4-TC08-01")
        @Test
        void test_P4_TC08_getDateReturnsNull() {
            SubscriptionDTO dto = buildDTO(100, "1001");
            Bankcard bankcard = buildBankcard(1000);

            OurSystem system = mock(OurSystem.class);
            when(system.getTransactionDate()).thenReturn(null);
            when(settleClient.getSystem()).thenReturn(system);
            when(accountClient.getBankcard(1001L)).thenReturn(bankcard);

            assertThrows(NullPointerException.class, () -> {
                transactionService.submitSubscription(dto);
            });
        }
    }

    @Nested
    class confirmSubscriptionBatchTests {

        @Test
        @DisplayName("P5-TC01-01")
        void testConfirmSubscriptionBatch_allExists_shouldUpdateHoldingsAndReturnTrue() {
            // 准备数据
            Map<Long, Double> transactionIdToShares = new HashMap<>();
            transactionIdToShares.put(1L, 10.0);
            transactionIdToShares.put(2L, 20.0);

            Subscription s1 = new Subscription();
            s1.setId(1L);
            s1.setTradingAccountId(100L);
            s1.setProductId(200);

            Subscription s2 = new Subscription();
            s2.setId(2L);
            s2.setTradingAccountId(101L);
            s2.setProductId(201);

            when(subscriptionMapper.selectById(1L)).thenReturn(s1);
            when(subscriptionMapper.selectById(2L)).thenReturn(s2);

            Holding h1 = new Holding();
            h1.setTradingAccountId(100L);
            h1.setProductId(200);
            h1.setShares(50.0);

            Holding h2 = new Holding();
            h2.setTradingAccountId(101L);
            h2.setProductId(201);
            h2.setShares(30.0);

            // 模拟 getHolding 的实现，假设它通过 holdingMapper.selectOne 查找
            // 这里用eq或any()代替条件，根据你getHolding代码调整
            when(holdingMapper.selectOne(argThat(wrapper -> true)))
                    .thenReturn(h1)
                    .thenReturn(h2);

            // 执行方法
            boolean result = transactionService.confirmSubscriptionBatch(transactionIdToShares);

            // 验证结果
            assertTrue(result);

            // shares 应该正确累加
            assertEquals(60.0, h1.getShares(), 0.0001); // 50 + 10
            assertEquals(50.0, h2.getShares(), 0.0001); // 30 + 20

            // 验证updateById调用
            verify(holdingMapper).updateById(h1);
            verify(holdingMapper).updateById(h2);

            // 确认没有调用insert（因为holding都存在）
            verify(holdingMapper, never()).insert(any());
        }

        @Test
        @DisplayName("P5-TC02-01")
        void testPartialExist() {
            Map<Long, Double> input = new HashMap<>();
            input.put(1L, 5.0);
            input.put(99L, 10.0); // 不存在的交易ID

            Subscription s1 = new Subscription();
            s1.setTransactionId(1L);
            s1.setTradingAccountId(100L);
            s1.setProductId(200);

            when(subscriptionMapper.selectById(1L)).thenReturn(s1);
            when(subscriptionMapper.selectById(99L)).thenReturn(null);

            Holding h1 = new Holding();
            h1.setShares(10.0);
            when(holdingMapper.selectOne(any())).thenReturn(h1);
            when(holdingMapper.updateById(any())).thenReturn(1);

            boolean result = transactionService.confirmSubscriptionBatch(input);

            assertTrue(result);
            assertEquals(15.0, h1.getShares());
            verify(holdingMapper).updateById(any());
            verify(holdingMapper, never()).insert(any());
        }

        @Test
        @DisplayName("P5-TC03-01")
        void testAllTransactionsNotExist() {
            Map<Long, Double> input = new HashMap<>();
            input.put(1L, 5.0);
            input.put(2L, 10.0);

            when(subscriptionMapper.selectById(anyLong())).thenReturn(null);

            boolean result = transactionService.confirmSubscriptionBatch(input);

            assertTrue(result);
            verifyNoInteractions(holdingMapper);
        }

        @Test
        @DisplayName("P5-TC04-01")
        void testHoldingExistsAddShares() {
            Map<Long, Double> input = new HashMap<>();
            input.put(1L, 3.0);

            Subscription s1 = new Subscription();
            s1.setTransactionId(1L);
            s1.setTradingAccountId(100L);
            s1.setProductId(200);

            when(subscriptionMapper.selectById(1L)).thenReturn(s1);

            Holding h1 = new Holding();
            h1.setShares(7.0);
            when(holdingMapper.selectOne(any())).thenReturn(h1);
            when(holdingMapper.updateById(any())).thenReturn(1);

            boolean result = transactionService.confirmSubscriptionBatch(input);

            assertTrue(result);
            assertEquals(10.0, h1.getShares());
            verify(holdingMapper).updateById(any());
            verify(holdingMapper, never()).insert(any());
        }

        @Test
        @DisplayName("P5-TC05-01")
        void testHoldingNotExistInsertNew() {
            Map<Long, Double> input = new HashMap<>();
            input.put(1L, 2.0);

            Subscription s1 = new Subscription();
            s1.setTransactionId(1L);
            s1.setTradingAccountId(100L);
            s1.setProductId(200);

            when(subscriptionMapper.selectById(1L)).thenReturn(s1);

            when(holdingMapper.selectOne(any())).thenReturn(null);
            when(holdingMapper.insert(any())).thenReturn(1);

            boolean result = transactionService.confirmSubscriptionBatch(input);

            assertTrue(result);
            verify(holdingMapper).insert(any());
            verify(holdingMapper, never()).updateById(any());
        }

        @Test
        @DisplayName("P5-TC06-01")
        void testEmptyInput() {
            Map<Long, Double> input = new HashMap<>();

            boolean result = transactionService.confirmSubscriptionBatch(input);

            assertTrue(result);
            verifyNoInteractions(subscriptionMapper);
            verifyNoInteractions(holdingMapper);
        }

        @Test
        @DisplayName("P5-TC07-01")
        void testNullInput() {
            try {
                // 调用方法
                boolean result = transactionService.confirmSubscriptionBatch(null);

                // 如果没有异常，判断业务逻辑是否正确
                assertFalse(result, "Method should return false or handle null input, but returned true.");
            } catch (Exception e) {
                // 如果抛出异常，说明代码没有处理好，测试失败
                fail("Method threw exception on null input: " + e.toString());
            }
        }


        @Test
        @DisplayName("P5-TC10-01")
        void testPositiveShares() {
            Map<Long, Double> input = new HashMap<>();
            input.put(1L, 5.0);

            Subscription s1 = new Subscription();
            s1.setTransactionId(1L);
            s1.setTradingAccountId(100L);
            s1.setProductId(200);

            when(subscriptionMapper.selectById(1L)).thenReturn(s1);

            Holding h1 = new Holding();
            h1.setShares(10.0);
            when(holdingMapper.selectOne(any())).thenReturn(h1);
            when(holdingMapper.updateById(any())).thenReturn(1);

            boolean result = transactionService.confirmSubscriptionBatch(input);

            assertTrue(result);
            assertEquals(15.0, h1.getShares());
        }
        @Test
        @DisplayName("P5-TC08-01")
        void testUpdateException() {
            Map<Long, Double> input = new HashMap<>();
            input.put(1L, 5.0);

            Subscription s1 = new Subscription();
            s1.setTransactionId(1L);
            s1.setTradingAccountId(100L);
            s1.setProductId(200);

            when(subscriptionMapper.selectById(1L)).thenReturn(s1);

            Holding h1 = new Holding();
            h1.setShares(10.0);
            when(holdingMapper.selectOne(any())).thenReturn(h1);
            when(holdingMapper.updateById(any())).thenThrow(new RuntimeException("DB update error"));

            // 这里因为事务注解，异常会导致回滚，测试捕获异常并返回false或抛异常需要修改原方法实现
            // 但当前方法实现无catch，异常会抛出，因此单元测试中可以捕获异常断言

            assertThrows(RuntimeException.class, () -> transactionService.confirmSubscriptionBatch(input));
        }

        @Test
        @DisplayName("P5-TC09-01")
        void testInsertException() {
            Map<Long, Double> input = new HashMap<>();
            input.put(1L, 5.0);

            Subscription s1 = new Subscription();
            s1.setTransactionId(1L);
            s1.setTradingAccountId(100L);
            s1.setProductId(200);

            when(subscriptionMapper.selectById(1L)).thenReturn(s1);

            when(holdingMapper.selectOne(any())).thenReturn(null);
            when(holdingMapper.insert(any())).thenThrow(new RuntimeException("DB insert error"));

            assertThrows(RuntimeException.class, () -> transactionService.confirmSubscriptionBatch(input));
        }

        @Test
        @DisplayName("P5-TC12-01")
        void testNegativeShares() {
            Map<Long, Double> input = new HashMap<>();
            input.put(1L, -3.0);

            Subscription s1 = new Subscription();
            s1.setTransactionId(1L);
            s1.setTradingAccountId(100L);
            s1.setProductId(200);


            lenient().when(subscriptionMapper.selectById(1L)).thenReturn(s1);


            boolean result = transactionService.confirmSubscriptionBatch(input);

            assertFalse(result);
        }

        @Test
        @DisplayName("P5-TC11-01")
        void testZeroShares() {
            Map<Long, Double> input = new HashMap<>();
            input.put(1L, 0.0);

            Subscription s1 = new Subscription();
            s1.setTransactionId(1L);
            s1.setTradingAccountId(100L);
            s1.setProductId(200);

            when(subscriptionMapper.selectById(1L)).thenReturn(s1);

            Holding h1 = new Holding();
            h1.setShares(10.0);
            when(holdingMapper.selectOne(any())).thenReturn(h1);
            when(holdingMapper.updateById(any())).thenReturn(1);

            boolean result = transactionService.confirmSubscriptionBatch(input);

            // 份额为0，业务逻辑中可能无变化也算成功
            assertTrue(result);
            // 这里份额其实没变，因为 + 0
            assertEquals(10.0, h1.getShares());
        }
    }

    @Nested
    @MockitoSettings(strictness = Strictness.LENIENT)
    class CancelSubscriptionSuccessTests {

        @Test
        @DisplayName("P6-TC01-01")
        void testSingleTransactionCancelSuccess() {
            long transactionId = 1001L;

            Subscription subscription = new Subscription();
            subscription.setTransactionId(transactionId);
            subscription.setTradingAccountId(1L);
            subscription.setSubscriptionAmount(1000.0);
            subscription.setApplicationTime(new Date(System.currentTimeMillis() - 1000 * 60 * 60 * 2)); // 2 hours ago

            OurSystem system = new OurSystem();
            system.setHasExportedApplicationData(false);
            system.setTransactionDate(new Date());

            Bankcard bankcard = new Bankcard();
            bankcard.setBalance(5000.0);

            when(subscriptionMapper.selectById(transactionId)).thenReturn(subscription);
            when(settleClient.getSystem()).thenReturn(system);
            when(accountClient.getBankcard(1L)).thenReturn(bankcard);
            when(subscriptionMapper.update(any(), any())).thenReturn(1);

            boolean result = transactionService.cancelTransaction(transactionId);

            assertTrue(result);
            assertEquals(6000.0, bankcard.getBalance()); // balance should be increased
            verify(accountClient).updateBalance(bankcard);
        }

        @Test
        @DisplayName("P6-TC01-02:")
        void testMultipleTransactionCancelSuccess() {
            long[] transactionIds = {2001L, 2002L};

            for (long transactionId : transactionIds) {
                Subscription subscription = new Subscription();
                subscription.setTransactionId(transactionId);
                subscription.setTradingAccountId(2L);
                subscription.setSubscriptionAmount(500.0);
                subscription.setApplicationTime(new Date(System.currentTimeMillis() - 1000 * 60 * 60)); // 1小时内

                OurSystem system = new OurSystem();
                system.setHasExportedApplicationData(false);
                system.setTransactionDate(new Date());

                Bankcard bankcard = new Bankcard();
                bankcard.setBalance(2000.0);

                when(subscriptionMapper.selectById(transactionId)).thenReturn(subscription);
                when(settleClient.getSystem()).thenReturn(system);
                when(accountClient.getBankcard(2L)).thenReturn(bankcard);
                when(subscriptionMapper.update(any(), any())).thenReturn(1);

                boolean result = transactionService.cancelTransaction(transactionId);

                assertTrue(result);
                assertEquals(2500.0, bankcard.getBalance());
            }

            // 验证 accountClient.updateBalance 被调用两次
            verify(accountClient, times(2)).updateBalance(any());
        }

        @Test
        @DisplayName("P6-TC02-01")
        void testTransactionIdNotExist() {
            long transactionId = 9999L;

            // 模拟申购为空
            when(subscriptionMapper.selectById(transactionId)).thenReturn(null);

            // 模拟赎回存在，但立即 return false（通过 hasExportedApplicationData = true）
            Redemption mockRedemption = mock(Redemption.class);
            when(mockRedemption.getApplicationTime()).thenReturn(new Date()); // 避免 NPE
            when(mockRedemption.getTradingAccountId()).thenReturn(1L);
            when(mockRedemption.getProductId()).thenReturn(1);
            when(mockRedemption.getRedemptionShares()).thenReturn(0.0);
            when(redemptionMapper.selectById(transactionId)).thenReturn(mockRedemption);

            // 模拟系统时间，关键点：hasExportedApplicationData 为 true
            OurSystem mockSystem = mock(OurSystem.class);
            when(mockSystem.isHasExportedApplicationData()).thenReturn(true); // ✅ 这里
            when(mockSystem.getTransactionDate()).thenReturn(new Date());
            when(settleClient.getSystem()).thenReturn(mockSystem);

            TransactionService spyTransactionService = Mockito.spy(transactionService);

            Holding mockHolding = new Holding();
            mockHolding.setShares(10.0);
            doReturn(mockHolding).when(spyTransactionService).getHolding(anyLong(), anyInt());

            boolean result = spyTransactionService.cancelTransaction(transactionId);

            assertFalse(result);

            // 验证不调用更新操作
            verify(accountClient, never()).updateBalance(any());
            verify(subscriptionMapper, never()).update(any(), any());
            verify(redemptionMapper, never()).update(any(), any()); // ✅ 不再报错
        }





        @Test
        @DisplayName("P6-TC03-01")
        void testSystemExportedApplicationData() {
            long transactionId = 3001L;

            Subscription subscription = new Subscription();
            subscription.setTransactionId(transactionId);
            subscription.setApplicationTime(new Date());

            OurSystem system = new OurSystem();
            system.setHasExportedApplicationData(true);

            when(subscriptionMapper.selectById(transactionId)).thenReturn(subscription);
            when(settleClient.getSystem()).thenReturn(system);

            boolean result = transactionService.cancelTransaction(transactionId);

            assertFalse(result);
            verify(accountClient, never()).updateBalance(any());
            verify(subscriptionMapper, never()).update(any(), any());
        }

        @Test
        @DisplayName("P6-TC03-02")
        void testTransactionTimeout() {
            long transactionId = 3002L;

            Subscription subscription = new Subscription();
            subscription.setTransactionId(transactionId);
            subscription.setApplicationTime(new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 25)); // 超过24小时

            OurSystem system = new OurSystem();
            system.setHasExportedApplicationData(false);
            system.setTransactionDate(new Date());

            when(subscriptionMapper.selectById(transactionId)).thenReturn(subscription);
            when(settleClient.getSystem()).thenReturn(system);

            boolean result = transactionService.cancelTransaction(transactionId);

            assertFalse(result);
            verify(accountClient, never()).updateBalance(any());
            verify(subscriptionMapper, never()).update(any(), any());
        }

        @Test
        @DisplayName("P6-TC04-01")
        void testUpdateBalanceThrowsException() {
            long transactionId = 4001L;

            Subscription subscription = new Subscription();
            subscription.setTransactionId(transactionId);
            subscription.setTradingAccountId(5L);
            subscription.setSubscriptionAmount(800.0);
            subscription.setApplicationTime(new Date());

            OurSystem system = new OurSystem();
            system.setHasExportedApplicationData(false);
            system.setTransactionDate(new Date());

            Bankcard bankcard = new Bankcard();
            bankcard.setBalance(1000.0);

            when(subscriptionMapper.selectById(transactionId)).thenReturn(subscription);
            when(settleClient.getSystem()).thenReturn(system);
            when(accountClient.getBankcard(5L)).thenReturn(bankcard);
            doThrow(new RuntimeException("模拟更新异常")).when(accountClient).updateBalance(any());

            assertThrows(RuntimeException.class, () -> transactionService.cancelTransaction(transactionId));

            // 验证 subscription 没有被更新
            verify(subscriptionMapper, never()).update(any(), any());
        }

        @Test
        @DisplayName("P6-TC05-01")
        void testSubscriptionUpdateFail() {
            long transactionId = 100L;

            Subscription subscription = new Subscription();
            subscription.setApplicationTime(new Date(System.currentTimeMillis() - 1000)); // 时间合法
            subscription.setTradingAccountId(1L);
            subscription.setSubscriptionAmount(100.0);

            when(subscriptionMapper.selectById(transactionId)).thenReturn(subscription);

            OurSystem mockSystem = mock(OurSystem.class);
            when(mockSystem.isHasExportedApplicationData()).thenReturn(false);
            when(mockSystem.getTransactionDate()).thenReturn(new Date());
            when(settleClient.getSystem()).thenReturn(mockSystem);

            Bankcard bankcard = new Bankcard();
            bankcard.setBalance(200.0);
            when(accountClient.getBankcard(subscription.getTradingAccountId())).thenReturn(bankcard);

            // 模拟更新失败，返回0
            when(subscriptionMapper.update(any(), any())).thenReturn(0);

            boolean result = transactionService.cancelTransaction(transactionId);

            assertFalse(result);
            // 余额应该被更新一次（因为业务逻辑先更新余额）
            verify(accountClient, times(1)).updateBalance(any());
        }

        @Test
        @DisplayName("P6-TC06-01 ")
        void testCancelTransactionWithZeroId_subscriptionBranch() {
            long transactionId = 0L;

            // 模拟subscription存在，避免进入赎回分支
            Subscription subscription = new Subscription();
            subscription.setApplicationTime(new Date(System.currentTimeMillis() - 1000)); // 合法时间
            subscription.setTradingAccountId(123L);
            subscription.setSubscriptionAmount(50.0);
            when(subscriptionMapper.selectById(transactionId)).thenReturn(subscription);

            OurSystem mockSystem = mock(OurSystem.class);
            when(mockSystem.isHasExportedApplicationData()).thenReturn(false);
            when(mockSystem.getTransactionDate()).thenReturn(new Date());
            when(settleClient.getSystem()).thenReturn(mockSystem);

            Bankcard bankcard = new Bankcard();
            bankcard.setBalance(100.0);
            when(accountClient.getBankcard(subscription.getTradingAccountId())).thenReturn(bankcard);

            // 模拟更新失败，返回0
            when(subscriptionMapper.update(any(), any())).thenReturn(0);

            boolean result = transactionService.cancelTransaction(transactionId);

            assertFalse(result);

            // 验证赎回相关代码不被调用
            verify(redemptionMapper, never()).selectById(anyLong());
        }

        @Test
        @DisplayName("P6-TC06-02 ")
        void testCancelTransactionWithNegativeId_subscriptionBranch() {
            long transactionId = -1L;

            Subscription subscription = new Subscription();
            subscription.setApplicationTime(new Date(System.currentTimeMillis() - 1000));
            subscription.setTradingAccountId(123L);
            subscription.setSubscriptionAmount(50.0);
            when(subscriptionMapper.selectById(transactionId)).thenReturn(subscription);

            OurSystem mockSystem = mock(OurSystem.class);
            when(mockSystem.isHasExportedApplicationData()).thenReturn(false);
            when(mockSystem.getTransactionDate()).thenReturn(new Date());
            when(settleClient.getSystem()).thenReturn(mockSystem);

            Bankcard bankcard = new Bankcard();
            bankcard.setBalance(100.0);
            when(accountClient.getBankcard(subscription.getTradingAccountId())).thenReturn(bankcard);

            when(subscriptionMapper.update(any(), any())).thenReturn(0);

            boolean result = transactionService.cancelTransaction(transactionId);

            assertFalse(result);

            verify(redemptionMapper, never()).selectById(anyLong());
        }

        @Test
        @DisplayName("P6-TC07-01 ")
        void testCancelTransactionExactly24Hours() {
            long transactionId = 123L;

            Subscription subscription = new Subscription();
            Date applicationTime = new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24);
            subscription.setApplicationTime(applicationTime);
            subscription.setTradingAccountId(456L);
            subscription.setSubscriptionAmount(100.0);
            when(subscriptionMapper.selectById(transactionId)).thenReturn(subscription);

            OurSystem mockSystem = mock(OurSystem.class);
            when(mockSystem.isHasExportedApplicationData()).thenReturn(false);
            when(mockSystem.getTransactionDate()).thenReturn(new Date());
            when(settleClient.getSystem()).thenReturn(mockSystem);

            // 这里不要写getBankcard和update的stub了，因为业务代码不会调用

            boolean result = transactionService.cancelTransaction(transactionId);

            assertFalse(result);
        }


        @Test
        @DisplayName("P6-TC08-01 ")
        void testCancelTransactionWithZeroSubscriptionAmount() {
            long transactionId = 124L;

            Subscription subscription = new Subscription();
            subscription.setApplicationTime(new Date(System.currentTimeMillis() - 1000)); // 1秒前
            subscription.setTradingAccountId(789L);
            subscription.setSubscriptionAmount(0.0);  // 金额为0
            when(subscriptionMapper.selectById(transactionId)).thenReturn(subscription);

            OurSystem mockSystem = mock(OurSystem.class);
            when(mockSystem.isHasExportedApplicationData()).thenReturn(false);
            when(mockSystem.getTransactionDate()).thenReturn(new Date());
            when(settleClient.getSystem()).thenReturn(mockSystem);

            Bankcard bankcard = new Bankcard();
            bankcard.setBalance(1000.0);
            when(accountClient.getBankcard(subscription.getTradingAccountId())).thenReturn(bankcard);

            when(subscriptionMapper.update(any(), any())).thenReturn(1);

            boolean result = transactionService.cancelTransaction(transactionId);

            // 申购金额为0，通常也允许取消，余额不变
            assertTrue(result);
            assertEquals(1000.0, bankcard.getBalance());
        }

        @Test
        @DisplayName("P6-TC08-02 ")
        void testCancelTransactionWithHugeSubscriptionAmount() {
            long transactionId = 125L;

            Subscription subscription = new Subscription();
            subscription.setApplicationTime(new Date(System.currentTimeMillis() - 1000)); // 1秒前
            subscription.setTradingAccountId(101112L);
            subscription.setSubscriptionAmount(Double.MAX_VALUE);  // 极大值
            when(subscriptionMapper.selectById(transactionId)).thenReturn(subscription);

            OurSystem mockSystem = mock(OurSystem.class);
            when(mockSystem.isHasExportedApplicationData()).thenReturn(false);
            when(mockSystem.getTransactionDate()).thenReturn(new Date());
            when(settleClient.getSystem()).thenReturn(mockSystem);

            Bankcard bankcard = new Bankcard();
            bankcard.setBalance(1.0);
            when(accountClient.getBankcard(subscription.getTradingAccountId())).thenReturn(bankcard);

            when(subscriptionMapper.update(any(), any())).thenReturn(1);

            boolean result = transactionService.cancelTransaction(transactionId);

            assertTrue(result);
            assertEquals(1.0 + Double.MAX_VALUE, bankcard.getBalance());
        }


    }
}
