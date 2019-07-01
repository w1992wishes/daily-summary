ALTER TABLE public.odl_${bizCode}_event_face_${algVersion}
ADD COLUMN target_thumbnail_rect VARCHAR(255);

ALTER TABLE public.odl_${bizCode}_event_face_pending
ADD COLUMN target_thumbnail_rect VARCHAR(255);