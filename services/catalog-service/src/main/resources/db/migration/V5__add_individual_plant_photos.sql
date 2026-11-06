-- Phase 3: WYSIWYG photo gallery for individual plants.
CREATE TABLE individual_plant_photos (
    plant_id uuid          NOT NULL REFERENCES individual_plants (id) ON DELETE CASCADE,
    position integer       NOT NULL,
    url      varchar(1000) NOT NULL,
    kind     varchar(20)   NOT NULL,
    PRIMARY KEY (plant_id, position)
);
