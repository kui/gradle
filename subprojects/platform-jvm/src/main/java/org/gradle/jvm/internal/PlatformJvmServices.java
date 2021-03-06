/*
 * Copyright 2014 the original author or authors.
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

package org.gradle.jvm.internal;

import org.gradle.api.internal.artifacts.ResolveContext;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.DelegatingResolverProvider;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.ResolverProvider;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.ResolverProviderFactory;
import org.gradle.api.internal.resolve.LocalLibraryDependencyResolver;
import org.gradle.api.internal.resolve.ProjectModelResolver;
import org.gradle.internal.service.ServiceRegistration;
import org.gradle.internal.service.scopes.PluginServiceRegistry;
import org.gradle.jvm.internal.model.JarBinarySpecSpecializationSchemaExtractionStrategy;
import org.gradle.language.base.internal.resolve.DependentSourceSetResolveContext;

public class PlatformJvmServices implements PluginServiceRegistry {
    public void registerGlobalServices(ServiceRegistration registration) {
        registration.add(JarBinaryRenderer.class);
        registration.add(JarBinarySpecSpecializationSchemaExtractionStrategy.class);
    }

    public void registerBuildSessionServices(ServiceRegistration registration) {
    }

    public void registerBuildServices(ServiceRegistration registration) {
        registration.addProvider(new BuildScopeServices());
    }

    public void registerGradleServices(ServiceRegistration registration) {
    }

    public void registerProjectServices(ServiceRegistration registration) {
    }

    private static class BuildScopeServices {
        LocalLibraryDependencyResolverFactory createResolverProviderFactory(ProjectModelResolver projectModelResolver) {
            return new LocalLibraryDependencyResolverFactory(projectModelResolver);
        }
    }

    public static class LocalLibraryDependencyResolverFactory implements ResolverProviderFactory {
        private final ProjectModelResolver projectModelResolver;

        public LocalLibraryDependencyResolverFactory(ProjectModelResolver projectModelResolver) {
            this.projectModelResolver = projectModelResolver;
        }

        @Override
        public boolean canCreate(ResolveContext context) {
            return context instanceof DependentSourceSetResolveContext;
        }

        @Override
        public ResolverProvider create(ResolveContext context) {
            LocalLibraryDependencyResolver delegate = new LocalLibraryDependencyResolver(projectModelResolver,
                ((DependentSourceSetResolveContext) context).getVariants());
            return DelegatingResolverProvider.of(delegate);
        }
    }
}
