#!/usr/bin/env bash
set -euo pipefail

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-edu_saas}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-}"

mysql_args=(-h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" "$DB_NAME" --batch --skip-column-names)
if [[ -n "$DB_PASSWORD" ]]; then
  mysql_args=(-h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" "-p$DB_PASSWORD" "$DB_NAME" --batch --skip-column-names)
fi

index_exists() {
  local table_name="$1"
  local index_name="$2"
  mysql "${mysql_args[@]}" -e "select count(1) from information_schema.statistics where table_schema = database() and table_name = '${table_name}' and index_name = '${index_name}'"
}

add_index() {
  local table_name="$1"
  local index_name="$2"
  local definition="$3"
  if [[ "$(index_exists "$table_name" "$index_name")" == "0" ]]; then
    echo "adding ${table_name}.${index_name}"
    mysql "${mysql_args[@]}" -e "alter table ${table_name} add index ${index_name} ${definition}"
  else
    echo "exists ${table_name}.${index_name}"
  fi
}

add_index account idx_account_list "(tenant_id, deleted, created_at)"
add_index role idx_role_list "(tenant_id, deleted, data_scope, id)"
add_index role idx_role_builtin "(tenant_id, deleted, system_builtin)"
add_index menu_permission idx_menu_tree "(tenant_id, deleted, parent_id, sort_no)"
add_index menu_permission idx_menu_list "(tenant_id, deleted, type, sort_no)"
add_index operation_log idx_operation_log_module_time "(tenant_id, module, action, created_at)"
add_index operation_log idx_operation_log_username_time "(tenant_id, username, created_at)"
add_index campus idx_campus_list "(tenant_id, deleted, status, id)"
add_index course_product idx_course_product_list "(tenant_id, deleted, status, created_at)"
add_index course_product idx_course_product_owner "(tenant_id, created_by, deleted, created_at)"
add_index class_group idx_class_list "(tenant_id, deleted, status, created_at)"
add_index class_group idx_class_campus_list "(tenant_id, campus_id, deleted, status, created_at)"
add_index lesson_session idx_lesson_list "(tenant_id, deleted, status, planned_start_at)"
add_index lesson_session idx_lesson_owner "(tenant_id, created_by, deleted, planned_start_at)"
add_index student idx_student_guardian_phone "(tenant_id, guardian_phone)"
add_index student idx_student_list "(tenant_id, deleted, status, created_at)"
add_index student idx_student_owner "(tenant_id, created_by, deleted, created_at)"
add_index class_enrollment idx_enrollment_class_status "(tenant_id, class_group_id, deleted, enroll_status)"
add_index class_enrollment idx_enrollment_list "(tenant_id, deleted, created_at)"
add_index enrollment_order idx_enrollment_order_list "(tenant_id, deleted, created_at)"
add_index enrollment_order idx_enrollment_order_campus_list "(tenant_id, campus_id, deleted, created_at)"
add_index enrollment_order idx_enrollment_order_owner "(tenant_id, created_by, deleted, created_at)"
add_index payment_record idx_payment_record_list "(tenant_id, deleted, received_at)"
add_index refund_record idx_refund_record_list "(tenant_id, deleted, refunded_at)"
