/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.groovy.util.concurrentlinkedhashmap;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class ConcurrentLinkedHashMapTest {
    @Test
    public void computeIfAbsent() {
        ConcurrentLinkedHashMap m = new ConcurrentLinkedHashMap.Builder<>()
                .maximumWeightedCapacity(3)
                .build();

        assertEquals(1, m.computeIfAbsent("a", k -> 1));
        assertEquals(1, m.computeIfAbsent("a", k -> 2));

        assertEquals(1, m.get("a"));

        assertEquals(3, m.computeIfAbsent("b", k -> 3));
        assertEquals(4, m.computeIfAbsent("c", k -> 4));
        assertEquals(5, m.computeIfAbsent("d", k -> 5));
        assertEquals(5, m.computeIfAbsent("d", k -> 6));

        assertArrayEquals(new String[] {"b", "c", "d"}, m.keySet().toArray(new String[0]));
        assertArrayEquals(new Integer[] {3, 4, 5}, m.values().toArray(new Integer[0]));
    }

    @Test
    public void computeIfAbsentConcurrently() throws InterruptedException {
        final ConcurrentLinkedHashMap m = new ConcurrentLinkedHashMap.Builder<>()
                .maximumWeightedCapacity(3)
                .build();

        final int threadNum = 20;
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final CountDownLatch countDownLatch2 = new CountDownLatch(threadNum);

        for (int i = 0; i < threadNum; i++) {
            final int num = i;
            new Thread(() -> {
                try {
                    countDownLatch.await();

                    if (num != 0 && num != 1 && num != 2) {
                        Thread.sleep(100);
                    }

                    if (num == 1) {
                        Thread.sleep(30);
                    }

                    if (num == 2) {
                        Thread.sleep(60);
                    }

                    m.computeIfAbsent(num % 3, k -> num);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    countDownLatch2.countDown();
                }
            }).start();
        }

        countDownLatch.countDown();
        countDownLatch2.await();

        assertArrayEquals(new Integer[] {0, 1, 2}, m.keySet().toArray(new Integer[0]));
        assertArrayEquals(new Integer[] {0, 1, 2}, m.values().toArray(new Integer[0]));
    }
}