CREATE TABLE `custom_vectors` (
  `sentiment_score` decimal(5,5) NOT NULL,
  `length_page_text` int(11) NOT NULL,
  `length_title_text` int(11) NOT NULL,
  `length_url` int(11) NOT NULL,
  `num_prop_nouns` int(11) NOT NULL,
  `num_kw_login` int(11) NOT NULL,
  `num_kw_logout` int(11) NOT NULL,
  `num_kw_createaccount` int(11) NOT NULL,
  `num_kw_pass` int(11) NOT NULL,
  `num_kw_user` int(11) NOT NULL,
  `num_kw_forgot` int(11) NOT NULL,
  `num_kw_reset` int(11) NOT NULL,
  `num_kw_locked` int(11) NOT NULL,
  `num_kw_contactadmin` int(11) NOT NULL,
  `num_divs` int(11) NOT NULL,
  `num_frames` int(11) NOT NULL,
  `num_forms` int(11) NOT NULL,
  `num_inputs` int(11) NOT NULL,
  `num_images` int(11) NOT NULL,
  `num_links` int(11) NOT NULL,
  `num_colors` int(11) NOT NULL,
  `num_text_sizes` int(11) NOT NULL,
  `num_text_families` int(11) NOT NULL,
  `has_red_text` int(11) NOT NULL,
  `pixel_area_images` int(11) NOT NULL,
  `response_code` int(11) NOT NULL,
  `num_cookies` int(11) NOT NULL,
  `num_cookie_domains` int(11) NOT NULL,
  `num_secure_cookies` int(11) NOT NULL,
  `classification` enum('t','f') NOT NULL,
  `portal_code` varchar(45) NOT NULL,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`timestamp`,`portal_code`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1

