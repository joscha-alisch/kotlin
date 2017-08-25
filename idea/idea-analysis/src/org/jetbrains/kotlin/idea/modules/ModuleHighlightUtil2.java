/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.modules;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.light.LightJavaModule;
import com.intellij.psi.search.FilenameIndex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

import static com.intellij.psi.PsiJavaModule.MODULE_INFO_FILE;

// Copied from com.intellij.codeInsight.daemon.impl.analysis.ModuleHighlightUtil
public class ModuleHighlightUtil2 {
    @Nullable
    static PsiJavaModule getModuleDescriptor(@NotNull VirtualFile file, @NotNull Project project) {
        ProjectFileIndex index = ProjectFileIndex.SERVICE.getInstance(project);
        if (index.isInLibraryClasses(file)) {
            VirtualFile classRoot = index.getClassRootForFile(file);
            if (classRoot != null) {
                VirtualFile descriptorFile = classRoot.findChild(PsiJavaModule.MODULE_INFO_CLS_FILE);
                if (descriptorFile == null) {
                    descriptorFile = classRoot.findFileByRelativePath("META-INF/versions/9/" + PsiJavaModule.MODULE_INFO_CLS_FILE);
                }
                if (descriptorFile != null) {
                    PsiFile psiFile = PsiManager.getInstance(project).findFile(descriptorFile);
                    if (psiFile instanceof PsiJavaFile) {
                        return ((PsiJavaFile) psiFile).getModuleDeclaration();
                    }
                }
                else if (classRoot.getFileSystem() instanceof JarFileSystem && "jar".equalsIgnoreCase(classRoot.getExtension())) {
                    return LightJavaModule.getModule(PsiManager.getInstance(project), classRoot);
                }
            }
        }
        else {
            Module module = index.getModuleForFile(file);
            if (module != null) {
                boolean isTest = index.isInTestSourceContent(file);
                List<VirtualFile> files = FilenameIndex.getVirtualFilesByName(project, MODULE_INFO_FILE, module.getModuleScope()).stream()
                        .filter(f -> index.isInTestSourceContent(f) == isTest)
                        .collect(Collectors.toList());
                if (files.size() == 1) {
                    PsiFile psiFile = PsiManager.getInstance(project).findFile(files.get(0));
                    if (psiFile instanceof PsiJavaFile) {
                        return ((PsiJavaFile) psiFile).getModuleDeclaration();
                    }
                }
            }
        }

        return null;
    }
}
