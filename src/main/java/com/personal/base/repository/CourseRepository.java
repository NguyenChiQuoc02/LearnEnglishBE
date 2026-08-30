package com.personal.base.repository;

import com.personal.base.models.Course;
import com.personal.base.models.CourseType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
  List<Course> findByPublishedTrue();

  List<Course> findByPublishedTrueAndCourseType(CourseType courseType);

  List<Course> findByTeacherId(Long teacherId);
}
