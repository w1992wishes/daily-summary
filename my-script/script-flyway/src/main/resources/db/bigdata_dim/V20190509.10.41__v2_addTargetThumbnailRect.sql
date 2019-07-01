ALTER TABLE dim_${bizCode}_event_face_person_${algVersion}
ADD COLUMN target_thumbnail_rect VARCHAR(255);

ALTER TABLE public.dim_${bizCode}_event_face_person_${algVersion}
ADD COLUMN field1 text DEFAULT ''::text;