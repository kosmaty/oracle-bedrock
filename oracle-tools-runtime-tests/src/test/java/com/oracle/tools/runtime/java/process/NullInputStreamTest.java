/*
 * File: NullInputStreamTest.java
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * The contents of this file are subject to the terms and conditions of 
 * the Common Development and Distribution License 1.0 (the "License").
 *
 * You may not use this file except in compliance with the License.
 *
 * You can obtain a copy of the License by consulting the LICENSE.txt file
 * distributed with this file, or by consulting https://oss.oracle.com/licenses/CDDL
 *
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file LICENSE.txt.
 *
 * MODIFICATIONS:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 */

package com.oracle.tools.runtime.java.process;

import com.oracle.tools.junit.AbstractTest;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Unit and Functional tests for {@link NullInputStream}s.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Jonathan Knight
 */
public class NullInputStreamTest extends AbstractTest
{
    /**
     * Ensure bytes are returned in order.
     *
     * @throws Exception
     */
    @Test
    public void shouldReturnBytesInOrder() throws Exception
    {
        NullInputStream stream = new NullInputStream();

        stream.addByte((byte) 2);
        stream.addByte((byte) 4);
        stream.addByte((byte) 19);

        assertThat(stream.read(), is(2));
        assertThat(stream.read(), is(4));
        assertThat(stream.read(), is(19));
    }
}
