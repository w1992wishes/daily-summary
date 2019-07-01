-- ----------------------------
-- 创建同行关系表-每天
-- ----------------------------
DROP TABLE IF EXISTS "public"."dwd_${bizCode}_relation_peer_day_${algVersion}";
CREATE TABLE "public"."dwd_${bizCode}_relation_peer_day_${algVersion}" (
  "aid1" varchar(255),
  "aid2" varchar(255),
  "t1" varchar(255),
  "t2" varchar(255),
  "along_interval" int4,
  "source_id" varchar(32),
  "create_time" timestamp(0),
  "dt" varchar(255)
)
;

COMMENT ON COLUMN public.dwd_${bizCode}_relation_peer_day_${algVersion}.aid1 IS '档案id1';
COMMENT ON COLUMN public.dwd_${bizCode}_relation_peer_day_${algVersion}.aid2 IS '档案id2';
COMMENT ON COLUMN public.dwd_${bizCode}_relation_peer_day_${algVersion}.t1 IS '档案1出现时间';
COMMENT ON COLUMN public.dwd_${bizCode}_relation_peer_day_${algVersion}.t2 IS '档案2出现时间';
COMMENT ON COLUMN public.dwd_${bizCode}_relation_peer_day_${algVersion}.along_interval IS '同行间隔';
COMMENT ON COLUMN public.dwd_${bizCode}_relation_peer_day_${algVersion}.source_id IS '摄像头id';
COMMENT ON COLUMN public.dwd_${bizCode}_relation_peer_day_${algVersion}.create_time IS '创建时间';
COMMENT ON COLUMN public.dwd_${bizCode}_relation_peer_day_${algVersion}.dt IS '创建日期';

-- ----------------------------
-- 创建同行关系表-间隔时段
-- ----------------------------
DROP TABLE IF EXISTS "public"."dwd_${bizCode}_relation_peer_interval_${algVersion}";
CREATE TABLE "public"."dwd_${bizCode}_relation_peer_interval_${algVersion}" (
  "aid1" varchar(255),
  "aid2" varchar(255),
  "t1" varchar(255),
  "t2" varchar(255),
  "along_interval" int4,
  "source_id" varchar(32),
  "create_time" timestamp(0),
  "dt" varchar(255)
)
;

COMMENT ON COLUMN public.dwd_${bizCode}_relation_peer_interval_${algVersion}.aid1 IS '档案id1';
COMMENT ON COLUMN public.dwd_${bizCode}_relation_peer_interval_${algVersion}.aid2 IS '档案id2';
COMMENT ON COLUMN public.dwd_${bizCode}_relation_peer_interval_${algVersion}.t1 IS '档案1出现时间';
COMMENT ON COLUMN public.dwd_${bizCode}_relation_peer_interval_${algVersion}.t2 IS '档案2出现时间';
COMMENT ON COLUMN public.dwd_${bizCode}_relation_peer_interval_${algVersion}.along_interval IS '同行间隔';
COMMENT ON COLUMN public.dwd_${bizCode}_relation_peer_interval_${algVersion}.source_id IS '摄像头id';
COMMENT ON COLUMN public.dwd_${bizCode}_relation_peer_interval_${algVersion}.create_time IS '创建时间';
COMMENT ON COLUMN public.dwd_${bizCode}_relation_peer_interval_${algVersion}.dt IS '创建日期';