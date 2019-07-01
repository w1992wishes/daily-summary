-- ----------------------------
-- Table structure bdl_${bizCode}_task_info_${algVersion}  任务信息表
-- ----------------------------
DROP TABLE IF EXISTS "public"."bdl_${bizCode}_task_info_${algVersion}";
CREATE TABLE "public"."bdl_${bizCode}_task_info_${algVersion}" (
  "task_id" varchar(256) primary key,
  "task_name" varchar(256),
  "task_type" varchar(256),
  "project_name" varchar(256),
  "exec_id" varchar(256),
  "status" varchar(256),
  "remark" varchar(1024),
  "create_time" timestamp(0)
)DISTRIBUTED BY (task_id);

COMMENT ON TABLE "public"."bdl_${bizCode}_task_info_${algVersion}" IS '任务信息表';
COMMENT ON COLUMN "public"."bdl_${bizCode}_task_info_${algVersion}"."task_id" IS '业务分配的任务 id';
COMMENT ON COLUMN "public"."bdl_${bizCode}_task_info_${algVersion}"."task_name" IS '任务名字';
COMMENT ON COLUMN "public"."bdl_${bizCode}_task_info_${algVersion}"."task_type" IS '任务类型';
COMMENT ON COLUMN "public"."bdl_${bizCode}_task_info_${algVersion}"."project_name" IS 'azkaban 项目名字';
COMMENT ON COLUMN "public"."bdl_${bizCode}_task_info_${algVersion}"."exec_id" IS 'azkaban 执行 id';
COMMENT ON COLUMN "public"."bdl_${bizCode}_task_info_${algVersion}"."status" IS '任务状态';
COMMENT ON COLUMN "public"."bdl_${bizCode}_task_info_${algVersion}"."remark" IS '状态描述';
COMMENT ON COLUMN "public"."bdl_${bizCode}_task_info_${algVersion}"."create_time" IS '创建时间';