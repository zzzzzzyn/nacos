/*
 * Copyright (C) 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.nacos.core.remoting.test;

import org.junit.Test;

import java.util.*;

/**
 * @author pbting
 * @date 2019-09-05 11:57 AM
 */
public class QueueSortTests {

    @Test
    public void sort() {

        Deque<Integer> values = new LinkedList<>();
        values.add(6);
        values.add(16);
        values.add(60);
        values.add(7);
        Collections.sort((List) values, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o1 - o2;
            }
        });
        System.err.println(values);

        Iterator<Integer> iter = values.iterator();
        while (iter.hasNext()) {
            System.err.println(iter.next());
        }
        System.err.println("<------>");
        Iterator<Integer> deIter = values.descendingIterator();
        while (deIter.hasNext()) {
            System.err.println(deIter.next());
        }

    }
}
