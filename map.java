/*      */ package com.contrastsecurity.agent.weakmap;
/*      */ 
/*      */ import com.contrastsecurity.agent.DontObfuscate;
/*      */ import com.contrastsecurity.agent.util.z;
/*      */ import java.io.IOException;
/*      */ import java.io.ObjectInputStream;
/*      */ import java.io.PrintStream;
/*      */ import java.io.Serializable;
/*      */ import java.lang.ref.Reference;
/*      */ import java.lang.ref.ReferenceQueue;
/*      */ import java.lang.ref.SoftReference;
/*      */ import java.lang.ref.WeakReference;
/*      */ import java.util.AbstractCollection;
/*      */ import java.util.AbstractMap;
/*      */ import java.util.AbstractSet;
/*      */ import java.util.Collection;
/*      */ import java.util.EnumSet;
/*      */ import java.util.Enumeration;
/*      */ import java.util.Iterator;
/*      */ import java.util.Map;
/*      */ import java.util.Map.Entry;
/*      */ import java.util.NoSuchElementException;
/*      */ import java.util.Set;
/*      */ import java.util.concurrent.ConcurrentHashMap;
/*      */ import java.util.concurrent.ConcurrentMap;
/*      */ import java.util.concurrent.locks.ReentrantLock;
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ @DontObfuscate
/*      */ public class ConcurrentReferenceHashMap<K, V>
/*      */   extends AbstractMap<K, V>
/*      */   implements Serializable, ConcurrentMap<K, V>
/*      */ {
/*      */   private static final long serialVersionUID = 7249069246763182397L;
/*  145 */   static final c DEFAULT_KEY_TYPE = c.b;
/*      */   
/*  147 */   static final c DEFAULT_VALUE_TYPE = c.a;
/*      */   
/*      */ 
/*      */ 
/*      */ 
/*      */   static final int DEFAULT_INITIAL_CAPACITY = 16;
/*      */   
/*      */ 
/*      */ 
/*      */ 
/*      */   static final float DEFAULT_LOAD_FACTOR = 0.75F;
/*      */   
/*      */ 
/*      */ 
/*      */   static final int DEFAULT_CONCURRENCY_LEVEL = 16;
/*      */   
/*      */ 
/*      */ 
/*      */   static final int MAXIMUM_CAPACITY = 1073741824;
/*      */   
/*      */ 
/*      */ 
/*      */   static final int MAX_SEGMENTS = 65536;
/*      */   
/*      */ 
/*      */ 
/*      */   static final int RETRIES_BEFORE_LOCK = 2;
/*      */   
/*      */ 
/*      */ 
/*      */   final int segmentMask;
/*      */   
/*      */ 
/*      */ 
/*      */   final int segmentShift;
/*      */   
/*      */ 
/*      */ 
/*      */   final Segment<K, V>[] segments;
/*      */   
/*      */ 
/*      */ 
/*      */   boolean identityComparisons;
/*      */   
/*      */ 
/*      */ 
/*      */   transient Set<K> keySet;
/*      */   
/*      */ 
/*      */ 
/*      */   transient Set<Map.Entry<K, V>> entrySet;
/*      */   
/*      */ 
/*      */ 
/*      */   transient Collection<V> values;
/*      */   
/*      */ 
/*      */ 
/*      */   private boolean trackTargets;
/*      */   
/*      */ 
/*      */ 
/*      */ 
/*      */   private static int hash(int paramInt)
/*      */   {
/*  212 */     paramInt += (paramInt << 15 ^ 0xCD7D);
/*  213 */     paramInt ^= paramInt >>> 10;
/*  214 */     paramInt += (paramInt << 3);
/*  215 */     paramInt ^= paramInt >>> 6;
/*  216 */     paramInt += (paramInt << 2) + (paramInt << 14);
/*  217 */     return paramInt ^ paramInt >>> 16;
/*      */   }
/*      */   
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   final Segment<K, V> segmentFor(int paramInt)
/*      */   {
/*  227 */     return segments[(paramInt >>> segmentShift & segmentMask)];
/*      */   }
/*      */   
/*      */ 
/*      */ 
/*      */ 
/*  233 */   Map<String, Long> hashTargets = new ConcurrentHashMap();
/*      */   
/*      */   private int hashOf(Object paramObject) {
/*  236 */     if (trackTargets) {
/*  237 */       String str = paramObject.getClass().getName();
/*  238 */       Long localLong = (Long)hashTargets.get(str);
/*  239 */       if (localLong == null) {
/*  240 */         localLong = Long.valueOf(0L);
/*      */       }
/*  242 */       hashTargets.put(str, Long.valueOf(localLong.longValue() + 1L));
/*      */     }
/*      */     
/*  245 */     int i = z.a(paramObject);
/*      */     
/*  247 */     return i;
/*      */   }
/*      */   
/*      */   public void dumpStats() {
/*  251 */     if (trackTargets) {
/*  252 */       Set localSet = hashTargets.keySet();
/*  253 */       for (String str : localSet) {
/*  254 */         System.out.println(str + "," + hashTargets.get(str));
/*      */       }
/*      */     }
/*      */   }
/*      */   
/*      */ 
/*      */ 
/*      */ 
/*      */   @DontObfuscate
/*      */   static final class WeakKeyReference<K>
/*      */     extends WeakReference<K>
/*      */     implements ConcurrentReferenceHashMap.a
/*      */   {
/*      */     final int hash;
/*      */     
/*      */ 
/*      */ 
/*      */     WeakKeyReference(K paramK, int paramInt, ReferenceQueue<Object> paramReferenceQueue)
/*      */     {
/*  273 */       super(paramReferenceQueue);
/*  274 */       hash = paramInt;
/*      */     }
/*      */     
/*      */     public int keyHash() {
/*  278 */       return hash;
/*      */     }
/*      */     
/*      */     public Object keyRef() {
/*  282 */       return this;
/*      */     }
/*      */   }
/*      */   
/*      */   static final class d<K> extends SoftReference<K> implements ConcurrentReferenceHashMap.a
/*      */   {
/*      */     final int a;
/*      */     
/*      */     d(K paramK, int paramInt, ReferenceQueue<Object> paramReferenceQueue) {
/*  291 */       super(paramReferenceQueue);
/*  292 */       a = paramInt;
/*      */     }
/*      */     
/*      */     public int keyHash() {
/*  296 */       return a;
/*      */     }
/*      */     
/*      */     public Object keyRef() {
/*  300 */       return this;
/*      */     }
/*      */   }
/*      */   
/*      */   static final class f<V> extends WeakReference<V> implements ConcurrentReferenceHashMap.a {
/*      */     final Object a;
/*      */     final int b;
/*      */     
/*      */     f(V paramV, Object paramObject, int paramInt, ReferenceQueue<Object> paramReferenceQueue) {
/*  309 */       super(paramReferenceQueue);
/*  310 */       a = paramObject;
/*  311 */       b = paramInt;
/*      */     }
/*      */     
/*      */     public int keyHash() {
/*  315 */       return b;
/*      */     }
/*      */     
/*      */     public Object keyRef() {
/*  319 */       return a;
/*      */     }
/*      */   }
/*      */   
/*      */   static final class e<V> extends SoftReference<V> implements ConcurrentReferenceHashMap.a {
/*      */     final Object a;
/*      */     final int b;
/*      */     
/*      */     e(V paramV, Object paramObject, int paramInt, ReferenceQueue<Object> paramReferenceQueue) {
/*  328 */       super(paramReferenceQueue);
/*  329 */       a = paramObject;
/*  330 */       b = paramInt;
/*      */     }
/*      */     
/*      */     public int keyHash() {
/*  334 */       return b;
/*      */     }
/*      */     
/*      */     public Object keyRef() {
/*  338 */       return a;
/*      */     }
/*      */   }
/*      */   
/*      */ 
/*      */ 
/*      */ 
/*      */   @DontObfuscate
/*      */   static final class HashEntry<K, V>
/*      */   {
/*      */     final Object keyRef;
/*      */     
/*      */ 
/*      */ 
/*      */     final int hash;
/*      */     
/*      */ 
/*      */ 
/*      */     volatile Object valueRef;
/*      */     
/*      */ 
/*      */ 
/*      */     final HashEntry<K, V> next;
/*      */     
/*      */ 
/*      */ 
/*      */ 
/*      */     HashEntry(K paramK, int paramInt, HashEntry<K, V> paramHashEntry, V paramV, ConcurrentReferenceHashMap.c paramc1, ConcurrentReferenceHashMap.c paramc2, ReferenceQueue<Object> paramReferenceQueue)
/*      */     {
/*  367 */       hash = paramInt;
/*  368 */       next = paramHashEntry;
/*  369 */       keyRef = newKeyReference(paramK, paramc1, paramReferenceQueue);
/*  370 */       valueRef = newValueReference(paramV, paramc2, paramReferenceQueue);
/*      */     }
/*      */     
/*      */     Object newKeyReference(K paramK, ConcurrentReferenceHashMap.c paramc, ReferenceQueue<Object> paramReferenceQueue) {
/*  374 */       if (paramc == ConcurrentReferenceHashMap.c.b) return new ConcurrentReferenceHashMap.WeakKeyReference(paramK, hash, paramReferenceQueue);
/*  375 */       if (paramc == ConcurrentReferenceHashMap.c.c) { return new ConcurrentReferenceHashMap.d(paramK, hash, paramReferenceQueue);
/*      */       }
/*  377 */       return paramK;
/*      */     }
/*      */     
/*      */     Object newValueReference(V paramV, ConcurrentReferenceHashMap.c paramc, ReferenceQueue<Object> paramReferenceQueue) {
/*  381 */       if (paramc == ConcurrentReferenceHashMap.c.b)
/*  382 */         return new ConcurrentReferenceHashMap.f(paramV, keyRef, hash, paramReferenceQueue);
/*  383 */       if (paramc == ConcurrentReferenceHashMap.c.c) {
/*  384 */         return new ConcurrentReferenceHashMap.e(paramV, keyRef, hash, paramReferenceQueue);
/*      */       }
/*  386 */       return paramV;
/*      */     }
/*      */     
/*      */     K key()
/*      */     {
/*  391 */       if ((keyRef instanceof ConcurrentReferenceHashMap.a)) { return (K)((Reference)keyRef).get();
/*      */       }
/*  393 */       return (K)keyRef;
/*      */     }
/*      */     
/*      */     V value() {
/*  397 */       return (V)dereferenceValue(valueRef);
/*      */     }
/*      */     
/*      */     V dereferenceValue(Object paramObject)
/*      */     {
/*  402 */       if ((paramObject instanceof ConcurrentReferenceHashMap.a)) { return (V)((Reference)paramObject).get();
/*      */       }
/*  404 */       return (V)paramObject;
/*      */     }
/*      */     
/*      */     void setValue(V paramV, ConcurrentReferenceHashMap.c paramc, ReferenceQueue<Object> paramReferenceQueue) {
/*  408 */       valueRef = newValueReference(paramV, paramc, paramReferenceQueue);
/*      */     }
/*      */     
/*      */     static <K, V> HashEntry<K, V>[] newArray(int paramInt)
/*      */     {
/*  413 */       return new HashEntry[paramInt];
/*      */     }
/*      */   }
/*      */   
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   @DontObfuscate
/*      */   static final class Segment<K, V>
/*      */     extends ReentrantLock
/*      */     implements Serializable
/*      */   {
/*      */     private static final long serialVersionUID = 2249069246763182397L;
/*      */     
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */     volatile transient int count;
/*      */     
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */     transient int modCount;
/*      */     
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */     transient int threshold;
/*      */     
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */     volatile transient ConcurrentReferenceHashMap.HashEntry<K, V>[] table;
/*      */     
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */     final float loadFactor;
/*      */     
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */     volatile transient ReferenceQueue<Object> refQueue;
/*      */     
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */     final ConcurrentReferenceHashMap.c keyType;
/*      */     
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */     final ConcurrentReferenceHashMap.c valueType;
/*      */     
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */     final boolean identityComparisons;
/*      */     
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */     Segment(int paramInt, float paramFloat, ConcurrentReferenceHashMap.c paramc1, ConcurrentReferenceHashMap.c paramc2, boolean paramBoolean)
/*      */     {
/*  508 */       loadFactor = paramFloat;
/*  509 */       keyType = paramc1;
/*  510 */       valueType = paramc2;
/*  511 */       identityComparisons = paramBoolean;
/*  512 */       setTable(ConcurrentReferenceHashMap.HashEntry.newArray(paramInt));
/*      */     }
/*      */     
/*      */     static <K, V> Segment<K, V>[] newArray(int paramInt)
/*      */     {
/*  517 */       return new Segment[paramInt];
/*      */     }
/*      */     
/*      */ 
/*      */     private boolean keyEq(Object paramObject1, Object paramObject2)
/*      */     {
/*  523 */       return paramObject1 == paramObject2;
/*      */     }
/*      */     
/*      */     void setTable(ConcurrentReferenceHashMap.HashEntry<K, V>[] paramArrayOfHashEntry)
/*      */     {
/*  528 */       threshold = ((int)(paramArrayOfHashEntry.length * loadFactor));
/*  529 */       table = paramArrayOfHashEntry;
/*  530 */       refQueue = new ReferenceQueue();
/*      */     }
/*      */     
/*      */     ConcurrentReferenceHashMap.HashEntry<K, V> getFirst(int paramInt)
/*      */     {
/*  535 */       ConcurrentReferenceHashMap.HashEntry[] arrayOfHashEntry = table;
/*  536 */       return arrayOfHashEntry[(paramInt & arrayOfHashEntry.length - 1)];
/*      */     }
/*      */     
/*      */     ConcurrentReferenceHashMap.HashEntry<K, V> newHashEntry(K paramK, int paramInt, ConcurrentReferenceHashMap.HashEntry<K, V> paramHashEntry, V paramV) {
/*  540 */       return new ConcurrentReferenceHashMap.HashEntry(paramK, paramInt, paramHashEntry, paramV, keyType, valueType, refQueue);
/*      */     }
/*      */     
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */     V readValueUnderLock(ConcurrentReferenceHashMap.HashEntry<K, V> paramHashEntry)
/*      */     {
/*  549 */       lock();
/*      */       try {
/*  551 */         removeStale();
/*  552 */         return (V)paramHashEntry.value();
/*      */       } finally {
/*  554 */         unlock();
/*      */       }
/*      */     }
/*      */     
/*      */ 
/*      */     V get(Object paramObject, int paramInt)
/*      */     {
/*  561 */       if (count != 0) {
/*  562 */         ConcurrentReferenceHashMap.HashEntry localHashEntry = getFirst(paramInt);
/*  563 */         while (localHashEntry != null) {
/*  564 */           if ((hash == paramInt) && (keyEq(paramObject, localHashEntry.key()))) {
/*  565 */             Object localObject = valueRef;
/*  566 */             if (localObject != null) { return (V)localHashEntry.dereferenceValue(localObject);
/*      */             }
/*  568 */             return (V)readValueUnderLock(localHashEntry);
/*      */           }
/*  570 */           localHashEntry = next;
/*      */         }
/*      */       }
/*  573 */       return null;
/*      */     }
/*      */     
/*      */     boolean containsKey(Object paramObject, int paramInt) {
/*  577 */       if (count != 0) {
/*  578 */         ConcurrentReferenceHashMap.HashEntry localHashEntry = getFirst(paramInt);
/*  579 */         while (localHashEntry != null) {
/*  580 */           if ((hash == paramInt) && (keyEq(paramObject, localHashEntry.key()))) return true;
/*  581 */           localHashEntry = next;
/*      */         }
/*      */       }
/*  584 */       return false;
/*      */     }
/*      */     
/*      */     boolean containsValue(Object paramObject) {
/*  588 */       if (count != 0) {
/*  589 */         ConcurrentReferenceHashMap.HashEntry[] arrayOfHashEntry = table;
/*  590 */         int i = arrayOfHashEntry.length;
/*  591 */         for (int j = 0; j < i; j++) {
/*  592 */           for (ConcurrentReferenceHashMap.HashEntry localHashEntry = arrayOfHashEntry[j]; localHashEntry != null; localHashEntry = next) {
/*  593 */             Object localObject1 = valueRef;
/*      */             
/*      */             Object localObject2;
/*  596 */             if (localObject1 == null) localObject2 = readValueUnderLock(localHashEntry); else {
/*  597 */               localObject2 = localHashEntry.dereferenceValue(localObject1);
/*      */             }
/*  599 */             if (paramObject.equals(localObject2)) return true;
/*      */           }
/*      */         }
/*      */       }
/*  603 */       return false;
/*      */     }
/*      */     
/*      */     boolean replace(K paramK, int paramInt, V paramV1, V paramV2) {
/*  607 */       lock();
/*      */       try {
/*  609 */         removeStale();
/*  610 */         ConcurrentReferenceHashMap.HashEntry localHashEntry = getFirst(paramInt);
/*  611 */         while ((localHashEntry != null) && ((hash != paramInt) || (!keyEq(paramK, localHashEntry.key())))) { localHashEntry = next;
/*      */         }
/*  613 */         boolean bool1 = false;
/*  614 */         if ((localHashEntry != null) && (paramV1.equals(localHashEntry.value()))) {
/*  615 */           bool1 = true;
/*  616 */           localHashEntry.setValue(paramV2, valueType, refQueue);
/*      */         }
/*  618 */         return bool1;
/*      */       } finally {
/*  620 */         unlock();
/*      */       }
/*      */     }
/*      */     
/*      */     V replace(K paramK, int paramInt, V paramV) {
/*  625 */       lock();
/*      */       try {
/*  627 */         removeStale();
/*  628 */         ConcurrentReferenceHashMap.HashEntry localHashEntry = getFirst(paramInt);
/*  629 */         while ((localHashEntry != null) && ((hash != paramInt) || (!keyEq(paramK, localHashEntry.key())))) { localHashEntry = next;
/*      */         }
/*  631 */         Object localObject1 = null;
/*  632 */         if (localHashEntry != null) {
/*  633 */           localObject1 = localHashEntry.value();
/*  634 */           localHashEntry.setValue(paramV, valueType, refQueue);
/*      */         }
/*  636 */         return (V)localObject1;
/*      */       } finally {
/*  638 */         unlock();
/*      */       }
/*      */     }
/*      */     
/*      */     V put(K paramK, int paramInt, V paramV, boolean paramBoolean) {
/*  643 */       lock();
/*      */       try {
/*  645 */         removeStale();
/*  646 */         int i = count;
/*  647 */         if (i++ > threshold) {
/*  648 */           int j = rehash();
/*  649 */           if (j > 0) {
/*  650 */             count = (i -= j - 1);
/*      */           }
/*      */         }
/*  653 */         ConcurrentReferenceHashMap.HashEntry[] arrayOfHashEntry = table;
/*  654 */         int k = paramInt & arrayOfHashEntry.length - 1;
/*  655 */         ConcurrentReferenceHashMap.HashEntry localHashEntry1 = arrayOfHashEntry[k];
/*  656 */         ConcurrentReferenceHashMap.HashEntry localHashEntry2 = localHashEntry1;
/*  657 */         while ((localHashEntry2 != null) && ((hash != paramInt) || (!keyEq(paramK, localHashEntry2.key())))) { localHashEntry2 = next;
/*      */         }
/*      */         Object localObject1;
/*  660 */         if (localHashEntry2 != null) {
/*  661 */           localObject1 = localHashEntry2.value();
/*  662 */           if (!paramBoolean) localHashEntry2.setValue(paramV, valueType, refQueue);
/*      */         } else {
/*  664 */           localObject1 = null;
/*  665 */           modCount += 1;
/*  666 */           arrayOfHashEntry[k] = newHashEntry(paramK, paramInt, localHashEntry1, paramV);
/*  667 */           count = i;
/*      */         }
/*  669 */         return (V)localObject1;
/*      */       } finally {
/*  671 */         unlock();
/*      */       }
/*      */     }
/*      */     
/*      */     int rehash() {
/*  676 */       ConcurrentReferenceHashMap.HashEntry[] arrayOfHashEntry1 = table;
/*  677 */       int i = arrayOfHashEntry1.length;
/*  678 */       if (i >= 1073741824) { return 0;
/*      */       }
/*      */       
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*  694 */       ConcurrentReferenceHashMap.HashEntry[] arrayOfHashEntry2 = ConcurrentReferenceHashMap.HashEntry.newArray(i << 1);
/*  695 */       threshold = ((int)(arrayOfHashEntry2.length * loadFactor));
/*  696 */       int j = arrayOfHashEntry2.length - 1;
/*  697 */       int k = 0;
/*  698 */       for (int m = 0; m < i; m++)
/*      */       {
/*      */ 
/*  701 */         ConcurrentReferenceHashMap.HashEntry localHashEntry1 = arrayOfHashEntry1[m];
/*      */         
/*  703 */         if (localHashEntry1 != null) {
/*  704 */           ConcurrentReferenceHashMap.HashEntry localHashEntry2 = next;
/*  705 */           int n = hash & j;
/*      */           
/*      */ 
/*  708 */           if (localHashEntry2 == null) { arrayOfHashEntry2[n] = localHashEntry1;
/*      */           }
/*      */           else {
/*  711 */             Object localObject1 = localHashEntry1;
/*  712 */             int i1 = n;
/*  713 */             for (ConcurrentReferenceHashMap.HashEntry localHashEntry3 = localHashEntry2; localHashEntry3 != null; localHashEntry3 = next) {
/*  714 */               int i2 = hash & j;
/*  715 */               if (i2 != i1) {
/*  716 */                 i1 = i2;
/*  717 */                 localObject1 = localHashEntry3;
/*      */               }
/*      */             }
/*  720 */             arrayOfHashEntry2[i1] = localObject1;
/*      */             
/*  722 */             for (localHashEntry3 = localHashEntry1; localHashEntry3 != localObject1; localHashEntry3 = next)
/*      */             {
/*  724 */               Object localObject2 = localHashEntry3.key();
/*  725 */               if (localObject2 == null) {
/*  726 */                 k++;
/*      */               }
/*      */               else {
/*  729 */                 int i3 = hash & j;
/*  730 */                 ConcurrentReferenceHashMap.HashEntry localHashEntry4 = arrayOfHashEntry2[i3];
/*  731 */                 arrayOfHashEntry2[i3] = newHashEntry(localObject2, hash, localHashEntry4, localHashEntry3.value());
/*      */               }
/*      */             }
/*      */           }
/*      */         } }
/*  736 */       table = arrayOfHashEntry2;
/*  737 */       return k;
/*      */     }
/*      */     
/*      */     V remove(Object paramObject1, int paramInt, Object paramObject2, boolean paramBoolean)
/*      */     {
/*  742 */       lock();
/*      */       try {
/*  744 */         if (!paramBoolean) removeStale();
/*  745 */         int i = count - 1;
/*  746 */         ConcurrentReferenceHashMap.HashEntry[] arrayOfHashEntry = table;
/*  747 */         int j = paramInt & arrayOfHashEntry.length - 1;
/*  748 */         ConcurrentReferenceHashMap.HashEntry localHashEntry1 = arrayOfHashEntry[j];
/*  749 */         ConcurrentReferenceHashMap.HashEntry localHashEntry2 = localHashEntry1;
/*      */         
/*  751 */         while ((localHashEntry2 != null) && 
/*  752 */           (paramObject1 != keyRef) && (
/*  753 */           (paramBoolean) || (paramInt != hash) || (!keyEq(paramObject1, localHashEntry2.key())))) { localHashEntry2 = next;
/*      */         }
/*  755 */         Object localObject1 = null;
/*  756 */         if (localHashEntry2 != null) {
/*  757 */           Object localObject2 = localHashEntry2.value();
/*  758 */           if ((paramObject2 == null) || (paramObject2.equals(localObject2))) {
/*  759 */             localObject1 = localObject2;
/*      */             
/*      */ 
/*      */ 
/*  763 */             modCount += 1;
/*  764 */             ConcurrentReferenceHashMap.HashEntry localHashEntry3 = next;
/*  765 */             for (ConcurrentReferenceHashMap.HashEntry localHashEntry4 = localHashEntry1; localHashEntry4 != localHashEntry2; localHashEntry4 = next) {
/*  766 */               Object localObject3 = localHashEntry4.key();
/*  767 */               if (localObject3 == null) {
/*  768 */                 i--;
/*      */               }
/*      */               else
/*      */               {
/*  772 */                 localHashEntry3 = newHashEntry(localObject3, hash, localHashEntry3, localHashEntry4.value()); }
/*      */             }
/*  774 */             arrayOfHashEntry[j] = localHashEntry3;
/*  775 */             count = i;
/*      */           }
/*      */         }
/*  778 */         return (V)localObject1;
/*      */       } finally {
/*  780 */         unlock();
/*      */       }
/*      */     }
/*      */     
/*      */     void removeStale() {
/*      */       ConcurrentReferenceHashMap.a locala;
/*  786 */       while ((locala = (ConcurrentReferenceHashMap.a)refQueue.poll()) != null) {
/*  787 */         remove(locala.keyRef(), locala.keyHash(), null, true);
/*      */       }
/*      */     }
/*      */     
/*      */     /* Error */
/*      */     void clear()
/*      */     {
/*      */       // Byte code:
/*      */       //   0: aload_0
/*      */       //   1: getfield 101	com/contrastsecurity/agent/weakmap/ConcurrentReferenceHashMap$Segment:count	I
/*      */       //   4: ifeq +70 -> 74
/*      */       //   7: aload_0
/*      */       //   8: invokevirtual 87	com/contrastsecurity/agent/weakmap/ConcurrentReferenceHashMap$Segment:lock	()V
/*      */       //   11: aload_0
/*      */       //   12: getfield 70	com/contrastsecurity/agent/weakmap/ConcurrentReferenceHashMap$Segment:table	[Lcom/contrastsecurity/agent/weakmap/ConcurrentReferenceHashMap$HashEntry;
/*      */       //   15: astore_1
/*      */       //   16: iconst_0
/*      */       //   17: istore_2
/*      */       //   18: goto +10 -> 28
/*      */       //   21: aload_1
/*      */       //   22: iload_2
/*      */       //   23: aconst_null
/*      */       //   24: aastore
/*      */       //   25: iinc 2 1
/*      */       //   28: iload_2
/*      */       //   29: aload_1
/*      */       //   30: arraylength
/*      */       //   31: if_icmplt -10 -> 21
/*      */       //   34: aload_0
/*      */       //   35: dup
/*      */       //   36: getfield 149	com/contrastsecurity/agent/weakmap/ConcurrentReferenceHashMap$Segment:modCount	I
/*      */       //   39: iconst_1
/*      */       //   40: iadd
/*      */       //   41: putfield 149	com/contrastsecurity/agent/weakmap/ConcurrentReferenceHashMap$Segment:modCount	I
/*      */       //   44: aload_0
/*      */       //   45: new 72	java/lang/ref/ReferenceQueue
/*      */       //   48: dup
/*      */       //   49: invokespecial 73	java/lang/ref/ReferenceQueue:<init>	()V
/*      */       //   52: putfield 75	com/contrastsecurity/agent/weakmap/ConcurrentReferenceHashMap$Segment:refQueue	Ljava/lang/ref/ReferenceQueue;
/*      */       //   55: aload_0
/*      */       //   56: iconst_0
/*      */       //   57: putfield 101	com/contrastsecurity/agent/weakmap/ConcurrentReferenceHashMap$Segment:count	I
/*      */       //   60: goto +10 -> 70
/*      */       //   63: astore_3
/*      */       //   64: aload_0
/*      */       //   65: invokevirtual 97	com/contrastsecurity/agent/weakmap/ConcurrentReferenceHashMap$Segment:unlock	()V
/*      */       //   68: aload_3
/*      */       //   69: athrow
/*      */       //   70: aload_0
/*      */       //   71: invokevirtual 97	com/contrastsecurity/agent/weakmap/ConcurrentReferenceHashMap$Segment:unlock	()V
/*      */       //   74: return
/*      */       // Line number table:
/*      */       //   Java source line #792	-> byte code offset #0
/*      */       //   Java source line #793	-> byte code offset #7
/*      */       //   Java source line #795	-> byte code offset #11
/*      */       //   Java source line #796	-> byte code offset #16
/*      */       //   Java source line #797	-> byte code offset #34
/*      */       //   Java source line #799	-> byte code offset #44
/*      */       //   Java source line #800	-> byte code offset #55
/*      */       //   Java source line #801	-> byte code offset #60
/*      */       //   Java source line #802	-> byte code offset #64
/*      */       //   Java source line #803	-> byte code offset #68
/*      */       //   Java source line #802	-> byte code offset #70
/*      */       //   Java source line #805	-> byte code offset #74
/*      */       // Local variable table:
/*      */       //   start	length	slot	name	signature
/*      */       //   0	75	0	this	Segment
/*      */       //   15	15	1	arrayOfHashEntry	ConcurrentReferenceHashMap.HashEntry[]
/*      */       //   17	15	2	i	int
/*      */       //   63	6	3	localObject	Object
/*      */       // Exception table:
/*      */       //   from	to	target	type
/*      */       //   11	63	63	finally
/*      */     }
/*      */   }
/*      */   
/*      */   public ConcurrentReferenceHashMap(int paramInt1, float paramFloat, int paramInt2, c paramc1, c paramc2, EnumSet<b> paramEnumSet)
/*      */   {
/*  836 */     if ((paramFloat <= 0.0F) || (paramInt1 < 0) || (paramInt2 <= 0)) {
/*  837 */       throw new IllegalArgumentException();
/*      */     }
/*  839 */     if (paramInt2 > 65536) { paramInt2 = 65536;
/*      */     }
/*      */     
/*  842 */     int i = 0;
/*  843 */     int j = 1;
/*  844 */     while (j < paramInt2) {
/*  845 */       i++;
/*  846 */       j <<= 1;
/*      */     }
/*  848 */     segmentShift = (32 - i);
/*  849 */     segmentMask = (j - 1);
/*  850 */     segments = Segment.newArray(j);
/*      */     
/*  852 */     if (paramInt1 > 1073741824) paramInt1 = 1073741824;
/*  853 */     int k = paramInt1 / j;
/*  854 */     if (k * j < paramInt1) k++;
/*  855 */     int m = 1;
/*  856 */     while (m < k) { m <<= 1;
/*      */     }
/*  858 */     identityComparisons = ((paramEnumSet != null) && (paramEnumSet.contains(b.a)));
/*      */     
/*  860 */     for (int n = 0; n < segments.length; n++) {
/*  861 */       segments[n] = 
/*  862 */         new Segment(m, paramFloat, paramc1, paramc2, identityComparisons);
/*      */     }
/*      */   }
/*      */   
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   public ConcurrentReferenceHashMap(int paramInt1, float paramFloat, int paramInt2)
/*      */   {
/*  879 */     this(paramInt1, paramFloat, paramInt2, DEFAULT_KEY_TYPE, DEFAULT_VALUE_TYPE, null);
/*      */   }
/*      */   
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   public ConcurrentReferenceHashMap(int paramInt, float paramFloat)
/*      */   {
/*  895 */     this(paramInt, paramFloat, 16);
/*      */   }
/*      */   
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   public ConcurrentReferenceHashMap(int paramInt, c paramc1, c paramc2)
/*      */   {
/*  910 */     this(paramInt, 0.75F, 16, paramc1, paramc2, null);
/*      */   }
/*      */   
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   public ConcurrentReferenceHashMap(c paramc1, c paramc2)
/*      */   {
/*  927 */     this(16, 0.75F, 16, paramc1, paramc2, null);
/*      */   }
/*      */   
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   public ConcurrentReferenceHashMap(c paramc1, c paramc2, EnumSet<b> paramEnumSet)
/*      */   {
/*  945 */     this(16, 0.75F, 16, paramc1, paramc2, paramEnumSet);
/*      */   }
/*      */   
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   public ConcurrentReferenceHashMap(int paramInt)
/*      */   {
/*  957 */     this(paramInt, 0.75F, 16);
/*      */   }
/*      */   
/*      */ 
/*      */ 
/*      */ 
/*      */   public ConcurrentReferenceHashMap()
/*      */   {
/*  965 */     this(16, 0.75F, 16);
/*      */   }
/*      */   
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   public ConcurrentReferenceHashMap(Map<? extends K, ? extends V> paramMap)
/*      */   {
/*  979 */     this(Math.max((int)(paramMap.size() / 0.75F) + 1, 16), 0.75F, 16);
/*  980 */     putAll(paramMap);
/*      */   }
/*      */   
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   public boolean isEmpty()
/*      */   {
/*  989 */     Segment[] arrayOfSegment = segments;
/*      */     
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*  999 */     int[] arrayOfInt = new int[arrayOfSegment.length];
/* 1000 */     int i = 0;
/* 1001 */     for (int j = 0; j < arrayOfSegment.length; j++) {
/* 1002 */       if (count != 0) return false;
/* 1003 */       i += (arrayOfInt[j] = modCount);
/*      */     }
/*      */     
/*      */ 
/*      */ 
/* 1008 */     if (i != 0) {
/* 1009 */       for (j = 0; j < arrayOfSegment.length; j++) {
/* 1010 */         if ((count != 0) || (arrayOfInt[j] != modCount)) return false;
/*      */       }
/*      */     }
/* 1013 */     return true;
/*      */   }
/*      */   
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   public int size()
/*      */   {
/* 1023 */     Segment[] arrayOfSegment = segments;
/* 1024 */     long l1 = 0L;
/* 1025 */     long l2 = 0L;
/* 1026 */     int[] arrayOfInt = new int[arrayOfSegment.length];
/*      */     
/*      */ 
/* 1029 */     for (int i = 0; i < 2; i++) {
/* 1030 */       l2 = 0L;
/* 1031 */       l1 = 0L;
/* 1032 */       int j = 0;
/* 1033 */       for (int k = 0; k < arrayOfSegment.length; k++) {
/* 1034 */         l1 += count;
/* 1035 */         j += (arrayOfInt[k] = modCount);
/*      */       }
/* 1037 */       if (j != 0) {
/* 1038 */         for (k = 0; k < arrayOfSegment.length; k++) {
/* 1039 */           l2 += count;
/* 1040 */           if (arrayOfInt[k] != modCount) {
/* 1041 */             l2 = -1L;
/* 1042 */             break;
/*      */           }
/*      */         }
/*      */       }
/* 1046 */       if (l2 == l1) break;
/*      */     }
/* 1048 */     if (l2 != l1) {
/* 1049 */       l1 = 0L;
/* 1050 */       for (i = 0; i < arrayOfSegment.length; i++) arrayOfSegment[i].lock();
/* 1051 */       for (i = 0; i < arrayOfSegment.length; i++) l1 += count;
/* 1052 */       for (i = 0; i < arrayOfSegment.length; i++) arrayOfSegment[i].unlock();
/*      */     }
/* 1054 */     if (l1 > 2147483647L) return Integer.MAX_VALUE;
/* 1055 */     return (int)l1;
/*      */   }
/*      */   
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   public V get(Object paramObject)
/*      */   {
/* 1069 */     int i = hashOf(paramObject);
/* 1070 */     return (V)segmentFor(i).get(paramObject, i);
/*      */   }
/*      */   
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   public boolean containsKey(Object paramObject)
/*      */   {
/* 1082 */     int i = hashOf(paramObject);
/* 1083 */     return segmentFor(i).containsKey(paramObject, i);
/*      */   }
/*      */   
/*      */   /* Error */
/*      */   public boolean containsValue(Object paramObject)
/*      */   {
/*      */     // Byte code:
/*      */     //   0: aload_1
/*      */     //   1: ifnonnull +11 -> 12
/*      */     //   4: new 306	java/lang/NullPointerException
/*      */     //   7: dup
/*      */     //   8: invokespecial 307	java/lang/NullPointerException:<init>	()V
/*      */     //   11: athrow
/*      */     //   12: aload_0
/*      */     //   13: getfield 125	com/contrastsecurity/agent/weakmap/ConcurrentReferenceHashMap:segments	[Lcom/contrastsecurity/agent/weakmap/ConcurrentReferenceHashMap$Segment;
/*      */     //   16: astore_2
/*      */     //   17: aload_2
/*      */     //   18: arraylength
/*      */     //   19: newarray <illegal type>
/*      */     //   21: astore_3
/*      */     //   22: iconst_0
/*      */     //   23: istore 4
/*      */     //   25: goto +124 -> 149
/*      */     //   28: iconst_0
/*      */     //   29: pop
/*      */     //   30: iconst_0
/*      */     //   31: istore 5
/*      */     //   33: iconst_0
/*      */     //   34: istore 6
/*      */     //   36: goto +44 -> 80
/*      */     //   39: aload_2
/*      */     //   40: iload 6
/*      */     //   42: aaload
/*      */     //   43: getfield 278	com/contrastsecurity/agent/weakmap/ConcurrentReferenceHashMap$Segment:count	I
/*      */     //   46: pop
/*      */     //   47: iload 5
/*      */     //   49: aload_3
/*      */     //   50: iload 6
/*      */     //   52: aload_2
/*      */     //   53: iload 6
/*      */     //   55: aaload
/*      */     //   56: getfield 281	com/contrastsecurity/agent/weakmap/ConcurrentReferenceHashMap$Segment:modCount	I
/*      */     //   59: dup_x2
/*      */     //   60: iastore
/*      */     //   61: iadd
/*      */     //   62: istore 5
/*      */     //   64: aload_2
/*      */     //   65: iload 6
/*      */     //   67: aaload
/*      */     //   68: aload_1
/*      */     //   69: invokevirtual 309	com/contrastsecurity/agent/weakmap/ConcurrentReferenceHashMap$Segment:containsValue	(Ljava/lang/Object;)Z
/*      */     //   72: ifeq +5 -> 77
/*      */     //   75: iconst_1
/*      */     //   76: ireturn
/*      */     //   77: iinc 6 1
/*      */     //   80: iload 6
/*      */     //   82: aload_2
/*      */     //   83: arraylength
/*      */     //   84: if_icmplt -45 -> 39
/*      */     //   87: iconst_1
/*      */     //   88: istore 6
/*      */     //   90: iload 5
/*      */     //   92: ifeq +47 -> 139
/*      */     //   95: iconst_0
/*      */     //   96: istore 7
/*      */     //   98: goto +34 -> 132
/*      */     //   101: aload_2
/*      */     //   102: iload 7
/*      */     //   104: aaload
/*      */     //   105: getfield 278	com/contrastsecurity/agent/weakmap/ConcurrentReferenceHashMap$Segment:count	I
/*      */     //   108: pop
/*      */     //   109: aload_3
/*      */     //   110: iload 7
/*      */     //   112: iaload
/*      */     //   113: aload_2
/*      */     //   114: iload 7
/*      */     //   116: aaload
/*      */     //   117: getfield 281	com/contrastsecurity/agent/weakmap/ConcurrentReferenceHashMap$Segment:modCount	I
/*      */     //   120: if_icmpeq +9 -> 129
/*      */     //   123: iconst_0
/*      */     //   124: istore 6
/*      */     //   126: goto +13 -> 139
/*      */     //   129: iinc 7 1
/*      */     //   132: iload 7
/*      */     //   134: aload_2
/*      */     //   135: arraylength
/*      */     //   136: if_icmplt -35 -> 101
/*      */     //   139: iload 6
/*      */     //   141: ifeq +5 -> 146
/*      */     //   144: iconst_0
/*      */     //   145: ireturn
/*      */     //   146: iinc 4 1
/*      */     //   149: iload 4
/*      */     //   151: iconst_2
/*      */     //   152: if_icmplt -124 -> 28
/*      */     //   155: iconst_0
/*      */     //   156: istore 4
/*      */     //   158: goto +13 -> 171
/*      */     //   161: aload_2
/*      */     //   162: iload 4
/*      */     //   164: aaload
/*      */     //   165: invokevirtual 286	com/contrastsecurity/agent/weakmap/ConcurrentReferenceHashMap$Segment:lock	()V
/*      */     //   168: iinc 4 1
/*      */     //   171: iload 4
/*      */     //   173: aload_2
/*      */     //   174: arraylength
/*      */     //   175: if_icmplt -14 -> 161
/*      */     //   178: iconst_0
/*      */     //   179: istore 4
/*      */     //   181: iconst_0
/*      */     //   182: istore 5
/*      */     //   184: goto +23 -> 207
/*      */     //   187: aload_2
/*      */     //   188: iload 5
/*      */     //   190: aaload
/*      */     //   191: aload_1
/*      */     //   192: invokevirtual 309	com/contrastsecurity/agent/weakmap/ConcurrentReferenceHashMap$Segment:containsValue	(Ljava/lang/Object;)Z
/*      */     //   195: ifeq +9 -> 204
/*      */     //   198: iconst_1
/*      */     //   199: istore 4
/*      */     //   201: goto +44 -> 245
/*      */     //   204: iinc 5 1
/*      */     //   207: iload 5
/*      */     //   209: aload_2
/*      */     //   210: arraylength
/*      */     //   211: if_icmplt -24 -> 187
/*      */     //   214: goto +31 -> 245
/*      */     //   217: astore 6
/*      */     //   219: iconst_0
/*      */     //   220: istore 7
/*      */     //   222: goto +13 -> 235
/*      */     //   225: aload_2
/*      */     //   226: iload 7
/*      */     //   228: aaload
/*      */     //   229: invokevirtual 289	com/contrastsecurity/agent/weakmap/ConcurrentReferenceHashMap$Segment:unlock	()V
/*      */     //   232: iinc 7 1
/*      */     //   235: iload 7
/*      */     //   237: aload_2
/*      */     //   238: arraylength
/*      */     //   239: if_icmplt -14 -> 225
/*      */     //   242: aload 6
/*      */     //   244: athrow
/*      */     //   245: iconst_0
/*      */     //   246: istore 7
/*      */     //   248: goto +13 -> 261
/*      */     //   251: aload_2
/*      */     //   252: iload 7
/*      */     //   254: aaload
/*      */     //   255: invokevirtual 289	com/contrastsecurity/agent/weakmap/ConcurrentReferenceHashMap$Segment:unlock	()V
/*      */     //   258: iinc 7 1
/*      */     //   261: iload 7
/*      */     //   263: aload_2
/*      */     //   264: arraylength
/*      */     //   265: if_icmplt -14 -> 251
/*      */     //   268: iload 4
/*      */     //   270: ireturn
/*      */     // Line number table:
/*      */     //   Java source line #1096	-> byte code offset #0
/*      */     //   Java source line #1100	-> byte code offset #12
/*      */     //   Java source line #1101	-> byte code offset #17
/*      */     //   Java source line #1104	-> byte code offset #22
/*      */     //   Java source line #1105	-> byte code offset #28
/*      */     //   Java source line #1106	-> byte code offset #30
/*      */     //   Java source line #1107	-> byte code offset #33
/*      */     //   Java source line #1108	-> byte code offset #39
/*      */     //   Java source line #1109	-> byte code offset #47
/*      */     //   Java source line #1110	-> byte code offset #64
/*      */     //   Java source line #1107	-> byte code offset #77
/*      */     //   Java source line #1112	-> byte code offset #87
/*      */     //   Java source line #1113	-> byte code offset #90
/*      */     //   Java source line #1114	-> byte code offset #95
/*      */     //   Java source line #1115	-> byte code offset #101
/*      */     //   Java source line #1116	-> byte code offset #109
/*      */     //   Java source line #1117	-> byte code offset #123
/*      */     //   Java source line #1118	-> byte code offset #126
/*      */     //   Java source line #1114	-> byte code offset #129
/*      */     //   Java source line #1122	-> byte code offset #139
/*      */     //   Java source line #1104	-> byte code offset #146
/*      */     //   Java source line #1125	-> byte code offset #155
/*      */     //   Java source line #1126	-> byte code offset #178
/*      */     //   Java source line #1128	-> byte code offset #181
/*      */     //   Java source line #1129	-> byte code offset #187
/*      */     //   Java source line #1130	-> byte code offset #198
/*      */     //   Java source line #1131	-> byte code offset #201
/*      */     //   Java source line #1128	-> byte code offset #204
/*      */     //   Java source line #1134	-> byte code offset #214
/*      */     //   Java source line #1135	-> byte code offset #219
/*      */     //   Java source line #1136	-> byte code offset #242
/*      */     //   Java source line #1135	-> byte code offset #245
/*      */     //   Java source line #1137	-> byte code offset #268
/*      */     // Local variable table:
/*      */     //   start	length	slot	name	signature
/*      */     //   0	271	0	this	ConcurrentReferenceHashMap
/*      */     //   0	271	1	paramObject	Object
/*      */     //   16	248	2	arrayOfSegment	Segment[]
/*      */     //   21	89	3	arrayOfInt	int[]
/*      */     //   23	246	4	i	int
/*      */     //   31	181	5	j	int
/*      */     //   34	106	6	k	int
/*      */     //   217	26	6	localObject	Object
/*      */     //   96	170	7	m	int
/*      */     // Exception table:
/*      */     //   from	to	target	type
/*      */     //   181	217	217	finally
/*      */   }
/*      */   
/*      */   public boolean contains(Object paramObject)
/*      */   {
/* 1152 */     return containsValue(paramObject);
/*      */   }
/*      */   
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   public V put(K paramK, V paramV)
/*      */   {
/* 1169 */     if (paramV == null) throw new NullPointerException();
/* 1170 */     int i = hashOf(paramK);
/* 1171 */     return (V)segmentFor(i).put(paramK, i, paramV, false);
/*      */   }
/*      */   
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   public V putIfAbsent(K paramK, V paramV)
/*      */   {
/* 1182 */     if (paramV == null) throw new NullPointerException();
/* 1183 */     int i = hashOf(paramK);
/* 1184 */     return (V)segmentFor(i).put(paramK, i, paramV, true);
/*      */   }
/*      */   
/*      */ 
/*      */ 
/*      */   public void putAll(Map<? extends K, ? extends V> paramMap)
/*      */   {
/*      */     Map.Entry localEntry;
/*      */     
/*      */ 
/* 1194 */     for (Iterator localIterator = paramMap.entrySet().iterator(); localIterator.hasNext(); put(localEntry.getKey(), localEntry.getValue())) { localEntry = (Map.Entry)localIterator.next();
/*      */     }
/*      */   }
/*      */   
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   public V remove(Object paramObject)
/*      */   {
/* 1207 */     int i = hashOf(paramObject);
/* 1208 */     return (V)segmentFor(i).remove(paramObject, i, null, false);
/*      */   }
/*      */   
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   public boolean remove(Object paramObject1, Object paramObject2)
/*      */   {
/* 1217 */     int i = hashOf(paramObject1);
/* 1218 */     if (paramObject2 == null) return false;
/* 1219 */     return segmentFor(i).remove(paramObject1, i, paramObject2, false) != null;
/*      */   }
/*      */   
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   public boolean replace(K paramK, V paramV1, V paramV2)
/*      */   {
/* 1228 */     if ((paramV1 == null) || (paramV2 == null)) throw new NullPointerException();
/* 1229 */     int i = hashOf(paramK);
/* 1230 */     return segmentFor(i).replace(paramK, i, paramV1, paramV2);
/*      */   }
/*      */   
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   public V replace(K paramK, V paramV)
/*      */   {
/* 1241 */     if (paramV == null) throw new NullPointerException();
/* 1242 */     int i = hashOf(paramK);
/* 1243 */     return (V)segmentFor(i).replace(paramK, i, paramV);
/*      */   }
/*      */   
/*      */   public void clear()
/*      */   {
/* 1248 */     for (int i = 0; i < segments.length; i++) { segments[i].clear();
/*      */     }
/*      */   }
/*      */   
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   public void purgeStaleEntries()
/*      */   {
/* 1261 */     for (int i = 0; i < segments.length; i++) { segments[i].removeStale();
/*      */     }
/*      */   }
/*      */   
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   public Set<K> keySet()
/*      */   {
/* 1277 */     Set localSet = keySet;
/* 1278 */     return localSet != null ? localSet : (keySet = new KeySet());
/*      */   }
/*      */   
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   public Collection<V> values()
/*      */   {
/* 1295 */     Collection localCollection = values;
/* 1296 */     return localCollection != null ? localCollection : (values = new Values());
/*      */   }
/*      */   
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   public Set<Map.Entry<K, V>> entrySet()
/*      */   {
/* 1312 */     Set localSet = entrySet;
/* 1313 */     return localSet != null ? localSet : (entrySet = new EntrySet());
/*      */   }
/*      */   
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   public Enumeration<K> keys()
/*      */   {
/* 1323 */     return new KeyIterator();
/*      */   }
/*      */   
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   public Enumeration<V> elements()
/*      */   {
/* 1333 */     return new ValueIterator();
/*      */   }
/*      */   
/*      */   /* Error */
/*      */   private void writeObject(java.io.ObjectOutputStream paramObjectOutputStream)
/*      */     throws IOException
/*      */   {
    /*      */     // Byte code:
    /*      */     //   0: aload_1
    /*      */     //   1: invokevirtual 368	java/io/ObjectOutputStream:defaultWriteObject	()V
    /*      */     //   4: iconst_0
    /*      */     //   5: istore_2
    /*      */     //   6: goto +108 -> 114
    /*      */     //   9: aload_0
    /*      */     //   10: getfield 125	com/contrastsecurity/agent/weakmap/ConcurrentReferenceHashMap:segments	[Lcom/contrastsecurity/agent/weakmap/ConcurrentReferenceHashMap$Segment;
    /*      */     //   13: iload_2
    /*      */     //   14: aaload
    /*      */     //   15: astore_3
    /*      */     //   16: aload_3
    /*      */     //   17: invokevirtual 286	com/contrastsecurity/agent/weakmap/ConcurrentReferenceHashMap$Segment:lock	()V
    /*      */     //   20: aload_3
    /*      */     //   21: getfield 372	com/contrastsecurity/agent/weakmap/ConcurrentReferenceHashMap$Segment:table	[Lcom/contrastsecurity/agent/weakmap/ConcurrentReferenceHashMap$HashEntry;
    /*      */     //   24: astore 4
    /*      */     //   26: iconst_0
    /*      */     //   27: istore 5
    /*      */     //   29: goto +58 -> 87
    /*      */     //   32: aload 4
    /*      */     //   34: iload 5
    /*      */     //   36: aaload
    /*      */     //   37: astore 6
    /*      */     //   39: goto +40 -> 79
    /*      */     //   42: aload 6
    /*      */     //   44: invokevirtual 375	com/contrastsecurity/agent/weakmap/ConcurrentReferenceHashMap$HashEntry:key	()Ljava/lang/Object;
    /*      */     //   47: astore 7
    /*      */     //   49: aload 7
    /*      */     //   51: ifnonnull +6 -> 57
    /*      */     //   54: goto +18 -> 72
    /*      */     //   57: aload_1
    /*      */     //   58: aload 7
    /*      */     //   60: invokevirtual 378	java/io/ObjectOutputStream:writeObject	(Ljava/lang/Object;)V
    /*      */     //   63: aload_1
    /*      */     //   64: aload 6
    /*      */     //   66: invokevirtual 381	com/contrastsecurity/agent/weakmap/ConcurrentReferenceHashMap$HashEntry:value	()Ljava/lang/Object;
    /*      */     //   69: invokevirtual 378	java/io/ObjectOutputStream:writeObject	(Ljava/lang/Object;)V
    /*      */     //   72: aload 6
    /*      */     //   74: getfield 384	com/contrastsecurity/agent/weakmap/ConcurrentReferenceHashMap$HashEntry:next	Lcom/contrastsecurity/agent/weakmap/ConcurrentReferenceHashMap$HashEntry;
    /*      */     //   77: astore 6
    /*      */     //   79: aload 6
    /*      */     //   81: ifnonnull -39 -> 42
    /*      */     //   84: iinc 5 1
    /*      */     //   87: iload 5
    /*      */     //   89: aload 4
    /*      */     //   91: arraylength
    /*      */     //   92: if_icmplt -60 -> 32
    /*      */     //   95: goto +12 -> 107
    /*      */     //   98: astore 8
    /*      */     //   100: aload_3
    /*      */     //   101: invokevirtual 289	com/contrastsecurity/agent/weakmap/ConcurrentReferenceHashMap$Segment:unlock	()V
    /*      */     //   104: aload 8
    /*      */     //   106: athrow
    /*      */     //   107: aload_3
    /*      */     //   108: invokevirtual 289	com/contrastsecurity/agent/weakmap/ConcurrentReferenceHashMap$Segment:unlock	()V
    /*      */     //   111: iinc 2 1
    /*      */     //   114: iload_2
    /*      */     //   115: aload_0
    /*      */     //   116: getfield 125	com/contrastsecurity/agent/weakmap/ConcurrentReferenceHashMap:segments	[Lcom/contrastsecurity/agent/weakmap/ConcurrentReferenceHashMap$Segment;
    /*      */     //   119: arraylength
    /*      */     //   120: if_icmplt -111 -> 9
    /*      */     //   123: aload_1
    /*      */     //   124: aconst_null
    /*      */     //   125: invokevirtual 378	java/io/ObjectOutputStream:writeObject	(Ljava/lang/Object;)V
    /*      */     //   128: aload_1
    /*      */     //   129: aconst_null
    /*      */     //   130: invokevirtual 378	java/io/ObjectOutputStream:writeObject	(Ljava/lang/Object;)V
    /*      */     //   133: return
    /*      */     // Line number table:
    /*      */     //   Java source line #1608	-> byte code offset #0
    /*      */     //   Java source line #1610	-> byte code offset #4
    /*      */     //   Java source line #1611	-> byte code offset #9
    /*      */     //   Java source line #1612	-> byte code offset #16
    /*      */     //   Java source line #1614	-> byte code offset #20
    /*      */     //   Java source line #1615	-> byte code offset #26
    /*      */     //   Java source line #1616	-> byte code offset #32
    /*      */     //   Java source line #1617	-> byte code offset #42
    /*      */     //   Java source line #1618	-> byte code offset #49
    /*      */     //   Java source line #1619	-> byte code offset #54
    /*      */     //   Java source line #1621	-> byte code offset #57
    /*      */     //   Java source line #1622	-> byte code offset #63
    /*      */     //   Java source line #1616	-> byte code offset #72
    /*      */     //   Java source line #1615	-> byte code offset #84
    /*      */     //   Java source line #1625	-> byte code offset #95
    /*      */     //   Java source line #1626	-> byte code offset #100
    /*      */     //   Java source line #1627	-> byte code offset #104
    /*      */     //   Java source line #1626	-> byte code offset #107
    /*      */     //   Java source line #1610	-> byte code offset #111
    /*      */     //   Java source line #1629	-> byte code offset #123
    /*      */     //   Java source line #1630	-> byte code offset #128
    /*      */     //   Java source line #1631	-> byte code offset #133
    /*      */     // Local variable table:
    /*      */     //   start	length	slot	name	signature
    /*      */     //   0	134	0	this	ConcurrentReferenceHashMap
    /*      */     //   0	134	1	paramObjectOutputStream	java.io.ObjectOutputStream
    /*      */     //   5	116	2	i	int
    /*      */     //   15	93	3	localSegment	Segment
    /*      */     //   24	66	4	arrayOfHashEntry	HashEntry[]
    /*      */     //   27	66	5	j	int
    /*      */     //   37	43	6	localHashEntry	HashEntry
    /*      */     //   47	12	7	localObject1	Object
    /*      */     //   98	7	8	localObject2	Object
    /*      */     // Exception table:
    /*      */     //   from	to	target	type
    /*      */     //   20	98	98	finally
    /*      */   }
    /*      */   
    /*      */   @DontObfuscate
    /*      */   abstract class HashIterator
    /*      */   {
    /*      */     int nextSegmentIndex;
    /*      */     int nextTableIndex;
    /*      */     ConcurrentReferenceHashMap.HashEntry<K, V>[] currentTable;
    /*      */     ConcurrentReferenceHashMap.HashEntry<K, V> nextEntry;
    /*      */     ConcurrentReferenceHashMap.HashEntry<K, V> lastReturned;
    /*      */     K currentKey;
    /*      */     
    /*      */     HashIterator()
    /*      */     {
    /* 1347 */       nextSegmentIndex = (segments.length - 1);
    /* 1348 */       nextTableIndex = -1;
    /* 1349 */       advance();
    /*      */     }
    /*      */     
    /*      */     public boolean hasMoreElements() {
    /* 1353 */       return hasNext();
    /*      */     }
    /*      */     
    /*      */     final void advance() {
    /* 1357 */       if ((nextEntry != null) && ((nextEntry = nextEntry.next) != null)) { return;
    /*      */       }
    /* 1359 */       while (nextTableIndex >= 0) {
    /* 1360 */         if ((nextEntry = currentTable[(nextTableIndex--)]) != null) { return;
    /*      */         }
    /*      */       }
    /* 1363 */       while (nextSegmentIndex >= 0) {
    /* 1364 */         ConcurrentReferenceHashMap.Segment localSegment = segments[(nextSegmentIndex--)];
    /* 1365 */         if (count != 0) {
    /* 1366 */           currentTable = table;
    /* 1367 */           for (int i = currentTable.length - 1; i >= 0; i--) {
    /* 1368 */             if ((nextEntry = currentTable[i]) != null) {
    /* 1369 */               nextTableIndex = (i - 1);
    /* 1370 */               return;
    /*      */             }
    /*      */           }
    /*      */         }
    /*      */       }
    /*      */     }
    /*      */     
    /*      */     public boolean hasNext() {
    /* 1378 */       while (nextEntry != null) {
    /* 1379 */         if (nextEntry.key() != null) return true;
    /* 1380 */         advance();
    /*      */       }
    /*      */       
    /* 1383 */       return false;
    /*      */     }
    /*      */     
    /*      */     ConcurrentReferenceHashMap.HashEntry<K, V> nextEntry() {
    /*      */       do {
    /* 1388 */         if (nextEntry == null) { throw new NoSuchElementException();
    /*      */         }
    /* 1390 */         lastReturned = nextEntry;
    /* 1391 */         currentKey = lastReturned.key();
    /* 1392 */         advance();
    /* 1393 */       } while (currentKey == null);
    /*      */       
    /* 1395 */       return lastReturned;
    /*      */     }
    /*      */     
    /*      */     public void remove() {
    /* 1399 */       if (lastReturned == null) throw new IllegalStateException();
    /* 1400 */       remove(currentKey);
    /* 1401 */       lastReturned = null;
    /*      */     }
    /*      */   }
    /*      */   
    /*      */   @DontObfuscate
    /* 1406 */   final class KeyIterator extends ConcurrentReferenceHashMap<K, V>.HashIterator implements Enumeration<K>, Iterator<K> { KeyIterator() { super(); }
    /*      */     
    /* 1408 */     public K next() { return (K)super.nextEntry().key(); }
    /*      */     
    /*      */ 
    /*      */ 
    /* 1412 */     public K nextElement() { return (K)super.nextEntry().key(); }
    /*      */   }
    /*      */   
    /*      */   @DontObfuscate
    /*      */   final class ValueIterator extends ConcurrentReferenceHashMap<K, V>.HashIterator implements Enumeration<V>, Iterator<V> {
    /* 1417 */     ValueIterator() { super(); }
    /*      */     
    /* 1419 */     public V next() { return (V)super.nextEntry().value(); }
    /*      */     
    /*      */     public V nextElement()
    /*      */     {
    /* 1423 */       return (V)super.nextEntry().value();
    /*      */     }
    /*      */   }
    /*      */   
    /*      */ 
    /*      */   @DontObfuscate
    /*      */   static class SimpleEntry<K, V>
    /*      */     implements Serializable, Map.Entry<K, V>
    /*      */   {
    /*      */     private static final long serialVersionUID = -8499721149061103585L;
    /*      */     private final K key;
    /*      */     private V value;
    /*      */     
    /*      */     SimpleEntry(K paramK, V paramV)
    /*      */     {
    /* 1438 */       key = paramK;
    /* 1439 */       value = paramV;
    /*      */     }
    /*      */     
    /*      */     SimpleEntry(Map.Entry<? extends K, ? extends V> paramEntry) {
    /* 1443 */       key = paramEntry.getKey();
    /* 1444 */       value = paramEntry.getValue();
    /*      */     }
    /*      */     
    /*      */     public K getKey() {
    /* 1448 */       return (K)key;
    /*      */     }
    /*      */     
    /*      */     public V getValue() {
    /* 1452 */       return (V)value;
    /*      */     }
    /*      */     
    /*      */     public V setValue(V paramV) {
    /* 1456 */       Object localObject = value;
    /* 1457 */       value = paramV;
    /* 1458 */       return (V)localObject;
    /*      */     }
    /*      */     
    /*      */     public boolean equals(Object paramObject) {
    /* 1462 */       if (!(paramObject instanceof Map.Entry)) { return false;
    /*      */       }
    /* 1464 */       Map.Entry localEntry = (Map.Entry)paramObject;
    /* 1465 */       return (eq(key, localEntry.getKey())) && (eq(value, localEntry.getValue()));
    /*      */     }
    /*      */     
    /*      */     public int hashCode() {
    /* 1469 */       return (key == null ? 0 : key.hashCode()) ^ (value == null ? 0 : value.hashCode());
    /*      */     }
    /*      */     
    /*      */     public String toString() {
    /* 1473 */       return key + "=" + value;
    /*      */     }
    /*      */     
    /*      */     private static boolean eq(Object paramObject1, Object paramObject2) {
    /* 1477 */       return paramObject1 == null ? false : paramObject2 == null ? true : paramObject1.equals(paramObject2);
    /*      */     }
    /*      */   }
    /*      */   
    /*      */ 
    /*      */   @DontObfuscate
    /*      */   final class WriteThroughEntry
    /*      */     extends ConcurrentReferenceHashMap.SimpleEntry<K, V>
    /*      */   {
    /*      */     private static final long serialVersionUID = -7900634345345313646L;
    /*      */     
    /*      */     WriteThroughEntry(V paramV)
    /*      */     {
    /* 1490 */       super(localObject);
    /*      */     }
    /*      */     
    /*      */ 
    /*      */ 
    /*      */ 
    /*      */ 
    /*      */ 
    /*      */     public V setValue(V paramV)
    /*      */     {
    /* 1500 */       if (paramV == null) throw new NullPointerException();
    /* 1501 */       Object localObject = super.setValue(paramV);
    /* 1502 */       put(getKey(), paramV);
    /* 1503 */       return (V)localObject;
    /*      */     }
    /*      */   }
    /*      */   
    /*      */   @DontObfuscate
    /* 1508 */   final class EntryIterator extends ConcurrentReferenceHashMap<K, V>.HashIterator implements Iterator<Map.Entry<K, V>> { EntryIterator() { super(); }
    /*      */     
    /* 1510 */     public Map.Entry<K, V> next() { ConcurrentReferenceHashMap.HashEntry localHashEntry = super.nextEntry();
    /* 1511 */       return new ConcurrentReferenceHashMap.WriteThroughEntry(ConcurrentReferenceHashMap.this, localHashEntry.key(), localHashEntry.value());
    /*      */     }
    /*      */   }
    /*      */   
    /*      */   @DontObfuscate
    /*      */   final class KeySet extends AbstractSet<K> { KeySet() {}
    /*      */     
    /* 1518 */     public Iterator<K> iterator() { return new ConcurrentReferenceHashMap.KeyIterator(ConcurrentReferenceHashMap.this); }
    /*      */     
    /*      */     public int size()
    /*      */     {
    /* 1522 */       return ConcurrentReferenceHashMap.this.size();
    /*      */     }
    /*      */     
    /*      */     public boolean isEmpty() {
    /* 1526 */       return ConcurrentReferenceHashMap.this.isEmpty();
    /*      */     }
    /*      */     
    /*      */     public boolean contains(Object paramObject) {
    /* 1530 */       return containsKey(paramObject);
    /*      */     }
    /*      */     
    /*      */     public boolean remove(Object paramObject) {
    /* 1534 */       return remove(paramObject) != null;
    /*      */     }
    /*      */     
    /*      */ 
    /* 1538 */     public void clear() { ConcurrentReferenceHashMap.this.clear(); }
    /*      */   }
    /*      */   
    /*      */   @DontObfuscate
    /*      */   final class Values extends AbstractCollection<V> {
    /*      */     Values() {}
    /*      */     
    /* 1545 */     public Iterator<V> iterator() { return new ConcurrentReferenceHashMap.ValueIterator(ConcurrentReferenceHashMap.this); }
    /*      */     
    /*      */     public int size()
    /*      */     {
    /* 1549 */       return ConcurrentReferenceHashMap.this.size();
    /*      */     }
    /*      */     
    /*      */     public boolean isEmpty() {
    /* 1553 */       return ConcurrentReferenceHashMap.this.isEmpty();
    /*      */     }
    /*      */     
    /*      */     public boolean contains(Object paramObject) {
    /* 1557 */       return containsValue(paramObject);
    /*      */     }
    /*      */     
    /*      */ 
    /* 1561 */     public void clear() { ConcurrentReferenceHashMap.this.clear(); }
    /*      */   }
    /*      */   
    /*      */   @DontObfuscate
    /*      */   final class EntrySet extends AbstractSet<Map.Entry<K, V>> {
    /*      */     EntrySet() {}
    /*      */     
    /* 1568 */     public Iterator<Map.Entry<K, V>> iterator() { return new ConcurrentReferenceHashMap.EntryIterator(ConcurrentReferenceHashMap.this); }
    /*      */     
    /*      */     public boolean contains(Object paramObject)
    /*      */     {
    /* 1572 */       if (!(paramObject instanceof Map.Entry)) return false;
    /* 1573 */       Map.Entry localEntry = (Map.Entry)paramObject;
    /* 1574 */       Object localObject = get(localEntry.getKey());
    /* 1575 */       return (localObject != null) && (localObject.equals(localEntry.getValue()));
    /*      */     }
    /*      */     
    /*      */     public boolean remove(Object paramObject) {
    /* 1579 */       if (!(paramObject instanceof Map.Entry)) return false;
    /* 1580 */       Map.Entry localEntry = (Map.Entry)paramObject;
    /* 1581 */       return remove(localEntry.getKey(), localEntry.getValue());
    /*      */     }
    /*      */     
    /*      */     public int size() {
    /* 1585 */       return ConcurrentReferenceHashMap.this.size();
    /*      */     }
    /*      */     
    /*      */     public boolean isEmpty() {
    /* 1589 */       return ConcurrentReferenceHashMap.this.isEmpty();
    /*      */     }
    /*      */     
    /*      */     public void clear() {
    /* 1593 */       ConcurrentReferenceHashMap.this.clear();
    /*      */     }
    /*      */   }
    /*      */   
    /*      */ 
    /*      */ 
    /*      */ 
    /*      */ 
    /*      */ 
    /*      */ 
    /*      */ 
    /*      */ 
    /*      */ 
    /*      */ 
    /*      */ 
    /*      */ 
    /*      */ 
    /*      */ 
    /*      */ 
    /*      */ 
    /*      */ 
    /*      */ 
    /*      */ 
    /*      */ 
    /*      */ 
    /*      */ 
    /*      */ 
    /*      */ 
    /*      */ 
    /*      */ 
    /*      */ 
    /*      */ 
    /*      */ 
    /*      */ 
    /*      */ 
    /*      */ 
    /*      */ 
    /*      */ 
    /*      */ 
    /*      */ 
    /*      */ 
    /*      */ 
    /*      */ 
    /*      */ 
    /*      */ 
    /*      */   private void readObject(ObjectInputStream paramObjectInputStream)
    /*      */     throws IOException, ClassNotFoundException
    /*      */   {
    /* 1641 */     paramObjectInputStream.defaultReadObject();
    /*      */     
    /*      */ 
    /* 1644 */     for (int i = 0; i < segments.length; i++) {
    /* 1645 */       segments[i].setTable(new HashEntry[1]);
    /*      */     }
    /*      */     
    /*      */     for (;;)
    /*      */     {
    /* 1650 */       Object localObject1 = paramObjectInputStream.readObject();
    /* 1651 */       Object localObject2 = paramObjectInputStream.readObject();
    /* 1652 */       if (localObject1 == null) break;
    /* 1653 */       put(localObject1, localObject2);
    /*      */     }
    /*      */   }
    /*      */   
    /*      */   static abstract interface a
    /*      */   {
    /*      */     public abstract int keyHash();
    /*      */     
    /*      */     public abstract Object keyRef();
    /*      */   }
    /*      */   
    /*      */   public static enum b {}
    /*      */   
    /*      */   public static enum c {}
    /*      */ }
    
    /* Location:           /Users/santhana-16396/Desktop/contrast-3.5.5.jar
     * Qualified Name:     com.contrastsecurity.agent.weakmap.ConcurrentReferenceHashMap
     * Java Class Version: 5 (49.0)
     * JD-Core Version:    0.7.1
     */