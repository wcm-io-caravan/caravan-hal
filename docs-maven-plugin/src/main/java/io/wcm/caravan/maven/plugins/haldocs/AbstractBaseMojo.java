/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2015 wcm.io
 * %%
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
 * #L%
 */
package io.wcm.caravan.maven.plugins.haldocs;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.model.Build;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Common functionality for the MOJOs.
 */
abstract class AbstractBaseMojo extends AbstractMojo {

  @Parameter(property = "project", required = true, readonly = true)
  protected MavenProject project;

  private File generatedResourcesFolder;

  /**
   * Get a List of URLs of all "compile" dependencies of this project.
   * @return Class path URLs
   * @throws DependencyResolutionRequiredException
   */
  protected URL[] getCompileClasspathElementURLs() throws DependencyResolutionRequiredException {
    // build class loader to get classes to generate resources for
    return project.getCompileClasspathElements().stream()
        .map(path -> {
          try {
            return new File(path).toURI().toURL();
          }
          catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
          }
        })
        .toArray(size -> new URL[size]);
  }

  /**
   * Attach directory with generated resoruces to project to include it in JAR file.
   * @param sourceDirectory Source directory
   * @param targetPath Target path in JAR file
   */
  protected void addResource(String sourceDirectory, String targetPath) {

    // construct resource
    Resource resource = new Resource();
    resource.setDirectory(sourceDirectory);
    resource.setTargetPath(targetPath);

    // add to build
    Build build = this.project.getBuild();
    build.addResource(resource);
    getLog().debug("Added resource: " + resource.getDirectory() + " -> " + resource.getTargetPath());
  }

  /**
   * Get folder to temporarily generate the resources to.
   * @return Folder
   */
  protected File getGeneratedResourcesDirectory() {
    if (generatedResourcesFolder == null) {
      String generatedResourcesFolderAbsolutePath = this.project.getBuild().getDirectory() + "/" + getGeneratedResourcesDirectoryPath();
      generatedResourcesFolder = new File(generatedResourcesFolderAbsolutePath);
      if (!generatedResourcesFolder.exists()) {
        generatedResourcesFolder.mkdirs();
      }
    }
    return generatedResourcesFolder;
  }

  /**
   * @return Path name of directory for generated resources
   */
  protected abstract String getGeneratedResourcesDirectoryPath();

}
