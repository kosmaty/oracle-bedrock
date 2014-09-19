/*
 * File: AbstractApplication.java
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

package com.oracle.tools.runtime;

import com.oracle.tools.options.Timeout;

import com.oracle.tools.runtime.java.container.Container;

import com.oracle.tools.runtime.options.Diagnostics;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * A base implementation of an {@link Application}.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 * @author Harvey Raja
 *
 * @param <A>  the type of {@link AbstractApplication} to permit fluent method calls
 * @param <P>  the type of {@link ApplicationProcess} used to internally represent
 *             the underlying {@link Application} at runtime
 * @param <R>  the type of {@link ApplicationRuntime}
 */
public abstract class AbstractApplication<A extends AbstractApplication<A, P, R>, P extends ApplicationProcess,
                                          R extends ApplicationRuntime<P>> implements FluentApplication<A>
{
    /**
     * The {@link ApplicationRuntime} for the {@link Application}.
     */
    protected final R runtime;

    /**
     * The {@link Thread} that is used to capture standard output from the underlying {@link Process}.
     */
    private final Thread stdoutThread;

    /**
     * The {@link Thread} that is used to capture standard error from the underlying {@link Process}.
     */
    private final Thread stderrThread;

    /**
     * The {@link Thread} that is used to pipe standard in into the underlying {@link Process}.
     */
    private final Thread stdinThread;

    /**
     * The {@link LifecycleEventInterceptor}s that must be executed for
     * {@link LifecycleEvent}s on the {@link Application}.
     */
    private List<LifecycleEventInterceptor<? super A>> interceptors;

    /**
     * The default {@link Timeout} to use for the {@link Application}.
     */
    private Timeout defaultTimeout;


    /**
     * Construct an {@link AbstractApplication}.
     *
     * @param runtime       the {@link ApplicationRuntime}
     * @param interceptors  the {@link LifecycleEventInterceptor}s
     */
    public AbstractApplication(R                                              runtime,
                               Iterable<LifecycleEventInterceptor<? super A>> interceptors)
    {
        this.runtime = runtime;

        // establish the default Timeout for the application
        this.defaultTimeout = runtime.getOptions().get(Timeout.class, Timeout.autoDetect());

        // make a copy of the interceptors
        this.interceptors = new ArrayList<LifecycleEventInterceptor<? super A>>();

        if (interceptors != null)
        {
            for (LifecycleEventInterceptor<? super A> interceptor : interceptors)
            {
                this.interceptors.add(interceptor);
            }
        }

        // establish the standard input, output and error redirection threads
        String             displayName = runtime.getApplicationName();
        P                  process     = runtime.getApplicationProcess();
        ApplicationConsole console     = runtime.getApplicationConsole();
        boolean diagnosticsEnabled = runtime.getOptions().get(Diagnostics.class, Diagnostics.autoDetect()).isEnabled();

        // start a thread to redirect standard out to the console
        stdoutThread = new Thread(new OutputRedirector(displayName,
                                                       "out",
                                                       process.getInputStream(),
                                                       console.getOutputWriter(),
                                                       process.getId(),
                                                       diagnosticsEnabled,
                                                       console.isDiagnosticsEnabled()));
        stdoutThread.setDaemon(true);
        stdoutThread.setName(displayName + " StdOut Thread");
        stdoutThread.start();

        // start a thread to redirect standard err to the console
        stderrThread = new Thread(new OutputRedirector(displayName,
                                                       "err",
                                                       process.getErrorStream(),
                                                       console.getErrorWriter(),
                                                       process.getId(),
                                                       diagnosticsEnabled,
                                                       console.isDiagnosticsEnabled()));
        stderrThread.setDaemon(true);
        stderrThread.setName(displayName + " StdErr Thread");
        stderrThread.start();

        stdinThread = new Thread(new InputRedirector(console.getInputReader(), process.getOutputStream()));
        stdinThread.setDaemon(true);
        stdinThread.setName(displayName + " StdIn Thread");
        stdinThread.start();
    }


    @Override
    public Timeout getDefaultTimeout()
    {
        return defaultTimeout;
    }


    @Override
    public Properties getEnvironmentVariables()
    {
        return runtime.getEnvironmentVariables();
    }


    @Override
    public String getName()
    {
        return runtime.getApplicationName();
    }


    @Override
    public Platform getPlatform()
    {
        return runtime.getPlatform();
    }


    @Override
    public void close()
    {
        // close the process
        runtime.getApplicationProcess().close();

        // terminate the thread that is writing to the process standard in
        try
        {
            stdinThread.interrupt();
        }
        catch (Exception e)
        {
            // nothing to do here as we don't care
        }

        // terminate the thread that is reading from the process standard out
        try
        {
            stdoutThread.interrupt();
        }
        catch (Exception e)
        {
            // nothing to do here as we don't care
        }

        try
        {
            stdoutThread.join();
        }
        catch (InterruptedException e)
        {
            // nothing to do here as we don't care
        }

        // terminate the thread that is reading from the process standard err
        try
        {
            stderrThread.interrupt();
        }
        catch (Exception e)
        {
            // nothing to do here as we don't care
        }

        try
        {
            stderrThread.join();
        }
        catch (InterruptedException e)
        {
            // nothing to do here as we don't care
        }

        try
        {
            runtime.getApplicationConsole().close();
        }
        catch (Exception e)
        {
            // nothing to do here as we don't care
        }

        try
        {
            // wait for the process to actually terminate (because the above statements may not finish for a while)
            // (if we don't wait the process may be left hanging/orphaned)
            runtime.getApplicationProcess().waitFor();
        }
        catch (RuntimeException e)
        {
            // nothing to do here as we don't care
        }

        // raise the starting / realized event for the application
        @SuppressWarnings("rawtypes") LifecycleEvent event = new LifecycleEvent<Application>()
        {
            @Override
            public Enum<?> getType()
            {
                return Application.EventKind.DESTROYED;
            }

            @Override
            public Application getObject()
            {
                return AbstractApplication.this;
            }
        };

        for (LifecycleEventInterceptor<? super A> interceptor : this.getLifecycleInterceptors())
        {
            interceptor.onEvent(event);
        }
    }


    @Override
    public long getId()
    {
        return runtime.getApplicationProcess().getId();
    }


    @Override
    public Iterable<LifecycleEventInterceptor<? super A>> getLifecycleInterceptors()
    {
        return interceptors;
    }


    @Override
    public int waitFor()
    {
        return runtime.getApplicationProcess().waitFor();
    }


    @Override
    public int exitValue()
    {
        return runtime.getApplicationProcess().exitValue();
    }


    /**
     * An {@link InputRedirector} pipes input to an {@link OutputStream},
     * typically from an {@link ApplicationConsole} to a {@link Process}.
     */
    private static class InputRedirector implements Runnable
    {
        /**
         * The {@link Reader} from which content will be read.
         */
        private Reader reader;

        /**
         * The {@link OutputStream} to which the content read from the
         * {@link Reader} will be written.
         */
        private OutputStream outputStream;


        /**
         * Constructs an {@link OutputRedirector}.
         *
         * @param reader        the {@link Reader} from which to read content
         * @param outputStream  the {@link OutputStream} to which to write content
         */
        private InputRedirector(Reader       reader,
                                OutputStream outputStream)
        {
            this.reader       = reader;
            this.outputStream = outputStream;
        }


        @Override
        public void run()
        {
            try
            {
                BufferedReader bufferedReader = new BufferedReader(reader);
                PrintWriter    printWriter    = new PrintWriter(outputStream);

                while (true)
                {
                    String line = bufferedReader.readLine();

                    if (line == null)
                    {
                        break;
                    }

                    printWriter.println(line);
                    printWriter.flush();
                }
            }
            catch (Exception exception)
            {
                // SKIP: deliberately empty as we safely assume exceptions
                // are always due to process termination.
            }
        }
    }


    /**
     * An {@link OutputRedirector} pipes output from an {@link InputStream},
     * typically of some {@link ApplicationProcess} to an {@link ApplicationConsole}.
     */
    static class OutputRedirector implements Runnable
    {
        private String applicationName;

        /**
         * The prefix to write in-front of lines sent to the {@link ApplicationConsole}.
         */
        private String prefix;

        /**
         * The {@link ApplicationProcess} identifier.
         */
        private long processId;

        /**
         * Should diagnostic information be logged/output.
         */
        private boolean diagnosticsEnabled;

        /**
         * The {@link InputStream} from which context will be read.
         */
        private InputStream inputStream;

        /**
         * The {@link PrintWriter} to which the content read from the
         * {@link InputStream} will be written.
         */
        private PrintWriter printWriter;

        /**
         * Flag indicating whether output to the {@link ApplicationConsole}
         * should be prefixed with details of the application.
         */
        private boolean consoleDiagnosticsEnabled;


        /**
         * Constructs an {@link OutputRedirector}.
         *
         * @param applicationName            the name of the application
         * @param prefix                     the prefix to output on each console line
         *                                   (typically this is the abbreviation of the stream
         *                                   like "stderr" or "stdout")
         * @param inputStream                the {@link InputStream} from which to read content
         * @param printWriter                the {@link PrintWriter} to which to write content
         * @param processId                  the {@link ApplicationProcess} identifier
         * @param diagnosticsEnabled         should diagnostic information be logged/output
         * @param consoleDiagnosticsEnabled  if false then the process output is redirected
         *                                   without prefixing with application information
         */
        OutputRedirector(String      applicationName,
                         String      prefix,
                         InputStream inputStream,
                         PrintWriter printWriter,
                         long        processId,
                         boolean     diagnosticsEnabled,
                         boolean     consoleDiagnosticsEnabled)
        {
            this.applicationName           = applicationName;
            this.prefix                    = prefix;
            this.inputStream               = inputStream;
            this.printWriter               = printWriter;
            this.processId                 = processId;
            this.diagnosticsEnabled        = diagnosticsEnabled;
            this.consoleDiagnosticsEnabled = consoleDiagnosticsEnabled;
        }


        @Override
        public void run()
        {
            long lineNumber = 1;

            try
            {
                BufferedReader reader  =
                    new BufferedReader(new InputStreamReader(new BufferedInputStream(inputStream)));

                boolean        running = true;

                while (running || reader.ready())
                {
                    try
                    {
                        String line = reader.readLine();

                        if (line == null)
                        {
                            break;
                        }

                        String diagnosticOutput = (diagnosticsEnabled || consoleDiagnosticsEnabled)
                                                  ? String.format("[%s:%s%s] %4d: %s",
                                                                  applicationName,
                                                                  prefix,
                                                                  processId < 0 ? "" : ":" + processId,
                                                                  lineNumber++,
                                                                  line) : null;

                        String output = consoleDiagnosticsEnabled ? diagnosticOutput : line;

                        if (diagnosticsEnabled)
                        {
                            Container.getPlatformScope().getStandardOutput().println(output);
                        }

                        printWriter.println(output);
                        printWriter.flush();
                    }
                    catch (InterruptedIOException e)
                    {
                        running = false;
                    }
                }
            }
            catch (Exception exception)
            {
                // SKIP: deliberately empty as we safely assume exceptions
                // are always due to process termination.
            }

            try
            {
                String diagnosticOutput = (diagnosticsEnabled || consoleDiagnosticsEnabled)
                                          ? String.format("[%s:%s%s] %4d: (terminated)",
                                                          applicationName,
                                                          prefix,
                                                          processId < 0 ? "" : ":" + processId,
                                                          lineNumber) : null;

                String output = consoleDiagnosticsEnabled ? diagnosticOutput : "(terminated)";

                if (diagnosticsEnabled)
                {
                    Container.getPlatformScope().getStandardOutput().println(output);
                }

                printWriter.println(output);
                printWriter.flush();
            }
            catch (Exception e)
            {
                // SKIP: deliberately empty as we safely assume exceptions
                // are always due to process termination.
            }
        }
    }
}