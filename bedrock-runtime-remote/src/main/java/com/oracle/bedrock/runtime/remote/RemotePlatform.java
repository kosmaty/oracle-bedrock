/*
 * File: RemotePlatform.java
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

package com.oracle.bedrock.runtime.remote;

import com.oracle.bedrock.Option;
import com.oracle.bedrock.OptionsByType;
import com.oracle.bedrock.runtime.AbstractPlatform;
import com.oracle.bedrock.runtime.Application;
import com.oracle.bedrock.runtime.ApplicationLauncher;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.MetaClass;
import com.oracle.bedrock.runtime.Platform;
import com.oracle.bedrock.runtime.java.JavaApplication;
import com.oracle.bedrock.runtime.remote.java.RemoteJavaApplicationLauncher;

import java.net.InetAddress;

/**
 * A {@link Platform} that is remote from the
 * current {@link LocalPlatform}.
 * <p>
 * Copyright (c) 2014. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Jonathan Knight
 */
public class RemotePlatform extends AbstractPlatform
{
    /**
     * The default port for secure connection to a remote server (over SSH)
     */
    public static final int DEFAULT_PORT = 22;

    /**
     * The {@link InetAddress} of the {@link RemotePlatform}.
     */
    protected InetAddress address;

    /**
     * The port of the remote host to connect for the SSH-based session.
     */
    protected int port;

    /**
     * The {@link Authentication} to use for the SSH-based session.
     */
    protected Authentication authentication;

    /**
     * The user name to use for the SSH-based session.
     */
    protected String userName;


    /**
     * Constructs a new {@link RemotePlatform}, using the default ssh port
     * for connections, named according to the address provided.
     *
     * @param address         the remote address
     * @param userName        the user name on the remote host
     * @param authentication  the {@link Authentication} for connecting to the host
     * @param options         the {@link Option}s for the {@link RemotePlatform}
     */
    public RemotePlatform(InetAddress    address,
                          String         userName,
                          Authentication authentication,
                          Option...      options)
    {
        this(address.toString(), address, DEFAULT_PORT, userName, authentication, options);
    }


    /**
     * Constructs a new {@link RemotePlatform}, using the default ssh port
     * for connections.
     *
     * @param name            the symbolic name of the {@link RemotePlatform}
     * @param address         the remote address
     * @param userName        the user name on the remote host
     * @param authentication  the {@link Authentication} for connecting to the host
     * @param options         the {@link Option}s for the {@link RemotePlatform}
     */
    public RemotePlatform(String         name,
                          InetAddress    address,
                          String         userName,
                          Authentication authentication,
                          Option...      options)
    {
        this(name, address, DEFAULT_PORT, userName, authentication, options);
    }


    /**
     * Constructs a new {@link RemotePlatform}.
     *
     * @param name            the symbolic name of the {@link RemotePlatform}
     * @param address         the remote address
     * @param port            the remote port (for ssh)
     * @param userName        the user name on the remote host
     * @param authentication  the {@link Authentication} for connecting to the host
     * @param options         the {@link Option}s for the {@link RemotePlatform}
     */
    public RemotePlatform(String         name,
                          InetAddress    address,
                          int            port,
                          String         userName,
                          Authentication authentication,
                          Option...      options)
    {
        super(name, options);

        this.address        = address;
        this.port           = port;
        this.userName       = userName;
        this.authentication = authentication;
    }


    @Override
    public InetAddress getAddress()
    {
        return address;
    }


    @Override
    protected <A extends Application, B extends ApplicationLauncher<A>> B getApplicationLauncher(MetaClass<A>  metaClass,
                                                                                                 OptionsByType optionsByType)
                                                                                                 throws UnsupportedOperationException
    {
        Class<? extends A> applicationClass = metaClass.getImplementationClass(this, optionsByType);

        if (JavaApplication.class.isAssignableFrom(applicationClass))
        {
            return (B) new RemoteJavaApplicationLauncher();
        }
        else
        {
            return (B) new SimpleRemoteApplicationLauncher();
        }
    }


    /**
     * Obtain the port to use with the {@link InetAddress}
     * to use to SSH to the remote host.
     *
     * @return the port to use with the {@link InetAddress}
     *         to use to SSH to the remote host.
     */
    public int getPort()
    {
        return port;
    }


    /**
     * Obtain the username to use to SSH to the remote host.
     *
     * @return the username to use to SSH to the remote host.
     */
    public String getUserName()
    {
        return userName;
    }


    /**
     * Obtain the {@link Authentication} to use to SSH to the remote host.
     *
     * @return the {@link Authentication} to SSH to the remote host.
     */
    public Authentication getAuthentication()
    {
        return authentication;
    }
}
