package com.jhl.admin.controller;

import com.jhl.admin.Interceptor.PreAuth;
import com.jhl.admin.VO.NoticeVO;
import com.jhl.admin.model.Notice;
import com.jhl.admin.repository.NoticeRepository;
import com.ljh.common.model.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@Slf4j
@Controller
public class NoticeController {
    @Autowired
    NoticeRepository noticeRepository;

    /**
     *Создать уведомление
     *
     * @return
     */
    @PreAuth("admin")
    @ResponseBody
    @PostMapping("/notice")
    public Result createNotice(@RequestBody NoticeVO notice) {
        addOrUpdate(notice);
        return Result.doSuccess();
    }

    /**
     * возобновлять
     *
     * @param notice
     * @return
     */
    @PreAuth("admin")
    @ResponseBody
    @PutMapping("/notice")
    public Result updateNotice(@RequestBody NoticeVO notice) {
        addOrUpdate(notice);
        return Result.doSuccess();
    }

    /**
     * удалить
     *
     * @param id
     * @return
     */
    @PreAuth("admin")
    @ResponseBody
    @DeleteMapping("/notice/{id}")
    public Result delNotice(@PathVariable Integer id) {
        if (id == null) throw new NullPointerException("Id не может быть пустым");
        noticeRepository.deleteById(id);
        return Result.doSuccess();
    }

    /**
     * Получать
     *
     * @param id
     * @return
     */
    @ResponseBody
    @GetMapping("/notice/{id}")
    public Result getNotice(@PathVariable Integer id) {
        if (id == null) throw new NullPointerException("Id не может быть пустым");
        Notice notice = noticeRepository.findById(id).orElse(new Notice());

        return Result.buildSuccess(notice.toVO(NoticeVO.class), null);
    }

    /**
     * Получите первые 7 отображаемых объявлений
     *
     * @return
     */
    @ResponseBody
    @GetMapping("/notice")
    public Result list() {
        List<Notice> notices = noticeRepository.findTop7ByStatusAndToDateAfterOrderByUpdateTimeDesc(1, new Date());
        Result success = Result.doSuccess();
        success.setObj(notices);
        return success;
    }


    private void addOrUpdate(@RequestBody NoticeVO notice) {

        if ( notice ==null|| StringUtils.isBlank(notice.getContent())
                || StringUtils.isBlank(notice.getName())
                || notice.getToDate() == null
                || notice.getStatus() == null
        ) {
            log.warn("notice:{}", notice);
            if (notice == null) throw new NullPointerException("Не может быть пустым");
        }
        noticeRepository.save(notice.toModel(Notice.class));
    }
}
