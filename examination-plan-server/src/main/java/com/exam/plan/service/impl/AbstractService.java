package com.exam.plan.service.impl;

import com.exam.plan.mapper.MyMapper;
import com.exam.plan.entity.ResultCode;
import com.exam.plan.exception.ResourcesNotFoundException;
import com.exam.plan.exception.ServiceException;
import com.exam.plan.service.Service;
import org.apache.ibatis.exceptions.TooManyResultsException;
import org.springframework.beans.factory.annotation.Autowired;
import tk.mybatis.mapper.entity.Condition;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Optional;

/**
 * 基于通用 MyBatis Mapper 插件的 Service 接口的实现
 *
 * @author Zoctan
 * @date 2018/05/27
 */
public abstract class AbstractService<T> implements Service<T> {
    /**
     * 当前泛型的实体 Class
     */
    private final Class<T> entityClass;

    @Autowired
    protected MyMapper<T> mapper;

    protected AbstractService() {
        final ParameterizedType pt = (ParameterizedType) this.getClass().getGenericSuperclass();
        //noinspection unchecked
        this.entityClass = (Class<T>) pt.getActualTypeArguments()[0];
    }

    private void assertSave(final boolean statement) {
        asserts(statement, ResultCode.SAVE_FAILED);
    }

    private void assertDelete(final boolean statement) {
        asserts(statement, ResultCode.DELETE_FAILED);
    }

    private void assertUpdate(final boolean statement) {
        asserts(statement, ResultCode.UPDATE_FAILED);
    }

    private T getEntity(final String fieldName, final Object value) throws Exception {
        final T entity = this.entityClass.getDeclaredConstructor().newInstance();
        final Field field = this.entityClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(entity, value);
        return entity;
    }

    @Override
    public void assertById(final Object id) {
        Optional.ofNullable(this.mapper.selectByPrimaryKey(id))
                .orElseThrow(ResourcesNotFoundException::new);
    }

    @Override
    public void assertBy(final T entity) {
        Optional.ofNullable(this.mapper.select(entity)).orElseThrow(ResourcesNotFoundException::new);
    }

    @Override
    public void assertByIds(final String ids) {
        final int count = this.countByIds(ids);
        // id数和列表数不对应
        if (ids.split(",").length > count) {
            throw new ResourcesNotFoundException();
        }
    }

    @Override
    public int countByIds(final String ids) {
        return this.mapper.selectByIds(ids).size();
    }

    @Override
    public int countByCondition(final Condition condition) {
        return this.mapper.selectByCondition(condition).size();
    }

    @Override
    public void save(final T entity) {
        this.assertSave(this.mapper.insertSelective(entity) == 1);
    }

    @Override
    public void save(final List<T> entities) {
        this.assertSave(this.mapper.insertList(entities) == entities.size());
    }

    @Override
    public void deleteById(final Object id) {
        this.assertById(id);
        this.assertDelete(this.mapper.deleteByPrimaryKey(id) == 1);
    }

    @Override
    public void deleteBy(final String fieldName, final Object value) throws TooManyResultsException {
        try {
            final T entity = this.getEntity(fieldName, value);
            this.assertBy(entity);
            this.assertDelete(this.mapper.delete(entity) == 1);
        } catch (final Exception e) {
            throw new ServiceException(e.getMessage(), e);
        }
    }

    @Override
    public void deleteByIds(final String ids) {
        this.assertByIds(ids);
        this.assertDelete(this.mapper.deleteByIds(ids) == ids.split(",").length);
    }

    @Override
    public void deleteByCondition(final Condition condition) {
        final int count = this.countByCondition(condition);
        this.assertDelete(this.mapper.deleteByCondition(condition) == count);
    }

    @Override
    public void update(final T entity) {
        this.assertUpdate(this.mapper.updateByPrimaryKeySelective(entity) == 1);
    }

    @Override
    public void updateByCondition(final T entity, final Condition condition) {
        this.assertUpdate(this.mapper.updateByConditionSelective(entity, condition) == 1);
    }

    @Override
    public T getById(final Object id) {
        return this.mapper.selectByPrimaryKey(id);
    }

    @Override
    public T getBy(final String fieldName, final Object value) throws TooManyResultsException {
        try {
            final T entity = this.getEntity(fieldName, value);
            return this.mapper.selectOne(entity);
        } catch (final Exception e) {
            throw new ServiceException(e.getMessage(), e);
        }
    }

    @Override
    public List<T> listByIds(final String ids) {
        return this.mapper.selectByIds(ids);
    }

    public List<T> listByCondition(final Condition condition) {
        return this.mapper.selectByCondition(condition);
    }

    @Override
    public List<T> listAll() {
        return this.mapper.selectAll();
    }


    private void throwIf(
            final boolean statement, final ResultCode resultCode, final String message) {
        if (statement) {
            throw toThrow(resultCode, message);
        }
    }

    private void throwIf(
            final boolean statement, final ResultCode resultCode, final Object... messages) {
        throwIf(statement, resultCode, resultCode.format(messages));
    }

    private RuntimeException toThrow(final ResultCode resultCode, final Object... messages) {
        return new ServiceException(resultCode, resultCode.format(messages));
    }

    private void asserts(
            final boolean statement, final ResultCode resultCode, final Object... messages) {
        throwIf(!statement, resultCode, messages);
    }

}
