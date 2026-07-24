package com.lion.benefito.domain.todo.repository;

import com.lion.benefito.domain.todo.entity.Todo;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface TodoRepository extends JpaRepository<Todo, Long> {

    @Query("SELECT t FROM Todo t " +
           "JOIN FETCH t.categoryCode " +
           "WHERE t.user.id = :userId " +
           "ORDER BY t.isCompleted ASC, t.endDate ASC, t.createdAt DESC")
    List<Todo> findTodosByUserIdWithCategory(@Param("userId") Long userId);
}
