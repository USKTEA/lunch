package com.usktea.lunch.service.entity

import com.usktea.lunch.entity.SeoulRestaurantEntity
import com.usktea.lunch.repository.SeoulRestaurantRepository
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.sql.PreparedStatement

@Service
class SeoulRestaurantEntityService(
    private val jdbcTemplate: JdbcTemplate,
    private val seoulRestaurantRepository: SeoulRestaurantRepository,
) {
    private val bulkUpsertSql =
        """
        INSERT INTO open_data_cloud.seoul_restaurant (
            management_number,
            open_self_team_code,
            approval_date,
            approval_cancel_date,
            trade_state_code,
            trade_state_name,
            detail_trade_state_code,
            detail_trade_state_name,
            close_date,
            pause_start_date,
            pause_end_date,
            reopen_date,
            site_tel,
            site_area,
            site_post_no,
            site_whole_address,
            road_whole_address,
            road_post_no,
            business_place_name,
            last_modified_timestamp,
            update_type,
            update_date,
            business_type,
            x_coordinate,
            y_coordinate,
            sanitary_business_type,
            male_employee_count,
            female_employee_count,
            trade_surrounding_category,
            grade_category,
            water_supply_facility,
            total_employees,
            headquarters_employees,
            factory_office_workers,
            factory_sales_workers,
            factory_production_workers,
            building_ownership_category,
            security_deposit,
            monthly_rent,
            multi_use_business,
            total_facility_size,
            traditional_business_number,
            traditional_main_dish,
            homepage,
            created_at,
            updated_at
        ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
        ON CONFLICT (management_number) 
        DO UPDATE SET
            open_self_team_code = EXCLUDED.open_self_team_code,
            approval_date = EXCLUDED.approval_date,
            approval_cancel_date = EXCLUDED.approval_cancel_date,
            trade_state_code = EXCLUDED.trade_state_code,
            trade_state_name = EXCLUDED.trade_state_name,
            detail_trade_state_code = EXCLUDED.detail_trade_state_code,
            detail_trade_state_name = EXCLUDED.detail_trade_state_name,
            close_date = EXCLUDED.close_date,
            pause_start_date = EXCLUDED.pause_start_date,
            pause_end_date = EXCLUDED.pause_end_date,
            reopen_date = EXCLUDED.reopen_date,
            site_tel = EXCLUDED.site_tel,
            site_area = EXCLUDED.site_area,
            site_post_no = EXCLUDED.site_post_no,
            site_whole_address = EXCLUDED.site_whole_address,
            road_whole_address = EXCLUDED.road_whole_address,
            road_post_no = EXCLUDED.road_post_no,
            business_place_name = EXCLUDED.business_place_name,
            last_modified_timestamp = EXCLUDED.last_modified_timestamp,
            update_type = EXCLUDED.update_type,
            update_date = EXCLUDED.update_date,
            business_type = EXCLUDED.business_type,
            x_coordinate = EXCLUDED.x_coordinate,
            y_coordinate = EXCLUDED.y_coordinate,
            sanitary_business_type = EXCLUDED.sanitary_business_type,
            male_employee_count = EXCLUDED.male_employee_count,
            female_employee_count = EXCLUDED.female_employee_count,
            trade_surrounding_category = EXCLUDED.trade_surrounding_category,
            grade_category = EXCLUDED.grade_category,
            water_supply_facility = EXCLUDED.water_supply_facility,
            total_employees = EXCLUDED.total_employees,
            headquarters_employees = EXCLUDED.headquarters_employees,
            factory_office_workers = EXCLUDED.factory_office_workers,
            factory_sales_workers = EXCLUDED.factory_sales_workers,
            factory_production_workers = EXCLUDED.factory_production_workers,
            building_ownership_category = EXCLUDED.building_ownership_category,
            security_deposit = EXCLUDED.security_deposit,
            monthly_rent = EXCLUDED.monthly_rent,
            multi_use_business = EXCLUDED.multi_use_business,
            total_facility_size = EXCLUDED.total_facility_size,
            traditional_business_number = EXCLUDED.traditional_business_number,
            traditional_main_dish = EXCLUDED.traditional_main_dish,
            homepage = EXCLUDED.homepage,
            updated_at = EXCLUDED.updated_at
        """.trimIndent()

    fun batchUpsertRestaurants(restaurants: List<SeoulRestaurantEntity>) {
        jdbcTemplate.batchUpdate(
            bulkUpsertSql,
            object : BatchPreparedStatementSetter {
                override fun setValues(
                    ps: PreparedStatement,
                    i: Int,
                ) {
                    val restaurant = restaurants[i]
                    var idx = 1
                    ps.setString(idx++, restaurant.managementNumber)
                    ps.setString(idx++, restaurant.openSelfTeamCode)
                    ps.setObject(idx++, restaurant.approvalDate)
                    ps.setObject(idx++, restaurant.approvalCancelDate)
                    ps.setString(idx++, restaurant.tradeStateCode)
                    ps.setString(idx++, restaurant.tradeStateName)
                    ps.setString(idx++, restaurant.detailTradeStateCode)
                    ps.setString(idx++, restaurant.detailTradeStateName)
                    ps.setObject(idx++, restaurant.closeDate)
                    ps.setObject(idx++, restaurant.pauseStartDate)
                    ps.setObject(idx++, restaurant.pauseEndDate)
                    ps.setObject(idx++, restaurant.reopenDate)
                    ps.setString(idx++, restaurant.siteTel)
                    ps.setString(idx++, restaurant.siteArea)
                    ps.setString(idx++, restaurant.sitePostNo)
                    ps.setString(idx++, restaurant.siteWholeAddress)
                    ps.setString(idx++, restaurant.roadWholeAddress)
                    ps.setString(idx++, restaurant.roadPostNo)
                    ps.setString(idx++, restaurant.businessPlaceName)
                    ps.setObject(idx++, restaurant.lastModifiedTimestamp)
                    ps.setString(idx++, restaurant.updateType)
                    ps.setObject(idx++, restaurant.updateDate)
                    ps.setString(idx++, restaurant.businessType)
                    ps.setObject(idx++, restaurant.xCoordinate)
                    ps.setObject(idx++, restaurant.yCoordinate)
                    ps.setString(idx++, restaurant.sanitaryBusinessType)
                    ps.setObject(idx++, restaurant.maleEmployeeCount)
                    ps.setObject(idx++, restaurant.femaleEmployeeCount)
                    ps.setString(idx++, restaurant.tradeSurroundingCategory)
                    ps.setString(idx++, restaurant.gradeCategory)
                    ps.setString(idx++, restaurant.waterSupplyFacility)
                    ps.setObject(idx++, restaurant.totalEmployees)
                    ps.setObject(idx++, restaurant.headquartersEmployees)
                    ps.setObject(idx++, restaurant.factoryOfficeWorkers)
                    ps.setObject(idx++, restaurant.factorySalesWorkers)
                    ps.setObject(idx++, restaurant.factoryProductionWorkers)
                    ps.setString(idx++, restaurant.buildingOwnershipCategory)
                    ps.setObject(idx++, restaurant.securityDeposit)
                    ps.setObject(idx++, restaurant.monthlyRent)
                    ps.setString(idx++, restaurant.multiUseBusiness)
                    ps.setObject(idx++, restaurant.totalFacilitySize)
                    ps.setString(idx++, restaurant.traditionalBusinessNumber)
                    ps.setString(idx++, restaurant.traditionalMainDish)
                    ps.setString(idx++, restaurant.homepage)
                    ps.setObject(idx++, restaurant.createdAt)
                    ps.setObject(idx++, restaurant.updatedAt)
                }

                override fun getBatchSize(): Int {
                    return restaurants.size
                }
            },
        )
    }

    fun getSeoulRestaurantId(): SeoulRestaurantEntity? {
        return seoulRestaurantRepository.findFirstByOrderByIdDesc()
    }
}
