package com.jhl.admin.repository;

import com.jhl.admin.model.Notice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface NoticeRepository extends  JpaRepository<Notice,Integer> {

    /**
     * top7
     * @param status The status of the notice.
     * @param date The date of the notice.
     * @return A list of notices that match the criteria.
     */
    public List<Notice> findTop7ByStatusAndToDateAfterOrderByUpdateTimeDesc(Integer status, Date date);
}
