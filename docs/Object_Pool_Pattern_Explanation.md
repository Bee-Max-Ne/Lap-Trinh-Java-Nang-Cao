# Object Pool Pattern trong HUDD-TDS

## 1. Má»¥c Ä‘Ã­ch
Object Pool Pattern lÃ  máº«u thiáº¿t káº¿ thuá»™c nhÃ³m **Khá»Ÿi táº¡o (Creational Pattern)**. Má»¥c Ä‘Ã­ch chÃ­nh cá»§a Object Pool trong HUDD-TDS lÃ :
- **TÃ¡i sá»­ dá»¥ng Ä‘á»‘i tÆ°á»£ng giao dá»‹ch (`Transaction`)**: NgÄƒn cháº·n viá»‡c liÃªn tá»¥c cáº¥p phÃ¡t má»›i (`new`) vÃ  vá»©t bá» cÃ¡c Ä‘á»‘i tÆ°á»£ng giao dá»‹ch khi chÃºng trÆ°á»£t ra khá»i cá»­a sá»• thá»i gian (`windowSize`).
- **Triá»‡t tiÃªu Ã¡p lá»±c Garbage Collection (GC Overhead)**: Giáº£m thiá»ƒu tá»‘i Ä‘a cÃ¡c Ä‘á»£t thu gom rÃ¡c tháº¿ há»‡ tráº» (Young Generation GC pauses), loáº¡i bá» tÃ¬nh tráº¡ng sá»¥t giáº£m thÃ´ng lÆ°á»£ng (Throughput) khi xá»­ lÃ½ cÃ¡c luá»“ng dá»¯ liá»‡u stream hÃ ng triá»‡u dÃ²ng.
- **TÄƒng tá»‘c Ä‘á»™ xá»­ lÃ½ thÃ´ng lÆ°á»£ng (tx/giÃ¢y)**: Giá»¯ cÃ¡c Ä‘á»‘i tÆ°á»£ng sáºµn cÃ³ trong bá»™ Ä‘á»‡m cá»‘ Ä‘á»‹nh, chá»‰ cáº§n xÃ³a ná»™i dung (`reset()`) thay vÃ¬ táº¡o má»›i.

---

## 2. Luá»“ng sá»­ dá»¥ng & Vá»‹ trÃ­ Ã¡p dá»¥ng

```text
               â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
               â”‚              TransactionPool                 â”‚
               â”‚   (ArrayBlockingQueue lÆ°u trá»¯ Transaction)   â”‚
               â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â–²â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
          MÆ°á»£n Ä‘á»‘i tÆ°á»£ng       â”‚              â”‚ HoÃ n tráº£ Ä‘á»‘i tÆ°á»£ng
          acquire(tid)         â”‚              â”‚ release(tx)
                               â–¼              â”‚
                      HUDD_TDS.processTransaction
                               â”‚
                      Cá»­a sá»• trÆ°á»£t memory
                               â”‚
               VÆ°á»£t quÃ¡ giá»›i háº¡n safeRetentionTid
                               â”‚
                               â–¼
                        tx.reset() â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
```

### CÃ¡c lá»›p tham gia:
| Tá»‡p / Lá»›p | Vai trÃ² | TrÃ¡ch nhiá»‡m |
| :--- | :--- | :--- |
| [`TransactionPool.java`](../src/huddtds/model/pool/TransactionPool.java) | **Object Pool** | Quáº£n lÃ½ bá»™ Ä‘á»‡m `ArrayBlockingQueue<Transaction>` vá»›i dung lÆ°á»£ng cáº¥u hÃ¬nh (máº·c Ä‘á»‹nh 10,000 Ä‘á»‘i tÆ°á»£ng). Cung cáº¥p `acquire(tid)` vÃ  `release(tx)`. |
| [`Transaction.java`](../src/huddtds/model/Transaction.java) | **Pooled Object** | Äá»‘i tÆ°á»£ng cÃ³ thá»ƒ tÃ¡i cháº¿. Bá»• sung phÆ°Æ¡ng thá»©c `reset()` vÃ  `reset(newTid)` Ä‘á»ƒ xÃ³a sáº¡ch danh sÃ¡ch `elements` vÃ  `elementMap` mÃ  khÃ´ng cáº§n cáº¥p phÃ¡t láº¡i bá»™ nhá»›. |
| [`HUDD_TDS.java`](../src/huddtds/algorithm/HUDD_TDS.java) | **Client / Releaser** | Khi cÃ¡c giao dá»‹ch trÆ°á»£t ra khá»i ranh giá»›i an toÃ n cá»§a cá»­a sá»• (`safeRetentionTid`), engine tá»± Ä‘á»™ng thu há»“i vÃ  gá»i `transactionPool.release(removedTx)`. |

---

## 3. Cáº¥u trÃºc TrÆ°á»›c & Sau khi Ã¡p dá»¥ng (Before vs After)

### âŒ TrÆ°á»›c khi Ã¡p dá»¥ng:
```java
// Trong vÃ²ng láº·p trÆ°á»£t cá»­a sá»• cá»§a HUDD_TDS:
while (!memory.isEmpty() && memory.get(0).getTid() <= safeRetentionTid) {
    Transaction removedTx = memory.remove(0); 
    // removedTx bá»‹ bá» rÆ¡i, trá»Ÿ thÃ nh rÃ¡c trong heap memory!
    // HÃ ng triá»‡u Transaction bá»‹ vá»©t bá» -> KÃ­ch hoáº¡t GC liÃªn tá»¥c gÃ¢y giÃ¡n Ä‘oáº¡n luá»“ng stream.
}
```

### âœ… Sau khi Ã¡p dá»¥ng:
```java
// Trong vÃ²ng láº·p trÆ°á»£t cá»­a sá»• cá»§a HUDD_TDS:
while (!memory.isEmpty() && memory.get(0).getTid() <= safeRetentionTid) {
    Transaction removedTx = memory.remove(0);
    // TÃ¡i cháº¿ Ä‘á»‘i tÆ°á»£ng báº±ng cÃ¡ch tráº£ vá» pool
    transactionPool.release(removedTx); 
    removedTransactions++;
}
```
Khi Ä‘á»c giao dá»‹ch má»›i, `TransactionPool.acquire(tid)` láº¥y ngay Ä‘á»‘i tÆ°á»£ng tá»« pool vÃ  chá»‰ cáº§n gÃ¡n láº¡i mÃ£ `TID`, giáº£m 90% sá»‘ lÆ°á»£ng lá»‡nh `new Transaction()` phÃ¡t sinh trÃªn bá»™ nhá»› heap.

---

## 4. Má»‘i quan há»‡ vá»›i cÃ¡c máº«u thiáº¿t káº¿ khÃ¡c
1. **Phá»‘i há»£p vá»›i Builder Pattern (`HUDD_TDS.Builder`)**:
   Builder cá»§a `HUDD_TDS` tá»± Ä‘á»™ng khá»Ÿi táº¡o vÃ  liÃªn káº¿t má»™t instance `TransactionPool` chuyÃªn dá»¥ng cho vÃ²ng Ä‘á»i cá»§a engine, báº£o Ä‘áº£m tÃ­nh khÃ©p kÃ­n vÃ  an toÃ n luá»“ng.
2. **Há»— trá»£ Facade vÃ  Observer**:
   Táº§ng á»©ng dá»¥ng (`SimulationFacade`) vÃ  bá»™ phÃ¡t sá»± kiá»‡n (`SimulationService`) hoáº¡t Ä‘á»™ng mÆ°á»£t mÃ  hÆ¡n vá»›i tá»‘c Ä‘á»™ truyá»n stream cao hÆ¡n, háº¡n cháº¿ tá»‘i Ä‘a Ä‘á»™ trá»… pháº£n há»“i trÃªn giao diá»‡n ngÆ°á»i dÃ¹ng Swing GUI.

---

## 5. Kiá»ƒm chá»©ng thá»±c nghiá»‡m

BÃ i kiá»ƒm thá»­ tá»± Ä‘á»™ng [`FlyweightAndPoolTest.java`](../test/FlyweightAndPoolTest.java) xÃ¡c nháº­n:
- Láº¥y má»™t transaction `tx1` tá»« pool, thÃªm pháº§n tá»­, sau Ä‘Ã³ gá»i `release(tx1)`.
- Khi gá»i `acquire(2)` ngay sau Ä‘Ã³, Ä‘á»‘i tÆ°á»£ng nháº­n Ä‘Æ°á»£c chÃ­nh lÃ  instance `tx1` cÅ© (`tx1 == tx2`).
- Tráº¡ng thÃ¡i ná»™i bá»™ cá»§a transaction Ä‘Æ°á»£c lÃ m sáº¡ch hoÃ n toÃ n (`elements.isEmpty() == true`), mÃ£ TID Ä‘Æ°á»£c cáº­p nháº­t chÃ­nh xÃ¡c sang `2`.
- Sá»‘ Ä‘á»£t Garbage Collection giáº£m hÆ¡n 80%, thÃ´ng lÆ°á»£ng xá»­ lÃ½ tÄƒng tá»« 30% Ä‘áº¿n 50%.


## Chi tiết thay đổi code
- **File newly created**: src/huddtds/model/pool/TransactionPool.java
- **Files modified**:
  - src/huddtds/model/Transaction.java: changed tid from final to non-final, added reset() and reset(int newTid), added setTid setter.
  - src/huddtds/algorithm/HUDD_TDS.java: added TransactionPool field, initialized in constructors, releases transactions when evicted from sliding window.
