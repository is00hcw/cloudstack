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
package com.cloud.api.query.vo;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

import com.cloud.storage.DiskOfferingVO.Type;
import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = "disk_offering_view")
public class DiskOfferingJoinVO extends BaseViewVO implements InternalIdentity, Identity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "name")
    private String name;

    @Column(name = "display_text")
    private String displayText;

    @Column(name = "disk_size")
    long diskSize;

    @Column(name = "tags", length = 4096)
    String tags;

    @Column(name = "use_local_storage")
    private boolean useLocalStorage;

    @Column(name = "system_use")
    private boolean systemUse;

    @Column(name = "customized")
    private boolean customized;

    @Column(name = "customized_iops")
    private Boolean customizedIops;

    @Column(name = "min_iops")
    private Long minIops;

    @Column(name = "max_iops")
    private Long maxIops;

    @Column(name = "hv_ss_reserve")
    private Integer hypervisorSnapshotReserve;

    @Column(name = "sort_key")
    int sortKey;

    @Column(name = "bytes_read_rate")
    Long bytesReadRate;

    @Column(name = "bytes_write_rate")
    Long bytesWriteRate;

    @Column(name = "iops_read_rate")
    Long iopsReadRate;

    @Column(name = "iops_write_rate")
    Long iopsWriteRate;

    @Column(name = "cache_mode")
    String cacheMode;

    @Column(name = "type")
    Type type;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

    @Column(name = GenericDao.REMOVED_COLUMN)
    private Date removed;

    @Column(name = "domain_id")
    private long domainId;

    @Column(name = "domain_uuid")
    private String domainUuid;

    @Column(name = "domain_name")
    private final String domainName = null;

    @Column(name = "domain_path")
    private final String domainPath = null;

    @Column(name = "display_offering")
    boolean displayOffering;

    public DiskOfferingJoinVO() {
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public String getDisplayText() {
        return displayText;
    }

    public long getDiskSize() {
        return diskSize;
    }

    public String getTags() {
        return tags;
    }

    public boolean isUseLocalStorage() {
        return useLocalStorage;
    }

    public boolean isSystemUse() {
        return systemUse;
    }

    public boolean isCustomized() {
        return customized;
    }

    public Boolean isCustomizedIops() {
        return customizedIops;
    }

    public Long getMinIops() {
        return minIops;
    }

    public Long getMaxIops() {
        return maxIops;
    }

    public Integer getHypervisorSnapshotReserve() {
        return hypervisorSnapshotReserve;
    }

    public String getCacheMode() {
        return cacheMode;
    }

    public void setCacheMode(String cacheMode) {
        this.cacheMode = cacheMode;
    }

    public boolean isDisplayOffering() {
        return displayOffering;
    }

    public Date getCreated() {
        return created;
    }

    public Date getRemoved() {
        return removed;
    }

    public long getDomainId() {
        return domainId;
    }

    public String getDomainUuid() {
        return domainUuid;
    }

    public String getDomainName() {
        return domainName;
    }

    public String getDomainPath() {
        return domainPath;
    }

    public int getSortKey() {
        return sortKey;
    }

    public Type getType() {
        return type;
    }

    public Long getBytesReadRate() {
        return bytesReadRate;
    }

    public Long getBytesWriteRate() {
        return bytesWriteRate;
    }

    public Long getIopsReadRate() {
        return iopsReadRate;
    }

    public Long getIopsWriteRate() {
        return iopsWriteRate;
    }
}
