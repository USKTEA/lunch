package com.usktea.lunch.client.vo

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonRootName
import java.time.LocalDate
import java.time.LocalDateTime

data class OpenDataResponseWrapper(
    @JsonProperty("LOCALDATA_072404")
    val response: OpenDataResponseVo? = null,
    @JsonProperty("RESULT")
    val result: ResultInfoVo? = null,
)

/**
 * 공공 데이터 API 응답을 나타내는 DTO.
 *
 * @property listTotalCount 총 데이터 개수
 * @property result API 응답 결과 정보
 * @property rows 개별 사업장 정보 리스트
 */
@JsonRootName("LOCALDATA_072404")
data class OpenDataResponseVo(
    @JsonProperty("list_total_count")
    val listTotalCount: Int,
    @JsonProperty("RESULT")
    val result: ResultInfoVo,
    @JsonProperty("row")
    val rows: List<BusinessInfoVo>,
)

/**
 * API 요청 결과 정보를 포함하는 DTO.
 *
 * @property code 응답 코드 (예: "INFO-000")
 * @property message 응답 메시지 (예: "정상 처리되었습니다")
 */
data class ResultInfoVo(
    @JsonProperty("CODE")
    val code: String,
    @JsonProperty("MESSAGE")
    val message: String,
)

/**
 * 개별 사업장 정보를 나타내는 DTO.
 *
 * @property openSelfTeamCode 개방 자치단체 코드
 * @property managementNumber 관리번호
 * @property approvalDate 인허가 일자
 * @property approvalCancelDate 인허가 취소일자
 * @property tradeStateCode 영업 상태 코드
 * @property tradeStateName 영업 상태명
 * @property detailTradeStateCode 상세 영업 상태 코드
 * @property detailTradeStateName 상세 영업 상태명
 * @property closeDate 폐업 일자
 * @property pauseStartDate 휴업 시작일자
 * @property pauseEndDate 휴업 종료일자
 * @property reopenDate 재개업 일자
 * @property siteTel 전화번호
 * @property siteArea 소재지 면적
 * @property sitePostNo 소재지 우편번호
 * @property siteWholeAddress 지번 주소
 * @property roadWholeAddress 도로명 주소
 * @property roadPostNo 도로명 우편번호
 * @property businessPlaceName 사업장명
 * @property lastModifiedTimestamp 최종 수정 일자
 * @property updateType 데이터 갱신 구분
 * @property updateDate 데이터 갱신 일자
 * @property businessType 업태 구분명
 * @property xCoordinate 좌표 정보 (X) EPSG:5174
 * @property yCoordinate 좌표 정보 (Y) EPSG:5174
 * @property sanitaryBusinessType 위생 업태명
 * @property maleEmployeeCount 남성 종사자 수
 * @property femaleEmployeeCount 여성 종사자 수
 * @property tradeSurroundingCategory 영업장 주변 구분명
 * @property gradeCategory 등급 구분명
 * @property waterSupplyFacility 급수 시설 구분명
 * @property totalEmployees 총 종업원 수
 * @property headquartersEmployees 본사 종업원 수
 * @property factoryOfficeWorkers 공장 사무직 종업원 수
 * @property factorySalesWorkers 공장 판매직 종업원 수
 * @property factoryProductionWorkers 공장 생산직 종업원 수
 * @property buildingOwnershipCategory 건물 소유 구분명
 * @property securityDeposit 보증액
 * @property monthlyRent 월세액
 * @property multiUseBusiness 다중이용업소 여부 (Y/N)
 * @property totalFacilitySize 시설 총 규모
 * @property traditionalBusinessNumber 전통업소 지정번호
 * @property traditionalMainDish 전통업소 주된 음식
 * @property homepage 홈페이지 URL
 */
data class BusinessInfoVo(
    @JsonProperty("OPNSFTEAMCODE")
    val openSelfTeamCode: String,
    @JsonProperty("MGTNO")
    val managementNumber: String,
    @JsonProperty("APVPERMYMD")
    val approvalDate: LocalDate,
    @JsonProperty("APVCANCELYMD")
    val approvalCancelDate: LocalDate?,
    @JsonProperty("TRDSTATEGBN")
    val tradeStateCode: String,
    @JsonProperty("TRDSTATENM")
    val tradeStateName: String,
    @JsonProperty("DTLSTATEGBN")
    val detailTradeStateCode: String,
    @JsonProperty("DTLSTATENM")
    val detailTradeStateName: String,
    @JsonProperty("DCBYMD")
    val closeDate: LocalDate?,
    @JsonProperty("CLGSTDT")
    val pauseStartDate: LocalDate?,
    @JsonProperty("CLGENDDT")
    val pauseEndDate: LocalDate?,
    @JsonProperty("ROPNYMD")
    val reopenDate: LocalDate?,
    @JsonProperty("SITETEL")
    val siteTel: String?,
    @JsonProperty("SITEAREA")
    val siteArea: String?,
    @JsonProperty("SITEPOSTNO")
    val sitePostNo: String,
    @JsonProperty("SITEWHLADDR")
    val siteWholeAddress: String,
    @JsonProperty("RDNWHLADDR")
    val roadWholeAddress: String,
    @JsonProperty("RDNPOSTNO")
    val roadPostNo: String,
    @JsonProperty("BPLCNM")
    val businessPlaceName: String?,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss[.S]")
    @JsonProperty("LASTMODTS")
    val lastModifiedTimestamp: LocalDateTime?,
    @JsonProperty("UPDATEGBN")
    val updateType: String?,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss[.S]")
    @JsonProperty("UPDATEDT")
    val updateDate: LocalDateTime?,
    @JsonProperty("UPTAENM")
    val businessType: String?,
    @JsonProperty("X")
    val xCoordinate: Double?,
    @JsonProperty("Y")
    val yCoordinate: Double?,
    @JsonProperty("SNTUPTAENM")
    val sanitaryBusinessType: String?,
    @JsonProperty("MANEIPCNT")
    val maleEmployeeCount: Int?,
    @JsonProperty("WMEIPCNT")
    val femaleEmployeeCount: Int?,
    @JsonProperty("TRDPJUBNSENM")
    val tradeSurroundingCategory: String?,
    @JsonProperty("LVSENM")
    val gradeCategory: String?,
    @JsonProperty("WTRSPLYFACILSENM")
    val waterSupplyFacility: String?,
    @JsonProperty("TOTEPNUM")
    val totalEmployees: Int?,
    @JsonProperty("HOFFEPCNT")
    val headquartersEmployees: Int?,
    @JsonProperty("FCTYOWKEPCNT")
    val factoryOfficeWorkers: Int?,
    @JsonProperty("FCTYSILJOBEPCNT")
    val factorySalesWorkers: Int?,
    @JsonProperty("FCTYPDTJOBEPCNT")
    val factoryProductionWorkers: Int?,
    @JsonProperty("BDNGOWNSENM")
    val buildingOwnershipCategory: String?,
    @JsonProperty("ISREAM")
    val securityDeposit: Double?,
    @JsonProperty("MONAM")
    val monthlyRent: Double?,
    @JsonProperty("MULTUSNUPSOYN")
    val multiUseBusiness: String?,
    @JsonProperty("FACILTOTSCP")
    val totalFacilitySize: Double?,
    @JsonProperty("JTUPSOASGNNO")
    val traditionalBusinessNumber: String?,
    @JsonProperty("JTUPSOMAINEDF")
    val traditionalMainDish: String?,
    @JsonProperty("HOMEPAGE")
    val homepage: String?,
)
