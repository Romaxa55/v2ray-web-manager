package com.jhl.admin.service;

import com.jhl.admin.model.Message;
import com.jhl.admin.model.MessageReceiver;
import com.jhl.admin.model.User;
import com.jhl.admin.repository.MessageReceiverRepository;
import com.jhl.admin.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.transaction.Transactional;
import java.util.List;

@Service
public class MessageService {

    @Autowired
    MessageRepository messageRepository;

    @Autowired
    MessageReceiverRepository messageReceiverRepository;


    /**
     * @param message объект сообщения
     * @param userIds получатель сообщений
     */
    @Transactional
    public void addOrUpdate(Message message, List<Integer> userIds) {
        Assert.notNull(message, "Message is null");
        Assert.notNull(userIds, "userIds is null");
        if (message.getBatchNo() == null) message.setBatchNo(System.currentTimeMillis());
        final Message message2 = messageRepository.save(message);

        userIds.forEach(userId -> {
            User user = new User();
            user.setId(userId);
            MessageReceiver messageReceiver = MessageReceiver.builder().message(message2).received(false).user(user).build();
            messageReceiverRepository.save(messageReceiver);
        });
    }

    /**
     * Удаленное сообщение
     *
     * @param messageId
     */
    public void deleteMessage(Integer messageId) {
        messageRepository.deleteById(messageId);

    }

    public void deleteByBatchNo(Long batchNo) {
        for (Message message1 : messageRepository.findAll(Example.of(Message.builder().batchNo(batchNo).build()))) {
            messageRepository.deleteById(message1.getId());
        }
    }


    public Page<MessageReceiver> listByReceiver(Integer userId, Pageable pageable) {
        Assert.notNull(userId);
        User user = new User();
        user.setId(userId);
        Page<MessageReceiver> all = messageReceiverRepository.findAll(Example.of(MessageReceiver.builder().user(user).build()), pageable);
        return all;
    }
}
