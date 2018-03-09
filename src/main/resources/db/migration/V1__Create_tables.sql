CREATE TABLE `jobs` (
  id SERIAL PRIMARY KEY,
  text TEXT
);
CREATE TABLE `tokens` (
  id SERIAL PRIMARY KEY,
  `job_id` SERIAL NOT NULL,
  `position` INT NOT NULL,
  `token` VARCHAR(50) NOT NULL,
  `part_of_speech` VARCHAR(10) NOT NULL,
  CONSTRAINT `fk_tokens_job`
  FOREIGN KEY (`job_id`)
  REFERENCES `jobs` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION
);