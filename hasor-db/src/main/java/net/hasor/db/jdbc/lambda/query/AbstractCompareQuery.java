/*
 * Copyright 2002-2010 the original author or authors.
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
package net.hasor.db.jdbc.lambda.query;
import net.hasor.db.jdbc.core.JdbcTemplate;
import net.hasor.db.jdbc.lambda.Compare;
import net.hasor.db.jdbc.lambda.segment.MergeSqlSegment;
import net.hasor.db.jdbc.lambda.segment.Segment;
import net.hasor.db.jdbc.lambda.segment.SqlLike;
import net.hasor.db.types.mapping.FieldInfo;
import net.hasor.db.types.mapping.TableInfo;
import net.hasor.utils.ArrayUtils;
import net.hasor.utils.StringUtils;
import net.hasor.utils.reflect.MethodUtils;
import net.hasor.utils.reflect.SFunction;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

import static net.hasor.db.jdbc.lambda.segment.SqlKeyword.*;

/**
 * 提供 lambda 方式生成 SQL。
 * @version : 2020-10-27
 * @author 赵永春 (zyc@hasor.net)
 */
public abstract class AbstractCompareQuery<T, R> extends AbstractQueryExecute<T> implements Compare<T, R> {
    private static final Map<String, FieldInfo> COLUMN_CACHE      = Collections.synchronizedMap(new WeakHashMap<>());
    private static final ReadWriteLock          COLUMN_CACHE_LOCK = new ReentrantReadWriteLock();
    protected            MergeSqlSegment        queryTemplate     = new MergeSqlSegment();
    protected            List<Object>           queryParam        = new ArrayList<>();
    private              Segment                nextSegmentPrefix = null;
    private              boolean                lookCondition     = false;

    public AbstractCompareQuery(Class<T> exampleType, JdbcTemplate jdbcTemplate) {
        super(exampleType, jdbcTemplate);
    }

    protected FieldInfo columnName(SFunction<T> property) {
        Method targetMethod = MethodUtils.lambdaMethodName(property);
        String cacheKey = targetMethod.toGenericString();
        Lock readLock = COLUMN_CACHE_LOCK.readLock();
        try {
            readLock.lock();
            FieldInfo fieldMeta = COLUMN_CACHE.get(cacheKey);
            if (fieldMeta != null) {
                return fieldMeta;
            }
        } finally {
            readLock.unlock();
        }
        //
        Lock writeLock = COLUMN_CACHE_LOCK.writeLock();
        try {
            writeLock.lock();
            FieldInfo fieldMeta = COLUMN_CACHE.get(cacheKey);
            if (fieldMeta != null) {
                return fieldMeta;
            }
            String methodName = targetMethod.getName();
            String attr;
            if (methodName.startsWith("get")) {
                attr = methodName.substring(3);
            } else {
                attr = methodName.substring(2);
            }
            attr = StringUtils.firstCharToLowerCase(attr);
            //
            FieldInfo fieldInfo = super.getRowMapper().findFieldInfoByProperty(attr);
            COLUMN_CACHE.put(cacheKey, fieldInfo);
            return fieldInfo;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public R or() {
        this.nextSegmentPrefix = OR;
        return this.getSelf();
    }

    @Override
    public R and() {
        this.nextSegmentPrefix = AND;
        return this.getSelf();
    }

    @Override
    public R nested(Consumer<Compare<T, R>> lambda) {
        this.addCondition(LEFT);
        this.nextSegmentPrefix = EMPTY;
        lambda.accept(this);
        this.nextSegmentPrefix = EMPTY;
        this.addCondition(RIGHT);
        return this.getSelf();
    }

    public R eq(SFunction<T> property, Object value) {
        return this.addCondition(() -> conditionName(property), EQ, formatValue(value));
    }

    public R ne(SFunction<T> property, Object value) {
        return this.addCondition(() -> conditionName(property), NE, formatValue(value));
    }

    public R gt(SFunction<T> property, Object value) {
        return this.addCondition(() -> conditionName(property), GT, formatValue(value));
    }

    public R ge(SFunction<T> property, Object value) {
        return this.addCondition(() -> conditionName(property), GE, formatValue(value));
    }

    public R lt(SFunction<T> property, Object value) {
        return this.addCondition(() -> conditionName(property), LT, formatValue(value));
    }

    public R le(SFunction<T> property, Object value) {
        return this.addCondition(() -> conditionName(property), LE, formatValue(value));
    }

    public R like(SFunction<T> property, Object value) {
        return this.addCondition(() -> conditionName(property), LIKE, formatLikeValue(SqlLike.DEFAULT, value));
    }

    public R notLike(SFunction<T> property, Object value) {
        return this.addCondition(() -> conditionName(property), NOT, LIKE, formatLikeValue(SqlLike.DEFAULT, value));
    }

    public R likeRight(SFunction<T> property, Object value) {
        return this.addCondition(() -> conditionName(property), LIKE, formatLikeValue(SqlLike.RIGHT, value));
    }

    public R notLikeRight(SFunction<T> property, Object value) {
        return this.addCondition(() -> conditionName(property), NOT, LIKE, formatLikeValue(SqlLike.RIGHT, value));
    }

    public R likeLeft(SFunction<T> property, Object value) {
        return this.addCondition(() -> conditionName(property), LIKE, formatLikeValue(SqlLike.LEFT, value));
    }

    public R notLikeLeft(SFunction<T> property, Object value) {
        return this.addCondition(() -> conditionName(property), NOT, LIKE, formatLikeValue(SqlLike.LEFT, value));
    }

    public R isNull(SFunction<T> property) {
        return this.addCondition(() -> conditionName(property), IS_NULL);
    }

    public R isNotNull(SFunction<T> property) {
        return this.addCondition(() -> conditionName(property), IS_NOT_NULL);
    }

    public R in(SFunction<T> property, Collection<?> value) {
        return this.addCondition(() -> conditionName(property), IN, LEFT, formatValue(value.toArray()), RIGHT);
    }

    public R notIn(SFunction<T> property, Collection<?> value) {
        return this.addCondition(() -> conditionName(property), NOT, IN, LEFT, formatValue(value.toArray()), RIGHT);
    }

    public R between(SFunction<T> property, Object value1, Object value2) {
        return this.addCondition(() -> conditionName(property), BETWEEN, formatValue(value1), AND, formatValue(value2));
    }

    public R notBetween(SFunction<T> property, Object value1, Object value2) {
        return this.addCondition(() -> conditionName(property), NOT, BETWEEN, formatValue(value1), AND, formatValue(value2));
    }

    public R apply(String sqlString, Object... args) {
        if (StringUtils.isBlank(sqlString)) {
            return this.getSelf();
        }
        this.queryTemplate.addSegment(() -> {
            if (args != null && args.length > 0) {
                for (Object arg : args) {
                    format(arg);
                }
            }
            return sqlString;
        });
        return this.getSelf();
    }

    protected void lockCondition() {
        this.lookCondition = true;
    }

    protected final R addCondition(Segment... segments) {
        if (this.lookCondition) {
            throw new UnsupportedOperationException("condition is locked.");
        }
        //
        if (this.nextSegmentPrefix == EMPTY) {
            this.nextSegmentPrefix = null;
        } else if (this.nextSegmentPrefix == null) {
            this.queryTemplate.addSegment(AND);
            this.nextSegmentPrefix = null;
        } else {
            this.queryTemplate.addSegment(this.nextSegmentPrefix);
            this.nextSegmentPrefix = null;
        }
        //
        for (Segment segment : segments) {
            this.queryTemplate.addSegment(segment);
        }
        return this.getSelf();
    }

    protected abstract R getSelf();

    @Override
    public String getSqlString() {
        return this.queryTemplate.noFirstSqlSegment();
    }

    @Override
    public Object[] getArgs() {
        return this.queryParam.toArray().clone();
    }

    private Segment formatLikeValue(SqlLike like, Object param) {
        return () -> {
            format(param);
            return this.dialect().buildLike(like, param);
        };
    }

    private Segment formatValue(Object... params) {
        if (ArrayUtils.isEmpty(params)) {
            return () -> "";
        }
        MergeSqlSegment mergeSqlSegment = new MergeSqlSegment();
        Iterator<Object> iterator = Arrays.asList(params).iterator();
        while (iterator.hasNext()) {
            mergeSqlSegment.addSegment(formatSegment(iterator.next()));
            if (iterator.hasNext()) {
                mergeSqlSegment.addSegment(() -> ",");
            }
        }
        return mergeSqlSegment;
    }

    protected Segment formatSegment(Object param) {
        return () -> format(param);
    }

    protected String format(Object param) {
        this.queryParam.add(param);
        return "?";
    }

    protected String conditionName(SFunction<T> property) {
        TableInfo tableInfo = super.getRowMapper().getTableInfo();
        return this.dialect().buildConditionName(tableInfo, columnName(property));
    }
}