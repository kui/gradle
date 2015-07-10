/*
 * Copyright 2011 the original author or authors.
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
package org.gradle.api.internal.artifacts.repositories.transport;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Transformer;
import org.gradle.api.artifacts.repositories.PasswordCredentials;
import org.gradle.api.authentication.Authentication;
import org.gradle.api.internal.authentication.AuthenticationInternal;
import org.gradle.api.credentials.Credentials;
import org.gradle.api.internal.artifacts.ivyservice.CacheLockingManager;
import org.gradle.api.internal.file.TemporaryFileProvider;
import org.gradle.internal.resource.cached.CachedExternalResourceIndex;
import org.gradle.internal.resource.connector.ResourceConnectorFactory;
import org.gradle.internal.resource.connector.ResourceConnectorSpecification;
import org.gradle.internal.resource.transfer.ExternalResourceConnector;
import org.gradle.internal.resource.transport.ResourceConnectorRepositoryTransport;
import org.gradle.internal.resource.transport.file.FileTransport;
import org.gradle.logging.ProgressLoggerFactory;
import org.gradle.util.BuildCommencedTimeProvider;
import org.gradle.util.CollectionUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class RepositoryTransportFactory {
    private final List<ResourceConnectorFactory> registeredProtocols = Lists.newArrayList();

    private final TemporaryFileProvider temporaryFileProvider;
    private final CachedExternalResourceIndex<String> cachedExternalResourceIndex;
    private final ProgressLoggerFactory progressLoggerFactory;
    private final BuildCommencedTimeProvider timeProvider;
    private final CacheLockingManager cacheLockingManager;

    public RepositoryTransportFactory(Collection<ResourceConnectorFactory> resourceConnectorFactory,
                                      ProgressLoggerFactory progressLoggerFactory,
                                      TemporaryFileProvider temporaryFileProvider,
                                      CachedExternalResourceIndex<String> cachedExternalResourceIndex,
                                      BuildCommencedTimeProvider timeProvider,
                                      CacheLockingManager cacheLockingManager) {
        this.progressLoggerFactory = progressLoggerFactory;
        this.temporaryFileProvider = temporaryFileProvider;
        this.cachedExternalResourceIndex = cachedExternalResourceIndex;
        this.timeProvider = timeProvider;
        this.cacheLockingManager = cacheLockingManager;

        for (ResourceConnectorFactory connectorFactory : resourceConnectorFactory) {
            register(connectorFactory);
        }
    }

    public void register(ResourceConnectorFactory resourceConnectorFactory) {
        registeredProtocols.add(resourceConnectorFactory);
    }

    public Set<String> getRegisteredProtocols() {
        Set<String> validSchemes = Sets.newLinkedHashSet();
        validSchemes.add("file");
        for (ResourceConnectorFactory registeredProtocol : registeredProtocols) {
            validSchemes.addAll(registeredProtocol.getSupportedProtocols());
        }
        return validSchemes;
    }

    public RepositoryTransport createTransport(String scheme, String name, Credentials credentials, Set<Authentication> authentications) {
        return createTransport(Collections.singleton(scheme), name, credentials, authentications);
    }

    /**
     * TODO René: why do we have two different PasswordCredentials
     * */
    private org.gradle.internal.resource.PasswordCredentials convertPasswordCredentials(Credentials credentials) {
        if (!(credentials instanceof PasswordCredentials)) {
            throw new IllegalArgumentException(String.format("Credentials must be an instance of: %s", PasswordCredentials.class.getCanonicalName()));
        }
        PasswordCredentials passwordCredentials = (PasswordCredentials) credentials;
        return new org.gradle.internal.resource.PasswordCredentials(passwordCredentials.getUsername(), passwordCredentials.getPassword());
    }

    public RepositoryTransport createTransport(Set<String> schemes, String name, Credentials credentials, Set<Authentication> authentications) {
        validateSchemes(schemes);

        // File resources are handled slightly differently at present.
        if (Collections.singleton("file").containsAll(schemes)) {
            return new FileTransport(name);
        }
        ResourceConnectorSpecification connectionDetails = new DefaultResourceConnectorSpecification(credentials);
        ResourceConnectorFactory connectorFactory = findConnectorFactory(schemes);

        // Ensure resource transport protocol, authentication types and credentials are all compatible
        validateConnectorFactoryCredentials(connectorFactory, credentials, authentications);

        ExternalResourceConnector resourceConnector = connectorFactory.createResourceConnector(connectionDetails);
        return new ResourceConnectorRepositoryTransport(name, progressLoggerFactory, temporaryFileProvider, cachedExternalResourceIndex, timeProvider, cacheLockingManager, resourceConnector);
    }

    private void validateSchemes(Set<String> schemes) {
        Set<String> validSchemes = getRegisteredProtocols();
        for (String scheme : schemes) {
            if (!validSchemes.contains(scheme)) {
                throw new InvalidUserDataException(String.format("Not a supported repository protocol '%s': valid protocols are %s", scheme, validSchemes));
            }
        }
    }

    private void validateConnectorFactoryCredentials(ResourceConnectorFactory factory, Credentials credentials, Set<Authentication> authentications) {
        if (authentications.size() > 0 && credentials == null) {
            throw new InvalidUserDataException("You cannot configure authentication protocols for a repository if no credentials are provided.");
        }

        for (Authentication authentication : authentications) {
            boolean isAuthenticationSupported = false;
            for (Class<?> authenticationType : factory.getSupportedAuthentication()) {
                if (authenticationType.isAssignableFrom(authentication.getClass())) {
                    isAuthenticationSupported = true;
                    break;
                }
            }

            if (!isAuthenticationSupported) {
                throw new InvalidUserDataException(String.format("Authentication type of '%s' is not supported by protocols %s",
                    authentication.getClass().getSimpleName(), factory.getSupportedProtocols()));
            }

            boolean isCredentialsSupported = false;
            for (Class<? extends Credentials> credentialsType : ((AuthenticationInternal)authentication).getSupportedCredentials()) {
                if (credentialsType.isAssignableFrom(credentials.getClass())) {
                    isCredentialsSupported = true;
                    break;
                }
            }

            if (!isCredentialsSupported) {
                Set<String> authenticationTypeNames = CollectionUtils.collect(authentications, new Transformer<String, Authentication>() {
                    @Override
                    public String transform(Authentication authentication) {
                        return authentication.getClass().getSimpleName();
                    }
                });
                throw new InvalidUserDataException(String.format("Credentials type of '%s' is not supported by authentication protocols %s",
                    credentials.getClass().getSimpleName(), authenticationTypeNames));
            }
        }
    }

    private ResourceConnectorFactory findConnectorFactory(Set<String> schemes) {
        for (ResourceConnectorFactory protocolRegistration : registeredProtocols) {
            if (protocolRegistration.getSupportedProtocols().containsAll(schemes)) {
                return protocolRegistration;
            }
        }
        throw new InvalidUserDataException("You cannot mix different URL schemes for a single repository. Please declare separate repositories.");
    }

    private class DefaultResourceConnectorSpecification implements ResourceConnectorSpecification {
        private final Credentials credentials;

        private DefaultResourceConnectorSpecification(Credentials credentials) {
            this.credentials = credentials;
        }

        @Override
        public <T> T getCredentials(Class<T> type) {
            if(credentials == null){
                return null;
            }
            if (org.gradle.internal.resource.PasswordCredentials.class.isAssignableFrom(type)) {
                return type.cast(convertPasswordCredentials(credentials));
            }
            if (type.isAssignableFrom(credentials.getClass())) {
                return type.cast(credentials);
            } else {
                throw new IllegalArgumentException(String.format("Credentials must be an instance of '%s'.", type.getCanonicalName()));
            }
        }
    }
}
