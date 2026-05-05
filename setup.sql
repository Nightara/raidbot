-- --------------------------------------------------------
-- Host:                         127.0.0.1
-- Server version:               12.2.2-MariaDB - MariaDB Server
-- Server OS:                    Win64
-- HeidiSQL Version:             12.14.0.7165
-- --------------------------------------------------------

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET NAMES utf8 */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;


-- Dumping database structure for raidbot
CREATE DATABASE IF NOT EXISTS `raidbot` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci */;
USE `raidbot`;

-- Dumping structure for table raidbot.boss
CREATE TABLE IF NOT EXISTS `boss` (
  `id` varchar(5) NOT NULL,
  `name` varchar(50) NOT NULL,
  `wing` varchar(5) DEFAULT NULL,
  `after` varchar(5) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `boss_boss_id_fk` (`after`),
  KEY `boss_wing_id_fk` (`wing`),
  CONSTRAINT `boss_boss_id_fk` FOREIGN KEY (`after`) REFERENCES `boss` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `boss_wing_id_fk` FOREIGN KEY (`wing`) REFERENCES `wing` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

-- Data exporting was unselected.

-- Dumping structure for table raidbot.role
CREATE TABLE IF NOT EXISTS `role` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `boss` varchar(5) NOT NULL,
  `name` varchar(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `role_boss_id_fk` (`boss`),
  CONSTRAINT `role_boss_id_fk` FOREIGN KEY (`boss`) REFERENCES `boss` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

-- Data exporting was unselected.

-- Dumping structure for table raidbot.run
CREATE TABLE IF NOT EXISTS `run` (
  `date` date NOT NULL DEFAULT current_timestamp(),
  `ordinal` int(10) unsigned NOT NULL,
  `wing` varchar(5) NOT NULL,
  PRIMARY KEY (`date`,`wing`),
  UNIQUE KEY `run_pk_2` (`ordinal`,`date`),
  KEY `run_wing_id_fk` (`wing`),
  CONSTRAINT `run_wing_id_fk` FOREIGN KEY (`wing`) REFERENCES `wing` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

-- Data exporting was unselected.

-- Dumping structure for table raidbot.signup
CREATE TABLE IF NOT EXISTS `signup` (
  `date` date NOT NULL,
  `role` int(10) unsigned NOT NULL,
  `player` bigint(20) NOT NULL,
  KEY `signup_role_id_fk` (`role`),
  KEY `signup_run_date_fk` (`date`),
  CONSTRAINT `signup_role_id_fk` FOREIGN KEY (`role`) REFERENCES `role` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `signup_run_date_fk` FOREIGN KEY (`date`) REFERENCES `run` (`date`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

-- Data exporting was unselected.

-- Dumping structure for table raidbot.wing
CREATE TABLE IF NOT EXISTS `wing` (
  `id` varchar(5) NOT NULL,
  `name` varchar(50) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

-- Data exporting was unselected.

/*!40103 SET TIME_ZONE=IFNULL(@OLD_TIME_ZONE, 'system') */;
/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IFNULL(@OLD_FOREIGN_KEY_CHECKS, 1) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40111 SET SQL_NOTES=IFNULL(@OLD_SQL_NOTES, 1) */;
