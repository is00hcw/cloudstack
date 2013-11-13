﻿// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
using log4net;
using System;
using System.Collections.Generic;
using System.Linq;
using System.ServiceProcess;
using System.Text;
using System.Threading.Tasks;
using System.Configuration.Install;
using System.Collections;

namespace CloudStack.Plugin.AgentShell
{
    static class Program
    {
        private static ILog logger = LogManager.GetLogger(typeof(Program));
        private static string serviceName = "CloudStack ServerResource";

        /// <summary>
        /// Application entry point allows service to run in console application or as a Windows service.
        /// Add '--console' to the commandline for the former, the latter is the default.
        /// </summary>
        static void Main(params string[] args)
        {
            if (args.Length == 0)
            {
                logger.InfoFormat("CloudStack ServerResource running as Windows Service");
                ServiceBase[] ServicesToRun = new ServiceBase[] { new AgentService() };
                ServiceBase.Run(ServicesToRun);
            }
            else if (args.Length == 1)
            {
                logger.DebugFormat("CloudStack ServerResource arg is ", args[0]);
                switch (args[0])
                {
                    case "--install":
                        logger.InfoFormat("Installing and running CloudStack ServerResource ");
                        InstallService();
                        StartService();
                        break;
                    case "--uninstall":
                        logger.InfoFormat("stopping and uninstalling CloudStack ServerResource ");
                        StopService();
                        UninstallService();
                        break;
                    case "--console":
                        logger.InfoFormat("CloudStack ServerResource running as console app");
                        new AgentService().RunConsole(args);
                        break;
                    default:
                        throw new NotImplementedException();
                }
            }
        }

        private static bool IsInstalled()
        {
            using (ServiceController controller =
                new ServiceController(serviceName))
            {
                try
                {
                    ServiceControllerStatus status = controller.Status;
                }
                catch
                {
                    return false;
                }
                return true;
            }
        }

        private static bool IsRunning()
        {
            using (ServiceController controller =
                new ServiceController(serviceName))
            {
                if (!IsInstalled()) return false;
                return (controller.Status == ServiceControllerStatus.Running);
            }
        }

        private static AssemblyInstaller GetInstaller()
        {
            AssemblyInstaller installer = new AssemblyInstaller(
                typeof(Program).Assembly, null);
            installer.UseNewContext = true;
            return installer;
        }

        private static void InstallService()
        {
            if (IsInstalled()) return;

            try
            {
                using (AssemblyInstaller installer = GetInstaller())
                {
                    IDictionary state = new Hashtable();
                    try
                    {
                        installer.Install(state);
                        installer.Commit(state);
                    }
                    catch
                    {
                        try
                        {
                            installer.Rollback(state);
                        }
                        catch { }
                        throw;
                    }
                }
            }
            catch (Exception ex)
            {
                logger.ErrorFormat(" Error occured in installing service " + ex.Message);
                throw;
            }
        }

        private static void UninstallService()
        {
            if (!IsInstalled()) return;
            try
            {
                using (AssemblyInstaller installer = GetInstaller())
                {
                    IDictionary state = new Hashtable();
                    try
                    {
                        installer.Uninstall(state);
                    }
                    catch
                    {
                        throw;
                    }
                }
            }
            catch (Exception ex)
            {
                logger.ErrorFormat(" Error occured in uninstalling service " + ex.Message);
                throw;
            }
        }

        private static void StartService()
        {
            if (!IsInstalled()) return;

            using (ServiceController controller =
                new ServiceController(serviceName))
            {
                try
                {
                    if (controller.Status != ServiceControllerStatus.Running)
                    {
                        controller.Start();
                        controller.WaitForStatus(ServiceControllerStatus.Running,
                            TimeSpan.FromSeconds(10));
                    }
                }
                catch (Exception ex)
                {
                    logger.ErrorFormat(" Error occured in starting service " + ex.Message);
                    throw;
                }
            }
        }

        private static void StopService()
        {
            if (!IsInstalled()) return;
            using (ServiceController controller =
                new ServiceController(serviceName))
            {
                try
                {
                    if (controller.Status != ServiceControllerStatus.Stopped)
                    {
                        controller.Stop();
                        controller.WaitForStatus(ServiceControllerStatus.Stopped,
                             TimeSpan.FromSeconds(10));
                    }
                }
                catch (Exception ex)
                {
                    logger.ErrorFormat(" Error occured in stopping service " + ex.Message);
                    throw;
                }
            }
        }
    }
}
