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

    /**
     * Регистрирует нового пользователя.
     *
     * @param user Пользователь, который должен быть зарегистрирован.
     */
    public void reg(User user) {
        Validator.isNotNull(user);
        if (!emailService.verifyCode(user.getEmail(), user.getVCode())) {
            throw new IllegalArgumentException("Ошибка кода подтверждения");
        }

        adminReg(user);
    }

    /**
     * Регистрирует нового пользователя от имени администратора.
     *
     * @param user Пользователь для регистрации.
     */
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

    /**
     * Меняет пароль пользователя после проверки кода подтверждения.
     *
     * @param user Пользователь, которому нужно изменить пароль.
     */
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
     * Изменяет пароль пользователя.
     *
     * @param userId Идентификатор пользователя.
     * @param oldPw  Старый пароль.
     * @param newPw  Новый пароль.
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

    /**
     * Создает нового пользователя.
     *
     * @param user Пользователь для создания.
     */
    public void create(User user) {
        String password = user.getPassword();
        String digestPW = DigestUtils.md5Hex(password);
        user.setPassword(digestPW);
        if (user.getStatus() == null) user.setStatus(1);
        userRepository.save(user);
    }

    /**
     * Аутентификация пользователя.
     *
     * @param user Данные пользователя для аутентификации.
     * @return Аутентифицированный пользователь.
     */
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

    /**
     * Получает пользователя по его идентификатору.
     *
     * @param id Идентификатор пользователя.
     * @return Пользователь или null, если пользователь не найден.
     */
    public User get(Integer id) {

        return userRepository.findById(id).orElse(null);
    }

    /**
     * Получает мапу пользователей по списку идентификаторов.
     *
     * @param ids Список идентификаторов пользователей.
     * @return Карта пользователей.
     */
    public Map<Integer, User> getUserMapBy(Iterable<Integer> ids) {
        Map<Integer, User> userMap = new HashMap<>();
        if (ids == null) return userMap;
        final List<User> regUsers = userRepository.findAllById(ids);
        regUsers.forEach(user1 -> userMap.put(user1.getId(), user1));

        return userMap;
    }

    /**
     * Получает одного пользователя по заданным критериям.
     *
     * @param user Критерии для поиска.
     * @return Пользователь или null, если пользователь не найден.
     */
    public UserVO getOne(User user) {
        user.setStatus(1);
        Optional<User> one = userRepository.findOne(Example.of(user));
        if (!one.isPresent()) return null;
        UserVO userVO = one.get().toVO(UserVO.class);
        userVO.setPassword(null);
        return userVO;
    }

    /**
     * Получает одного пользователя по критериям от имени администратора.
     *
     * @param user Критерии для поиска.
     * @return Пользователь или null.
     */
    public User getOneByAdmin(User user) {
        user.setStatus(1);
        Optional<User> one = userRepository.findOne(Example.of(user));
        return one.orElse(null);
    }

    /**
     * Получает пользователя без пароля.
     *
     * @param id Идентификатор пользователя.
     * @return Пользователь без пароля или null.
     */
    public User getUserButRemovePW(
            Integer id) {

        User user = get(id);
        if (user != null)
            user.setPassword(null);
        return user;
    }

    /**
     * Добавляет комментарий к пользователю.
     *
     * @param userId Идентификатор пользователя.
     * @param remark Комментарий.
     */
    public void addRemark(Integer userId, String remark) {
        User user = new User();
        user.setId(userId);
        user.setRemark(remark);
        userRepository.save(user);
    }

    /**
     * Кодирует пароль пользователя.
     *
     * @param pw Пароль для кодирования.
     * @return Закодированный пароль.
     */
    public String encodePassword(String pw) {
        return DigestUtils.md5Hex(pw);
    }
}
