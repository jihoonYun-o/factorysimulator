package controller.repository;

import controller.entity.OhtLog;
import org.springframework.data.jpa.repository.JpaRepository;

// JpaRepository를 상속받으면, Insert/Select 등의 DB 쿼리를 스프링이 자동으로 만들어줍니다.
public interface OhtLogRepository extends JpaRepository<OhtLog, Long> {
}
