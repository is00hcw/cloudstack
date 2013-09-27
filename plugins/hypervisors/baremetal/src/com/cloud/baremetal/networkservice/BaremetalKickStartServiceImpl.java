// Licensed to the Apache Software Foundation (ASF) under one
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
//
// Automatically generated by addcopyright.py at 01/29/2013
package com.cloud.baremetal.networkservice;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.AddBaremetalKickStartPxeCmd;
import org.apache.cloudstack.api.AddBaremetalPxeCmd;
import org.apache.cloudstack.api.ListBaremetalPxeServersCmd;
import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.baremetal.IpmISetBootDevCommand;
import com.cloud.agent.api.baremetal.IpmISetBootDevCommand.BootDev;
import com.cloud.baremetal.database.BaremetalPxeDao;
import com.cloud.baremetal.database.BaremetalPxeVO;
import com.cloud.baremetal.networkservice.BaremetalPxeManager.BaremetalPxeType;
import com.cloud.deploy.DeployDestination;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderVO;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ServerResource;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.SearchCriteria2;
import com.cloud.utils.db.SearchCriteria2;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachineProfile;

@Local(value = BaremetalPxeService.class)
public class BaremetalKickStartServiceImpl extends BareMetalPxeServiceBase implements BaremetalPxeService {
    private static final Logger s_logger = Logger.getLogger(BaremetalKickStartServiceImpl.class);
    @Inject
    ResourceManager _resourceMgr;
    @Inject
    PhysicalNetworkDao _physicalNetworkDao;
    @Inject
    PhysicalNetworkServiceProviderDao _physicalNetworkServiceProviderDao;
    @Inject
    HostDetailsDao _hostDetailsDao;
    @Inject
    BaremetalPxeDao _pxeDao;
    @Inject
    NetworkDao _nwDao;
    @Inject
    VMTemplateDao _tmpDao;

    @Override
    public boolean prepare(VirtualMachineProfile profile, NicProfile nic, DeployDestination dest, ReservationContext context) {
        NetworkVO nwVO = _nwDao.findById(nic.getNetworkId());
        SearchCriteria2<BaremetalPxeVO, BaremetalPxeVO> sc = SearchCriteria2.create(BaremetalPxeVO.class);
        sc.addAnd(sc.getEntity().getDeviceType(), Op.EQ, BaremetalPxeType.KICK_START.toString());
        sc.addAnd(sc.getEntity().getPhysicalNetworkId(), Op.EQ, nwVO.getPhysicalNetworkId());
        BaremetalPxeVO pxeVo = sc.find();
        if (pxeVo == null) {
            throw new CloudRuntimeException("No kickstart PXE server found in pod: " + dest.getPod().getId() + ", you need to add it before starting VM");
        }
        VMTemplateVO template = _tmpDao.findById(profile.getTemplateId());

        try {
            String tpl = profile.getTemplate().getUrl();
            assert tpl != null : "How can a null template get here!!!";
            String[] tpls = tpl.split(";");
            CloudRuntimeException err = new CloudRuntimeException(String.format("template url[%s] is not correctly encoded. it must be in format of ks=http_link_to_kickstartfile;kernel=nfs_path_to_pxe_kernel;initrd=nfs_path_to_pxe_initrd", tpl));
            if (tpls.length != 3) {
                throw err;
            }
            
            String ks = null;
            String kernel = null;
            String initrd = null;
            
            for (String t : tpls) {
                String[] kv = t.split("=");
                if (kv.length != 2) {
                    throw err;
                }
                if (kv[0].equals("ks")) {
                    ks = kv[1];
                } else if (kv[0].equals("kernel")) {
                    kernel = kv[1];
                } else if (kv[0].equals("initrd")) {
                    initrd = kv[1];
                } else {
                    throw err;
                }
            }

            PrepareKickstartPxeServerCommand cmd = new PrepareKickstartPxeServerCommand();
            cmd.setKsFile(ks);
            cmd.setInitrd(initrd);
            cmd.setKernel(kernel);
            cmd.setMac(nic.getMacAddress());
            cmd.setTemplateUuid(template.getUuid());
            Answer aws = _agentMgr.send(pxeVo.getHostId(), cmd);
            if (!aws.getResult()) {
                s_logger.warn("Unable to set host: " + dest.getHost().getId() + " to PXE boot because " + aws.getDetails());
                return aws.getResult();
            }

            IpmISetBootDevCommand bootCmd = new IpmISetBootDevCommand(BootDev.pxe);
            aws = _agentMgr.send(dest.getHost().getId(), bootCmd);
            if (!aws.getResult()) {
                s_logger.warn("Unable to set host: " + dest.getHost().getId() + " to PXE boot because " + aws.getDetails());
            }

            return aws.getResult();
        } catch (Exception e) {
            s_logger.warn("Cannot prepare PXE server", e);
            return false;
        }
    }

    @Override
    public boolean prepareCreateTemplate(Long pxeServerId, UserVm vm, String templateUrl) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    @DB
    public BaremetalPxeVO addPxeServer(AddBaremetalPxeCmd cmd) {
        AddBaremetalKickStartPxeCmd kcmd = (AddBaremetalKickStartPxeCmd)cmd;
        PhysicalNetworkVO pNetwork = null;
        long zoneId;

        if (cmd.getPhysicalNetworkId() == null || cmd.getUrl() == null || cmd.getUsername() == null || cmd.getPassword() == null) {
            throw new IllegalArgumentException("At least one of the required parameters(physical network id, url, username, password) is null");
        }

        pNetwork = _physicalNetworkDao.findById(cmd.getPhysicalNetworkId());
        if (pNetwork == null) {
            throw new IllegalArgumentException("Could not find phyical network with ID: " + cmd.getPhysicalNetworkId());
        }
        zoneId = pNetwork.getDataCenterId();

        PhysicalNetworkServiceProviderVO ntwkSvcProvider = _physicalNetworkServiceProviderDao.findByServiceProvider(pNetwork.getId(), BaremetalPxeManager.BAREMETAL_PXE_SERVICE_PROVIDER.getName());
        if (ntwkSvcProvider == null) {
            throw new CloudRuntimeException("Network Service Provider: " + BaremetalPxeManager.BAREMETAL_PXE_SERVICE_PROVIDER.getName() +
                    " is not enabled in the physical network: " + cmd.getPhysicalNetworkId() + "to add this device");
        } else if (ntwkSvcProvider.getState() == PhysicalNetworkServiceProvider.State.Shutdown) {
            throw new CloudRuntimeException("Network Service Provider: " + ntwkSvcProvider.getProviderName() +
                    " is in shutdown state in the physical network: " + cmd.getPhysicalNetworkId() + "to add this device");
        }

        List<HostVO> pxes = _resourceMgr.listAllHostsInOneZoneByType(Host.Type.BaremetalPxe, zoneId);
        if (!pxes.isEmpty()) {
            throw new IllegalArgumentException("Already had a PXE server zone: " + zoneId);
        }

        String tftpDir = kcmd.getTftpDir();
        if (tftpDir == null) {
            throw new IllegalArgumentException("No TFTP directory specified");
        }

        URI uri;
        try {
            uri = new URI(cmd.getUrl());
        } catch (Exception e) {
            s_logger.debug(e);
            throw new IllegalArgumentException(e.getMessage());
        }
        String ipAddress = uri.getHost();
        if (ipAddress == null) {
            ipAddress = cmd.getUrl();
        }

        String guid = getPxeServerGuid(Long.toString(zoneId), BaremetalPxeType.KICK_START.toString(), ipAddress);

        ServerResource resource = null;
        Map params = new HashMap<String, String>();
        params.put(BaremetalPxeService.PXE_PARAM_ZONE, Long.toString(zoneId));
        params.put(BaremetalPxeService.PXE_PARAM_IP, ipAddress);
        params.put(BaremetalPxeService.PXE_PARAM_USERNAME, cmd.getUsername());
        params.put(BaremetalPxeService.PXE_PARAM_PASSWORD, cmd.getPassword());
        params.put(BaremetalPxeService.PXE_PARAM_TFTP_DIR, tftpDir);
        params.put(BaremetalPxeService.PXE_PARAM_GUID, guid);
        resource = new BaremetalKickStartPxeResource();
        try {
            resource.configure("KickStart PXE resource", params);
        } catch (Exception e) {
            throw new CloudRuntimeException(e.getMessage(), e);
        }

        Host pxeServer = _resourceMgr.addHost(zoneId, resource, Host.Type.BaremetalPxe, params);
        if (pxeServer == null) {
            throw new CloudRuntimeException("Cannot add PXE server as a host");
        }

        BaremetalPxeVO vo = new BaremetalPxeVO();
        Transaction txn = Transaction.currentTxn();
        vo.setHostId(pxeServer.getId());
        vo.setNetworkServiceProviderId(ntwkSvcProvider.getId());
        vo.setPhysicalNetworkId(kcmd.getPhysicalNetworkId());
        vo.setDeviceType(BaremetalPxeType.KICK_START.toString());
        txn.start();
        _pxeDao.persist(vo);
        txn.commit();
        return vo;
    }

    @Override
    public BaremetalPxeResponse getApiResponse(BaremetalPxeVO vo) {
        BaremetalPxeResponse response = new BaremetalPxeResponse();
        response.setId(vo.getUuid());
        HostVO host = _hostDao.findById(vo.getHostId());
        response.setUrl(host.getPrivateIpAddress());
        PhysicalNetworkServiceProviderVO providerVO = _physicalNetworkServiceProviderDao.findById(vo.getNetworkServiceProviderId());
        response.setPhysicalNetworkId(providerVO.getUuid());
        PhysicalNetworkVO nwVO = _physicalNetworkDao.findById(vo.getPhysicalNetworkId());
        response.setPhysicalNetworkId(nwVO.getUuid());
        response.setObjectName("baremetalpxeserver");
        return response;
    }

    @Override
    public List<BaremetalPxeResponse> listPxeServers(ListBaremetalPxeServersCmd cmd) {
        List<BaremetalPxeResponse> responses = new ArrayList<BaremetalPxeResponse>();
        if (cmd.getId() != null) {
            BaremetalPxeVO vo = _pxeDao.findById(cmd.getId());
            responses.add(getApiResponse(vo));
            return responses;
        }

        List<BaremetalPxeVO> vos = _pxeDao.listAll();
        for (BaremetalPxeVO vo : vos) {
            responses.add(getApiResponse(vo));
        }
        return responses;
    }

    @Override
    public String getPxeServiceType() {
        return BaremetalPxeManager.BaremetalPxeType.KICK_START.toString();
    }

}
