/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.compileflow.engine.process.preruntime.compiler.impl;

import com.alibaba.compileflow.engine.common.CompileFlowException;
import com.alibaba.compileflow.engine.common.utils.FileUtils;
import com.alibaba.compileflow.engine.process.preruntime.compiler.*;
import com.alibaba.compileflow.engine.process.preruntime.compiler.Compiler;
import com.alibaba.compileflow.engine.process.preruntime.compiler.impl.support.EcJavaCompiler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;

/**
 * @author yusu
 */
public class CompilerImpl implements Compiler {

    private static JavaCompiler javaCompiler = new EcJavaCompiler();

    @Override
    public Class<?> compileJavaCode(String fullClassName, String sourceCode) {

        try {
            String dirPath = CompileConstants.FLOW_COMPILE_CLASS_DIR;
            File dirFile = new File(dirPath);
            if (!dirFile.exists()) {
                if (!dirFile.mkdir()) {
                    throw new RuntimeException(
                        "Output directory for process class can't be created, directory is " + dirPath);
                }
            }

            File javaSourceFile = writeJavaFile(dirFile, fullClassName, sourceCode);
            JavaSource javaSource = JavaSource.of(javaSourceFile, sourceCode, fullClassName);

            javaCompiler.compile(javaSource, new File(dirPath), new CompileOption());

            File classFile = new File(dirFile, fullClassName.replace('.', File.separatorChar) + ".class");
            byte[] classBytes = FileUtils.readFileToByteArray(classFile);
            return FlowClassLoader.getInstance().defineClass(fullClassName, classBytes);
        } catch (CompileFlowException e) {
            throw e;
        } catch (Exception e) {
            throw new CompileFlowException(e.getMessage(), e);
        }
    }

    private File writeJavaFile(File dirFile, String fullClassName, String javaCode) {

        int index = fullClassName.lastIndexOf(".");
        if (index >= 0) {
            String packageName = fullClassName.substring(0, index);
            createClassDir(dirFile, packageName);
        }
        File file = new File(dirFile, fullClassName.replace('.', File.separatorChar) + ".java");
        try (PrintWriter printWriter = new PrintWriter(new FileOutputStream(file))) {
            printWriter.write(javaCode);
            printWriter.flush();
            return file;
        } catch (Exception e) {
            throw new CompileFlowException(e.getMessage(), e);
        }
    }

    private void createClassDir(File dir, String packageName) {

        String[] packagePaths = packageName.split("\\.");
        for (String packagePath : packagePaths) {
            dir = new File(dir, packagePath);
            if (!dir.exists()) {
                if (!dir.mkdir()) {
                    throw new CompileFlowException("Failed to mkdir, dir name is " + dir);
                }
            }
        }
    }

}
