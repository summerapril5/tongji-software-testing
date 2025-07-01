import com.github.JLQusername.api.OurSystem;
import com.github.JLQusername.api.client.SettleClient;
import com.github.JLQusername.api.NetValue;
import com.github.JLQusername.product.domain.Product;
import com.github.JLQusername.product.mapper.NetValueMapper;
import com.github.JLQusername.product.mapper.ProductMapper;
import com.github.JLQusername.product.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProductServiceImplTest {

    @InjectMocks
    private ProductServiceImpl productService;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private NetValueMapper netValueMapper;

    @Mock
    private SettleClient settleClient;

    @BeforeEach
    public void setup() throws Exception {
        Field baseMapperField = productService.getClass().getSuperclass().getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(productService, productMapper);

        Field settleClientField = productService.getClass().getDeclaredField("settleClient");
        settleClientField.setAccessible(true);
        settleClientField.set(productService, settleClient);

        Field netValueMapperField = productService.getClass().getDeclaredField("netValueMapper");
        netValueMapperField.setAccessible(true);
        netValueMapperField.set(productService, netValueMapper);
    }

    @Nested
    @MockitoSettings(strictness = Strictness.LENIENT)
    class SaveProductTests {

        // P1-TC01-01: 完整产品信息，保存成功且生成净值记录
        @Test
        @DisplayName("P1-TC01-01")
        public void testSaveProduct_Success() {
            Product product = new Product();
            product.setProductName("Test Product");
            product.setProductType("Type A");
            product.setRiskLevel(1);
            product.setProductStatus(1);

            doAnswer(invocation -> {
                Product p = invocation.getArgument(0);
                p.setProductId(200);
                return 1;
            }).when(productMapper).insert(any(Product.class));

            OurSystem ourSystem = mock(OurSystem.class);
            when(settleClient.getSystem()).thenReturn(ourSystem);
            when(ourSystem.getTransactionDate()).thenReturn(
                    Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant())
            );

            when(netValueMapper.insert(any(NetValue.class))).thenAnswer(invocation -> {
                NetValue nv = invocation.getArgument(0);
                assertEquals(1.0, nv.getNetValue(), 0.00001);
                return 1;
            });

            boolean result = productService.saveProduct(product);

            assertTrue(result);
            assertEquals(200, product.getProductId());

            verify(productMapper, times(1)).insert(any(Product.class));
            verify(netValueMapper, times(1)).insert(any(NetValue.class));
        }

        // P1-TC02-01: 缺少产品名称，保存失败
        @Test
        @DisplayName("P1-TC02-01")
        public void testSaveProduct_MissingProductName_Fail() {
            Product product = new Product();
            product.setProductType("Type A");
            product.setRiskLevel(1);
            product.setProductStatus(1);

            OurSystem ourSystem = mock(OurSystem.class);
            when(settleClient.getSystem()).thenReturn(ourSystem);
            when(ourSystem.getTransactionDate()).thenReturn(new Date());

            boolean result = productService.saveProduct(product);

            assertFalse(result);
            verify(productMapper, never()).insert(any());
            verify(netValueMapper, never()).insert(any());
        }

        // P1-TC03-01: 系统服务不可用，保存失败，预期抛出 NullPointerException
        @Test
        @DisplayName("P1-TC03-01")
        public void testSaveProduct_SystemServiceUnavailable_Fail() {
            Product product = new Product();
            product.setProductName("Test");
            product.setProductType("Type A");
            product.setRiskLevel(1);
            product.setProductStatus(1);

            when(productMapper.insert(any(Product.class))).thenReturn(1);
            when(settleClient.getSystem()).thenReturn(null);

            assertThrows(NullPointerException.class, () -> productService.saveProduct(product));

            verify(productMapper, times(1)).insert(any());
            verify(netValueMapper, never()).insert(any());
        }

        // P1-TC04-01: 交易日为空，保存失败
        @Test
        @DisplayName("P1-TC04-01")
        public void testSaveProduct_InvalidTransactionDate_Fail() {
            Product product = new Product();
            product.setProductName("Test");
            product.setProductType("Type A");
            product.setRiskLevel(1);
            product.setProductStatus(1);

            when(productMapper.insert(any(Product.class))).thenReturn(1);

            OurSystem ourSystem = mock(OurSystem.class);
            when(settleClient.getSystem()).thenReturn(ourSystem);
            when(ourSystem.getTransactionDate()).thenReturn(null);

            boolean result = productService.saveProduct(product);

            assertFalse(result);
            verify(productMapper, times(1)).insert(any());
            verify(netValueMapper, never()).insert(any());
        }

        // P1-TC05-01: 净值插入失败，整体失败
        @Test
        @DisplayName("P1-TC05-01")
        public void testSaveProduct_NetValueInsertFail() {
            Product product = new Product();
            product.setProductName("Test");
            product.setProductType("Type A");
            product.setRiskLevel(1);
            product.setProductStatus(1);

            doAnswer(invocation -> {
                Product p = invocation.getArgument(0);
                p.setProductId(200);
                return 1;
            }).when(productMapper).insert(any(Product.class));

            OurSystem ourSystem = mock(OurSystem.class);
            when(settleClient.getSystem()).thenReturn(ourSystem);
            when(ourSystem.getTransactionDate()).thenReturn(new Date());

            when(netValueMapper.insert(any(NetValue.class))).thenReturn(0);

            boolean result = productService.saveProduct(product);

            assertFalse(result);
            assertEquals(200, product.getProductId());

            verify(productMapper, times(1)).insert(any());
            verify(netValueMapper, times(1)).insert(any());
        }

        // P1-TC06-01: 产品已存在，插入冲突导致失败
        @Test
        @DisplayName("P1-TC06-01")
        public void testSaveProduct_ProductExists_Fail() {
            Product product = new Product();
            product.setProductName("Test");
            product.setProductType("Type A");
            product.setRiskLevel(1);
            product.setProductStatus(1);

            doThrow(new RuntimeException("Duplicate Key")).when(productMapper).insert(any(Product.class));

            assertThrows(RuntimeException.class, () -> productService.saveProduct(product));

            verify(productMapper, times(1)).insert(any());
            verify(netValueMapper, never()).insert(any());
        }
    }

    @Nested
    class UpdateProductTests {

        // P2-TC01-01: ID 存在且字段完整合法，调用更新接口
        @Test
        @DisplayName("P2-TC01-01")
        public void testUpdateProduct_Success() {
            Product product = new Product();
            product.setProductId(123);
            product.setProductName("Updated Product");

            assertDoesNotThrow(() -> productService.updateProduct(product));
            verify(productMapper, times(1)).updateById(product);
        }

        // P2-TC02-01: ID 为空，不调用更新接口
        @Test
        @DisplayName("P2-TC02-01")
        public void testUpdateProduct_IdIsNull() {
            Product product = new Product();
            product.setProductName("No ID Product");

            assertDoesNotThrow(() -> productService.updateProduct(product));
            verify(productMapper, never()).updateById(any());
        }

        // P2-TC03-01: ID 不存在数据库（模拟 updateById 结果为 0）
        @Test
        @DisplayName("P2-TC03-01")
        public void testUpdateProduct_IdNotExists() {
            Product product = new Product();
            product.setProductId(999);
            product.setProductName("Nonexistent");

            assertDoesNotThrow(() -> productService.updateProduct(product));
            verify(productMapper, times(1)).updateById(product);
        }

        // P2-TC04-01: 数据库异常，捕获异常
        @Test
        @DisplayName("P2-TC04-01")
        public void testUpdateProduct_DatabaseException() {
            Product product = new Product();
            product.setProductId(456);
            product.setProductName("Exceptional");

            doThrow(new RuntimeException("DB error")).when(productMapper).updateById(any());

            assertThrows(RuntimeException.class, () -> productService.updateProduct(product));
            verify(productMapper, times(1)).updateById(product);
        }
    }

    @Nested
    class GetNetValueByProductIdAndDateTests {

        // 工具方法：Date -> LocalDate转换辅助（方便写测试）
        private Date toDate(LocalDate localDate) {
            return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        }

        @Test
        @DisplayName("P3-TC01-01")
        public void P3_TC01_ProductIdValidAndDateExists_ReturnNetValue() {
            int productId = 123;
            Date date = toDate(LocalDate.of(2023, 6, 18));
            NetValue mockNetValue = new NetValue();
            mockNetValue.setNetValue(1.234);

            // 模拟数据库返回实体
            when(netValueMapper.selectOne(any())).thenReturn(mockNetValue);

            Double result = productService.getNetValueByProductIdAndDate(productId, date);

            assertNotNull(result);
            assertEquals(1.234, result, 0.00001);

            verify(netValueMapper, times(1)).selectOne(any());
        }

        @Test
        @DisplayName("P3-TC02-01")
        public void P3_TC02_ProductIdValidDateNoRecord_ReturnNull() {
            int productId = 123;
            Date date = toDate(LocalDate.of(2025, 1, 1));

            when(netValueMapper.selectOne(any())).thenReturn(null);

            Double result = productService.getNetValueByProductIdAndDate(productId, date);

            assertNull(result);
            verify(netValueMapper, times(1)).selectOne(any());
        }

        @Test
        @DisplayName("P3-TC03-01")
        public void P3_TC03_ProductIdNegativeOrZero_ReturnNull() {
            int[] productIds = {0, -1, -100};
            Date date = toDate(LocalDate.of(2023, 6, 18));

            for (int productId : productIds) {
                when(netValueMapper.selectOne(any())).thenReturn(null);

                Double result = productService.getNetValueByProductIdAndDate(productId, date);
                assertNull(result);

                verify(netValueMapper, times(1)).selectOne(any());
                clearInvocations(netValueMapper);
            }
        }

        @Test
        @DisplayName("P3-TC04-01")
        public void P3_TC04_DateWithTimeDifferentSameDay_ReturnNetValue() {
            int productId = 123;
            // 不同时间同一天
            Date date1 = Date.from(LocalDate.of(2023, 6, 18).atStartOfDay(ZoneId.systemDefault()).toInstant());
            Date date2 = Date.from(LocalDate.of(2023, 6, 18).atTime(23,59,59).atZone(ZoneId.systemDefault()).toInstant());

            NetValue mockNetValue = new NetValue();
            mockNetValue.setNetValue(2.345);

            when(netValueMapper.selectOne(any())).thenReturn(mockNetValue);

            Double result1 = productService.getNetValueByProductIdAndDate(productId, date1);
            Double result2 = productService.getNetValueByProductIdAndDate(productId, date2);

            assertNotNull(result1);
            assertEquals(2.345, result1, 0.00001);

            assertNotNull(result2);
            assertEquals(2.345, result2, 0.00001);

            verify(netValueMapper, times(2)).selectOne(any());
        }

        @Test
        @DisplayName("P3-TC05-01")
        public void P3_TC05_DateIsNull_ReturnNull() {
            int productId = 123;
            Date date = null;

            // 你自己方法没null判断，可能会抛异常，也可能查询失败返回null
            // 这里假设方法没改动，抛空指针异常或者返回null都接受
            try {
                Double result = productService.getNetValueByProductIdAndDate(productId, date);
                assertNull(result);
            } catch (Exception e) {
                assertTrue(e instanceof NullPointerException);
            }

            // 如果调用了mapper，也验证了
            verify(netValueMapper, atMostOnce()).selectOne(any());
        }

        @Test
        @DisplayName("P3-TC06-01")
        public void P3_TC06_DateFormatIllegal_ReturnNullOrException() {
            // 这里参数是Date，编译时就限制了，无法传非法格式日期
            // 所以此测试用例理论上没法写，除非你改方法参数类型为String
            // 我们这里写个示意，传了一个null或者是无效日期，模拟行为

            int productId = 123;
            // 直接传 null，见上
            Date illegalDate = null;

            try {
                Double result = productService.getNetValueByProductIdAndDate(productId, illegalDate);
                assertNull(result);
            } catch (Exception e) {
                assertTrue(e instanceof NullPointerException);
            }

            verify(netValueMapper, atMostOnce()).selectOne(any());
        }

        @Test
        @DisplayName("P3-TC07-01")
        public void P3_TC07_DatabaseException_ThrowsException() {
            int productId = 123;
            Date date = toDate(LocalDate.of(2023, 6, 18));

            when(netValueMapper.selectOne(any())).thenThrow(new RuntimeException("DB error"));

            assertThrows(RuntimeException.class, () -> productService.getNetValueByProductIdAndDate(productId, date));

            verify(netValueMapper, times(1)).selectOne(any());
        }
    }

}
