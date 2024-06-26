package com.yyi.projectStudy.service;

import com.yyi.projectStudy.dto.JobDTO;
import com.yyi.projectStudy.dto.UserDTO;
import com.yyi.projectStudy.dto.UserJobDTO;
import com.yyi.projectStudy.entity.JobEntity;
import com.yyi.projectStudy.entity.UserEntity;
import com.yyi.projectStudy.entity.UserImageFileEntity;
import com.yyi.projectStudy.entity.UserJobEntity;
import com.yyi.projectStudy.repository.JobRepository;
import com.yyi.projectStudy.repository.UserImageFileRepository;
import com.yyi.projectStudy.repository.UserJobRepository;
import com.yyi.projectStudy.repository.UserRepository;
import com.yyi.projectStudy.util.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserImageFileRepository userImageFileRepository;
    private final JobRepository jobRepository;
    private final UserJobRepository userJobRepository;

    /* 회원가입 프로세스 */
    public void save(UserDTO userDTO, UserJobDTO userJobDTO) throws IOException {
        if (userDTO.getProfileImageFile() == null || userDTO.getProfileImageFile().isEmpty()) {
            // 비밀번호 암호화
            String password = userDTO.getPassword();
            userDTO.setPassword(PasswordEncoder.encoding(password));

            Long savedUserId = userRepository.save(UserEntity.toUserEntity(userDTO)).getId();

            // 직군 데이터 추가
            UserEntity userEntity = userRepository.findById(savedUserId).get();
            JobEntity jobEntity = jobRepository.findById(userJobDTO.getJobId()).get();
            UserJobEntity userJobEntity = UserJobEntity.toUserJobEntity(userEntity, jobEntity);
            userJobRepository.save(userJobEntity);

        } else {
            // 이미지 파일이 존재할 경우
            // 1. DTO에 담긴 이미지 파일 꺼내기
            MultipartFile profileImageFile = userDTO.getProfileImageFile();

            // 2. 파일의 이름 가져옴 (실제 사용자가 올린 파일 이름)
            String originalFilename = profileImageFile.getOriginalFilename();

            // 3. 서버 저장용 이름으로 수정 : 내 사진.jpg --> 8423424252525_내사진.jpg (currentTimeMills() -> 이거 대신 UUID도 사용가능)
            String storedFileName = System.currentTimeMillis() + "_" + originalFilename;

            // 4. 저장 경로 설정 (해당 폴더는 미리 만들어진 상태여야 한다.)
            String savePath = "C:/toyProject_img/" + storedFileName;

            // 5. 해당 경로에 파일 저장
            profileImageFile.transferTo(new File(savePath));

            // 비밀번호 암호화
            String password = userDTO.getPassword();
            userDTO.setPassword(PasswordEncoder.encoding(password));

            // 6. user_table에 해당 데이터 save 처리
            UserEntity userEntity = UserEntity.toSaveFileEntity(userDTO);
            // id 값을 얻어오는 이유: 자식 테이블 입장에서 부모가 어떤 id(pk)인지 필요해서
            Long savedUserId = userRepository.save(userEntity).getId();
            UserEntity user = userRepository.findById(savedUserId).get();

            // 직군 데이터 추가
            UserEntity newUserEntity = userRepository.findById(savedUserId).get();
            JobEntity jobEntity = jobRepository.findById(userJobDTO.getJobId()).get();
            UserJobEntity userJobEntity = UserJobEntity.toUserJobEntity(newUserEntity, jobEntity);
            userJobRepository.save(userJobEntity);

            UserImageFileEntity userImageFileEntity
                    = UserImageFileEntity.toUserImageFileEntity(user, originalFilename, storedFileName);

            userImageFileRepository.save(userImageFileEntity);
        }
    }

    /* 로그인 프로세스 */
    @Transactional
    public UserDTO login(UserDTO userDTO) {
        // 암호화된 비밀번호와 비교
        userDTO.setPassword(PasswordEncoder.encoding(userDTO.getPassword()));
        Optional<UserEntity> optionalUserEntity = userRepository.findByEmail(userDTO.getEmail());
        if (optionalUserEntity.isPresent()) {
            UserEntity userEntity = optionalUserEntity.get();
            if (userEntity.getPassword().equals(userDTO.getPassword())) {
                return UserDTO.toUserDTO(userEntity);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }


    /* 소셜 로그인 프로세스 */
    @Transactional
    public UserDTO socialLogin(UserDTO userDTO) {
        Optional<UserEntity> optionalUserEntity = userRepository.findByEmail(userDTO.getEmail());
        if (optionalUserEntity.isPresent()) {
            UserEntity userEntity = optionalUserEntity.get();
             return UserDTO.toUserDTO(userEntity);
        } else {
            return null;
        }
    }


    /* 마이페이지 - 회원정보 조회 */
    @Transactional
    public UserDTO findById(Long id) {
        Optional<UserEntity> optionalUserEntity = userRepository.findById(id);
        if (optionalUserEntity.isPresent()) {
            return UserDTO.toUserDTO(optionalUserEntity.get());
        } else {
            return null;
        }
    }

    /* 모든 직군 조회 */
    @Transactional
    public List<JobDTO> findAllJobs() {
        List<JobEntity> jobEntityList = jobRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        List<JobDTO> jobDTOList = new ArrayList<>();
        for (JobEntity jobEntity : jobEntityList) {
            jobDTOList.add(JobDTO.toJobDTO(jobEntity));
        }
        return jobDTOList;
    }

    /* 회원의 직군 조회 */
    public JobDTO findJob(Long id) {
        UserEntity userEntity = userRepository.findById(id).get();
        UserJobEntity userJobEntity = userJobRepository.findByUserEntity(userEntity).get();

        JobEntity jobEntity = jobRepository.findById(userJobEntity.getJobEntity().getId()).get();
        return JobDTO.toJobDTO(jobEntity);
    }


    /*  회원정보 수정 */
    @Transactional
    public void update(UserDTO userDTO, UserJobDTO userJobDTO) {
        userRepository.updateUser(userDTO.getNickname(), userDTO.getEmail(), userDTO.getId());
        userJobRepository.updateUserJob(userJobDTO.getJobId(), userDTO.getId());
    }

    /* 이메일 중복 체크 */
    public boolean existsUserId(String email) {
        int count = userRepository.countByEmail(email);
        if (count > 0) {
            return true;
        } else {
            return false;
        }
    }

    /* 닉네임 중복 체크 */
    public boolean existsNickname(String nickname) {
        int count = userRepository.countByNickname(nickname);
        if (count > 0) {
            return true;
        } else {
            return false;
        }
    }


    /* 회원 이미지 수정 */
    @Transactional
    public void updateUserImg(UserDTO userDTO) throws IOException {
        UserEntity userEntity = userRepository.findById(userDTO.getId()).get();
        if (userDTO.getProfileImageFile() == null) {
            System.out.println("새로 들어온 이미지가 없어요");
            // 1. 기존 이미지가 존재한다면 삭제
            Optional<UserImageFileEntity> optionalUserImageFileEntity
                    = userImageFileRepository.findByUserEntity(userEntity);
            if (optionalUserImageFileEntity.isPresent()) {
                System.out.println("기존에 이미지가 잇씁니다.");
                UserImageFileEntity userImageFileEntity = optionalUserImageFileEntity.get();
                userImageFileRepository.deleteById(userImageFileEntity.getId());
            }
            // 2. 회원 이미지 존재 여부 수정
            userRepository.updateUserFileAttached(0, userDTO.getId());
        } else {
            // 1. DTO에 담긴 이미지 파일 꺼내기
            MultipartFile profileImageFile = userDTO.getProfileImageFile();

            // 2. 파일의 이름 가져옴 (실제 사용자가 올린 파일 이름)
            String originalFilename = profileImageFile.getOriginalFilename();

            // 3. 서버 저장용 이름으로 수정 : 내 사진.jpg --> 8423424252525_내사진.jpg (currentTimeMills() -> 이거 대신 UUID도 사용가능)
            String storedFileName = System.currentTimeMillis() + "_" + originalFilename;

            // 4. 저장 경로 설정 (해당 폴더는 미리 만들어진 상태여야 한다.)
            String savePath = "C:/toyProject_img/" + storedFileName;

            // 5. 해당 경로에 파일 저장
            profileImageFile.transferTo(new File(savePath));

            // 6. 회원 이미지 수정 or 추가
            Optional<UserImageFileEntity> optionalUserImageFileEntity
                    = userImageFileRepository.findByUserEntity(userEntity);
            if (optionalUserImageFileEntity.isPresent()) {
                /* 기존에 이미지가 존재했다면 업데이트 */
                userImageFileRepository.updateUserImg(originalFilename, storedFileName, userDTO.getId());
            } else {
                /* 기존에 이미지가 없었다면 이미지 추가 */
                UserImageFileEntity userImageFileEntity
                    = UserImageFileEntity.toUserImageFileEntity(userEntity, originalFilename, storedFileName);
                userImageFileRepository.save(userImageFileEntity);
                /* 회원 이미지 존재 여부 수정 */
                userRepository.updateUserFileAttached(1, userDTO.getId());
            }
        }
    }

    @Transactional
    public UserDTO findByEmail(String email) {
        Optional<UserEntity> optionalUserEntity = userRepository.findByEmail(email);
        if (optionalUserEntity.isPresent()) {
            return UserDTO.toUserDTO(optionalUserEntity.get());
        } else {
            return null;
        }
    }

}
