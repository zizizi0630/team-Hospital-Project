package healthcare.appointment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import healthcare.DataNotFoundException;
import healthcare.user.SiteUser;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class AppointmentService {
    private final AppointmentRepository apmtRepository;
    private final healthcare.code.CodeRepository codeRepository;

    public List<Appointment> getList() {
        return this.apmtRepository.findAll();
    }

    public List<Appointment> getList(SiteUser user) {
        return this.apmtRepository.findByApmtAuthor(user);
    }

    public Appointment insert(String apmt_Name, String apmt_Telp, String apmt_Addr, LocalDateTime apmt_Date,
            String apmt_Gender, String codeId, String symptoms, SiteUser user) {

        // 1. Validate 10-minute interval
        if (apmt_Date.getMinute() % 10 != 0) {
            throw new IllegalArgumentException("예약 시간은 10분 단위여야 합니다. (예: 10:00, 10:10, 10:20)");
        }

        Appointment apmt = new Appointment();
        apmt.setApmt_name(apmt_Name);
        apmt.setApmt_telp(apmt_Telp);
        apmt.setApmt_addr(apmt_Addr);
        apmt.setApmt_date(apmt_Date);
        apmt.setApmt_gender(apmt_Gender);
        apmt.setC_U_Date(LocalDateTime.now());
        apmt.setApmtAuthor(user);

        // Code(Department) Setting & Overlap Check
        if (codeId != null && !codeId.isEmpty()) {
            healthcare.code.Code code = this.codeRepository.findBycodeId(codeId)
                    .orElseThrow(() -> new DataNotFoundException("진료과 코드를 찾을 수 없습니다."));

            // 2. Validate Overlap
            if (this.apmtRepository.existsByCodeAndDate(code, apmt_Date)) {
                throw new IllegalArgumentException("해당 시간에는 이미 예약이 존재합니다.");
            }
            apmt.setCode(code);
        }
        // Symptoms Setting
        apmt.setSymptoms(symptoms);

        this.apmtRepository.save(apmt);
        return apmt;
    }

    public Appointment getAppointment(Long id) {
        Optional<Appointment> apmt = this.apmtRepository.findById(id);
        if (apmt.isPresent()) {
            return apmt.get();
        } else {
            throw new DataNotFoundException("code not found");
        }
    }

    public void delete(Appointment apmt) {
        this.apmtRepository.delete(apmt);
    }

    public void modify(Appointment apmt, String apmt_Name, String apmt_Telp, LocalDateTime apmt_Date,
            String apmt_Addr, String codeId, String symptoms) {

        // 1. Validate 10-minute interval
        if (apmt_Date.getMinute() % 10 != 0) {
            throw new IllegalArgumentException("예약 시간은 10분 단위여야 합니다. (예: 10:00, 10:10, 10:20)");
        }

        apmt.setApmt_name(apmt_Name);
        apmt.setApmt_telp(apmt_Telp);
        apmt.setApmt_addr(apmt_Addr);
        apmt.setApmt_date(apmt_Date);
        apmt.setC_U_Date(LocalDateTime.now());

        if (codeId != null && !codeId.isEmpty()) {
            healthcare.code.Code code = this.codeRepository.findBycodeId(codeId)
                    .orElseThrow(() -> new DataNotFoundException("진료과 코드를 찾을 수 없습니다."));

            // 2. Validate Overlap (exclude self if checking same slot? actually if date
            // changed or code changed)
            // Ideally check if (code, date) exists and ID != currentID using repository
            // query,
            // but for now, simple check is safer. If same user keeps same slot, it might
            // error.
            // Let's rely on simple check for now, user can cancel then re-book or we accept
            // strictness.
            // Actually, if modifying to SAME slot, it will overlap with ITSELF.
            // Modification should check if DIFFERENT from current.

            boolean isSameSlot = (apmt.getCode() != null && apmt.getCode().getId().equals(code.getId()))
                    && apmt.getApmt_date().isEqual(apmt_Date);

            if (!isSameSlot && this.apmtRepository.existsByCodeAndDate(code, apmt_Date)) {
                throw new IllegalArgumentException("해당 시간에는 이미 예약이 존재합니다.");
            }
            apmt.setCode(code);
        }
        apmt.setSymptoms(symptoms);

        this.apmtRepository.save(apmt);
    }
}
