-- phpMyAdmin SQL Dump
-- version 5.0.2
-- https://www.phpmyadmin.net/
-- 
-- Host: 127.0.0.1
-- Generation Time: Feb 13, 2022 at 02:14 PM
-- Server version: 10.4.14-MariaDB
-- PHP Version: 7.4.10

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `goldcoast_esports`
--
CREATE DATABASE /*!32312 IF NOT EXISTS*/`goldcoast_esports` /*!40100 DEFAULT CHARACTER SET utf8mb4 */;

USE `goldcoast_esports`;
-- --------------------------------------------------------
-- --------------------------------------------------------

DROP TABLE IF EXISTS `competition`;

--
-- Table structure for table `event`
--
DROP TABLE IF EXISTS `event`;

CREATE TABLE `event` (
  `name` varchar(64) NOT NULL,
  `date` date NOT NULL,
  `location` varchar(64) NOT NULL,
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Dumping data for table `event`
--

INSERT INTO `event` (`name`, `date`, `location`) VALUES
('DOTA 2 Battle Royale', '2023-01-21', 'TAFE Coomera'),
('LoL Epic 2022', '2023-02-18', 'TAFE Coomera'),
('Rocket League Swipers', '2023-03-18', 'TAFE Coomera'),
('Fortnite Battle Royale', '2023-04-15', 'TAFE Coomera'),
('NBA Championships', '2023-05-20', 'TAFE Coomera'),
('Call of Duty Warzone', '2023-06-17', 'TAFE Coomera');


-- --------------------------------------------------------

--
-- Table structure for table `game`
--
DROP TABLE IF EXISTS `game`;

CREATE TABLE `game` (
  `name` varchar(64) NOT NULL,
  `type` varchar(64) NOT NULL,
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Dumping data for table `game`
--

INSERT INTO `game` (`name`, `type`) VALUES
('DOTA 2', 'Team or Solo (MOBA)'),
('Fortnite', 'Team (Battle Royale)'),
('League of Legends', 'Team (MOBA)'),
('NBA 2K', 'Team (Basketball)'),
('Rocket League', 'Team or Solo (Vehicular Soccer)'),
('Super Smash Bros.', 'Team (Action)');

-- --------------------------------------------------------

--
-- Table structure for table `team`
--
DROP TABLE IF EXISTS `team`;

CREATE TABLE `team` (
  `name` varchar(64) NOT NULL,
  `contact` varchar(64) NOT NULL,
  `phone` varchar(64) NOT NULL,
  `email` varchar(64) NOT NULL,
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Dumping data for table `team`
--

INSERT INTO `team` (`name`, `contact`, `phone`, `email`) VALUES
('BioHazards', 'Zheng Lee', '0418999888', 'zhenglee99@geemail.com'),
('Buttercups', 'Eric Stratton', '040765123', 'eric_stratto@deltaomni.com'),
('Coomera Bombers', 'James Taylor', '0433948765', 'jamestaylor123@coolmail.com'),
('Nerang Necros', 'Sophie Jamieson', '0440888222', 'sophie_jamieson@geemail.com');

-- --------------------------------------------------------
--
-- Table structure for table `competition`
--

CREATE TABLE `competition` (
  `competitionID` int(10) UNSIGNED NOT NULL,
  `eventName` varchar(64) NOT NULL,
  `gameName` varchar(64) NOT NULL,
  `team1` varchar(64) NOT NULL,
  `team2` varchar(64) NOT NULL,
  `team1Points` int(10) UNSIGNED NOT NULL,
  `team2Points` int(10) UNSIGNED NOT NULL,
  PRIMARY KEY (`competitionID`),
  CONSTRAINT `competition_ibfk_1` FOREIGN KEY (`eventName`) REFERENCES `event` (`name`),
  CONSTRAINT `competition_ibfk_2` FOREIGN KEY (`gameName`) REFERENCES `game` (`name`),
  CONSTRAINT `competition_ibfk_3` FOREIGN KEY (`team1`) REFERENCES `team` (`name`),
  CONSTRAINT `competition_ibfk_4` FOREIGN KEY (`team2`) REFERENCES `team` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Dumping data for table `competition`
--

INSERT INTO `competition` (`competitionID`, `eventName`, `gameName`, `team1`, `team2`, `team1Points`, `team2Points`) VALUES
(1, 'DOTA 2 Battle Royale', 'DOTA 2', 'BioHazards', 'Buttercups', 0, 2),
(2, 'DOTA 2 Battle Royale', 'DOTA 2', 'BioHazards', 'Coomera Bombers', 0, 2),
(3, 'DOTA 2 Battle Royale', 'DOTA 2', 'BioHazards', 'Nerang Necros', 0, 2),
(4, 'DOTA 2 Battle Royale', 'DOTA 2', 'Buttercups', 'Coomera Bombers', 0, 2),
(5, 'DOTA 2 Battle Royale', 'DOTA 2', 'Buttercups', 'Nerang Necros', 1, 1),
(6, 'DOTA 2 Battle Royale', 'DOTA 2', 'Coomera Bombers', 'Nerang Necros', 0, 2),
(7, 'LoL Epic 2022', 'League of Legends', 'BioHazards', 'Buttercups', 0, 2),
(8, 'LoL Epic 2022', 'League of Legends', 'BioHazards', 'Coomera Bombers', 1, 1),
(9, 'LoL Epic 2022', 'League of Legends', 'BioHazards', 'Nerang Necros', 2, 0),
(10, 'LoL Epic 2022', 'League of Legends', 'Buttercups', 'Coomera Bombers', 0, 2),
(11, 'LoL Epic 2022', 'League of Legends', 'Buttercups', 'Nerang Necros', 0, 2),
(12, 'LoL Epic 2022', 'League of Legends', 'Coomera Bombers', 'Nerang Necros', 2, 0),
(13, 'Rocket League Swipers', 'Rocket League', 'BioHazards', 'Buttercups', 2, 0),
(14, 'Rocket League Swipers', 'Rocket League', 'BioHazards', 'Coomera Bombers', 2, 0),
(15, 'Rocket League Swipers', 'Rocket League', 'BioHazards', 'Nerang Necros', 2, 0),
(16, 'Rocket League Swipers', 'Rocket League', 'Buttercups', 'Coomera Bombers', 0, 2),
(17, 'Rocket League Swipers', 'Rocket League', 'Buttercups', 'Nerang Necros', 0, 2),
(18, 'Rocket League Swipers', 'Rocket League', 'Coomera Bombers', 'Nerang Necros', 1, 1);

-- --------------------------------------------------------

--
-- Indexes for dumped tables
--

--
-- AUTO_INCREMENT for dumped tables
--

--
-- Constraints for dumped tables
--

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
