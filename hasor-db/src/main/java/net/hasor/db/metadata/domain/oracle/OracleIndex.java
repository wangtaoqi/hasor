/*
 * Copyright 2008-2009 the original author or authors.
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
package net.hasor.db.metadata.domain.oracle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Oracle 索引
 * @version : 2021-04-29
 * @author 赵永春 (zyc@hasor.net)
 */
public class OracleIndex {
    private String              schema;
    private String              name;
    private OracleIndexType     indexType;
    private List<String>        columns     = new ArrayList<>();
    private Map<String, String> storageType = new HashMap<>();
    //
    private boolean             primaryKey;
    private boolean             unique;
    private boolean             partitioned;// YES / NO
    /** Indicates whether the index is on a temporary table (Y) or not (N) */
    private boolean             temporary;//
    /** Indicates whether the name of the index is system-generated */
    private boolean             generated;//system-generated (Y) or not (N)

    public String getSchema() {
        return this.schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public OracleIndexType getIndexType() {
        return this.indexType;
    }

    public void setIndexType(OracleIndexType indexType) {
        this.indexType = indexType;
    }

    public List<String> getColumns() {
        return this.columns;
    }

    public void setColumns(List<String> columns) {
        this.columns = columns;
    }

    public Map<String, String> getStorageType() {
        return this.storageType;
    }

    public void setStorageType(Map<String, String> storageType) {
        this.storageType = storageType;
    }

    public boolean isPrimaryKey() {
        return this.primaryKey;
    }

    public void setPrimaryKey(boolean primaryKey) {
        this.primaryKey = primaryKey;
    }

    public boolean isUnique() {
        return this.unique;
    }

    public void setUnique(boolean unique) {
        this.unique = unique;
    }

    public boolean isPartitioned() {
        return this.partitioned;
    }

    public void setPartitioned(boolean partitioned) {
        this.partitioned = partitioned;
    }

    public boolean isTemporary() {
        return this.temporary;
    }

    public void setTemporary(boolean temporary) {
        this.temporary = temporary;
    }

    public boolean isGenerated() {
        return this.generated;
    }

    public void setGenerated(boolean generated) {
        this.generated = generated;
    }
}
