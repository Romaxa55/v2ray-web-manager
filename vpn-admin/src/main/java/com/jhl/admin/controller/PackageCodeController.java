package com.jhl.admin.controller;

import com.jhl.admin.model.Package;
import com.jhl.admin.model.PackageCode;
import com.jhl.admin.repository.PackageCodeRepository;
import com.jhl.admin.repository.PackageRepository;
import com.jhl.admin.util.Utils;
import com.ljh.common.model.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class PackageCodeController {
    @Autowired
    PackageRepository packageRepository;
    @Autowired
    PackageCodeRepository packageCodeRepository;
    private static  final  int  PACKAGE_CODE_VALID_DAY =7;
    @ResponseBody
    @GetMapping("/package/code/generate")
    public Result generate(Integer packageId) {
        if (packageId == null) throw new NullPointerException("packageId не может быть пустым");
        Package aPackage = packageRepository.findById(packageId).orElse(null);
        if (aPackage == null) throw new NullPointerException("package не может быть пустым");
        PackageCode pc = new PackageCode();
        pc.setCode(Utils.getCharAndNum(10));
        pc.setStatus(0);
        pc.setExpire(Utils.getDateBy(PACKAGE_CODE_VALID_DAY));
        Package aPackage1 = new Package();
        aPackage1.setId(aPackage.getId());
        packageCodeRepository.save(pc);
        return Result.builder().code(Result.CODE_SUCCESS).obj(aPackage).build();
    }

    /**
     * Запросить все PKcodes
     * @param page Страница
     * @param pageSize Размер ее
     * @return Запросить все PKcodes
     */
    @ResponseBody
    @GetMapping("/package/code")
    public Result findAll(Integer page , Integer pageSize) {
        Page<PackageCode> all = packageCodeRepository.findAll(PageRequest.of(page, pageSize));
        List<PackageCode> packageCodes = all.getContent();
        return Result.builder().code(Result.CODE_SUCCESS).obj(packageCodes).build();
    }
    /**
     * Обновить код
     * @param packageCode Код
     * @return Обновить код
     */
    @ResponseBody
    @PutMapping("/package/code")
    public Result update(@RequestBody PackageCode packageCode) {
        if (packageCode == null || packageCode.getId() ==null) throw new NullPointerException("Id не может быть пустым");

        packageCodeRepository.save(packageCode);
        return Result.doSuccess();
    }

    /**
     * Недействительный pkcode
     * @param id id пакета
     * @return Недействительный pkcode
     */
    @ResponseBody
    @GetMapping("/package/code/invalid/{id}")
    public Result invalidCode(@PathVariable Integer id) {
        if (id ==null) throw new NullPointerException("Id не может быть пустым");

        PackageCode packageCode = new PackageCode();
         packageCode.setId(id);
         packageCode.setStatus(-1);
        packageCodeRepository.save(packageCode);
        return Result.doSuccess();
    }


}
