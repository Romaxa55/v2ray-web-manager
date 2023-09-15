package com.jhl.admin.service;

import com.jhl.admin.VO.UserVO;
import com.jhl.admin.cache.DefendBruteForceAttackUser;
import com.jhl.admin.model.Account;
import com.jhl.admin.model.User;
import com.jhl.admin.repository.UserRepository;
import com.jhl.admin.util.Validator;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class UserService {

    @Autowired
    UserRepository userRepository;
    @Autowired
    AccountService accountService;
    @Autowired
    EmailService emailService;
    @Autowired
    StatService StatService;
    @Autowired
    DefendBruteForceAttackUser defendBruteForceAttackUser;

    public void reg(User user) {
        Validator.isNotNull(user);
        if (!emailService.verifyCode(user.getEmail(), user.getVCode())) {
            throw new IllegalArgumentException("Ошибка кода подтверждения");
        }

        adminReg(user);
    }

    public void adminReg(User user) {
        User exist = userRepository.findOne(Example.of(User.builder().email(user.getEmail()).build())).orElse(null);
        if (exist != null) {
            throw new RuntimeException("Аккаунт уже существует. Если вы забыли свой пароль, восстановите его.");
        }
        create(user);
        Account account = Account.builder().userId(user.getId()).build();
        accountService.create(account);
        StatService.createOrGetStat(account);
    }

    public void changePassword(User user) {
        Validator.isNotNull(user);
        if (!emailService.verifyCode(user.getEmail(), user.getVCode())) {
            throw new IllegalArgumentException("Ошибка кода подтверждения");
        }
        User dbUser = userRepository.findOne(Example.of(User.builder().email(user.getEmail()).build())).orElse(null);
        Validator.isNotNull(user.getPassword());
        if (dbUser == null) {
            throw new NullPointerException("Пользователь не существует");
        }
        User newUser = User.builder().password(encodePassword(user.getPassword()))
                .build();
        newUser.setId(dbUser.getId());
        userRepository.save(newUser);
        //Снять ограничения доступа
        defendBruteForceAttackUser.rmCache(user.getEmail());
    }

    /**
     * @param userId
     * @param oldPw  Старый пароль
     * @param newPw  Новый пароль
     */
    public void changePassword(Integer userId, String oldPw, String newPw) {
        Validator.isNotNull(userId);
        Validator.isNotNull(oldPw);
        Validator.isNotNull(newPw);
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new NullPointerException("Пользователь не существует");
        }
        String password = user.getPassword();

        if (!encodePassword(oldPw).equals(password)) throw new RuntimeException("Старый пароль неверен");
        user.setPassword(encodePassword(newPw));
        userRepository.save(user);
    }

    public void create(User user) {
        String password = user.getPassword();
        String digestPW = DigestUtils.md5Hex(password);
        user.setPassword(digestPW);
        if (user.getStatus() == null) user.setStatus(1);
        userRepository.save(user);
    }

    public User login(UserVO user) {
        Validator.isNotNull(user);
        String email = user.getEmail();
        Validator.isNotNull(email, "Email пуст");
        String password = user.getPassword();
        Validator.isNotNull(password, "Пароль пуст");
        AtomicInteger tryCount = defendBruteForceAttackUser.getCache(email);
        if (tryCount != null && tryCount.get() > 4) throw new RuntimeException("В целях вашей безопасности ваш аккаунт заблокирован. Повторите попытку через час или смените пароль.");


        Example<User> userExample = Example.of(User.builder().email(StringUtils.trim(email))
                .password(StringUtils.trim(password)).build());
        User dbUser = userRepository.findOne(userExample).orElse(null);

        if (dbUser == null || dbUser.getId() == null) {

            if (tryCount == null) {
                tryCount = new AtomicInteger(1);
                defendBruteForceAttackUser.setCache(email, tryCount);
            } else {
                tryCount.addAndGet(1);
            }
            throw new IllegalArgumentException("Ошибка учетной записи/пароля");
        }
        if (dbUser.getStatus() != 1) throw new RuntimeException("Аккаунт отключен");
        dbUser.setPassword(null);
        defendBruteForceAttackUser.rmCache(email);
        return dbUser;
    }


    public User get(Integer id) {

        return userRepository.findById(id).orElse(null);
    }

    public Map<Integer, User> getUserMapBy(Iterable<Integer> ids) {
        Map<Integer, User> userMap = new HashMap<>();
        if (ids == null) return userMap;
        final List<User> regUsers = userRepository.findAllById(ids);
        regUsers.forEach(user1 -> userMap.put(user1.getId(), user1));

        return userMap;
    }

    public UserVO getOne(User user) {
        user.setStatus(1);
        Optional<User> one = userRepository.findOne(Example.of(user));
        if (!one.isPresent()) return null;
        UserVO userVO = one.get().toVO(UserVO.class);
        userVO.setPassword(null);
        return userVO;
    }

    public User getOneByAdmin(User user) {
        user.setStatus(1);
        Optional<User> one = userRepository.findOne(Example.of(user));
        return one.orElse(null);
    }

    public User getUserButRemovePW(
            Integer id) {

        User user = get(id);
        if (user != null)
            user.setPassword(null);
        return user;
    }

    public void addRemark(Integer userId, String remark) {
        User user = new User();
        user.setId(userId);
        user.setRemark(remark);
        userRepository.save(user);
    }

    public String encodePassword(String pw) {
        return DigestUtils.md5Hex(pw);
    }
}
