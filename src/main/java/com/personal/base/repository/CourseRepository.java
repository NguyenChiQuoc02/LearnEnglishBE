package com.personal.base.repository;

import com.personal.base.models.Course;
import com.personal.base.models.CourseType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
  List<Course> findByPublishedTrue();

  List<Course> findByPublishedTrueAndCourseType(CourseType courseType);

  List<Course> findByTeacherId(Long teacherId);

  // Backs the admin Courses list page's server-side pagination + search box.
  @Query("""
      select c from Course c
      where (:keyword is null
             or lower(c.title) like concat('%', :keyword, '%')
             or lower(c.teacher.username) like concat('%', :keyword, '%'))
      order by c.id asc
      """)
  Page<Course> search(@Param("keyword") String keyword, Pageable pageable);

  @Query("""
      select c from Course c
      where c.teacher.id = :teacherId
        and (:keyword is null
             or lower(c.title) like concat('%', :keyword, '%')
             or lower(c.teacher.username) like concat('%', :keyword, '%'))
      order by c.id asc
      """)
  Page<Course> searchByTeacher(@Param("teacherId") Long teacherId, @Param("keyword") String keyword, Pageable pageable);
}
