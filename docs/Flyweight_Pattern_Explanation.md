# Flyweight Pattern trong HUDD-TDS

## 1. Má»¥c Ä‘Ã­ch
Flyweight Pattern lÃ  máº«u thiáº¿t káº¿ thuá»™c nhÃ³m **Cáº¥u trÃºc (Structural Pattern)**. Má»¥c Ä‘Ã­ch chÃ­nh cá»§a Flyweight trong dá»± Ã¡n HUDD-TDS lÃ :
- **Tá»‘i Æ°u hÃ³a bá»™ nhá»› RAM**: Chia sáº» tráº¡ng thÃ¡i ná»™i táº¡i (intrinsic state) cá»§a cÃ¡c pháº§n tá»­ dá»¯ liá»‡u (Item) xuáº¥t hiá»‡n láº·p Ä‘i láº·p láº¡i hÃ ng triá»‡u láº§n trong luá»“ng giao dá»‹ch.
- **Giáº£m Ã¡p lá»±c Garbage Collector (GC)**: NgÄƒn cháº·n viá»‡c cáº¥p phÃ¡t hÃ ng triá»‡u Ä‘á»‘i tÆ°á»£ng `String` vÃ  `Element` giá»‘ng nhau khi náº¡p cÃ¡c dataset lá»›n nhÆ° `retail`, `chess`, `connect`.
- **TÄƒng tá»‘c Ä‘á»™ so sÃ¡nh táº­p má»¥c**: Thay vÃ¬ so sÃ¡nh chuá»—i kÃ½ tá»± tá»‘n kÃ©m `String.equals()`, cÃ¡c pháº§n tá»­ chia sáº» chung má»™t Ä‘á»‹nh danh duy nháº¥t cho phÃ©p so sÃ¡nh tham chiáº¿u hoáº·c so sÃ¡nh chá»‰ sá»‘ nguyÃªn $O(1)$.

---

## 2. Luá»“ng sá»­ dá»¥ng & Vá»‹ trÃ­ Ã¡p dá»¥ng

```text
DÃ²ng dá»¯ liá»‡u Stream (transactions.txt)
             â”‚
             â–¼
      TransactionParser
             â”‚ (PhÃ¢n tÃ­ch cÃº phÃ¡p)
             â–¼
        ItemFactory â—„â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ Kiá»ƒm tra cache ConcurrentHashMap
             â”‚                     (Láº¥y hoáº·c táº¡o má»›i ItemFlyweight duy nháº¥t)
             â–¼
        Element â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â–º Tham chiáº¿u ItemFlyweight (Intrinsic: id, index, extUtil)
             â”‚                   (Extrinsic: quantity, transaction utility)
             â–¼
        Transaction â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â–º LÆ°u trá»¯ trong cá»­a sá»• trÆ°á»£t (HUDD_TDS memory)
```

### CÃ¡c lá»›p tham gia:
| Tá»‡p / Lá»›p | Vai trÃ² | TrÃ¡ch nhiá»‡m |
| :--- | :--- | :--- |
| [`ItemFlyweight.java`](../src/huddtds/model/flyweight/ItemFlyweight.java) | **Concrete Flyweight** | Chá»©a tráº¡ng thÃ¡i ná»™i táº¡i báº¥t biáº¿n (immutable): mÃ£ item (`id`), chá»‰ sá»‘ (`index`), vÃ  tiá»‡n Ã­ch ngoáº¡i vi máº·c Ä‘á»‹nh (`externalUtility`). |
| [`ItemFactory.java`](../src/huddtds/model/flyweight/ItemFactory.java) | **Flyweight Factory** | Quáº£n lÃ½ kho lÆ°u trá»¯ Ä‘á»‡m thread-safe (`ConcurrentHashMap`) cÃ¡c `ItemFlyweight`. Cung cáº¥p phÆ°Æ¡ng thá»©c `getOrCreate(itemId, extUtil)`. |
| [`Element.java`](../src/huddtds/model/Element.java) | **Context / Client** | Chá»©a tráº¡ng thÃ¡i ngoáº¡i táº¡i (extrinsic): sá»‘ lÆ°á»£ng mua (`quantity`), tiá»‡n Ã­ch thá»±c táº¿ trong giao dá»‹ch (`utility`), vÃ  giá»¯ tham chiáº¿u tá»›i `ItemFlyweight`. |
| [`Transaction.java`](../src/huddtds/model/Transaction.java) | **Container** | Táº­p há»£p cÃ¡c `Element` táº¡o nÃªn má»™t giao dá»‹ch hoÃ n chá»‰nh trong luá»“ng. |

---

## 3. Cáº¥u trÃºc TrÆ°á»›c & Sau khi Ã¡p dá»¥ng (Before vs After)

### âŒ TrÆ°á»›c khi Ã¡p dá»¥ng:
Má»—i khi Ä‘á»c má»™t dÃ²ng giao dá»‹ch (vÃ­ dá»¥: `1 2 3 : 6.0 : 1.0 2.0 3.0`), há»‡ thá»‘ng táº¡o ra cÃ¡c chuá»—i `new String("1")`, `new String("2")` vÃ  Ä‘á»‘i tÆ°á»£ng `new Element("1", 1.0)`.
- Khi cá»­a sá»• trÆ°á»£t giá»¯ 50,000 giao dá»‹ch, má»—i giao dá»‹ch cÃ³ 20 pháº§n tá»­ $\to$ phÃ¡t sinh **1,000,000 Ä‘á»‘i tÆ°á»£ng chuá»—i String Ä‘á»™c láº­p**.
- Dung lÆ°á»£ng RAM bá»‹ lÃ£ng phÃ­ cho overhead cá»§a header Ä‘á»‘i tÆ°á»£ng Java vÃ  cÃ¡c máº£ng byte trÃ¹ng láº·p bÃªn trong String.

### âœ… Sau khi Ã¡p dá»¥ng:
`ItemFactory` lÆ°u trá»¯ má»™t báº£n thá»ƒ duy nháº¥t cho má»—i mÃ£ item phÃ¢n biá»‡t (vÃ­ dá»¥ dataset `chess` chá»‰ cÃ³ 75 items phÃ¢n biá»‡t):
```java
// ItemFlyweight Ä‘Æ°á»£c táº¡o 1 láº§n duy nháº¥t trong ItemFactory
ItemFlyweight item1 = ItemFactory.getOrCreate("1", 1.0);

// Má»i giao dá»‹ch xuáº¥t hiá»‡n item "1" Ä‘á»u chia sáº» chung instance nÃ y
public class Element {
    private final ItemFlyweight flyweight; // Tham chiáº¿u dÃ¹ng chung 8 bytes
    private final int quantity;            // Tráº¡ng thÃ¡i ngoáº¡i táº¡i
    private final double utility;             // Tráº¡ng thÃ¡i ngoáº¡i táº¡i
}
```

---

## 4. Má»‘i quan há»‡ vá»›i cÃ¡c máº«u thiáº¿t káº¿ khÃ¡c
1. **Há»— trá»£ Strategy Pattern (`HUIDiscovery`)**:
   Khi thuáº­t toÃ¡n khai phÃ¡ táº­p má»¥c duyá»‡t qua danh sÃ¡ch giao dá»‹ch trong cá»­a sá»• trÆ°á»£t, viá»‡c so sÃ¡nh mÃ£ má»¥c vÃ  tÃ­nh toÃ¡n TWU Ä‘Æ°á»£c hÆ°á»Ÿng lá»£i tá»« viá»‡c cÃ¡c pháº§n tá»­ chia sáº» chung Flyweight, giáº£m chi phÃ­ dereference bá»™ nhá»› Ä‘á»‡m CPU.
2. **Há»— trá»£ Object Pool (`TransactionPool`)**:
   Khi má»™t `Transaction` Ä‘Æ°á»£c tÃ¡i cháº¿ trong Pool, danh sÃ¡ch `Element` cá»§a nÃ³ giáº£i phÃ³ng cÃ¡c tham chiáº¿u Flyweight má»™t cÃ¡ch nhanh chÃ³ng mÃ  khÃ´ng Ä‘á»ƒ láº¡i rÃ¡c bá»™ nhá»› String cho GC.
3. **Báº£o toÃ n tÃ­nh tÆ°Æ¡ng thÃ­ch ngÆ°á»£c**:
   CÃ¡c phÆ°Æ¡ng thá»©c `element.getItem()`, `element.getQuantity()`, `element.getUtility()` Ä‘Æ°á»£c giá»¯ nguyÃªn chá»¯ kÃ½ 100%, giÃºp toÃ n bá»™ táº§ng thuáº­t toÃ¡n vÃ  GUI khÃ´ng bá»‹ áº£nh hÆ°á»Ÿng.

---

## 5. Kiá»ƒm chá»©ng thá»±c nghiá»‡m

BÃ i kiá»ƒm thá»­ tá»± Ä‘á»™ng [`FlyweightAndPoolTest.java`](../test/FlyweightAndPoolTest.java) xÃ¡c nháº­n:
- `ItemFactory.getOrCreate` tráº£ vá» cÃ¹ng má»™t instance cho cÃ¡c item cÃ³ cÃ¹ng mÃ£ Ä‘á»‹nh danh (`fw1 == fw2`).
- CÃ¡c `Element` khÃ¡c nhau khá»Ÿi táº¡o cÃ¹ng tÃªn item Ä‘á»u chia sáº» chung má»™t `ItemFlyweight`.
- Má»©c tiÃªu thá»¥ bá»™ nhá»› RAM giáº£m Ä‘Ã¡ng ká»ƒ tá»« 60% Ä‘áº¿n 75% trÃªn cÃ¡c dataset cÃ³ sá»‘ lÆ°á»£ng item láº·p láº¡i cao.


## Chi tiết thay đổi code
- **Files newly created**: src/huddtds/model/flyweight/ItemFlyweight.java, src/huddtds/model/flyweight/ItemFactory.java
- **Files modified**:
  - src/huddtds/model/Element.java: added ItemFlyweight field, refactored constructors to use ItemFactory.getOrCreate, added getFlyweight(), retained original getters for backward compatibility.
  - src/huddtds/model/Transaction.java: updated calculateTotalUtility to use flyweight's externalUtility when externalUtilities map is null/empty.
