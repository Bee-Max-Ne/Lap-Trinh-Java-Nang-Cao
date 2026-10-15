# Template Method Pattern trong HUDD-TDS

## 1. Má»¥c Ä‘Ã­ch
Template Method Pattern lÃ  máº«u thiáº¿t káº¿ thuá»™c nhÃ³m **HÃ nh vi (Behavioral Pattern)**. Má»¥c Ä‘Ã­ch chÃ­nh cá»§a Template Method trong HUDD-TDS lÃ :
- **Chuáº©n hÃ³a bá»™ khung kiá»ƒm Ä‘á»‹nh trÃ´i dáº¡t (Drift Detection Framework)**: Äá»‹nh nghÄ©a ká»‹ch báº£n kiá»ƒm Ä‘á»‹nh cá»‘ Ä‘á»‹nh 6 bÆ°á»›c báº¥t biáº¿n táº¡i lá»›p cha trá»«u tÆ°á»£ng `AbstractDriftDetector`.
- **Loáº¡i bá» mÃ£ trÃ¹ng láº·p cáº¥u trÃºc**: CÃ¡c bá»™ phÃ¡t hiá»‡n trÃ´i dáº¡t (`GlobalDriftDetector`, `LocalDriftDetector`) khÃ´ng cáº§n pháº£i viáº¿t láº¡i ká»‹ch báº£n kiá»ƒm tra dá»¯ liá»‡u, so sÃ¡nh giÃ¡ trá»‹ vÃ  ghi log.
- **TÄƒng cÆ°á»ng vÃ  phá»‘i há»£p cháº·t cháº½ vá»›i Strategy Pattern**: Strategy chá»‹u trÃ¡ch nhiá»‡m phÃ¢n quyá»n bÃªn ngoÃ i (`HUDD_TDS`), trong khi Template Method chá»‹u trÃ¡ch nhiá»‡m quy Ä‘á»‹nh trÃ¬nh tá»± bÃªn trong cá»§a tá»«ng thuáº­t toÃ¡n.

---

## 2. Luá»“ng sá»­ dá»¥ng & Vá»‹ trÃ­ Ã¡p dá»¥ng

```text
HUDD_TDS Engine
     â”‚
     â”‚ á»¦y quyá»n gá»i (Strategy API)
     â–¼
AbstractDriftDetector.runDetection (final Template Method)
     â”‚
     â”œâ”€â”€ 1. hasEnoughData(input) â”€â”€â”€â”€â–º [Náº¿u false: Dá»«ng sá»›m, tráº£ vá» noDrift]
     â”‚
     â”œâ”€â”€ 2. computeStatistic(input) â”€â”€â–º Lá»›p con tÃ­nh toÃ¡n Ä‘áº¡i lÆ°á»£ng thá»‘ng kÃª
     â”‚
     â”œâ”€â”€ 3. computeEpsilon(input) â”€â”€â”€â”€â–º Lá»›p con tÃ­nh ngÆ°á»¡ng epsilon lÃ½ thuyáº¿t
     â”‚
     â”œâ”€â”€ 4. PhÃ¡n quyáº¿t cá»‘ Ä‘á»‹nh: |statistic| >= epsilon
     â”‚
     â”œâ”€â”€ 5. traceResult(...) â”€â”€â”€â”€â”€â”€â”€â”€â–º Ghi trace cháº©n Ä‘oÃ¡n chuáº©n hÃ³a
     â”‚
     â””â”€â”€ 6. buildDetectedResult / buildNoDriftResult â”€â”€â–º Tráº£ vá» DriftResult
```

### CÃ¡c lá»›p tham gia:
| Tá»‡p / Lá»›p | Vai trÃ² | TrÃ¡ch nhiá»‡m |
| :--- | :--- | :--- |
| [`AbstractDriftDetector.java`](../src/huddtds/algorithm/drift/AbstractDriftDetector.java) | **Abstract Class (Template)** | Khai bÃ¡o phÆ°Æ¡ng thá»©c khuÃ´n máº«u `final runDetection(DriftInput)` chá»©a 6 bÆ°á»›c cá»‘ Ä‘á»‹nh; Ä‘á»‹nh nghÄ©a cÃ¡c hook methods báº¯t buá»™c vÃ  tÃ¹y chá»n. |
| [`GlobalDriftDetector.java`](../src/huddtds/algorithm/GlobalDriftDetector.java) | **Concrete Class** | Káº¿ thá»«a `AbstractDriftDetector` vÃ  cÃ i Ä‘áº·t `GlobalDriftStrategy`. CÃ i Ä‘áº·t hook tÃ­nh $|U_{drift} - V|$ vÃ  ngÆ°á»¡ng $\epsilon$ theo Equation (8) / Hoeffding bound. |
| [`LocalDriftDetector.java`](../src/huddtds/algorithm/LocalDriftDetector.java) | **Concrete Class** | Káº¿ thá»«a `AbstractDriftDetector` vÃ  cÃ i Ä‘áº·t `LocalDriftStrategy`. CÃ i Ä‘áº·t hook tÃ­nh chÃªnh lá»‡ch utility tá»«ng itemset vÃ  ngÆ°á»¡ng $\epsilon$ theo Equation (9) / Bonferroni. |

---

## 3. Cáº¥u trÃºc TrÆ°á»›c & Sau khi Ã¡p dá»¥ng (Before vs After)

### âŒ TrÆ°á»›c khi Ã¡p dá»¥ng:
Má»—i lá»›p detector pháº£i tá»± quáº£n lÃ½ viá»‡c kiá»ƒm tra dá»¯ liá»‡u Ä‘áº§u vÃ o, tá»± tÃ­nh toÃ¡n vÃ  tá»± quyáº¿t Ä‘á»‹nh cáº¥u trÃºc káº¿t quáº£ tráº£ vá»:
```java
public class GlobalDriftDetector implements GlobalDriftStrategy {
    // Tá»± láº·p láº¡i kiá»ƒm tra n < 2
    // Tá»± so sÃ¡nh difference >= epsilon
    // Tá»± ghi log trace phÃ¢n tÃ¡n
}
```

### âœ… Sau khi Ã¡p dá»¥ng:
Lá»›p cha kiá»ƒm soÃ¡t toÃ n bá»™ vÃ²ng Ä‘á»i thá»±c thi, lá»›p con chá»‰ cáº§n cung cáº¥p cÃ´ng thá»©c tÃ­nh toÃ¡n:
```java
public abstract class AbstractDriftDetector {
    public final DriftResult runDetection(DriftInput input) {
        if (!hasEnoughData(input)) return buildNoDriftResult(input);
        double statistic = computeStatistic(input);
        double epsilon = computeEpsilon(input);
        boolean detected = Math.abs(statistic) >= epsilon;
        traceResult(input, statistic, epsilon, detected);
        return detected ? buildDetectedResult(input, statistic, epsilon) : buildNoDriftResult(input);
    }
}
```

---

## 4. Sá»± phá»‘i há»£p Äƒn Ã½ giá»¯a Template Method vÃ  Strategy Pattern

> [!TIP]
> **Strategy** vÃ  **Template Method** giáº£i quyáº¿t 2 gÃ³c nhÃ¬n bá»• trá»£ cho nhau:
> - **Strategy Pattern**: Ranh giá»›i bÃªn ngoÃ i â€” cho phÃ©p `HUDD_TDS` tiÃªm vÃ  hoÃ¡n Ä‘á»•i linh hoáº¡t giá»¯a cÃ¡c bá»™ dÃ² drift (`GlobalDriftStrategy`, `LocalDriftStrategy`).
> - **Template Method Pattern**: Cáº¥u trÃºc bÃªn trong â€” chuáº©n hÃ³a quy trÃ¬nh tÃ­nh toÃ¡n 6 bÆ°á»›c cho má»i bá»™ dÃ² drift, Ä‘áº£m báº£o khÃ´ng bá»™ dÃ² nÃ o bá» quÃªn bÆ°á»›c kiá»ƒm tra dá»«ng sá»›m hay ghi trace.
>
> âžœ **Strategy quy Ä‘á»‹nh "AI LÃ€M", cÃ²n Template Method quy Ä‘á»‹nh "LÃ€M NHÆ¯ THáº¾ NÃ€O".**

---

## 5. Kiá»ƒm chá»©ng thá»±c nghiá»‡m

BÃ i kiá»ƒm thá»­ tá»± Ä‘á»™ng [`TemplateMethodPatternTest.java`](../test/TemplateMethodPatternTest.java) xÃ¡c nháº­n:
- Khi `hasEnoughData() == false`: Luá»“ng thá»±c thi láº­p tá»©c dá»«ng láº¡i (short-circuit), khÃ´ng tÃ­nh toÃ¡n thá»‘ng kÃª hay epsilon tá»‘n kÃ©m.
- Khi `|statistic| >= epsilon`: Luá»“ng gá»i Ä‘áº§y Ä‘á»§ theo Ä‘Ãºng thá»© tá»± 6 bÆ°á»›c vÃ  tráº£ vá» `DriftResult` vá»›i tráº¡ng thÃ¡i `detected = true`.
- Káº¿ thá»«a Ä‘a hÃ¬nh hoáº¡t Ä‘á»™ng hoÃ n háº£o, báº£o toÃ n tÃ­nh tÆ°Æ¡ng thÃ­ch vá»›i toÃ n bá»™ mÃ£ nguá»“n cÅ©.


## Chi tiết thay đổi code
- **File newly created**: src/huddtds/algorithm/drift/AbstractDriftDetector.java
- **Files modified**:
  - src/huddtds/algorithm/GlobalDriftDetector.java: changed to extend AbstractDriftDetector while implementing GlobalDriftStrategy, added GlobalDriftInput class, refactored updateAndCheck to delegate to runDetection, implemented six abstract methods.
  - src/huddtds/algorithm/LocalDriftDetector.java: similar changes for LocalDriftDetector.
