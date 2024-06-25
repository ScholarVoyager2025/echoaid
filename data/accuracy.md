1. **PDD Fruits: Panzhihua Mango**  
   Score: TTTFFFTTTT=7. Successfully identified "insufficient weight, free delivery" as after-sales service. The error occurred when the product sub-product was mistaken as the product name.

2. **PDD Make-up Products: Pore Cleanser**  
   Score: TTTTTTTFF=8. Understood the host's metaphor, identified the counter price and the actual price, but the actual price display was incorrect.

3. **PDD Clothing: Dress**  
   Score: FTTTTTTTTT=9. Captured different prices and listed them simultaneously. Understood the size range. However, during the first condensed extraction, the prompt was forgotten.

4. **PDD Furniture: Solid Wood Sofa**  
   Score: FFFFTTTTTT=6. Captured the original price, discount price, and after-sales service (three times compensation for fakes), but Xunfei misidentified "cushion" as "electronics"/"battery" four times, misleading the LLM. More context gradually helped the LLM to respond correctly.

5. **Kwai Fruits: Fresh Fruits**  
   Score: FFTTTTTTTF=7. Inaccurate identification at the start, producing meaningless output. Finally, the sub-product (Fruit King) was identified as the product name.

6. **Kwai Make-up Products: Various Make-up Products**  
   Score: TTTFFTTTTT=8. Background noise and unclear speech were present, but multiple product keyword frameworks were successfully generated automatically. However, the erroneous identification of the "tea" transcript led to false products being included two times.

7. **Kwai Clothing: Cotton Clothing**  
   Score: TTTTFTTTTT=9. Identified limited-time purchase information and marked the original price and the purchase price. The original price was 59 yuan, the current price was 9.9 yuan, with one identification of 5.9 yuan.

8. **Kwai Furniture: Simmons Mattress**  
   Score: TTTTTTTTTT=10. Identification was more and more accurate until the end.

9. **TikTok Volcano Fruits: Mango**  
   Score: FFFFTFTTTT=5. Misidentified 9 pounds as alcohol, leading the LLM off track.

10. **TikTok Volcano Make-up Products: Cheap Cosmetics**  
    Score: FTTTTTTTTT=9. Automatically distinguished different products. The first one had no meaningful words, resulting in hallucinations.

11. **TikTok Volcano Furniture: Sofa**  
    Score: TTTTTTTTTT=10. Identification was very accurate until the end.

12. **TikTok Volcano Clothing: Cotton T-Shirt**  
    Score: TFFFFFFTT=3. Discussed color, guessed it was clothing. However, the product name was misidentified as a sub-product's name (dragon fruit-colored rose-red clothes), leading to persistent errors in the product name display.

13. **Taobao Fruits: Red Fuji Apple**  
    Score: FTFFFFFFFF=1. 29.9 yuan for 9 pounds, but "King of the Fruits" was misidentified as "King of the Country", "Big Fruit" as "Big Country", "Medium Fruit" as "China", and "9 pounds" as "alcohol," leading to a misidentification of 29.9 yuan per pound. The product name was also misidentified as sub-product "King of the Fruits".

14. **Taobao Make-up Products: Mini Highlighter**  
    Score: TTTTTTTTTT=10. Continuously improved and correctly identified. Knowing "rice" is a slang for the Chinese currency "yuan" and understanding the meaning.

15. **Taobao Clothing: Dress**  
    Score: TTTTFFFTTT=7. Misidentified the sub-product as the main product and forgot the instructions, treating it as a math problem.

16. **Taobao Furniture: Bed**  
    Score: TTTTTTTTTT=10. The host did not introduce specific products much, mostly reading the script.

17. **Xigua Video Fruits: Durian**  
    Score: TTFTFTFFFF=4. 89 yuan reduced to 49 yuan was identified as 19 and 119 yuan, leading the LLM off track.

18. **Xigua Make-up Products: Essence Water + Eye Cream**  
    Score: TTTTTTTTTT=10.

19. **Xigua Clothing: T-Shirt**  
    Score: TTTTTTTTTT=10.

20. **Xigua Furniture: TV Cabinet**  
    Score: TTTTTTTTTT=10.
