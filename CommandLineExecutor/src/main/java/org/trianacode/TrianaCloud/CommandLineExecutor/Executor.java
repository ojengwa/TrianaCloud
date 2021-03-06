/*
 * Licensed to the Apache Software Foundation (ASF) under one
 *        or more contributor license agreements.  See the NOTICE file
 *        distributed with this work for additional information
 *        regarding copyright ownership.  The ASF licenses this file
 *        to you under the Apache License, Version 2.0 (the
 *        "License"); you may not use this file except in compliance
 *        with the License.  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *        Unless required by applicable law or agreed to in writing,
 *        software distributed under the License is distributed on an
 *        "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *        KIND, either express or implied.  See the License for the
 *        specific language governing permissions and limitations
 *        under the License.
 */

package org.trianacode.TrianaCloud.CommandLineExecutor;

import org.apache.log4j.Logger;
import org.trianacode.TrianaCloud.Utils.*;

import java.io.*;

/**
 * Hello world!
 */
public class Executor extends TaskExecutor {

    private Logger logger = Logger.getLogger(this.getClass().toString());

    private Task task;
    File temp;

    @Override
    public byte[] executeTask() throws TaskExecutionException {
        try {
            String line;
            Process p = Runtime.getRuntime().exec("ls -alh");
            BufferedReader bri = new BufferedReader
                    (new InputStreamReader(p.getInputStream()));
            BufferedReader bre = new BufferedReader
                    (new InputStreamReader(p.getErrorStream()));

            StringBuilder b = new StringBuilder();

            while ((line = bri.readLine()) != null) {
                b.append(line);
            }
            bri.close();
            while ((line = bre.readLine()) != null) {
                b.append(line);
            }
            bre.close();
            p.waitFor();

            task.setReturnDataType("String");
            String retData = b.toString();

            task.setReturnData(retData.getBytes());
            task.setReturnDataMD5(MD5.getMD5Hash(retData.getBytes()));

            System.out.println(new String(task.getReturnData(), "UTF-8") + "Done.");
            return TaskOps.encodeTask(task);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception err) {
            err.printStackTrace();
        }
        return new byte[0];
    }

    @Override
    public void setTask(Task task) {
        this.task = task;
        temp = null;
        try {
            temp = File.createTempFile("data", ".bundle");
            //temp.deleteOnExit();

            System.out.println(temp.getPath());

            FileOutputStream o = new FileOutputStream(temp);
            o.write(task.getData());

            System.out.println(task.getName());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getRoutingKey() {
        return "*.cmd";
    }
}
