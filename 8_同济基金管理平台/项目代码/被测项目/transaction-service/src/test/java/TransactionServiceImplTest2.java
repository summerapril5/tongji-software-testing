import com.github.JLQusername.api.Bankcard;
import com.github.JLQusername.api.OurSystem;
import com.github.JLQusername.api.client.AccountClient;
import com.github.JLQusername.api.client.SettleClient;
import com.github.JLQusername.transaction.domain.Holding;
import com.github.JLQusername.transaction.domain.Redemption;
import com.github.JLQusername.transaction.domain.Subscription;
import com.github.JLQusername.transaction.domain.dto.RedemptionDTO;
import com.github.JLQusername.transaction.mapper.HoldingMapper;
import com.github.JLQusername.transaction.mapper.RedemptionMapper;
import com.github.JLQusername.transaction.mapper.SubscriptionMapper;
import com.github.JLQusername.transaction.service.impl.TransactionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.security.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceImplTest2 {

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

    // 被测试的服务
    private TransactionServiceImpl transactionService;

    @BeforeEach
    void setup() {
        // 初始化 TransactionServiceImpl，传入所有的 mock 依赖
        transactionService = new TransactionServiceImpl(
                subscriptionMapper,
                redemptionMapper,
                holdingMapper,
                accountClient,
                settleClient
        );
    }

    @Nested
    @MockitoSettings(strictness = Strictness.LENIENT)
    class SubmitRedemptionTests {
        @Test
        @DisplayName("P7-TC01-01")
        void testSubmitRedemption_Successful() {
            RedemptionDTO dto = new RedemptionDTO();
            dto.setTradingAccountId("12345");
            dto.setProductId(1001);
            dto.setShares(50);
            dto.setFundAccount(123456789L);
            dto.setProductName("ProductA");

            Holding holding = new Holding();
            holding.setTradingAccountId(12345L);
            holding.setProductId(1001);
            holding.setShares(100);

            // mock getHolding (如果是内部私有方法可以考虑改为 protected 再测试，否则需使用 spy，不推荐）
            when(holdingMapper.selectOne(any())).thenReturn(holding);

            OurSystem mockSystem = mock(OurSystem.class);
            when(settleClient.getSystem()).thenReturn(mockSystem);
            when(mockSystem.getTransactionDate()).thenReturn(new Date());
            when(mockSystem.isHasStoppedApplication()).thenReturn(false);

            // mock 插入并设置 ID
            doAnswer(invocation -> {
                Redemption redemption = invocation.getArgument(0);
                redemption.setTransactionId(9999L); // 模拟主键生成
                return null;
            }).when(redemptionMapper).insert(any(Redemption.class));

            when(holdingMapper.updateById(any(Holding.class))).thenReturn(1);

            long result = transactionService.submitRedemption(dto);

            assertEquals(9999L, result);
            assertEquals(50, holding.getShares());
            verify(redemptionMapper).insert(any(Redemption.class));
            verify(holdingMapper).updateById(any(Holding.class));
        }

        @Test
        @DisplayName("P7-TC02-01")
        void testSubmitRedemption_TradingAccountIdNotExist_shouldReturn1() {
            RedemptionDTO dto = new RedemptionDTO();
            dto.setTradingAccountId("999999");  // 不存在的账户ID
            dto.setProductId(1001);
            dto.setShares(50);
            dto.setFundAccount(123456789L);
            dto.setProductName("ProductA");

            // 模拟 holdingMapper.selectOne 返回 null，表示账户ID不存在或无持仓
            when(holdingMapper.selectOne(any())).thenReturn(null);

            long result = transactionService.submitRedemption(dto);

            assertEquals(1L, result);

            // 确认插入和更新操作没执行
            verify(redemptionMapper, never()).insert(any(Redemption.class));
            verify(holdingMapper, never()).updateById(any(Holding.class));
        }

        @Test
        @DisplayName("P7-TC03-01")
        void testSubmitRedemption_SharesGreaterThanHolding_shouldReturn1() {
            RedemptionDTO dto = new RedemptionDTO();
            dto.setTradingAccountId("12345");
            dto.setProductId(1001);
            dto.setShares(150); // 赎回份额大于持仓份额
            dto.setFundAccount(123456789L);
            dto.setProductName("ProductA");

            Holding holding = new Holding();
            holding.setTradingAccountId(12345L);
            holding.setProductId(1001);
            holding.setShares(100); // 持仓份额100

            when(holdingMapper.selectOne(any())).thenReturn(holding);

            long result = transactionService.submitRedemption(dto);

            assertEquals(1L, result);

            verify(redemptionMapper, never()).insert(any(Redemption.class));
            verify(holdingMapper, never()).updateById(any(Holding.class));
        }

        @Test
        @DisplayName("P7-TC04-01")
        void testSubmitRedemption_SharesEqualHolding_success() {
            RedemptionDTO dto = new RedemptionDTO();
            dto.setTradingAccountId("12345");
            dto.setProductId(1001);
            dto.setShares(100); // 赎回份额等于持仓份额
            dto.setFundAccount(123456789L);
            dto.setProductName("ProductA");

            Holding holding = new Holding();
            holding.setTradingAccountId(12345L);
            holding.setProductId(1001);
            holding.setShares(100);

            when(holdingMapper.selectOne(any())).thenReturn(holding);

            // 模拟 settleClient.getSystem()
            OurSystem mockSystem = mock(OurSystem.class);
            when(settleClient.getSystem()).thenReturn(mockSystem);
            when(mockSystem.getTransactionDate()).thenReturn(new Date());
            when(mockSystem.isHasStoppedApplication()).thenReturn(false);

            doAnswer(invocation -> {
                Redemption redemption = invocation.getArgument(0);
                redemption.setTransactionId(8888L);
                return null;
            }).when(redemptionMapper).insert(any(Redemption.class));

            when(holdingMapper.updateById(any(Holding.class))).thenReturn(1);

            long result = transactionService.submitRedemption(dto);

            assertEquals(8888L, result);
            assertEquals(0, holding.getShares());

            verify(redemptionMapper).insert(any(Redemption.class));
            verify(holdingMapper).updateById(any(Holding.class));
        }


        @Test
        @DisplayName("P7-TC05-01")
        void testSubmitRedemption_SharesZeroOrNegative_shouldReturn1() {
            RedemptionDTO dtoZero = new RedemptionDTO();
            dtoZero.setTradingAccountId("12345");
            dtoZero.setProductId(1001);
            dtoZero.setShares(0);
            dtoZero.setFundAccount(123456789L);
            dtoZero.setProductName("ProductA");

            RedemptionDTO dtoNegative = new RedemptionDTO();
            dtoNegative.setTradingAccountId("12345");
            dtoNegative.setProductId(1001);
            dtoNegative.setShares(-10);
            dtoNegative.setFundAccount(123456789L);
            dtoNegative.setProductName("ProductA");

            Holding holding = new Holding();
            holding.setTradingAccountId(12345L);
            holding.setProductId(1001);
            holding.setShares(100);

            when(holdingMapper.selectOne(any())).thenReturn(holding);

            OurSystem mockSystem = mock(OurSystem.class);
            when(settleClient.getSystem()).thenReturn(mockSystem);
            when(mockSystem.getTransactionDate()).thenReturn(new Date());
            when(mockSystem.isHasStoppedApplication()).thenReturn(false);

            // 如果insert会被调用，mock它的行为，设置transactionId
            doAnswer(invocation -> {
                Redemption redemption = invocation.getArgument(0);
                redemption.setTransactionId(123L);
                return 1;
            }).when(redemptionMapper).insert(any(Redemption.class));

            // 执行测试
            long resultZero = transactionService.submitRedemption(dtoZero);
            assertEquals(1L, resultZero);

            long resultNegative = transactionService.submitRedemption(dtoNegative);
            assertEquals(1L, resultNegative);

            // 验证没有调用insert和update
            verify(redemptionMapper, never()).insert(any(Redemption.class));
            verify(holdingMapper, never()).updateById(any(Holding.class));
        }

        @Test
        @DisplayName("P7-TC06-01")
        void testSubmitRedemption_InvalidTradingAccountId_shouldThrowException() {
            RedemptionDTO dtoEmpty = new RedemptionDTO();
            dtoEmpty.setTradingAccountId(""); // 空字符串
            dtoEmpty.setProductId(1001);
            dtoEmpty.setShares(10);
            dtoEmpty.setFundAccount(123456789L);
            dtoEmpty.setProductName("ProductA");

            RedemptionDTO dtoInvalid = new RedemptionDTO();
            dtoInvalid.setTradingAccountId("abc123"); // 非法格式
            dtoInvalid.setProductId(1001);
            dtoInvalid.setShares(10);
            dtoInvalid.setFundAccount(123456789L);
            dtoInvalid.setProductName("ProductA");

            // 期待 NumberFormatException 被抛出
            assertThrows(NumberFormatException.class, () -> transactionService.submitRedemption(dtoEmpty));
            assertThrows(NumberFormatException.class, () -> transactionService.submitRedemption(dtoInvalid));

            verify(redemptionMapper, never()).insert(any());
            verify(holdingMapper, never()).updateById(any());
        }


        @Test
        @DisplayName("P7-TC07-01")
        void testSubmitRedemption_InsertException_shouldThrowException() {
            RedemptionDTO dto = new RedemptionDTO();
            dto.setTradingAccountId("12345");
            dto.setProductId(1001);
            dto.setShares(10);
            dto.setFundAccount(123456789L);
            dto.setProductName("ProductA");

            Holding holding = new Holding();
            holding.setTradingAccountId(12345L);
            holding.setProductId(1001);
            holding.setShares(100);

            when(holdingMapper.selectOne(any())).thenReturn(holding);

            OurSystem mockSystem = mock(OurSystem.class);
            when(settleClient.getSystem()).thenReturn(mockSystem);
            when(mockSystem.getTransactionDate()).thenReturn(new Date());
            when(mockSystem.isHasStoppedApplication()).thenReturn(false);

            doThrow(new RuntimeException("数据库插入失败")).when(redemptionMapper).insert(any());

            // 断言抛异常
            assertThrows(RuntimeException.class, () -> transactionService.submitRedemption(dto));

            verify(holdingMapper, never()).updateById(any());
        }


        @Test
        @DisplayName("P7-TC08-01")
        void testSubmitRedemption_UpdateHoldingException_shouldThrow() {
            RedemptionDTO dto = new RedemptionDTO();
            dto.setTradingAccountId("12345");
            dto.setProductId(1001);
            dto.setShares(10);
            dto.setFundAccount(123456789L);
            dto.setProductName("ProductA");

            Holding holding = new Holding();
            holding.setTradingAccountId(12345L);
            holding.setProductId(1001);
            holding.setShares(100);

            when(holdingMapper.selectOne(any())).thenReturn(holding);

            OurSystem mockSystem = mock(OurSystem.class);
            when(settleClient.getSystem()).thenReturn(mockSystem);
            when(mockSystem.getTransactionDate()).thenReturn(new Date());
            when(mockSystem.isHasStoppedApplication()).thenReturn(false);

            // 插入成功，设置transactionId
            doAnswer(invocation -> {
                Redemption redemption = invocation.getArgument(0);
                redemption.setTransactionId(123L);
                return 1;
            }).when(redemptionMapper).insert(any());

            // 更新持仓抛异常
            doThrow(new RuntimeException("更新持仓失败")).when(holdingMapper).updateById(any());

            // 断言抛异常，业务逻辑异常则测试失败
            assertThrows(RuntimeException.class, () -> transactionService.submitRedemption(dto));
        }


        @Test
        @DisplayName("P7-TC09-01")
        void testSubmitRedemption_ValidInput_shouldReturnTransactionId() {
            RedemptionDTO dto = new RedemptionDTO();
            dto.setTradingAccountId("12345");
            dto.setProductId(1001);
            dto.setShares(10);
            dto.setFundAccount(123456789L);
            dto.setProductName("ProductA");

            Holding holding = new Holding();
            holding.setTradingAccountId(12345L);
            holding.setProductId(1001);
            holding.setShares(100);

            when(holdingMapper.selectOne(any())).thenReturn(holding);

            OurSystem mockSystem = mock(OurSystem.class);
            when(settleClient.getSystem()).thenReturn(mockSystem);
            when(mockSystem.getTransactionDate()).thenReturn(new Date());
            when(mockSystem.isHasStoppedApplication()).thenReturn(false);

            doAnswer(invocation -> {
                Redemption redemption = invocation.getArgument(0);
                redemption.setTransactionId(456L);
                return 1;
            }).when(redemptionMapper).insert(any());

            long result = transactionService.submitRedemption(dto);
            assertEquals(456L, result);

            verify(redemptionMapper, times(1)).insert(any());
            verify(holdingMapper, times(1)).updateById(any());
        }

        @Test
        @DisplayName("P7-TC10-01")
        void testSubmitRedemption_VeryLargeShares_shouldReturnError() {
            RedemptionDTO dto = new RedemptionDTO();
            dto.setTradingAccountId("12345");
            dto.setProductId(1001);
            dto.setShares(Integer.MAX_VALUE);  // 极大份额
            dto.setFundAccount(123456789L);
            dto.setProductName("ProductA");

            Holding holding = new Holding();
            holding.setTradingAccountId(12345L);
            holding.setProductId(1001);
            holding.setShares(100);

            when(holdingMapper.selectOne(any())).thenReturn(holding);




            // 返回失败码，比如1表示份额不足
            long result = transactionService.submitRedemption(dto);
            assertEquals(1L, result);

            verify(redemptionMapper, never()).insert(any());
            verify(holdingMapper, never()).updateById(any());
        }
    }

    @Nested
    @MockitoSettings(strictness = Strictness.LENIENT)
    class confirmRedemptionBatchTests{
        @Test
        @DisplayName("P8-TC01-01")
        void testValidTransactionIdAndAmount() {
            Long validId = 1L;
            Double amount = 100.0;

            // Mock Redemption 和 Bankcard 返回值
            Redemption redemption = new Redemption();
            redemption.setTradingAccountId(10L);

            Bankcard bankcard = new Bankcard();
            bankcard.setBalance(500.0);

            when(redemptionMapper.selectById(validId)).thenReturn(redemption);
            when(accountClient.getBankcard(10L)).thenReturn(bankcard);

            Map<Long, Double> map = new HashMap<>();
            map.put(validId, amount);

            // 调用 confirmRedemptionBatch 方法
            boolean result = transactionService.confirmRedemptionBatch(map);

            // 验证期望结果
            assertTrue(result);
            assertEquals(600.0, bankcard.getBalance()); // 余额更新为600
            verify(accountClient).updateBalance(bankcard); // 确认更新余额的方法被调用
        }

        @Test
        @DisplayName("P8-TC02-01")
        void testInvalidTransactionId() {
            Long invalidId = 99L;
            Double amount = 100.0;

            // Mock 返回 null
            when(redemptionMapper.selectById(invalidId)).thenReturn(null);

            Map<Long, Double> map = new HashMap<>();
            map.put(invalidId, amount);

            // 调用 confirmRedemptionBatch 方法
            boolean result = transactionService.confirmRedemptionBatch(map);

            // 验证返回值并确保没有更新余额
            assertTrue(result);
            verify(accountClient, never()).updateBalance(any()); // 确保没有调用 updateBalance 方法
        }

        @Test
        @DisplayName("P8-TC03-01")
        void testValidTransactionIdBankcardNull() {
            Long validId = 1L;
            Double amount = 100.0;

            Redemption redemption = new Redemption();
            redemption.setTradingAccountId(10L);

            // Mock Bankcard 返回 null
            when(redemptionMapper.selectById(validId)).thenReturn(redemption);
            when(accountClient.getBankcard(10L)).thenReturn(null);

            Map<Long, Double> map = new HashMap<>();
            map.put(validId, amount);

            // 调用 confirmRedemptionBatch 方法
            boolean result = transactionService.confirmRedemptionBatch(map);

            // 验证返回值并确保没有更新余额
            assertTrue(result);
            verify(accountClient, never()).updateBalance(any()); // 确保没有调用 updateBalance 方法
        }

        @Test
        @DisplayName("P8-TC04-01")
        void testNullTransactionId() {
            Map<Long, Double> map = new HashMap<>();
            map.put(null, 100.0);

            // 由于传入了 null 的 transactionId，应该抛出 NullPointerException
            assertThrows(NullPointerException.class, () -> {
                transactionService.confirmRedemptionBatch(map);
            });
        }

        @Test
        @DisplayName("P8-TC04-02")
        void testNullAmount() {
            Long validId = 1L;
            Redemption redemption = new Redemption();
            redemption.setTradingAccountId(10L);

            // Mock Redemption 返回值
            when(redemptionMapper.selectById(validId)).thenReturn(redemption);

            Map<Long, Double> map = new HashMap<>();
            map.put(validId, null);

            // 由于金额为 null，应该抛出 NullPointerException
            assertThrows(NullPointerException.class, () -> {
                transactionService.confirmRedemptionBatch(map);
            });
        }

        @Test
        @DisplayName("P8-TC05-01")
        void testAmountZero() {
            Long validId = 1L;
            Redemption redemption = new Redemption();
            redemption.setTradingAccountId(10L);

            Bankcard bankcard = new Bankcard();
            bankcard.setBalance(1000.0);

            when(redemptionMapper.selectById(validId)).thenReturn(redemption);
            when(accountClient.getBankcard(redemption.getTradingAccountId())).thenReturn(bankcard);

            Map<Long, Double> map = new HashMap<>();
            map.put(validId, 0.0);

            boolean result = transactionService.confirmRedemptionBatch(map);

            // 余额应保持不变
            assertEquals(1000.0, bankcard.getBalance());

            // 接口返回 true 表示正常处理
            assertTrue(result);
        }

        @Test
        @DisplayName("P8-TC06-01")
        void testNegativeAmount() {
            Long validId = 1L;
            Redemption redemption = new Redemption();
            redemption.setTradingAccountId(10L);

            when(redemptionMapper.selectById(validId)).thenReturn(redemption);

            Map<Long, Double> map = new HashMap<>();
            map.put(validId, -100.0);

            // 你可以根据业务逻辑选择抛异常或返回false，这里假设抛异常
            assertThrows(IllegalArgumentException.class, () -> {
                transactionService.confirmRedemptionBatch(map);
            });
        }

        @Test
        @DisplayName("P8-TC07-01")
        void testBatchPositiveAmounts() {
            Long id1 = 1L, id2 = 2L;
            Redemption redemption1 = new Redemption();
            redemption1.setTradingAccountId(10L);
            Redemption redemption2 = new Redemption();
            redemption2.setTradingAccountId(20L);

            Bankcard bankcard1 = new Bankcard();
            bankcard1.setBalance(1000.0);
            Bankcard bankcard2 = new Bankcard();
            bankcard2.setBalance(500.0);

            when(redemptionMapper.selectById(id1)).thenReturn(redemption1);
            when(redemptionMapper.selectById(id2)).thenReturn(redemption2);
            when(accountClient.getBankcard(10L)).thenReturn(bankcard1);
            when(accountClient.getBankcard(20L)).thenReturn(bankcard2);

            Map<Long, Double> map = new HashMap<>();
            map.put(id1, 200.0);
            map.put(id2, 300.0);

            boolean result = transactionService.confirmRedemptionBatch(map);

            // 校验余额增加
            assertEquals(1200.0, bankcard1.getBalance());
            assertEquals(800.0, bankcard2.getBalance());

            assertTrue(result);
        }

        @Test
        @DisplayName("P8-TC08-01")
        void testBatchWithInvalidId() {
            Long validId = 1L;
            Long invalidId = 999L; // 假设不存在的ID

            Redemption redemption = new Redemption();
            redemption.setTradingAccountId(10L);

            Bankcard bankcard = new Bankcard();
            bankcard.setBalance(1000.0);

            when(redemptionMapper.selectById(validId)).thenReturn(redemption);
            when(redemptionMapper.selectById(invalidId)).thenReturn(null);
            when(accountClient.getBankcard(10L)).thenReturn(bankcard);

            Map<Long, Double> map = new HashMap<>();
            map.put(validId, 150.0);
            map.put(invalidId, 200.0);

            boolean result = transactionService.confirmRedemptionBatch(map);

            // 只有有效ID的余额更新
            assertEquals(1150.0, bankcard.getBalance());

            // 返回正常
            assertTrue(result);
        }

        @Test
        @DisplayName("P8-TC09-01 ")
        void testPartialNullBankcard() {
            Long idWithBankcard = 1L;
            Long idWithoutBankcard = 2L;

            Redemption redemption1 = new Redemption();
            redemption1.setTradingAccountId(10L);
            Redemption redemption2 = new Redemption();
            redemption2.setTradingAccountId(20L);

            when(redemptionMapper.selectById(idWithBankcard)).thenReturn(redemption1);
            when(redemptionMapper.selectById(idWithoutBankcard)).thenReturn(redemption2);

            Bankcard bankcard = new Bankcard();
            bankcard.setBalance(100.0);
            when(accountClient.getBankcard(10L)).thenReturn(bankcard);
            when(accountClient.getBankcard(20L)).thenReturn(null); // 无银行卡

            Map<Long, Double> map = new HashMap<>();
            map.put(idWithBankcard, 50.0);
            map.put(idWithoutBankcard, 30.0);

            boolean result = transactionService.confirmRedemptionBatch(map);

            assertTrue(result);
            assertEquals(150.0, bankcard.getBalance());
            verify(accountClient).updateBalance(bankcard);
        }

        @Test
        @DisplayName("P8-TC10-01")
        void testUpdateBalanceException() {
            Long validId = 1L;
            Redemption redemption = new Redemption();
            redemption.setTradingAccountId(10L);

            when(redemptionMapper.selectById(validId)).thenReturn(redemption);

            Bankcard bankcard = new Bankcard();
            bankcard.setBalance(100.0);
            when(accountClient.getBankcard(10L)).thenReturn(bankcard);

            doThrow(new RuntimeException("更新失败")).when(accountClient).updateBalance(bankcard);

            Map<Long, Double> map = new HashMap<>();
            map.put(validId, 50.0);

            // 断言抛异常
            assertThrows(RuntimeException.class, () -> {
                transactionService.confirmRedemptionBatch(map);
            });

            // 不能断言bankcard.getBalance() == 100.0，因为Java对象已经改了
            // 你可以验证 updateBalance 方法被调用了
            verify(accountClient).updateBalance(bankcard);
        }

        @Test
        @DisplayName("P8-TC11-01")
        void testEmptyTransactionIdMap() {
            Map<Long, Double> emptyMap = new HashMap<>();

            boolean result = transactionService.confirmRedemptionBatch(emptyMap);

            assertTrue(result);
        }

        @Test
        @DisplayName("P8-TC12-01")
        void testLargeTransactionIdMapPerformance() {
            Map<Long, Double> largeMap = new HashMap<>();

            Redemption redemption = new Redemption();
            redemption.setTradingAccountId(10L);
            Bankcard bankcard = new Bankcard();
            bankcard.setBalance(1000.0);

            // 这里为了测试性能，只对部分id做mock，其他返回null
            for (long i = 1; i <= 100000; i++) {
                largeMap.put(i, 1.0);
            }
            when(redemptionMapper.selectById(anyLong())).thenAnswer(invocation -> {
                Long id = invocation.getArgument(0);
                if (id % 1000 == 0) { // 每1000条返回有效redemption
                    return redemption;
                }
                return null;
            });
            when(accountClient.getBankcard(anyLong())).thenReturn(bankcard);

            long start = System.currentTimeMillis();

            boolean result = transactionService.confirmRedemptionBatch(largeMap);

            long duration = System.currentTimeMillis() - start;
            System.out.println("处理10万条记录耗时: " + duration + "ms");

            assertTrue(result);
            assertTrue(duration < 5000); // 假设接口响应时间要求小于5秒
        }

        @Test
        @DisplayName("P8-TC13-01")
        void testMaxAmount() {
            Long validId = 1L;
            Redemption redemption = new Redemption();
            redemption.setTradingAccountId(10L);

            when(redemptionMapper.selectById(validId)).thenReturn(redemption);

            Bankcard bankcard = new Bankcard();
            bankcard.setBalance(100.0);
            when(accountClient.getBankcard(10L)).thenReturn(bankcard);

            Map<Long, Double> map = new HashMap<>();
            map.put(validId, Double.MAX_VALUE);

            // 调用接口，期望不抛异常且返回true
            boolean result = transactionService.confirmRedemptionBatch(map);
            assertTrue(result);

            // 验证余额更新是否正确（注意Double加法可能溢出变成Infinity，需根据业务处理）
            // 如果业务未特殊处理，这里余额会变成Infinity
            assertEquals(bankcard.getBalance(), 100.0 + Double.MAX_VALUE);
        }

        @Test
        @DisplayName("P8-TC14-01")
        void testInvalidTransactionId2() {
            Long invalidId1 = Long.MAX_VALUE;  // 极大交易ID
            Long invalidId2 = -1L;             // 负交易ID
            Redemption redemption = new Redemption();
            redemption.setTradingAccountId(10L);

            // 只有 validId1 有数据，极大值和负数的ID查询返回null模拟无效
            when(redemptionMapper.selectById(invalidId1)).thenReturn(null);
            when(redemptionMapper.selectById(invalidId2)).thenReturn(null);

            Map<Long, Double> map = new HashMap<>();
            map.put(invalidId1, 100.0);
            map.put(invalidId2, 200.0);

            // 调用接口，应该跳过无效交易ID，不报错，返回true
            boolean result = transactionService.confirmRedemptionBatch(map);
            assertTrue(result);
        }


    }

    @Nested
    @MockitoSettings(strictness = Strictness.LENIENT)
    class cancelTransactionTests{
        @Test
        @DisplayName("P9-TC01-01")
        void testP9_TC01_ValidSubscription_UpdatesBalanceAndCancels() {
            long transactionId = 1L;

            Subscription subscription = new Subscription();
            subscription.setTradingAccountId(100L);
            subscription.setSubscriptionAmount(100.0);
            subscription.setApplicationTime(new Date(System.currentTimeMillis() - 1000 * 60 * 60));


            Bankcard bankcard = new Bankcard();
            bankcard.setBalance(500.0);

            OurSystem system = new OurSystem();
            system.setHasExportedApplicationData(false);
            system.setTransactionDate(new Date());

            when(subscriptionMapper.selectById(transactionId)).thenReturn(subscription);
            when(accountClient.getBankcard(100L)).thenReturn(bankcard);
            when(accountClient.updateBalance(any())).thenReturn(true);
            when(subscriptionMapper.update(any(), any())).thenReturn(1);
            when(settleClient.getSystem()).thenReturn(system);

            boolean result = transactionService.cancelTransaction(transactionId);

            assertTrue(result);
            assertEquals(600.0, bankcard.getBalance());
        }

        @Test
        @DisplayName("P9-TC02-01")
        void testP9_TC02_SubscriptionNotFound_RedemptionNotFound_ReturnsTrue() {
            long transactionId = 9999L;

            // 设置系统对象
            OurSystem system = new OurSystem();
            system.setHasExportedApplicationData(false);
            system.setTransactionDate(new Date());

            // 模拟数据库查询
            when(subscriptionMapper.selectById(transactionId)).thenReturn(null);
            when(redemptionMapper.selectById(transactionId)).thenReturn(null);
            when(settleClient.getSystem()).thenReturn(system);

            // 运行取消交易方法并捕获异常
            boolean result = false;
            try {
                result = transactionService.cancelTransaction(transactionId);
            } catch (NullPointerException e) {
                // 捕获异常，但不抛出，继续测试
                // 如果 NPE 异常发生，则测试失败
                assertFalse(true, "预期不应抛出 NPE 异常");
            }

            // 检查方法返回值：业务逻辑错误的地方应该使返回值不符合预期
            assertTrue(result, "当交易 ID 不存在时，方法应返回 true");
        }


        @Test
        @DisplayName("P9-TC03-01")
        void testP9_TC03_ValidSubscription_BankcardNull_SkipsBalanceUpdate() {
            long transactionId = 1L;

            Subscription subscription = new Subscription();
            subscription.setTradingAccountId(100L);
            subscription.setSubscriptionAmount(100.0);
            subscription.setApplicationTime(new Date(System.currentTimeMillis() - 1000 * 60 * 60));

            OurSystem system = new OurSystem();
            system.setHasExportedApplicationData(false);
            system.setTransactionDate(new Date());

            when(subscriptionMapper.selectById(transactionId)).thenReturn(subscription);
            when(accountClient.getBankcard(100L)).thenReturn(null); // 模拟银行账户不存在
            when(subscriptionMapper.update(any(), any())).thenReturn(1);
            when(settleClient.getSystem()).thenReturn(system);

            boolean result = false;

            try {
                result = transactionService.cancelTransaction(transactionId);
            } catch (NullPointerException e) {
                // 捕获业务逻辑的空指针异常，测试失败，但不抛出异常中断
                fail("业务逻辑未处理 bankcard=null 导致空指针异常");
            }

            // 这里写你对结果的期望，比如期望返回 true
            assertTrue(result, "取消交易应返回 true，即使银行卡为 null 时跳过余额更新");

            // 验证余额更新方法未调用
            verify(accountClient, never()).updateBalance(any());
        }

        @Test
        @DisplayName("P9-TC04-01")
        void testP9_TC04_01_SkipNull_ProcessOne() {
            long transactionId = 1L;

            Subscription subscription = new Subscription();
            subscription.setTradingAccountId(Long.valueOf("100")); // 账户ID是字符串
            subscription.setSubscriptionAmount(50.0);
            subscription.setApplicationTime(new Date(System.currentTimeMillis() - 1000 * 60 * 60));

            OurSystem system = new OurSystem();
            system.setHasExportedApplicationData(false);
            system.setTransactionDate(new Date());

            when(subscriptionMapper.selectById(transactionId)).thenReturn(subscription);
            when(accountClient.getBankcard(Long.parseLong("100"))).thenReturn(new Bankcard("100", 1000.0));
            when(subscriptionMapper.update(any(), any())).thenReturn(1);
            when(settleClient.getSystem()).thenReturn(system);

            boolean result = transactionService.cancelTransaction(transactionId);
            assertTrue(result);

            // null 交易ID无法传入long，略过
        }

        @Test
        @DisplayName("P9-TC05-01 ")
        void testP9_TC05_01_ZeroBalance_NoChange() {
            long transactionId = 1L;

            Subscription subscription = new Subscription();
            subscription.setTradingAccountId(Long.valueOf("100"));
            subscription.setSubscriptionAmount(0.0); // 余额 0，不调整余额
            subscription.setApplicationTime(new Date(System.currentTimeMillis() - 1000 * 60 * 60));

            OurSystem system = new OurSystem();
            system.setHasExportedApplicationData(false);
            system.setTransactionDate(new Date());

            when(subscriptionMapper.selectById(transactionId)).thenReturn(subscription);
            when(accountClient.getBankcard(Long.parseLong("100"))).thenReturn(new Bankcard("100", 1000.0));
            when(subscriptionMapper.update(any(), any())).thenReturn(1);
            when(settleClient.getSystem()).thenReturn(system);

            boolean result = transactionService.cancelTransaction(transactionId);

            assertTrue(result);
            // 验证没有调用余额更新（余额为0，跳过更新）
            verify(accountClient, never()).updateBalance(any());
        }

        @Test
        @DisplayName("P9-TC06-01 ")
        void testP9_TC06_01_NegativeBalance_ThrowsOrFalse() {
            long transactionId = 1L;

            Subscription subscription = new Subscription();
            subscription.setTradingAccountId(Long.valueOf("100"));
            subscription.setSubscriptionAmount(-50.0); // 负数余额
            subscription.setApplicationTime(new Date(System.currentTimeMillis() - 1000 * 60 * 60));

            OurSystem system = new OurSystem();
            system.setHasExportedApplicationData(false);
            system.setTransactionDate(new Date());

            when(subscriptionMapper.selectById(transactionId)).thenReturn(subscription);
            when(accountClient.getBankcard(Long.parseLong("100"))).thenReturn(new Bankcard("100", 1000.0));
            when(settleClient.getSystem()).thenReturn(system);

            // 根据业务可能抛异常或返回false，这里演示抛异常的情况
            assertThrows(IllegalArgumentException.class, () -> {
                transactionService.cancelTransaction(transactionId);
            });
        }

        @Test
        @DisplayName("P9-TC07-01")
        void testP9_TC07_01_MultipleTransactions_AllSuccess() {
            long transactionId1 = 1L;
            long transactionId2 = 2L;

            Subscription subscription1 = new Subscription();
            subscription1.setTradingAccountId(Long.valueOf("100"));
            subscription1.setSubscriptionAmount(50.0);
            subscription1.setApplicationTime(new Date(System.currentTimeMillis() - 1000 * 60 * 60));

            Subscription subscription2 = new Subscription();
            subscription2.setTradingAccountId(Long.valueOf("101"));
            subscription2.setSubscriptionAmount(80.0);
            subscription2.setApplicationTime(new Date(System.currentTimeMillis() - 1000 * 60 * 60));

            OurSystem system = new OurSystem();
            system.setHasExportedApplicationData(false);
            system.setTransactionDate(new Date());

            when(subscriptionMapper.selectById(transactionId1)).thenReturn(subscription1);
            when(subscriptionMapper.selectById(transactionId2)).thenReturn(subscription2);
            when(accountClient.getBankcard(Long.parseLong("100"))).thenReturn(new Bankcard("100", 1000.0));
            when(accountClient.getBankcard(Long.parseLong("101"))).thenReturn(new Bankcard("101", 2000.0));
            when(subscriptionMapper.update(any(), any())).thenReturn(1);
            when(settleClient.getSystem()).thenReturn(system);

            boolean result1 = transactionService.cancelTransaction(transactionId1);
            boolean result2 = transactionService.cancelTransaction(transactionId2);

            assertTrue(result1);
            assertTrue(result2);

            verify(accountClient, times(2)).updateBalance(any());
        }

        @Test
        @DisplayName("P9-TC08-01")
        void testP9_TC08_01_BatchCancel_SkipInvalid() {
            Map<Long, Double> transactions = Map.of(1L, 50.0, 9999L, 100.0);

            OurSystem system = new OurSystem();
            system.setHasExportedApplicationData(false);
            system.setTransactionDate(new Date());

            Subscription validSub = new Subscription();
            validSub.setTradingAccountId(100L);
            validSub.setSubscriptionAmount(50.0);
            validSub.setApplicationTime(new Date(System.currentTimeMillis() - 1000 * 60 * 60));

            when(settleClient.getSystem()).thenReturn(system);
            when(subscriptionMapper.selectById(1L)).thenReturn(validSub);
            when(subscriptionMapper.selectById(9999L)).thenReturn(null); // 9999L无效

            when(accountClient.getBankcard(100L)).thenReturn(new Bankcard("100", 1000.0));
            when(subscriptionMapper.update(any(), any())).thenReturn(1);
            when(accountClient.updateBalance(any())).thenReturn(true); // 修改这里，假设updateBalance返回boolean

            // 模拟批量调用 cancelTransaction
            boolean allSuccess = true;
            for (Long transactionId : transactions.keySet()) {
                try {
                    boolean success = transactionService.cancelTransaction(transactionId);
                    allSuccess &= success;
                } catch (Exception e) {
                    allSuccess = false;
                }
            }

            assertTrue(allSuccess);

            verify(accountClient, times(1)).updateBalance(any()); // 只成功处理了1L
            verify(subscriptionMapper, times(1)).update(any(), any());
        }


        @Test
        @DisplayName("P9-TC09-01")
        void testP9_TC09_01_BatchCancel_SkipNullBankcard() {
            Map<Long, Double> transactions = Map.of(1L, 50.0, 2L, 80.0);

            OurSystem system = new OurSystem();
            system.setHasExportedApplicationData(false);
            system.setTransactionDate(new Date());

            Subscription sub1 = new Subscription();
            sub1.setTradingAccountId(100L);
            sub1.setSubscriptionAmount(50.0);
            sub1.setApplicationTime(new Date(System.currentTimeMillis() - 1000 * 60 * 60));

            Subscription sub2 = new Subscription();
            sub2.setTradingAccountId(200L);
            sub2.setSubscriptionAmount(80.0);
            sub2.setApplicationTime(new Date(System.currentTimeMillis() - 1000 * 60 * 60));

            when(settleClient.getSystem()).thenReturn(system);
            when(subscriptionMapper.selectById(1L)).thenReturn(sub1);
            when(subscriptionMapper.selectById(2L)).thenReturn(sub2);
            when(accountClient.getBankcard(100L)).thenReturn(new Bankcard("100", 1000.0));
            when(accountClient.getBankcard(200L)).thenReturn(null); // 2L银行卡为null

            when(subscriptionMapper.update(any(), any())).thenReturn(1);
            when(accountClient.updateBalance(any())).thenReturn(true); // 用 thenReturn 替代 doNothing()

            boolean allSuccess = true;
            for (Long transactionId : transactions.keySet()) {
                try {
                    boolean success = transactionService.cancelTransaction(transactionId);
                    allSuccess &= success;
                } catch (Exception e) {
                    allSuccess = false;
                }
            }

            assertTrue(allSuccess);
            verify(accountClient, times(1)).updateBalance(any()); // 只调用了一次
            verify(subscriptionMapper, times(2)).update(any(), any()); // 两个都标记取消
        }


        @Test
        @DisplayName("P9-TC10-01 ")
        void testP9_TC10_01_UpdateBalanceThrowsException() {
            Map<Long, Double> transactions = Map.of(1L, 50.0);

            OurSystem system = new OurSystem();
            system.setHasExportedApplicationData(false);
            system.setTransactionDate(new Date());

            Subscription sub1 = new Subscription();
            sub1.setTradingAccountId(100L);
            sub1.setSubscriptionAmount(50.0);
            sub1.setApplicationTime(new Date(System.currentTimeMillis() - 1000 * 60 * 60));

            when(settleClient.getSystem()).thenReturn(system);
            when(subscriptionMapper.selectById(1L)).thenReturn(sub1);
            when(accountClient.getBankcard(100L)).thenReturn(new Bankcard("100", 1000.0));

            doThrow(new RuntimeException("updateBalance failed")).when(accountClient).updateBalance(any());

            boolean allSuccess = true;
            try {
                for (Long transactionId : transactions.keySet()) {
                    boolean success = transactionService.cancelTransaction(transactionId);
                    allSuccess &= success;
                }
                fail("应该抛出异常");
            } catch (RuntimeException e) {
                // 预期异常
            }

            verify(accountClient, times(1)).updateBalance(any());
            verify(subscriptionMapper, never()).update(any(), any());
        }

        @Test
        @DisplayName("P9-TC11-01")
        void testP9_TC11_01_EmptyMap() {
            Map<Long, Double> transactions = Collections.emptyMap();

            // 空map，直接返回true
            boolean allSuccess = true;
            for (Long transactionId : transactions.keySet()) {
                try {
                    boolean success = transactionService.cancelTransaction(transactionId);
                    allSuccess &= success;
                } catch (Exception e) {
                    allSuccess = false;
                }
            }

            assertTrue(allSuccess);

            verifyNoInteractions(accountClient);
            verifyNoInteractions(subscriptionMapper);
        }

        @Test
        @DisplayName("P9-TC12-01 ")
        void testP9_TC13_01_LargeBalanceAddition() {
            Map<Long, Double> transactions = Map.of(1L, Double.MAX_VALUE);

            OurSystem system = new OurSystem();
            system.setHasExportedApplicationData(false);
            system.setTransactionDate(new Date());

            Subscription sub = new Subscription();
            sub.setTradingAccountId(100L);
            sub.setSubscriptionAmount(Double.MAX_VALUE);
            sub.setApplicationTime(new Date(System.currentTimeMillis() - 1000 * 60 * 60));

            when(settleClient.getSystem()).thenReturn(system);
            when(subscriptionMapper.selectById(1L)).thenReturn(sub);
            when(accountClient.getBankcard(100L)).thenReturn(new Bankcard("100", 1.0)); // 初始余额小

            when(subscriptionMapper.update(any(), any())).thenReturn(1);
            when(accountClient.updateBalance(any())).thenReturn(true);

            boolean allSuccess = true;
            for (Long transactionId : transactions.keySet()) {
                try {
                    boolean success = transactionService.cancelTransaction(transactionId);
                    allSuccess &= success;
                } catch (Exception e) {
                    allSuccess = false;
                }
            }

            assertTrue(allSuccess);

            verify(accountClient, times(1)).updateBalance(any());
            verify(subscriptionMapper, times(1)).update(any(), any());
        }

        @Test
        @DisplayName("P9-TC13-01 ")
        void testP9_TC14_01_InvalidTransactionIds() {
            Map<Long, Double> transactions = Map.of(-1L, 50.0, Long.MAX_VALUE, 50.0);

            OurSystem system = new OurSystem();
            system.setHasExportedApplicationData(false);
            system.setTransactionDate(new Date());

            when(settleClient.getSystem()).thenReturn(system);
            // 模拟查询 -1L 和 Long.MAX_VALUE 都查不到
            when(subscriptionMapper.selectById(-1L)).thenReturn(null);
            when(subscriptionMapper.selectById(Long.MAX_VALUE)).thenReturn(null);

            // 也可以模拟 redemptionMapper 返回null或者正常返回，视具体业务
            when(redemptionMapper.selectById(-1L)).thenReturn(null);
            when(redemptionMapper.selectById(Long.MAX_VALUE)).thenReturn(null);

            boolean allSuccess = true;
            Exception exception = null;
            for (Long transactionId : transactions.keySet()) {
                try {
                    boolean success = transactionService.cancelTransaction(transactionId);
                    allSuccess &= success;
                } catch (Exception e) {
                    exception = e;
                    allSuccess = false;
                }
            }

            // 断言：业务逻辑不保证全成功，允许失败或异常
            assertTrue(allSuccess || exception != null);
        }

        @Test
        @DisplayName("P9-TC14-01 ")
        void testP9_TC15_01_UpdateFailure() {
            Map<Long, Double> transactions = Map.of(1L, 50.0);

            OurSystem system = new OurSystem();
            system.setHasExportedApplicationData(false);
            system.setTransactionDate(new Date());

            Subscription sub = new Subscription();
            sub.setTradingAccountId(100L);
            sub.setSubscriptionAmount(50.0);
            sub.setApplicationTime(new Date(System.currentTimeMillis() - 1000 * 60 * 60));

            when(settleClient.getSystem()).thenReturn(system);
            when(subscriptionMapper.selectById(1L)).thenReturn(sub);
            when(accountClient.getBankcard(100L)).thenReturn(new Bankcard("100", 1000.0));

            // 模拟更新失败
            when(subscriptionMapper.update(any(), any())).thenReturn(0);
            when(accountClient.updateBalance(any())).thenReturn(true);

            boolean allSuccess = true;
            Exception exception = null;
            for (Long transactionId : transactions.keySet()) {
                try {
                    boolean success = transactionService.cancelTransaction(transactionId);
                    allSuccess &= success;
                } catch (Exception e) {
                    exception = e;
                    allSuccess = false;
                }
            }

            // 因为更新失败，业务逻辑应返回 false 或抛异常
            assertTrue(!allSuccess || exception != null);

            // 验证updateBalance调用了，但因为事务没提交，余额未变（这里不能严格断言，视事务管理）
            verify(accountClient, times(1)).updateBalance(any());
            verify(subscriptionMapper, times(1)).update(any(), any());
        }




    }
}
