package com.jhl.admin.controller;

import com.jhl.admin.Interceptor.PreAuth;
import com.jhl.admin.VO.GenerateInviteCodeVO;
import com.jhl.admin.VO.InvitationCodeVO;
import com.jhl.admin.VO.UserVO;
import com.jhl.admin.cache.UserCache;
import com.jhl.admin.constant.enumObject.WebsiteConfigEnum;
import com.jhl.admin.model.InvitationCode;
import com.jhl.admin.model.User;
import com.jhl.admin.repository.InvitationCodeRepository;
import com.jhl.admin.service.ServerConfigService;
import com.jhl.admin.service.UserService;
import com.ljh.common.model.Result;
import org.hibernate.validator.constraints.Range;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.*;

@Controller
@Validated

public class InvitationCodeController {
    @Autowired
    InvitationCodeRepository invitationCodeRepository;
    @Autowired
    UserCache userCache;
    @Autowired
    ServerConfigService serverConfigService;
    @Autowired
    UserService userService;

    /**
     * Создать код приглашения
     * @param  generateInviteCodeVO vo
     * @param  auth userCache
     * @return Result.doSuccess
     */
    @PreAuth("vip")
    @ResponseBody
    @PostMapping("/invite-code")
    public Result generateInviteCode(@CookieValue(value = UserController.COOKIE_NAME, defaultValue = "") String auth,
                                     @RequestBody @Valid GenerateInviteCodeVO generateInviteCodeVO ) {
        UserVO user = userCache.getCache(auth);
        if (auth == null || user==null) throw new NullPointerException("Не могу получить пользователя");
      final Date effectiveTime = generateInviteCodeVO.getEffectiveTime();
        if ( effectiveTime==null  || effectiveTime.before(new Date())) throw new IllegalArgumentException("非法的有效时间:"+(effectiveTime==null?"null":effectiveTime.toString()));

        if (!user.getRole().equals("admin") && !serverConfigService.checkKey(WebsiteConfigEnum.VIP_CAN_INVITE.getKey())) {
            throw new RuntimeException("Администраторы не разрешают пользователям приглашать других");
        }
      final Integer quantity = generateInviteCodeVO.getQuantity();
        Integer userId = user.getId();
        //Users не могут создавать несколько кодов приглашения.
        if(!user.getRole().equals("admin")){
            if (quantity>1) throw new RuntimeException("Есть неиспользованные коды приглашений。");
            long count = invitationCodeRepository.count(Example.of(InvitationCode.builder().generateUserId(userId).status(0).build()));
            if (count > 0) throw new RuntimeException("Есть неиспользованные коды приглашений。");
        }
        //save
        List<InvitationCode> invitationCodeList = new ArrayList<>(quantity);
        for (int i =0;i<quantity;i++){
            InvitationCode code = InvitationCode.builder().generateUserId(userId).inviteCode(UUID.randomUUID().toString()).build();
            code.setStatus(0);
            code.setEffectiveTime(effectiveTime);
            invitationCodeList.add(code);
        }
        invitationCodeRepository.saveAll(invitationCodeList);
        return Result.doSuccess();
    }

    @PreAuth("vip")
    @ResponseBody
    @GetMapping("/invite-code")
    public Result listByUser(@CookieValue(value = UserController.COOKIE_NAME, defaultValue = "") String auth, Integer page, Integer pageSize) {

        if (auth == null) throw new NullPointerException("Не могу получить пользователя");
        UserVO user = userCache.getCache(auth);
        Integer userId = user.getId();
        Page<InvitationCode> codes = null;
        codes = invitationCodeRepository.findAll(Example.of(InvitationCode.builder().generateUserId(userId).build()),
                PageRequest.of(page - 1, pageSize, Sort.by(Sort.Order.asc("status"))));

        final List<InvitationCode> content = codes.getContent();

        Set<Integer> regUserIds = new HashSet<>();

        content.forEach(invitationCode -> {
            if (invitationCode.getRegUserId() != null) regUserIds.add(invitationCode.getRegUserId());
        });

        final Map<Integer, User> userMap = userService.getUserMapBy(regUserIds);

        List<InvitationCodeVO> result = new ArrayList<>();
        content.forEach(code -> {
            final InvitationCodeVO invitationCodeVO = code.toVO(InvitationCodeVO.class);
            if (invitationCodeVO != null &&  !userMap.isEmpty() && code.getRegUserId()!=null) {
                 String email = userMap.get(code.getRegUserId()).getEmail();
                final int indexOf = email.indexOf("@");
                //Десенсибилизирующая терапия для лиц, не являющихся администраторами
                 if (indexOf>0  &&!user.getRole().equals("admin")){
                     final String theLastPartStr = email.substring(indexOf);
                       final char firstChar = email.charAt(0);
                          email = firstChar + "***" + theLastPartStr;
                 }
                invitationCodeVO.setUserName(email);
            }
            result.add(invitationCodeVO);

        });
        // BaseEntity.toVOList(codes.getContent(), InvitationCodeVO.class)
        return Result.buildPageObject(codes.getTotalElements(), result);
    }

    @PreAuth("vip")
    @ResponseBody
    @DeleteMapping("/invite-code/{codeId}")
    public Result delete(@CookieValue(value = UserController.COOKIE_NAME, defaultValue = "") String auth, @PathVariable Integer codeId) {

        if (auth == null) throw new NullPointerException("Не могу получить пользователя");
        if (codeId == null) throw new NullPointerException("id Не может быть пустым");
        UserVO user = userCache.getCache(auth);
        if (user ==null) throw  new NullPointerException("Не вошел");
        Integer userId = user.getId();
        InvitationCode invitationCode = new InvitationCode();
        invitationCode.setId(codeId);
        invitationCode.setGenerateUserId(userId);
        invitationCodeRepository.delete(invitationCode);
        return Result.doSuccess();
    }
}
