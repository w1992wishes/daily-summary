--target_thumbnail_rect
ALTER TABLE public.dwd_${bizCode}_event_face_${algVersion}
ADD COLUMN target_thumbnail_rect VARCHAR(255);

ALTER TABLE public.dwd_${bizCode}_event_face_cluster_${algVersion}
ADD COLUMN target_thumbnail_rect VARCHAR(255);

ALTER TABLE public.dwd_${bizCode}_event_face_remaining_${algVersion}
ADD COLUMN target_thumbnail_rect VARCHAR(255);

--field1
ALTER TABLE public.dwd_${bizCode}_event_face_${algVersion}
ADD COLUMN field1 text DEFAULT ''::text;

ALTER TABLE public.dwd_${bizCode}_event_face_cluster_${algVersion}
ADD COLUMN field1 text DEFAULT ''::text;

ALTER TABLE public.dwd_${bizCode}_event_face_remaining_${algVersion}
ADD COLUMN field1 text DEFAULT ''::text;