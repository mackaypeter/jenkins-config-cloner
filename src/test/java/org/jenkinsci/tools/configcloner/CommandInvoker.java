package org.jenkinsci.tools.configcloner;

import java.io.IOException;
import java.net.URL;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

import org.jenkinsci.main.modules.cli.auth.ssh.PublicKeySignatureWriter;
import org.jenkinsci.main.modules.cli.auth.ssh.UserPropertyImpl;

import hudson.model.User;

public class CommandInvoker {

    protected final String kind;
    protected String[] options = new String[] {};
    protected String[] arguments = new String[] {};

    public CommandInvoker(String kind) {
        this.kind = kind;
    }

    public CommandInvoker opts(String... opts) {
        this.options = opts;
        return this;
    }

    public CommandInvoker args(String... args) {
        this.arguments = args;
        return this;
    }

    public CommandResponse.Accumulator invoke(String... immediateArgs) {

        if (immediateArgs.length != 0) {
            args(immediateArgs);
        }

        return (CommandResponse.Accumulator) main().run(commandArgs());
    }

    public String[] commandArgs() {
        final String[] args = new String[this.options.length + this.arguments.length + 1];
        args[0] = kind;
        System.arraycopy(this.options, 0, args, 1, this.options.length);
        System.arraycopy(this.arguments, 0, args, this.options.length + 1, this.arguments.length);
        return args;
    }

    public Main main() {
        return new Main(CommandResponse.accumulate(), getCLIPoolForTest());
    }

    public static CLIPool getCLIPoolForTest() {

        RandKeyCLIFactory factory = new RandKeyCLIFactory();

        try {

            User.current().addProperty(new UserPropertyImpl(
                   factory.publicKeyString()
            ));
        } catch (NullPointerException ex) {
            return new CLIPool(CLIFactory.system()); // Not running Jenkins
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        return new CLIPool(factory);
    }

    private static class RandKeyCLIFactory extends org.jenkinsci.tools.configcloner.CLIFactory {

        private final KeyPair pair;

        public RandKeyCLIFactory() {
            this.pair = generateKeyPair();
        }

        @Override
        protected List<KeyPair> userKeys() {

            return Arrays.asList(pair);
        }

        private KeyPair generateKeyPair() {
            try {
                KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
                keyGen.initialize(1024);
                return keyGen.generateKeyPair();
            } catch (NoSuchAlgorithmException ex) {
                throw new RuntimeException(ex);
            }
        }

        public String publicKeyString() {
            return new PublicKeySignatureWriter().asString(pair.getPublic());
        }
    }

    public final Url url(URL url) {
        return new Url(url, kind);
    }

    /**
     * Invoker prepending url to args.
     */
    public static class Url extends CommandInvoker {

        private final URL url;

        public Url(URL url, String kind) {
            super(kind);
            this.url = url;
        }

        @Override
        public CommandInvoker args(String... args) {

            String[] mainArgs = new String[args.length];
            int i = 0;

            for(String arg: args) {

                mainArgs[i++] = url + arg;
            }

            this.arguments = mainArgs;
            return this;
        }
    }
}