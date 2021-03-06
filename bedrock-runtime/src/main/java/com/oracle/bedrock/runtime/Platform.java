/*
 * File: Platform.java
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

package com.oracle.bedrock.runtime;

import com.oracle.bedrock.Option;
import com.oracle.bedrock.OptionsByType;
import com.oracle.bedrock.runtime.options.Executable;
import com.oracle.bedrock.runtime.options.PlatformPredicate;

import java.net.InetAddress;

/**
 * Provides a means to represent a platform at runtime, a server, machine or
 * operating system on which {@link Application}s may be running, managed or deployed.
 * <p>
 * Copyright (c) 2014. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Jonathan Knight
 * @author Brian Oliver
 */
public interface Platform extends Infrastructure
{
    /**
     * Obtain the name of this {@link Platform}.
     *
     * @return the name of this {@link Platform}
     */
    String getName();


    /**
     * Obtains the {@link InetAddress} that could <strong>feasibly</strong>
     * be used by {@link Application}s running on other {@link Platform}s to
     * connect to {@link Application}s running on this {@link Platform}.
     * <p>
     * There is no guarantee that the {@link InetAddress} returned by this
     * method is actually reachable by other {@link Platform}s.
     * <p>
     * In some cases it may not be possible to determine an {@link InetAddress}
     * of the {@link Platform}, in which case the {@link InetAddress#getLoopbackAddress()}
     * will be returned.  When this happens the {@link Platform} is considered
     * isolated to a single host; it may only be contacted by other {@link Platform}s
     * running on the same host.
     * <p>
     * Should a specific {@link InetAddress} be returned, applications
     * can define the "bedrock.runtime.address" system-property.
     *
     * @return the {@link InetAddress} of the {@link Platform}
     */
    InetAddress getAddress();


    /**
     * Obtains the {@link OptionsByType} configured for the {@link Platform}.
     * <p>
     * <strong>Changes to the {@link OptionsByType} may not be recognized
     * or used by the {@link Platform} after it was created.</strong>
     *
     * @return the {@link OptionsByType}
     */
    OptionsByType getOptions();


    @Override
    default Platform getPlatform(Option... options)
    {
        OptionsByType     platformOptions = OptionsByType.of(options);

        PlatformPredicate predicate       = platformOptions.get(PlatformPredicate.class);

        return predicate.test(this) ? this : null;
    }


    /**
     * Launches a new {@link Application} based on the specified program executable / command
     * and provided {@link Option}s.
     *
     * @param executable  the name of the executable / command to launch the application on the {@link Platform}
     * @param options     the {@link Option}s for the {@link Application}
     *
     * @return  an {@link Application} representing the launched application
     */
    default Application launch(String    executable,
                               Option... options)
    {
        // add the program as a launch option
        OptionsByType optionsByType = OptionsByType.of(options).add(Executable.named(executable));

        // launch as an Application.class
        return launch(Application.class, optionsByType.asArray());
    }


    /**
     * Launches a new {@link Application} based on the {@link Class} of the {@link Application} and
     * provided {@link Option}s.
     *
     * @param applicationClass  {@link Class} of {@link Application} to launch on the {@link Platform}
     * @param options           the {@link Option}s for the {@link Application}
     * @param <A>               the type of {@link Application}
     *
     * @return  an {@link Application} representing the launched application
     */
    default <A extends Application> A launch(Class<A>  applicationClass,
                                             Option... options)
    {
        // auto-detect the MetaClass for the application and launch it
        MetaClass<A> metaClass = MetaClass.of(applicationClass);

        if (metaClass == null)
        {
            throw new IllegalArgumentException("The specified application class " + applicationClass
                + ", the interfaces it implements and its super-class does not define a public non-static MetaClass");
        }
        else
        {
            return launch(metaClass, options);
        }
    }


    /**
     * Launches a new {@link Application} based on the {@link MetaClass} of the
     * {@link Application} and provided {@link Option}s.
     *
     * @param metaClass  the {@link MetaClass} of the {@link Application} to launch on the {@link Platform}
     * @param options    the {@link Option}s for the {@link Application}
     * @param <A>        the type of {@link Application}
     *
     * @return  an {@link Application} representing the launched application
     */
    <A extends Application> A launch(MetaClass<A> metaClass,
                                     Option...    options);


    /**
     * Obtains a {@link PlatformPredicate} that matches any {@link Platform}.
     *
     * @return a {@link PlatformPredicate}
     */
    static PlatformPredicate any()
    {
        return platform -> true;
    }


    /**
     * Obtains a {@link PlatformPredicate} that will match a specified {@link Platform#getName()}.
     *
     * @param regularExpression  the regular expression for matching the {@link Platform#getName()}
     *
     * @return a {@link PlatformPredicate}
     *
     * @see PlatformPredicate#named(String)
     */
    static PlatformPredicate named(String regularExpression)
    {
        return PlatformPredicate.named(regularExpression);
    }


    /**
     * Obtains a {@link PlatformPredicate} that matches a specific {@link Platform}.
     *
     * @param platform  the {@link Platform}
     *
     * @return a {@link PlatformPredicate}
     *
     * @see PlatformPredicate#is(Platform)
     */
    static PlatformPredicate is(Platform platform)
    {
        return PlatformPredicate.is(platform);
    }
}
