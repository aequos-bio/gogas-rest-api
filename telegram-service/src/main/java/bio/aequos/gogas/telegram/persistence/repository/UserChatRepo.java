package bio.aequos.gogas.telegram.persistence.repository;

import bio.aequos.gogas.telegram.persistence.model.UserChatEntity;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface UserChatRepo extends CrudRepository<UserChatEntity, String> {

    List<UserChatEntity> findByTenantIdAndUserId(String tenantId, String userId);

    List<UserChatEntity> findByTenantIdAndUserIdIn(String tenantId, List<String> userId);
}
