package com.jhl.admin.util;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import java.beans.PropertyDescriptor;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
 
/**
 * @author wuweifeng wrote on 2018/12/4.
 */
public class SimpleJpaRepositoryImpl<T, ID> extends SimpleJpaRepository<T, ID> {
 
    private final JpaEntityInformation<T, ?> entityInformation;
    private final EntityManager em;
 
    @Autowired
    public SimpleJpaRepositoryImpl(JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.entityInformation = entityInformation;
        this.em = entityManager;
    }
 
    /**
     * Универсальный метод сохранения: новое/выборочное обновление.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public <S extends T> S save(S entity) {
        //Получить идентификатор
        ID entityId = (ID) entityInformation.getId(entity);
        Optional<T> optionalT;
        if (StringUtils.isEmpty(entityId)) {
            optionalT = Optional.empty();
        } else {
            //若ID非空 则查询最新数据
            optionalT = findById(entityId);
        }
        //Получить пустые атрибуты и обработать их в ноль
        String[] nullProperties = getNullProperties(entity);
        //Если результат запроса на основе идентификатора пуст
        if (!optionalT.isPresent()) {
            //Новый
            em.persist(entity);
            return entity;
        } else {
            //1.Получить последний объект
            T target = optionalT.get();
            //2.Перезаписать ненулевые свойства на последний объект
            BeanUtils.copyProperties(entity, target, nullProperties);
            //3.Обновить непустые свойства
            em.merge(target);
            return entity;
        }
    }
 
    /**
     * Получить пустые свойства объекта
     */
    private static String[] getNullProperties(Object src) {
        //1.GetBean
        BeanWrapper srcBean = new BeanWrapperImpl(src);
        //2.Получить описание свойства компонента
        PropertyDescriptor[] pds = srcBean.getPropertyDescriptors();
        //3.Получить пустое свойство Bean
        Set<String> properties = new HashSet<>();
        for (PropertyDescriptor propertyDescriptor : pds) {
            String propertyName = propertyDescriptor.getName();
            Object propertyValue = srcBean.getPropertyValue(propertyName);
            if (StringUtils.isEmpty(propertyValue)) {
                srcBean.setPropertyValue(propertyName, null);
                properties.add(propertyName);
            }
        }
        return properties.toArray(new String[0]);
    }
}