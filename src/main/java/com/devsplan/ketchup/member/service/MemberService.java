package com.devsplan.ketchup.member.service;


import com.devsplan.ketchup.common.Criteria;
import com.devsplan.ketchup.member.dto.DepDTO;
import com.devsplan.ketchup.member.dto.MemberDTO;
import com.devsplan.ketchup.member.dto.PositionDTO;
import com.devsplan.ketchup.member.entity.Dep;
import com.devsplan.ketchup.member.entity.Member;
import com.devsplan.ketchup.member.entity.Position;
import com.devsplan.ketchup.member.repository.DepRepository;
import com.devsplan.ketchup.member.repository.MemberRepository;
import com.devsplan.ketchup.member.repository.PositionRepository;
import com.devsplan.ketchup.util.FileUtils;
import org.apache.tomcat.util.http.fileupload.FileUpload;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MemberService {

    private final ModelMapper modelMapper;

    private final MemberRepository memberRepository;

    private final DepRepository depRepository;

    private final PositionRepository positionRepository;

    private final PasswordEncoder passwordEncoder;


    @Autowired
    private JavaMailSender javaMailSender;



    @Value("${image.image-dir}")
    private String IMAGE_DIR;

    @Value("${image.image-url}")
    private String IMAGE_URL;



    public MemberService(MemberRepository memberRepository, ModelMapper modelMapper, PasswordEncoder passwordEncoder,DepRepository depRepository,PositionRepository positionRepository) {
        this.passwordEncoder = passwordEncoder;
        this.modelMapper = modelMapper;
        this.memberRepository = memberRepository;
        this.depRepository = depRepository;
        this.positionRepository = positionRepository;

    }


    @Transactional
    public void insertMember (MemberDTO newMemberDTO, MultipartFile memberImage){

        String imageName = UUID.randomUUID().toString().replace("-", "");
        String replaceFileName = null;

        try {
            replaceFileName = FileUtils.saveFile(IMAGE_DIR,imageName,memberImage);

            newMemberDTO.setImgUrl(replaceFileName);
            newMemberDTO.setMemberPW(passwordEncoder.encode(newMemberDTO.getMemberPW()));

            Member newMember = modelMapper.map(newMemberDTO,Member.class);

            memberRepository.save(newMember);


        }catch (Exception e){
            System.out.println("사원등록 실패");
            FileUtils.deleteFile(IMAGE_DIR,replaceFileName);
            e.printStackTrace();
            throw new RuntimeException(e);
        }


    }



    @Transactional
    public void insertPosition(PositionDTO newPositionDTO){

        System.out.println("DTO : "+newPositionDTO);

        Position newPosition = modelMapper.map(newPositionDTO, Position.class);

        System.out.println("ENTITY : "+ newPosition);

        positionRepository.save(newPosition);
    }

    @Transactional
    public void updatePosition(PositionDTO updatePositionDTO){

        System.out.println("수정할 직급 : "+updatePositionDTO);

        Position updatePosition = modelMapper.map(updatePositionDTO, Position.class);

        System.out.println("ENTITY : "+ updatePosition);

        positionRepository.save(updatePosition);
    }


    public Optional<MemberDTO> findMember(String memberNo){

        Optional<Member> member = memberRepository.findByMemberNo(memberNo);

        Optional<MemberDTO> memberDTO = Optional.ofNullable(modelMapper.map(member, MemberDTO.class));



        /*
         * 별도의 검증 로직 작성
         * */

        return memberDTO;
    }

    @Transactional
    public void insertDep(DepDTO newDepDTO) {
        Dep dep = modelMapper.map(newDepDTO, Dep.class);


        depRepository.save(dep);
    }


    public PositionDTO findPositionByPositionNo(int rPositionNo) {
        Position  rPosition =  positionRepository.findPositionByPositionNo(rPositionNo);
        PositionDTO rPositionDTO = modelMapper.map(rPosition, PositionDTO.class);
        return rPositionDTO;
    }

    public DepDTO findDepByDepNo(int rDepNo) {
        Dep rDep = depRepository.findDepByDepNo(rDepNo);
        DepDTO rDepDTO = modelMapper.map(rDep,DepDTO.class);

        return rDepDTO;
    }

    @Transactional
    public void resignMember(String memberNo, MemberDTO memberDTO) {
        memberDTO.setResignDateTime(LocalDateTime.now());
      Member foundMember = memberRepository.findByMemberNo(memberNo).orElseThrow(IllegalAccessError::new);

      foundMember = foundMember.status(memberDTO.getStatus()).build();

    }

    public Page<MemberDTO> findAllMembersWithPaging(Criteria cri,String search) {

        int index = cri.getPageNum() -1;
        int count = cri.getAmount();

        Pageable paging = PageRequest.of(index, count, Sort.by("memberNo").descending());

        Page<Member> result = memberRepository.findByStatusAndMemberNameContainingIgnoreCase("재직중",search,paging);

        Page<MemberDTO> memberList = result.map(member -> modelMapper.map(member, MemberDTO.class));



        return memberList;
    }

    public String sendVerifyEmail(String memberNo) {


        SimpleMailMessage simpleMessage = new SimpleMailMessage();

       Member findMember=  memberRepository.findByMemberNo(memberNo).get();

       String email = findMember.getPrivateEmail();


       String authCode = createAuthCode();


        simpleMessage.setTo(email);


        simpleMessage.setSubject("KETCHUP 인증번호 입니다.");


        simpleMessage.setText("인증번호는 " + authCode + " 입니다.");

        System.out.println("콘솔확인 이메일 인증번호  " + authCode);


        javaMailSender.send(simpleMessage);

        return authCode;
    }


    private String createAuthCode() {

        return String.valueOf((int)(Math.random() * 1000000));
    }


    public List<MemberDTO> findAllMembersWithNoPaging() {
        List<Member> members = memberRepository.findByStatus("재직중"); // 모든 회원 조회


        List<MemberDTO> memberList = members.stream()
                .map(member -> modelMapper.map(member, MemberDTO.class))
                .collect(Collectors.toList());

        return memberList;

    }

    public List<DepDTO> findAllDepsWithNoPaging() {
        List<Dep> deps = depRepository.findByStatus('y');

        List<DepDTO> depList = deps.stream()
                .map(dep -> modelMapper.map(dep, DepDTO.class))
                .collect(Collectors.toList());

        return depList;

    }

    public List<PositionDTO> findAllPositionWithNoPaging() {

        List<Position> positions = positionRepository.findByPositionStatus('y');

        List<PositionDTO> positionList = positions.stream()
                .map(position -> modelMapper.map(position,PositionDTO.class))
                .collect(Collectors.toList());

        return positionList;
    }

    public String updateMemberWithImage(MemberDTO updateMemberDTO, MultipartFile updateMemberImage) {
        System.out.println("[ProductService] updateProduct Start ===================================");
        System.out.println("updateMemberDTO : " + updateMemberDTO);
        Member member = memberRepository.findByMemberNo(updateMemberDTO.getMemberNo()).get();

        String replaceFileName = null;
        int result = 0;

        try {

            /* update 할 엔티티 조회 */

            String oriImage = member.getImgUrl();
            System.out.println("[updateMember] oriImage : " + oriImage);
            DepDTO updateDepDTO = findDepByDepNo(updateMemberDTO.getDepartment().getDepNo());
            Dep updateDep =modelMapper.map(updateDepDTO,Dep.class);
            PositionDTO updatePositionDTO = findPositionByPositionNo(updateMemberDTO.getPosition().getPositionNo());
            Position updatePosition =modelMapper.map(updatePositionDTO,Position.class);

            /* update를 위한 엔티티 값 수정 */


                member = member.memberName(updateMemberDTO.getMemberName()).build();
                member = member.phone(updateMemberDTO.getPhone()).build();
                member = member.birthDate(updateMemberDTO.getBirthDate()).build();
                member = member.address(updateMemberDTO.getAddress()).build();
                member = member.privateEmail(updateMemberDTO.getPrivateEmail()).build();
                member = member.companyEmail(updateMemberDTO.getCompanyEmail()).build();
                member = member.department(updateDep).build();
                member = member.position(updatePosition).build();
                member = member.account(updateMemberDTO.getAccount()).build();


//

            if(updateMemberImage != null){
                String imageName = UUID.randomUUID().toString().replace("-", "");
                replaceFileName = FileUtils.saveFile(IMAGE_DIR, imageName, updateMemberImage);

                member = member.imgUrl(replaceFileName).build();	// 새로운 파일 이름으로 update


                boolean isDelete = FileUtils.deleteFile(IMAGE_DIR, oriImage);
                System.out.println("[update] isDelete : " + isDelete);

            } else {

                /* 이미지 변경 없을 시 */
                member = member.imgUrl(oriImage).build();
            }

            result = 1;

        } catch (IOException e) {
            System.out.println("[updateMember] Exception!!");
            FileUtils.deleteFile(IMAGE_DIR, replaceFileName);
            throw new RuntimeException(e);
        }

        System.out.println("[MemberService] updateMember End ===================================" + member);
        memberRepository.save(member);
        return  "사원 업데이트 성공" ;
    }


    public Object updateMember(MemberDTO updateMemberDTO) {
        System.out.println("[ProductService] updateProduct Start ===================================");
        System.out.println("updateMemberDTO : " + updateMemberDTO);
        Member member = memberRepository.findByMemberNo(updateMemberDTO.getMemberNo()).get();

        String replaceFileName = null;
        int result = 0;


            /* update 할 엔티티 조회 */

            String oriImage = member.getImgUrl();
            System.out.println("[updateMember] oriImage : " + oriImage);
            DepDTO updateDepDTO = findDepByDepNo(updateMemberDTO.getDepartment().getDepNo());
            Dep updateDep =modelMapper.map(updateDepDTO,Dep.class);
            PositionDTO updatePositionDTO = findPositionByPositionNo(updateMemberDTO.getPosition().getPositionNo());
            Position updatePosition =modelMapper.map(updatePositionDTO,Position.class);

            /* update를 위한 엔티티 값 수정 */


            member = member.memberName(updateMemberDTO.getMemberName()).build();
            member = member.phone(updateMemberDTO.getPhone()).build();
            member = member.birthDate(updateMemberDTO.getBirthDate()).build();
            member = member.address(updateMemberDTO.getAddress()).build();
            member = member.privateEmail(updateMemberDTO.getPrivateEmail()).build();
            member = member.companyEmail(updateMemberDTO.getCompanyEmail()).build();
            member = member.department(updateDep).build();
            member = member.position(updatePosition).build();
            member = member.account(updateMemberDTO.getAccount()).build();



        System.out.println("[MemberService] updateMember End ===================================" + member);
        memberRepository.save(member);
        return  "사원 업데이트 성공";
    }
}
