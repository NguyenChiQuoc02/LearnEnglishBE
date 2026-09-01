package com.personal.base.services;

import com.personal.base.dto.common.PageResponse;
import com.personal.base.dto.course.CourseRequest;
import com.personal.base.dto.course.CourseResponse;
import com.personal.base.dto.course.CourseStudentResponse;
import com.personal.base.dto.course.VocabularyItemRequest;
import com.personal.base.dto.course.VocabularyItemResponse;
import com.personal.base.dto.session.LearningSessionSummaryResponse;
import com.personal.base.models.Course;
import com.personal.base.models.CourseType;
import com.personal.base.models.User;
import com.personal.base.models.VocabularyItem;
import com.personal.base.repository.CourseRepository;
import com.personal.base.repository.EnrollmentRepository;
import com.personal.base.repository.LearningSessionRepository;
import com.personal.base.repository.UserRepository;
import com.personal.base.repository.VocabularyItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CourseService {

  @Autowired
  private CourseRepository courseRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private VocabularyItemRepository vocabularyItemRepository;

  @Autowired
  private EnrollmentRepository enrollmentRepository;

  @Autowired
  private LearningSessionRepository learningSessionRepository;

  @Transactional
  public CourseResponse createCourse(CourseRequest request, Long adminId) {
    User teacher = userRepository.findById(request.getTeacherId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Teacher not found"));
    User admin = userRepository.findById(adminId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin not found"));

    Course course = new Course();
    course.setTitle(request.getTitle());
    course.setDescription(request.getDescription());
    course.setCourseType(request.getCourseType());
    course.setLevel(request.getLevel());
    course.setThumbnailUrl(request.getThumbnailUrl());
    course.setTeacher(teacher);
    course.setCreatedBy(admin);
    course.setWordsPerSession(request.getWordsPerSession() != null ? request.getWordsPerSession() : 10);
    course.setPointsPerCorrect(request.getPointsPerCorrect() != null ? request.getPointsPerCorrect() : 10);
    course.setPointsPerWrong(request.getPointsPerWrong() != null ? request.getPointsPerWrong() : -2);
    course.setTotalWords(0);
    course.setPublished(request.getPublished() != null ? request.getPublished() : false);
    course.setPrice(request.getPrice() != null ? request.getPrice() : java.math.BigDecimal.ZERO);

    return CourseResponse.from(courseRepository.save(course));
  }

  @Transactional
  public CourseResponse updateCourse(Long courseId, CourseRequest request, UserDetailsImpl currentUser) {
    Course course = getCourseEntity(courseId);
    assertCanManage(course, currentUser);

    course.setTitle(request.getTitle());
    course.setDescription(request.getDescription());
    course.setCourseType(request.getCourseType());
    course.setLevel(request.getLevel());
    course.setThumbnailUrl(request.getThumbnailUrl());

    if (request.getTeacherId() != null && !request.getTeacherId().equals(course.getTeacher().getId())) {
      User teacher = userRepository.findById(request.getTeacherId())
              .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Teacher not found"));
      course.setTeacher(teacher);
    }
    if (request.getWordsPerSession() != null) course.setWordsPerSession(request.getWordsPerSession());
    if (request.getPointsPerCorrect() != null) course.setPointsPerCorrect(request.getPointsPerCorrect());
    if (request.getPointsPerWrong() != null) course.setPointsPerWrong(request.getPointsPerWrong());
    if (request.getPublished() != null) course.setPublished(request.getPublished());
    course.setPrice(request.getPrice() != null ? request.getPrice() : java.math.BigDecimal.ZERO);

    return CourseResponse.from(courseRepository.save(course));
  }

  public List<CourseResponse> listCourses(CourseType type) {
    List<Course> courses = type != null
            ? courseRepository.findByPublishedTrueAndCourseType(type)
            : courseRepository.findByPublishedTrue();
    return courses.stream().map(CourseResponse::from).collect(Collectors.toList());
  }

  public CourseResponse getCourse(Long id) {
    return CourseResponse.from(getCourseEntity(id));
  }

  @Transactional(readOnly = true)
  public PageResponse<CourseResponse> listManagedCourses(int page, int size, String keyword, UserDetailsImpl currentUser) {
    boolean isAdmin = currentUser.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

    String normalizedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim().toLowerCase();
    PageRequest pageRequest = PageRequest.of(page, size);

    Page<Course> courses = isAdmin
            ? courseRepository.search(normalizedKeyword, pageRequest)
            : courseRepository.searchByTeacher(currentUser.getId(), normalizedKeyword, pageRequest);

    return PageResponse.of(courses, CourseResponse::from);
  }

  @Transactional
  public VocabularyItemResponse addVocabularyItem(Long courseId, VocabularyItemRequest request, UserDetailsImpl currentUser) {
    Course course = getCourseEntity(courseId);
    assertCanManage(course, currentUser);

    VocabularyItem item = new VocabularyItem();
    item.setCourse(course);
    item.setWord(request.getWord());
    item.setPhonetic(request.getPhonetic());
    item.setPartOfSpeech(request.getPartOfSpeech());
    item.setMeaning(request.getMeaning());
    item.setExampleSentence(request.getExampleSentence());
    item.setExampleTranslation(request.getExampleTranslation());
    item.setImageUrl(request.getImageUrl());
    item.setAudioUrl(request.getAudioUrl());
    item.setOrderIndex(request.getOrderIndex() != null ? request.getOrderIndex() : course.getTotalWords());

    VocabularyItem saved = vocabularyItemRepository.save(item);

    course.setTotalWords(course.getTotalWords() + 1);
    courseRepository.save(course);

    return VocabularyItemResponse.from(saved);
  }

  @Transactional
  public VocabularyItemResponse updateVocabularyItem(Long courseId, Long itemId, VocabularyItemRequest request,
                                                      UserDetailsImpl currentUser) {
    Course course = getCourseEntity(courseId);
    assertCanManage(course, currentUser);

    VocabularyItem item = vocabularyItemRepository.findById(itemId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vocabulary item not found"));
    if (!item.getCourse().getId().equals(courseId)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Word does not belong to this course");
    }

    item.setWord(request.getWord());
    item.setPhonetic(request.getPhonetic());
    item.setPartOfSpeech(request.getPartOfSpeech());
    item.setMeaning(request.getMeaning());
    item.setExampleSentence(request.getExampleSentence());
    item.setExampleTranslation(request.getExampleTranslation());
    item.setImageUrl(request.getImageUrl());
    item.setAudioUrl(request.getAudioUrl());
    if (request.getOrderIndex() != null) {
      item.setOrderIndex(request.getOrderIndex());
    }

    return VocabularyItemResponse.from(vocabularyItemRepository.save(item));
  }

  @Transactional
  public void deleteVocabularyItem(Long courseId, Long itemId, UserDetailsImpl currentUser) {
    Course course = getCourseEntity(courseId);
    assertCanManage(course, currentUser);

    VocabularyItem item = vocabularyItemRepository.findById(itemId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vocabulary item not found"));
    if (!item.getCourse().getId().equals(courseId)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Word does not belong to this course");
    }

    vocabularyItemRepository.delete(item);
    course.setTotalWords(Math.max(0, course.getTotalWords() - 1));
    courseRepository.save(course);
  }

  public List<CourseStudentResponse> listStudents(Long courseId, UserDetailsImpl currentUser) {
    Course course = getCourseEntity(courseId);
    assertCanManage(course, currentUser);

    return enrollmentRepository.findByCourseIdOrderByEnrolledAtDesc(courseId).stream()
            .map(CourseStudentResponse::from)
            .collect(Collectors.toList());
  }

  public List<LearningSessionSummaryResponse> listStudentSessions(Long courseId, Long userId, UserDetailsImpl currentUser) {
    Course course = getCourseEntity(courseId);
    assertCanManage(course, currentUser);

    if (!enrollmentRepository.findByUserIdAndCourseId(userId, courseId).isPresent()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student is not enrolled in this course");
    }

    return learningSessionRepository.findByUserIdAndCourseIdOrderByStartedAtDesc(userId, courseId).stream()
            .map(LearningSessionSummaryResponse::from)
            .collect(Collectors.toList());
  }

  public List<VocabularyItemResponse> listVocabularyItems(Long courseId, UserDetailsImpl currentUser) {
    Course course = getCourseEntity(courseId);
    assertCanManage(course, currentUser);

    return vocabularyItemRepository.findByCourseIdOrderByOrderIndexAsc(courseId).stream()
            .map(VocabularyItemResponse::from)
            .collect(Collectors.toList());
  }

  @Transactional
  public void deleteCourse(Long courseId) {
    Course course = getCourseEntity(courseId);
    if (enrollmentRepository.existsByCourseId(courseId)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete a course that already has enrolled students");
    }
    vocabularyItemRepository.deleteByCourseId(courseId);
    courseRepository.delete(course);
  }

  public Course getCourseEntity(Long id) {
    return courseRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
  }

  public void assertCanManage(Course course, UserDetailsImpl currentUser) {
    boolean isAdmin = currentUser.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    if (isAdmin) return;

    if (!course.getTeacher().getId().equals(currentUser.getId())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this course");
    }
  }

}
