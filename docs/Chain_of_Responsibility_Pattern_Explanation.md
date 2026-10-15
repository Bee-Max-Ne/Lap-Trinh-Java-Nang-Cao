# Chain of Responsibility Pattern trong HUDD-TDS

## 1. Má»¥c Ä‘Ã­ch
Chain of Responsibility Pattern lÃ  máº«u thiáº¿t káº¿ thuá»™c nhÃ³m **HÃ nh vi (Behavioral Pattern)**. Má»¥c Ä‘Ã­ch chÃ­nh cá»§a Chain of Responsibility trong HUDD-TDS lÃ :
- **TÃ¡ch rá»i cÃ¡c bá»™ phÃ¢n tÃ­ch cÃº phÃ¡p (Parser Decoupling)**: Chuyá»ƒn Ä‘á»•i logic phÃ¢n tÃ­ch chuá»—i dá»¯ liá»‡u thÃ´ tá»« cáº¥u trÃºc lá»‡nh `if-else` lá»“ng nhau thÃ nh má»™t chuá»—i cÃ¡c bá»™ xá»­ lÃ½ (Handlers) Ä‘á»™c láº­p.
- **Tiá»n xá»­ lÃ½ vÃ  lÃ m sáº¡ch dá»¯ liá»‡u stream (Data Preprocessing)**: Tá»± Ä‘á»™ng lá»c bá» cÃ¡c dÃ²ng trá»‘ng, dÃ²ng chÃº thÃ­ch (`#`, `//`) mÃ  khÃ´ng lÃ m giÃ¡n Ä‘oáº¡n luá»“ng stream.
- **Má»Ÿ rá»™ng Ä‘á»‹nh dáº¡ng dá»¯ liá»‡u linh hoáº¡t (Open/Closed Principle)**: Dá»… dÃ ng bá»• sung thÃªm cÃ¡c Ä‘á»‹nh dáº¡ng giao dá»‹ch má»›i trong tÆ°Æ¡ng lai (JSON, CSV, Parquet) chá»‰ báº±ng cÃ¡ch táº¡o thÃªm má»™t Handler má»›i cáº¯m vÃ o Pipeline.

---

## 2. Luá»“ng sá»­ dá»¥ng & Vá»‹ trÃ­ Ã¡p dá»¥ng

```text
Chuá»—i kÃ½ tá»± dÃ²ng thÃ´ (String line)
             â”‚
             â–¼
  TransactionParsingPipeline
             â”‚
             â–¼
[EmptyAndCommentFilterHandler] â”€â”€â”€â”€ DÃ²ng trá»‘ng hoáº·c báº¯t Ä‘áº§u báº±ng #, // â”€â”€â–º Bá» qua (tráº£ vá» null)
             â”‚ (KhÃ´ng khá»›p)
             â–¼
   [SpmfFormatHandler] â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ Khá»›p cÃº phÃ¡p "items : TU : utils" â”€â”€â–º Tráº£ vá» Transaction
             â”‚ (KhÃ´ng khá»›p)
             â–¼
  [LegacyFormatHandler] â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ Khá»›p cÃº phÃ¡p "item:qty ..." â”€â”€â”€â”€â”€â”€â”€â”€â–º Tráº£ vá» Transaction
             â”‚ (KhÃ´ng khá»›p)
             â–¼
   Káº¿t thÃºc chuá»—i (Handler tiáº¿p theo lÃ  null) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â–º BÃ¡o lá»—i Ä‘á»‹nh dáº¡ng khÃ´ng há»— trá»£
```

### CÃ¡c lá»›p tham gia:
| Tá»‡p / Lá»›p | Vai trÃ² | TrÃ¡ch nhiá»‡m |
| :--- | :--- | :--- |
| [`TransactionLineHandler.java`](../src/huddtds/data/pipeline/TransactionLineHandler.java) | **Handler Interface** | Äá»‹nh nghÄ©a phÆ°Æ¡ng thá»©c `handle(line, tid)` vÃ  `setNext(handler)`. |
| [`EmptyAndCommentFilterHandler.java`](../src/huddtds/data/pipeline/EmptyAndCommentFilterHandler.java) | **Concrete Handler** | Bá» qua dÃ²ng trá»‘ng, dÃ²ng khoáº£ng tráº¯ng hoáº·c dÃ²ng báº¯t Ä‘áº§u báº±ng dáº¥u chÃº thÃ­ch `#`, `//`. |
| [`SpmfFormatHandler.java`](../src/huddtds/data/pipeline/SpmfFormatHandler.java) | **Concrete Handler** | Nháº­n diá»‡n vÃ  bÃ³c tÃ¡ch Ä‘á»‹nh dáº¡ng SPMF / HUIM chuáº©n (`item1 item2 : TU : u1 u2`). |
| [`LegacyFormatHandler.java`](../src/huddtds/data/pipeline/LegacyFormatHandler.java) | **Concrete Handler** | Nháº­n diá»‡n vÃ  bÃ³c tÃ¡ch Ä‘á»‹nh dáº¡ng Running Example cá»• Ä‘iá»ƒn (`a:1 c:6 e:2`). |
| [`TransactionParsingPipeline.java`](../src/huddtds/data/pipeline/TransactionParsingPipeline.java) | **Pipeline / Client** | Äiá»u phá»‘i chuá»—i liÃªn káº¿t cÃ¡c handler vÃ  cung cáº¥p phÆ°Æ¡ng thá»©c `parse(line, tid)` cho á»©ng dá»¥ng. |
| [`TransactionParser.java`](../src/huddtds/data/TransactionParser.java) | **Integration Entry** | Äiá»ƒm truy cáº­p tÄ©nh truyá»n thá»‘ng cá»§a há»‡ thá»‘ng, nay á»§y quyá»n toÃ n bá»™ cho `TransactionParsingPipeline`. |

---

## 3. Cáº¥u trÃºc TrÆ°á»›c & Sau khi Ã¡p dá»¥ng (Before vs After)

### âŒ TrÆ°á»›c khi Ã¡p dá»¥ng:
PhÆ°Æ¡ng thá»©c tÄ©nh `TransactionParser.parseLine` chá»©a chuá»—i `if-else` cá»©ng nháº¯c:
```java
public static Transaction parseLine(String line, int tid) {
    if (trimmed.contains(":")) {
        String[] parts = trimmed.split(":");
        if (parts.length == 3) {
            return parseSpmfFormat(parts, tid);
        }
    }
    return parseLegacyFormat(trimmed, tid); // Dá»… phÃ¡t sinh lá»—i náº¿u gáº·p dÃ²ng chÃº thÃ­ch!
}
```

### âœ… Sau khi Ã¡p dá»¥ng:
Má»—i loáº¡i Ä‘á»‹nh dáº¡ng Ä‘Æ°á»£c Ä‘Ã³ng gÃ³i thÃ nh má»™t lá»›p Handler riÃªng biá»‡t vÃ  xÃ¢u chuá»—i linh hoáº¡t:
```java
// Khá»Ÿi táº¡o Pipeline theo thá»© tá»± Æ°u tiÃªn xá»­ lÃ½
TransactionParsingPipeline pipeline = new TransactionParsingPipeline(
    new EmptyAndCommentFilterHandler(),
    new SpmfFormatHandler(),
    new LegacyFormatHandler()
);

// Xá»­ lÃ½ dÃ²ng thÃ´ng qua chuá»—i trÃ¡ch nhiá»‡m
Transaction tx = pipeline.parse(line, tid);
```

---

## 4. Má»‘i quan há»‡ vá»›i cÃ¡c máº«u thiáº¿t káº¿ khÃ¡c
1. **Há»— trá»£ Facade Pattern (`SimulationFacade`)**:
   Khi `SimulationFacade` má»Ÿ transaction stream tá»« cÃ¡c tá»‡p dataset hoáº·c tá»« Ã´ nháº­p tay, dá»¯ liá»‡u Ä‘i tháº³ng qua `TransactionParser` (vá»‘n Ä‘Æ°á»£c váº­n hÃ nh bá»Ÿi Pipeline), giÃºp che giáº¥u hoÃ n toÃ n sá»± phá»©c táº¡p cá»§a viá»‡c phÃ¢n tÃ¡ch Ä‘á»‹nh dáº¡ng.
2. **Há»— trá»£ Flyweight vÃ  Object Pool**:
   CÃ¡c handler nhÆ° `SpmfFormatHandler` vÃ  `LegacyFormatHandler` tá»± Ä‘á»™ng káº¿t há»£p vá»›i `ItemFactory` Ä‘á»ƒ náº¡p cÃ¡c Ä‘á»‘i tÆ°á»£ng Flyweight vÃ  mÆ°á»£n `Transaction` tá»« `TransactionPool`, táº¡o nÃªn má»™t Ä‘Æ°á»ng á»‘ng khÃ©p kÃ­n tá»‘i Æ°u cáº£ CPU láº«n RAM.

---

## 5. Kiá»ƒm chá»©ng thá»±c nghiá»‡m

BÃ i kiá»ƒm thá»­ tá»± Ä‘á»™ng [`ParsingPipelineChainTest.java`](../test/ParsingPipelineChainTest.java) xÃ¡c nháº­n:
- Lá»c thÃ nh cÃ´ng cÃ¡c dÃ²ng rá»—ng, dÃ²ng chá»©a tab/khoáº£ng tráº¯ng, dÃ²ng chÃº thÃ­ch `#` vÃ  `//`.
- BÃ³c tÃ¡ch chÃ­nh xÃ¡c Ä‘á»‹nh dáº¡ng SPMF/HUIM gá»“m 3 thÃ nh pháº§n (items, TU, utilities).
- BÃ³c tÃ¡ch chÃ­nh xÃ¡c Ä‘á»‹nh dáº¡ng Running Example cá»• Ä‘iá»ƒn theo cáº·p `item:quantity`.
- CÆ¡ cháº¿ chuyá»ƒn tiáº¿p trÃ¡ch nhiá»‡m (`setNext()`) hoáº¡t Ä‘á»™ng chuáº©n xÃ¡c tá»« handler nÃ y sang handler káº¿ tiáº¿p khi dÃ²ng dá»¯ liá»‡u khÃ´ng thuá»™c pháº¡m vi xá»­ lÃ½ cá»§a handler hiá»‡n táº¡i.


## Chi tiết thay đổi code
- **Files newly created**:
  - src/huddtds/data/pipeline/TransactionLineHandler.java
  - src/huddtds/data/pipeline/EmptyAndCommentFilterHandler.java
  - src/huddtds/data/pipeline/SpmfFormatHandler.java
  - src/huddtds/data/pipeline/LegacyFormatHandler.java
  - src/huddtds/data/pipeline/TransactionParsingPipeline.java
- **File modified**:
  - src/huddtds/data/TransactionParser.java: replaced if-else logic with TransactionParsingPipeline, added static pipeline initializer, kept public parseLine signature unchanged.
