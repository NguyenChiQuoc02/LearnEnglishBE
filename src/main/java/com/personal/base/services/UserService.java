package com.personal.base.services;

import com.personal.base.dto.common.PageResponse;
import com.personal.base.dto.user.BulkDeleteResponse;
import com.personal.base.dto.user.BulkDeleteResult;
import com.personal.base.dto.user.ChangePasswordRequest;
import com.personal.base.dto.user.ProfileUpdateRequest;
import com.personal.base.dto.user.UserImportResponse;
import com.personal.base.dto.user.UserImportRowResult;
import com.personal.base.dto.user.UserRequest;
import com.personal.base.dto.user.UserResponse;
import com.personal.base.models.type.ERole;
import com.personal.base.models.Province;
import com.personal.base.models.Role;
import com.personal.base.models.StagingUserImport;
import com.personal.base.models.User;
import com.personal.base.models.Ward;
import com.personal.base.repository.ProvinceRepository;
import com.personal.base.repository.RoleRepository;
import com.personal.base.repository.StagingUserImportRepository;
import com.personal.base.repository.UserRepository;
import com.personal.base.repository.WardRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.data.repository.CrudRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {

  public static final String DEFAULT_PASSWORD = "123456";

  private static final String[] EXPECTED_HEADERS = {
          "username", "email", "password", "phonenumber", "dateofbirth", "address"
  };
  private static final String IMPORT_TEMPLATE_PATH = "templates/template_user.xlsx";
  private static final int IMPORT_BATCH_SIZE = 1000;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private RoleRepository roleRepository;

  @Autowired
  private ProvinceRepository provinceRepository;

  @Autowired
  private WardRepository wardRepository;

  @Autowired
  private StagingUserImportRepository stagingUserImportRepository;

  @Autowired
  private PasswordEncoder encoder;

  @Transactional(readOnly = true)
  public PageResponse<UserResponse> listUsers(int page, int size, String keyword) {
    String normalizedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim().toLowerCase();
    Page<User> result = userRepository.search(normalizedKeyword, PageRequest.of(page, size));
    return PageResponse.of(result, UserResponse::from);
  }

  public UserResponse getUser(Long id) {
    return UserResponse.from(getUserEntity(id));
  }

  @Cacheable("users")
  @Transactional(readOnly = true)
  public List<UserResponse> listAllUsers() {
    return userRepository.findAll(Sort.by("id")).stream()
            .map(UserResponse::from)
            .collect(Collectors.toList());
  }

  @Transactional
  public UserResponse createUser(UserRequest request) {
    if (userRepository.existsByUsername(request.getUsername())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username is already taken");
    }
    if (userRepository.existsByEmail(request.getEmail())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is already in use");
    }

    User user = new User(request.getUsername(), request.getEmail(), encoder.encode(DEFAULT_PASSWORD));
    user.setPhoneNumber(request.getPhoneNumber());
    user.setDateOfBirth(request.getDateOfBirth());
    user.setAddress(request.getAddress());
    user.setAvatarUrl(request.getAvatarUrl());
    user.setRoles(resolveRoles(request.getRoles()));
    applyAddress(user, request.getProvinceCode(), request.getWardCode());

    return UserResponse.from(userRepository.save(user));
  }

  @Transactional
  public UserResponse updateUser(Long id, UserRequest request) {
    User user = getUserEntity(id);

    if (userRepository.existsByUsernameAndIdNot(request.getUsername(), id)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username is already taken");
    }
    if (userRepository.existsByEmailAndIdNot(request.getEmail(), id)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is already in use");
    }

    user.setUsername(request.getUsername());
    user.setEmail(request.getEmail());
    user.setPhoneNumber(request.getPhoneNumber());
    user.setDateOfBirth(request.getDateOfBirth());
    user.setAddress(request.getAddress());
    user.setAvatarUrl(request.getAvatarUrl());
    user.setRoles(resolveRoles(request.getRoles()));
    applyAddress(user, request.getProvinceCode(), request.getWardCode());

    return UserResponse.from(userRepository.save(user));
  }

  @Transactional
  public UserResponse updateMyProfile(Long id, ProfileUpdateRequest request) {
    User user = getUserEntity(id);

    user.setPhoneNumber(request.getPhoneNumber());
    user.setDateOfBirth(request.getDateOfBirth());
    user.setAddress(request.getAddress());
    user.setAvatarUrl(request.getAvatarUrl());
    applyAddress(user, request.getProvinceCode(), request.getWardCode());

    return UserResponse.from(userRepository.save(user));
  }

  @Transactional
  public void deleteUser(Long id, Long currentUserId) {
    if (id.equals(currentUserId)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot delete your own account");
    }
    User user = getUserEntity(id);
    try {
      userRepository.delete(user);
      userRepository.flush();
    } catch (DataIntegrityViolationException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete a user that owns existing data");
    }
  }

  // Deliberately not @Transactional: each id is deleted (and committed) independently
  // via its own repository call, so one FK-conflict/not-found row doesn't roll back
  // or block the deletion of the other selected users.
  public BulkDeleteResponse bulkDeleteUsers(List<Long> ids, Long currentUserId) {
    List<BulkDeleteResult> results = new ArrayList<>();
    for (Long id : ids) {
      results.add(deleteOneForBulk(id, currentUserId));
    }
    return BulkDeleteResponse.of(results);
  }

  private BulkDeleteResult deleteOneForBulk(Long id, Long currentUserId) {
    if (id.equals(currentUserId)) {
      return new BulkDeleteResult(id, null, false, "You cannot delete your own account");
    }

    Optional<User> maybeUser = userRepository.findById(id);
    if (maybeUser.isEmpty()) {
      return new BulkDeleteResult(id, null, false, "User not found");
    }

    User user = maybeUser.get();
    try {
      userRepository.delete(user);
      return new BulkDeleteResult(id, user.getUsername(), true, null);
    } catch (DataIntegrityViolationException e) {
      return new BulkDeleteResult(id, user.getUsername(), false, "Cannot delete a user that owns existing data");
    }
  }

  @Transactional
  public void changePassword(Long userId, ChangePasswordRequest request) {
    User user = getUserEntity(userId);
    if (!encoder.matches(request.getOldPassword(), user.getPassword())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Old password is incorrect");
    }
    user.setPassword(encoder.encode(request.getNewPassword()));
    userRepository.save(user);
  }

  private void applyAddress(User user, String provinceCode, String wardCode) {
    Province province = resolveProvince(provinceCode);
    Ward ward = resolveWard(wardCode, province);
    user.setProvince(province);
    user.setWard(ward);
  }

  private Province resolveProvince(String provinceCode) {
    if (provinceCode == null || provinceCode.isBlank()) {
      return null;
    }
    return provinceRepository.findById(provinceCode)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid province code"));
  }

  private Ward resolveWard(String wardCode, Province province) {
    if (wardCode == null || wardCode.isBlank()) {
      return null;
    }
    Ward ward = wardRepository.findById(wardCode)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid ward code"));
    boolean belongsToProvince = province != null && ward.getProvince() != null
            && ward.getProvince().getCode().equals(province.getCode());
    if (!belongsToProvince) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ward does not belong to the selected province");
    }
    return ward;
  }

  private User getUserEntity(Long id) {
    return userRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
  }

  private Set<Role> resolveRoles(Set<String> roleNames) {
    Set<Role> roles = new HashSet<>();

    if (roleNames == null || roleNames.isEmpty()) {
      roles.add(findRole(ERole.ROLE_USER));
      return roles;
    }

    for (String roleName : roleNames) {
      roles.add(findRole(parseRole(roleName)));
    }
    return roles;
  }

  private ERole parseRole(String roleName) {
    try {
      return ERole.valueOf(roleName.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role: " + roleName);
    }
  }

  private Role findRole(ERole roleName) {
    return roleRepository.findByName(roleName)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Role is not found"));
  }

  public Resource getImportTemplate() {
    ClassPathResource resource = new ClassPathResource(IMPORT_TEMPLATE_PATH);
    if (!resource.exists()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Import template not found");
    }
    return resource;
  }

  @Transactional
  public UserImportResponse previewImportUsers(MultipartFile file) {
    return runImportPipeline(file, false);
  }

  @Transactional
  public UserImportResponse commitImportUsers(MultipartFile file) {
    return runImportPipeline(file, true);
  }

  private record RawImportRow(int rowNumber, String username, String email, String password,
                               String phoneNumber, String dateOfBirthRaw, String address, String role) {
  }

  /**
   * Pipeline: Excel -&gt; staging_users_import -&gt; validate -&gt; transform -&gt; users.
   * No step calls a repository once per row — every DB read/write below is either
   * a single bulk call, or a call made once per chunk of {@link #IMPORT_BATCH_SIZE}
   * rows; the per-row for-loops only ever touch in-memory Java objects.
   */
  private UserImportResponse runImportPipeline(MultipartFile file, boolean persist) {
    List<RawImportRow> rawRows = parseImportFile(file);

    String batchId = UUID.randomUUID().toString();
    try {
      // Excel -> staging_users_import (chunked writes, not one insert per row)
      List<StagingUserImport> staged = stageRows(batchId, rawRows);

      // validate (bulk prefetch once, then pure in-memory checks, then chunked write-back)
      validateStagedRows(staged);
      persistInChunks(stagingUserImportRepository, staged);

      List<UserImportRowResult> results = staged.stream()
              .map(row -> new UserImportRowResult(
                      row.getRowNumber(),
                      row.getUsername(),
                      row.getEmail(),
                      row.getPhoneNumber(),
                      row.getDateOfBirthRaw(),
                      row.getAddress(),
                      row.getRole(),
                      row.getErrorMessage() == null,
                      row.getErrorMessage()))
              .collect(Collectors.toList());

      // transform + load: staging_users_import -> users (chunked writes)
      if (persist) {
        loadValidUsers(staged);
      }

      return UserImportResponse.of(results);
    } finally {
      stagingUserImportRepository.deleteByBatchId(batchId);
    }
  }

  private List<RawImportRow> parseImportFile(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn file để import");
    }

    try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
      Sheet usersSheet = workbook.getSheet("Users");
      Sheet sheet = usersSheet != null ? usersSheet : (workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null);
      if (sheet == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File Excel không có dữ liệu");
      }
      return readRawRows(sheet);
    } catch (IOException | RuntimeException e) {
      if (e instanceof ResponseStatusException) {
        throw (ResponseStatusException) e;
      }
      throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST,
              "File không hợp lệ hoặc không đúng định dạng Excel (.xlsx). Vui lòng dùng file mẫu.");
    }
  }

  private List<RawImportRow> readRawRows(Sheet sheet) {
    DataFormatter formatter = new DataFormatter();
    Row headerRow = sheet.getRow(0);
    if (headerRow == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File Excel thiếu hàng tiêu đề (header)");
    }

    for (int i = 0; i < EXPECTED_HEADERS.length; i++) {
      String actual = readCell(headerRow, i, formatter);
      String normalized = actual == null ? "" : actual.trim().toLowerCase().replace(" ", "");
      if (!EXPECTED_HEADERS[i].equals(normalized)) {
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Sai định dạng template. Cột bắt buộc theo đúng thứ tự: username, email, password, phoneNumber, "
                        + "dateOfBirth, address. Vui lòng tải lại file mẫu.");
      }
    }

    String roleHeader = readCell(headerRow, 6, formatter);
    boolean hasRoleColumn = roleHeader != null && "role".equalsIgnoreCase(roleHeader.trim());

    // Read every non-blank row once into memory (POI cell access only, no DB calls).
    List<RawImportRow> rawRows = new ArrayList<>();
    int lastRow = sheet.getLastRowNum();
    for (int r = 1; r <= lastRow; r++) {
      Row row = sheet.getRow(r);
      if (row == null) continue;

      String username = readCell(row, 0, formatter);
      String email = readCell(row, 1, formatter);
      String password = readCell(row, 2, formatter);
      String phoneNumber = readCell(row, 3, formatter);
      String dateOfBirthRaw = readCell(row, 4, formatter);
      String address = readCell(row, 5, formatter);
      String role = hasRoleColumn ? readCell(row, 6, formatter) : null;

      boolean rowBlank = isBlank(username) && isBlank(email) && isBlank(password)
              && isBlank(phoneNumber) && isBlank(dateOfBirthRaw) && isBlank(address) && isBlank(role);
      if (rowBlank) continue;

      rawRows.add(new RawImportRow(r + 1, username, email, password, phoneNumber, dateOfBirthRaw, address, role));
    }
    return rawRows;
  }

  /** Excel -> staging_users_import. Repository is called once per chunk of {@link #IMPORT_BATCH_SIZE}, never per row. */
  private List<StagingUserImport> stageRows(String batchId, List<RawImportRow> rawRows) {
    List<StagingUserImport> entities = rawRows.stream()
            .map(raw -> new StagingUserImport(null, batchId, raw.rowNumber(), raw.username(), raw.email(),
                    raw.password(), raw.phoneNumber(), raw.dateOfBirthRaw(), raw.address(), raw.role(), null))
            .collect(Collectors.toList());

    List<StagingUserImport> saved = new ArrayList<>(entities.size());
    for (List<StagingUserImport> chunk : partition(entities, IMPORT_BATCH_SIZE)) {
      saved.addAll(stagingUserImportRepository.saveAll(chunk));
    }
    return saved;
  }

  /**
   * validate step. Prefetches existing usernames/emails/phones with 3 bulk queries
   * (never per row), then only touches the in-memory {@code staged} list — no
   * repository calls inside this loop at all.
   */
  private void validateStagedRows(List<StagingUserImport> staged) {
    Set<String> candidateUsernames = staged.stream()
            .map(StagingUserImport::getUsername).filter(u -> !isBlank(u)).map(String::trim).collect(Collectors.toSet());
    Set<String> candidateEmails = staged.stream()
            .map(StagingUserImport::getEmail).filter(e -> !isBlank(e)).map(String::trim).collect(Collectors.toSet());
    Set<String> candidatePhones = staged.stream()
            .map(StagingUserImport::getPhoneNumber).filter(p -> !isBlank(p)).map(String::trim).collect(Collectors.toSet());

    Set<String> existingUsernames = candidateUsernames.isEmpty() ? Set.of()
            : userRepository.findByUsernameIn(candidateUsernames).stream().map(User::getUsername).collect(Collectors.toSet());
    Set<String> existingEmails = candidateEmails.isEmpty() ? Set.of()
            : userRepository.findByEmailIn(candidateEmails).stream().map(User::getEmail).collect(Collectors.toSet());
    Set<String> existingPhones = candidatePhones.isEmpty() ? Set.of()
            : userRepository.findByPhoneNumberIn(candidatePhones).stream().map(User::getPhoneNumber).collect(Collectors.toSet());

    Set<String> seenUsernames = new HashSet<>();
    Set<String> seenEmails = new HashSet<>();
    Set<String> seenPhones = new HashSet<>();

    for (StagingUserImport row : staged) {
      List<String> errors = validateRow(row, existingUsernames, existingEmails, existingPhones,
              seenUsernames, seenEmails, seenPhones);
      row.setErrorMessage(errors.isEmpty() ? null : String.join("; ", errors));
    }
  }

  private List<String> validateRow(StagingUserImport row, Set<String> existingUsernames, Set<String> existingEmails,
                                    Set<String> existingPhones, Set<String> seenUsernames, Set<String> seenEmails,
                                    Set<String> seenPhones) {
    List<String> errors = new ArrayList<>();
    String username = row.getUsername();
    String email = row.getEmail();
    String phoneNumber = row.getPhoneNumber();
    String usernameKey = isBlank(username) ? null : username.trim().toLowerCase();
    String emailKey = isBlank(email) ? null : email.trim().toLowerCase();
    String phoneKey = isBlank(phoneNumber) ? null : phoneNumber.trim();

    if (isBlank(username)) {
      errors.add("Thiếu username");
    } else if (seenUsernames.contains(usernameKey)) {
      errors.add("Username bị trùng trong file");
    } else if (existingUsernames.contains(username.trim())) {
      errors.add("Username đã tồn tại");
    }

    if (isBlank(email)) {
      errors.add("Thiếu email");
    } else if (!email.trim().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
      errors.add("Email không đúng định dạng");
    } else if (seenEmails.contains(emailKey)) {
      errors.add("Email bị trùng trong file");
    } else if (existingEmails.contains(email.trim())) {
      errors.add("Email đã tồn tại");
    }

    if (!isBlank(phoneNumber)) {
      if (seenPhones.contains(phoneKey)) {
        errors.add("Số điện thoại bị trùng trong file");
      } else if (existingPhones.contains(phoneKey)) {
        errors.add("Số điện thoại đã được sử dụng");
      }
    }

    if (!isBlank(row.getDateOfBirthRaw())) {
      try {
        LocalDate.parse(row.getDateOfBirthRaw().trim(), DateTimeFormatter.ISO_LOCAL_DATE);
      } catch (DateTimeParseException e) {
        errors.add("Ngày sinh sai định dạng (yêu cầu yyyy-MM-dd)");
      }
    }

    if (!isBlank(row.getRole()) && parseImportRole(row.getRole()) == null) {
      errors.add("Vai trò không hợp lệ (user/teacher/admin)");
    }

    if (!isBlank(row.getPassword()) && row.getPassword().trim().length() < 6) {
      errors.add("Mật khẩu phải có ít nhất 6 ký tự");
    }

    if (errors.isEmpty()) {
      if (usernameKey != null) seenUsernames.add(usernameKey);
      if (emailKey != null) seenEmails.add(emailKey);
      if (phoneKey != null) seenPhones.add(phoneKey);
    }

    return errors;
  }

  /**
   * transform + load: staging_users_import -> users. Only valid rows (errorMessage
   * == null) are turned into User entities; Role lookup is prefetched once (not per
   * row) and writes go out in chunks of {@link #IMPORT_BATCH_SIZE}, never per row.
   */
  private void loadValidUsers(List<StagingUserImport> staged) {
    Map<ERole, Role> roleCache = prefetchRoles();

    List<User> toSave = new ArrayList<>();
    for (StagingUserImport row : staged) {
      if (row.getErrorMessage() != null) continue;

      String resolvedPassword = isBlank(row.getPassword()) ? DEFAULT_PASSWORD : row.getPassword().trim();
      LocalDate dateOfBirth = isBlank(row.getDateOfBirthRaw()) ? null
              : LocalDate.parse(row.getDateOfBirthRaw().trim(), DateTimeFormatter.ISO_LOCAL_DATE);
      ERole resolvedRole = parseImportRole(row.getRole());

      Role roleEntity = roleCache.get(resolvedRole);
      if (roleEntity == null) {
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Role is not found");
      }

      User user = new User(row.getUsername().trim(), row.getEmail().trim(), encoder.encode(resolvedPassword));
      user.setPhoneNumber(isBlank(row.getPhoneNumber()) ? null : row.getPhoneNumber().trim());
      user.setDateOfBirth(dateOfBirth);
      user.setAddress(isBlank(row.getAddress()) ? null : row.getAddress().trim());
      Set<Role> roles = new HashSet<>();
      roles.add(roleEntity);
      user.setRoles(roles);
      toSave.add(user);
    }

    for (List<User> chunk : partition(toSave, IMPORT_BATCH_SIZE)) {
      userRepository.saveAll(chunk);
    }
  }

  /** Returns null (not a validation exception) so the caller can decide preview vs. hard failure. */
  private ERole parseImportRole(String raw) {
    if (isBlank(raw)) return ERole.ROLE_USER;
    String normalized = raw.trim().toUpperCase();
    if (!normalized.startsWith("ROLE_")) {
      normalized = "ROLE_" + normalized;
    }
    try {
      return ERole.valueOf(normalized);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private Map<ERole, Role> prefetchRoles() {
    Map<ERole, Role> cache = new EnumMap<>(ERole.class);
    for (Role role : roleRepository.findAll()) {
      cache.put(role.getName(), role);
    }
    return cache;
  }

  /** Splits a list into chunks of at most {@code size}, used to keep every batch write <= IMPORT_BATCH_SIZE. */
  private static <T> List<List<T>> partition(List<T> list, int size) {
    List<List<T>> chunks = new ArrayList<>();
    for (int i = 0; i < list.size(); i += size) {
      chunks.add(list.subList(i, Math.min(i + size, list.size())));
    }
    return chunks;
  }

  private static <T> void persistInChunks(CrudRepository<T, ?> repository, List<T> entities) {
    for (List<T> chunk : partition(entities, IMPORT_BATCH_SIZE)) {
      repository.saveAll(chunk);
    }
  }

  private boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }

  private String readCell(Row row, int col, DataFormatter formatter) {
    Cell cell = row.getCell(col);
    if (cell == null || cell.getCellType() == CellType.BLANK) return null;
    if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
      return cell.getLocalDateTimeCellValue().toLocalDate().toString();
    }
    String value = formatter.formatCellValue(cell).trim();
    return value.isEmpty() ? null : value;
  }
}
